package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.luckyzyx.luckytool.IRefreshRateController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DisplayMode
import com.luckyzyx.luckytool.service.RefreshRateService
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialRadioItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsAutoStart
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsCur
import com.luckyzyx.luckytool.utils.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * 强制刷新率页（旧 ui.fragment.extension.ForceFpsFragment 的 Compose 等价物）。
 *
 * 控制器状态在 RootService 控制器而非 prefs，故整页用 custom() 手工构建
 * （不用 DSL switch/slider）。
 *
 * ## 状态模型
 * - [IRefreshRateController] 是唯一的「数据源」；`supportModes` / `refreshRateDisplay`
 *   都是**同步** root IPC，因此每次 load 只读一次落到 Compose 状态，不在组合期重复读；
 * - 持久化只属于本页：`keyFpsCur`（模式 id，同时被 AutoStartControllerService 拼进
 *   `service call SurfaceFlinger 1035 i32 <id>`）、`keyFpsAutoStart`；
 * - 进程级控制器缓存做种子，让「本来已绑定」的常见路径瞬时还原内容。
 *
 * ## 加载时机（三处触发，同一个幂等 load()）
 * 1. 进入页面：`LaunchedEffect(Unit)`；
 * 2. 回到前台：`LifecycleEventEffect(ON_RESUME)` —— 系统刷新率显示开关可能已被快捷开关改过，
 *    且 RootService 冷启动 daemon 未就绪时首次 bind 回调未必触发（与 OtherPage 双触发同理）；
 * 3. 下拉刷新：`spec.onRefresh` → [reloader]。
 *
 * ## 失败不摧毁已加载状态
 * `RefreshRateService.get()` 在 bind 失败时**不会**回调（`onDisconnected` 不 result），
 * 所以这里用 [BIND_TIMEOUT_MS] 兜底；拉取失败时保留仍存活的旧控制器（超时后才连上的 bind
 * 也能被 `getCachedController()` 捞回来），只有「拉不到且旧的已死」才真正清空。
 *
 * ## Context 要求
 * 共享层注入的是 applicationContext（ScopePageRegistry）。RootService 绑定只用一个 Context
 * 构造 Intent，libsu 内部自取 application 做 bindService，**不需要 Activity**：
 * 不要再沿 ContextWrapper 向上找 Activity，否则 findActivity() 恒为 null，
 * 每次拉取都直接返回 null，页面永远「无数据」。
 *
 * ## 布局（自上而下，对齐旧 fragment_fps.xml）
 * 1. FPS 数据卡：`supportModes` 模式单选列表（displayMode 数据）置顶，独立一张卡片；
 * 2. 加载中 / 无数据提示：数据卡没有内容时在该位置给出落点（旧 `fps_nodata_view`）；
 * 3. 设置卡：自启（`keyFpsAutoStart`）+ 显示刷新率合并为一张卡片；
 * 4. 恢复默认刷新率按钮 + 底部提示文案。
 *
 * 线分派：行呈现（`PrefGroup` / `PrefSwitchRow`）由共享层按 [LocalUiMode] 自行分派，
 * 页面只分派自己写死的 material 件 —— 文字（`MiuixText`）、重置按钮（miuix `Button`）、
 * 模式单选项（`MiuixRadioItem`，material 线仍为 `MaterialRadioItem`）。
 */
object ForceFpsPage {

    /** bind 回调可能永不触发，超时兜底避免页面卡在加载中 */
    private const val BIND_TIMEOUT_MS = 5_000L

    /** 由内容组合时注册的加载器驱动 onRefresh */
    private var reloader: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "force_fps",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { reloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "force_fps_body", bare = true) {
            // 进程级缓存做种子：进入子页不重新绑定，直接用 RefreshRateService 已缓存的存活控制器
            var controller by remember { mutableStateOf(RefreshRateService.getCachedController()) }
            // 每次 load 读一次的 root IPC 快照（不在组合期反复读）
            var modes by remember { mutableStateOf(emptyList<DisplayMode>()) }
            var displayRefreshRate by remember { mutableStateOf(false) }
            var loading by remember { mutableStateOf(true) }

            suspend fun fetchController(): IRefreshRateController? =
                withTimeoutOrNull(BIND_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        RefreshRateService.get(c) { cont.resume(it) { _, _, _ -> } }
                    }
                }

