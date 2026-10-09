package com.luckyzyx.luckytool.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 进程级 App 图标缓存（列表行专用）。
 *
 * 取图标是 `PackageManager.getApplicationIcon` 的 binder IPC，光栅化还要 `Drawable.draw` 到新位图
 * （自适应图标 intrinsic 为 -1，只能按目标尺寸重绘），两者都可能耗时数毫秒，因此：
 * - 两步都只在后台线程执行（旧实现在每行组合期同步 IPC + 主线程光栅化，直接堵住每一帧）；
 * - 结果按包名缓存并订阅（[state]），行滑出再滑回、切页后重进都不再重取。
 *
 * 缓存命中后，滑动期间的行绘制不再有任何 IPC / 光栅化开销。这正是「滑动越快越卡」的成因：
 * 单位时间新进视口的行数与滑动速度成正比，每行取图的开销被成倍放大。
 *
 * 线程约定：对外接口只在主线程（组合）调用；加载落在内部 IO 作用域，不随行滑出视口而取消。
 */
object AppIconCache {

    /** 图标目标边长：48dp（对齐 AppPicker 的自适应图标回退尺寸），避免按 intrinsic 生成超大位图 */
    private const val TARGET_DP = 48f

    /** 缓存上限（功能树 42 个应用页 + 余量） */
    private const val MAX_ENTRIES = 96

    private class Entry {
        /** 位图为 null = 已确认该包没有图标（结果同样缓存，避免重复 IPC） */
        val state: MutableState<ImageBitmap?> = mutableStateOf(null)
        var started = false
    }

    private val cache = object : LruCache<String, Entry>(MAX_ENTRIES) {}

    /** 加载作用域：与组合生命周期解耦，行滑出视口不会取消尚未完成的加载 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 并发闸门：首次进页面会一次性请求整棵树的图标，串行度过低会让加载抢占 UI 线程的 CPU */
    private val loadGate = Semaphore(3)

    /** 密度参与缓存键：同一包名在不同密度下需要不同像素尺寸的位图 */
    private fun cacheKey(context: Context, packName: String): String =
        "${context.resources.displayMetrics.density}@$packName"

    /**
     * 订阅指定包名的 App 图标：首次调用触发一次后台加载，加载完成后写入状态驱动组合刷新
     * （重复调用同一包名复用同一状态，不会重复加载）。
     *
     * @return 图标位图状态；值为 null 表示该包没有图标，或加载尚未完成
     */
    fun state(context: Context, packName: String): State<ImageBitmap?> {
        val key = cacheKey(context, packName)
        val entry = cache.get(key) ?: Entry().also { cache.put(key, it) }
        if (!entry.started) {
            entry.started = true
            val appContext = context.applicationContext
            scope.launch {
                val bitmap = loadGate.withPermit { loadIcon(appContext, packName) }
                // 状态写回主线程，保证订阅方按标准快照流程重组
                withContext(Dispatchers.Main.immediate) { entry.state.value = bitmap }
            }
        }
        return entry.state
    }

    /** 后台加载：IPC + 按 [TARGET_DP] 目标尺寸光栅化 */
    private fun loadIcon(context: Context, packName: String): ImageBitmap? = try {
        AppUtils(context).getAppIcon(packName)?.let { drawable ->
            val size = (TARGET_DP * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            bitmap.asImageBitmap()
        }
    } catch (t: Throwable) {
        // 单个包取图异常不能影响整棵列表，回退为无图标占位
        LogUtils.e("AppIconCache", packName, t.toString())
        null
    }
}
