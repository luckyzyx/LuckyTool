package com.luckyzyx.luckytool.utils

import android.content.Context
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

/**
 * 功能列表的进程级图标缓存。IPC、尺寸归一化与阴影都在后台完成；
 * 行滑出后继续加载，回到页面直接复用位图，不在滚动时重新绘制。
 *
 * 对外接口只在主线程调用，位图尺寸来自 Compose 密度，避免自定义界面缩放导致模糊。
 */
object AppIconCache {
    private class Entry {
        val state: MutableState<ImageBitmap?> = mutableStateOf(null)
    }

    private val cache = LruCache<String, Entry>(96)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadGate = Semaphore(3)

    fun state(context: Context, packName: String, size: Int): State<ImageBitmap?> =
        load(context, "app:$packName", size) { appContext ->
            val drawable = try {
                appContext.packageManager.getApplicationInfo(packName, 0)
                    .loadUnbadgedIcon(appContext.packageManager)
            } catch (e: Exception) {
                LogUtils.e("AppIconCache", packName, e.toString())
                appContext.packageManager.defaultActivityIcon
            }
            FunctionIcons.render(appContext, drawable, size).asImageBitmap()
        }

    private fun load(
        context: Context,
        source: String,
        size: Int,
        render: (Context) -> ImageBitmap,
    ): State<ImageBitmap?> {
        val key = "${context.resources.configuration.densityDpi}:$size@$source"
        cache.get(key)?.let { return it.state }
        val entry = Entry()
        cache.put(key, entry)
        val appContext = context.applicationContext
        scope.launch {
            val bitmap = loadGate.withPermit {
                try {
                    render(appContext)
                } catch (e: Exception) {
                    LogUtils.e("AppIconCache", source, e.toString())
                    null
                }
            }
            withContext(Dispatchers.Main.immediate) { entry.state.value = bitmap }
        }
        return entry.state
    }
}