            suspend fun load() {
                loading = true
                // 拉不到就退回「仍存活的缓存控制器」；两者都没有才清空，避免把已加载的页面打回无数据
                val live = fetchController() ?: RefreshRateService.getCachedController()
                if (live != null) {
                    val snapshot = withContext(Dispatchers.IO) {
                        @Suppress("UNCHECKED_CAST")
                        val list =
                            (live.supportModes ?: ArrayList<DisplayMode>()) as ArrayList<DisplayMode>
                        list to live.refreshRateDisplay
                    }
                    modes = snapshot.first
                    displayRefreshRate = snapshot.second
                } else {
                    modes = emptyList()
                }
                controller = live
                loading = false
            }

            // onRefresh 在组合之外调用加载器：用 SideEffect 注册，保证注册的是本次成功组合的实例
            SideEffect { reloader = ::load }
            DisposableEffect(Unit) {
                onDispose { reloader = null }
            }
            LaunchedEffect(Unit) { load() }
            // LifecycleEventEffect 的回调不是 suspend：经 rememberCoroutineScope 起协程
            val scope = rememberCoroutineScope()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { load() } }

            val isUnsupport = modes.isEmpty()
            val fpsCur = state.getInt(keyFpsCur, -1)
            val fpsAutostart = state.getBoolean(keyFpsAutoStart, false)

            // 行呈现由共享层分派；这里只分派页面写死的 material 件
            val miuix = LocalUiMode.current == UiMode.Miuix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // FPS 数据卡：displayMode 模式列表置顶，独立一张卡片
                // （旧 ListView CHOICE_MODE_SINGLE；选中/设置均以 mode.id 为准，
                // 与 RefreshRateService.setRefreshRateMode(modeId) 及 keyFpsCur 的持久化语义一致）
                PrefGroup {
                    if (!isUnsupport) {
                        modes.forEach { mode ->
                            item {
                                val title =
                                    "${mode.id}   ${mode.width} x ${mode.height}   ${mode.refreshRate}"
                                val onSelect: () -> Unit = {
                                    // 先落系统再落 prefs：IPC 抛错时不留「已持久化但未生效」的脏状态
                                    controller?.setRefreshRateMode(mode.id)
                                    state.set(keyFpsCur, mode.id)
                                }
                                if (miuix) {
                                    // Miuix 线等价件（库内 RadioButtonPreference）
                                    MiuixRadioItem(
                                        title = title,
                                        selected = mode.id == fpsCur,
                                        onClick = onSelect,
                                    )
                                } else {
                                    MaterialRadioItem(
                                        title = title,
                                        selected = mode.id == fpsCur,
                                        onClick = onSelect,
                                    )
                                }
                            }
                        }
                    }
                }
                // 数据卡没有内容时（loading / 控制器未连上 / 无模式）给出落点，位置与旧 fps_nodata_view 一致
                val hint = when {
                    !isUnsupport -> null
                    controller == null && loading -> R.string.loading
                    else -> R.string.fps_no_data
                }
                if (hint != null) {
                    val text = c.getString(hint)
                    if (miuix) {
                        MiuixText(
                            text = text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // 自启动 + 显示刷新率：设置项合并为一张卡片，位于 FPS 数据卡之下
                PrefGroup {
                    item {
                        PrefSwitchRow(
                            title = c.getString(R.string.fps_autostart),
                            checked = fpsAutostart,
                            enabled = controller != null && !isUnsupport && fpsCur != -1,
                            onCheckedChange = { v -> state.set(keyFpsAutoStart, v) },
                        )
                    }
                    item {
                        PrefSwitchRow(
                            title = c.getString(R.string.display_refresh_rate),
                            checked = displayRefreshRate,
                            enabled = controller != null,
                            onCheckedChange = { v ->
                                controller?.refreshRateDisplay = v
                                displayRefreshRate = v
                            },
                        )
                    }
                }
                // fpsRecover（旧 resetRefreshRate：持久化 -1 + 重置模式；开关可用性随 fpsCur==-1 自动失效）
                if (miuix) {
                    MiuixButton(
                        onClick = {
                            controller?.resetRefreshRateMode()
                            state.set(keyFpsCur, -1)
                        },
                        enabled = controller != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { MiuixText(text = c.getString(R.string.restore_default_refresh_rate)) }
                } else {
                    Button(
                        onClick = {
                            controller?.resetRefreshRateMode()
                            state.set(keyFpsCur, -1)
                        },
                        enabled = controller != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(c.getString(R.string.restore_default_refresh_rate)) }
                }
                if (miuix) {
                    MiuixText(
                        text = c.getString(R.string.fps_tips),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        c.getString(R.string.fps_tips),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
