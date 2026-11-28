package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ExtraDimUtils
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.checkResolveActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 极暗模式页：直接读写框架监听的那三个 Settings.Secure 键，绕开系统设置页。
 *
 * ## 为什么不用系统设置页
 * 部分 ROM 的极暗设置页里，每个控制器都以「颜色变换未加速」判为不支持而被过滤，进页是空白；
 * 但框架侧的显示服务仍无条件监听这三个键，键一变就激活滤镜，所以 root 写键即可控制该功能。
 *
 * ## 状态模型
 * 真相在 [android.provider.Settings.Secure]（[ExtraDimUtils] 的三个键），页面状态只做镜像：
 * 每次读/写后都回读真实键值，不做 prefs 落盘，也不长期依赖乐观值。
 * 键未设置时强度读作 null，此时滑块显示最低档并在说明里写明「跟随系统最弱档」，用户拖动前不写键。
 *
 * ## 门禁（关键，别删）
 * 颜色变换未加速（[ExtraDimUtils.isSupported] 为 false）时框架套用的是全零颜色矩阵，
 * 把激活键写成 1 会直接黑屏（只能再写 0 恢复）。因此该情况下两个开关与滑条一律禁用：
 * 页面禁用 + [ExtraDimUtils.writeActivated] 内部再挡一次，UI 无法绕过。
 *
 * ## 加载时机
 * 1. 进入页面与回到前台：[ScopePageSpec.onLoad]（系统设置或磁贴可能改过同样的键）；
 * 2. 下拉刷新：[ScopePageSpec.onRefresh]，复用 [onLoad] 存下的 application context。
 *
 * ## 写法
 * 整页都是 DSL 行（分组卡片、开关、滑条、说明、跳转），不再手写 Composable 或卡片：
 * 卡片与行间分隔线由分组给，行的值与可用性用状态驱动的重载从页面状态取。
 */
object ExtraDimPage {

    /**
     * 页面状态的唯一持有处：值来自 [ExtraDimUtils] 读的 Settings.Secure 键，不是 prefs，
     * 所以放对象里而不是 DSL 的键值槽。行的 lambda 值参数在渲染时求值，改这些状态即重画该行。
     */
    private val supported = mutableStateOf(false)
    private val activated = mutableStateOf<Boolean?>(null)
    private val level = mutableStateOf<Int?>(null)
    private val persist = mutableStateOf<Boolean?>(null)
    private val loaded = mutableStateOf(false)

    /** 系统页可解析才给入口：极暗页在部分 ROM 上是空白页，这里只作为兜底 */
    private var systemPageAvailable = false

    /** 写键要跑 root shell，事件回调里不能 suspend：写与回读都在这个作用域里串行完成 */
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** onRefresh 的 lambda 拿不到 Context：首次加载时存下 application context 供下拉刷新复用 */
    private var appContext: Context? = null

    private suspend fun load(c: Context) {
        val supportedNow = withContext(Dispatchers.IO) { ExtraDimUtils.isSupported(c) }
        val activatedNow = withContext(Dispatchers.IO) { ExtraDimUtils.readActivated() }
        val levelNow = withContext(Dispatchers.IO) { ExtraDimUtils.readLevel() }
        val persistNow = withContext(Dispatchers.IO) { ExtraDimUtils.readPersist() }
        supported.value = supportedNow
        activated.value = activatedNow
        level.value = levelNow
        persist.value = persistNow
        systemPageAvailable = c.checkResolveActivity(
            "com.android.settings",
            "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity",
        )
        // 放在最后：它置位时上面的镜像已就绪，避免「可用」先于「已加载」被看到
        loaded.value = true
    }

    /** 写入后统一回读：键可能被系统策略拦下，界面只反映读回来的真实值 */
    private suspend fun writeAndReload(c: Context, write: () -> Boolean) {
        withContext(Dispatchers.IO) { write() }
        load(c)
    }

    val spec = ScopePageSpec(
        pageKey = "extra_dim",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onLoad = { c ->
            appContext = c.applicationContext
            load(c)
        },
        onRefresh = { appContext?.let { load(it) } },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        group {
            switch(
                title = c.getString(R.string.extra_dim_enable),
                checked = { activated.value == true },
                summary = c.getString(R.string.extra_dim_enable_summary),
                enabled = { supported.value },
            ) { value ->
                // 先回显，回读到的真值随后覆盖；写入被挡时界面自动退回真实值
                activated.value = value
                writer.launch { writeAndReload(c) { ExtraDimUtils.writeActivated(c, value) } }
            }
            switch(
                title = c.getString(R.string.extra_dim_persist),
                checked = { persist.value == true },
                summary = c.getString(R.string.extra_dim_persist_summary),
                enabled = { supported.value },
            ) { value ->
                persist.value = value
                writer.launch { writeAndReload(c) { ExtraDimUtils.writePersist(value) } }
            }
        }

        group {
            // 强度：拖动结束才写键（每帧写 shell 会卡手势）；键缺失时显示最低档且不写
            slider(
                title = c.getString(R.string.extra_dim_level),
                value = { level.value ?: ExtraDimUtils.MIN_LEVEL },
                valueRange = ExtraDimUtils.MIN_LEVEL..ExtraDimUtils.MAX_LEVEL,
                // 键还没写过时滑条停在最弱档，说明为什么拖动才会写
                summary = if (level.value == null) {
                    c.getString(R.string.extra_dim_level_follow_min)
                } else {
                    null
                },
                enabled = { supported.value },
            ) { stepped ->
                level.value = stepped
                writer.launch { writeAndReload(c) { ExtraDimUtils.writeLevel(stepped) } }
            }
        }

        // 提示挂在卡片之外：字号与配色由 PrefNote 统一（两条线都是说明字重 + 中性前景色）
        note(
            text = c.getString(R.string.extra_dim_unsupported),
            visible = loaded.value && !supported.value,
        )

        click(
            title = c.getString(R.string.extra_dim_open_system_page),
            visible = systemPageAvailable,
        ) { IntentUtils(c).jumpVeryDarkMode() }
    }
}
