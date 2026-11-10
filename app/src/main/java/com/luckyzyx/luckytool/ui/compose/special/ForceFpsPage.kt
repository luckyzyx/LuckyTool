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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.IRefreshRateController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DisplayMode
import com.luckyzyx.luckytool.service.RefreshRateService
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.AppRadioItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilledButton
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

/**
 * 强制刷新率页：列出 SurfaceFlinger 支持的模式，设置当前刷新率、显示刷新率开关与开机自启。
 *
 * 控制器状态在 RootService 控制器而非 prefs，故整页用 custom() 手工构建
 * （不用 DSL switch/slider）。
 *
 * ## 状态模型
 * - [IRefreshRateController] 是唯一的「数据源」；`supportModes` / `refreshRateDisplay`
 *   都是**同步** root IPC，因此每次 load 只读一次落到 Compose 状态，不在组合期重复读；
 * - 持久化只属于本页：`keyFpsCur`（模式位置，即列表 index，同时被 AutoStartControllerService
 *   拼进 `service call SurfaceFlinger 1035 i32 <index>`）、
 *   `keyFpsAutoStart`；
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
 * ## 布局（自上而下）
 * 1. FPS 数据卡：`supportModes` 模式单选列表（displayMode 数据）置顶，独立一张卡片；
 * 2. 加载中 / 无数据提示：数据卡没有内容时在该位置给出落点；
 * 3. 设置卡：自启（`keyFpsAutoStart`）+ 显示刷新率合并为一张卡片；
 * 4. 恢复默认刷新率按钮 + 底部提示文案。
 *
 * 水平内缩：body 走 `custom(bare = true)`（整页无卡片容器）。COUI 线的分组卡片自带 16dp 水平内缩
 * （`CouiScopeCard`），页级 padding 只补给 Material 线，否则卡片比按钮、提示文字左右各多缩 16dp；
 * 页面自绘的文字与按钮不经卡片，自己补 16dp 才能与卡片对齐。
 * 行呈现（`PrefGroup` / `PrefSwitchRow`）与页面自绘的文字、按钮按外观分派
 * （COUI 线用 COUI 语义文字与胶囊按钮，Material 线用 material3 件）；
 * 模式单选项走 `AppRadioItem`，它本身已双线。
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
            val isCoui = LocalUiMode.current == UiMode.Coui
            // 分组卡片的内缩按外观分派：COUI 线卡片自带 16dp，Material 线列表不自带
            val cardInset = if (isCoui) Modifier else Modifier.padding(horizontal = 16.dp)
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

            // 页级只留垂直外边距：水平内缩按外观分派，见 cardInset
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // FPS 数据卡：displayMode 模式列表置顶，独立一张卡片
                // 单选以列表位置 index 为准：点击与选中态比的都是 index，mode.id 仅用于展示文本，
                // 选中/设置/持久化都用 index，保证恰一项选中且与 keyFpsCur 自启语义一致
                PrefGroup(modifier = cardInset) {
                    if (!isUnsupport) {
                        modes.forEachIndexed { index, mode ->
                            item {
                                val title =
                                    "${mode.id}   ${mode.width} x ${mode.height}   ${mode.refreshRate}"
                                val onSelect: () -> Unit = {
                                    // 先落系统再落 prefs：IPC 抛错时不留「已持久化但未生效」的脏状态
                                    controller?.setRefreshRateMode(index)
                                    state.set(keyFpsCur, index)
                                }
                                AppRadioItem(
                                    title = title,
                                    selected = index == fpsCur,
                                    onClick = onSelect,
                                )
                            }
                        }
                    }
                }
                // 数据卡没有内容时（loading / 控制器未连上 / 无模式）在该位置给出落点
                val hint = when {
                    !isUnsupport -> null
                    controller == null && loading -> R.string.loading
                    else -> R.string.fps_no_data
                }
                if (hint != null) {
                    val text = stringResource(hint)
                    // 空态提示按外观分派：COUI 线取说明色正文，Material 线保持 M3 Text
                    if (isCoui) {
                        CouiText(
                            text = text,
                            kind = CouiTextKind.Search,
                            color = CouiPageDefaults.themeColors(CouiTheme.isDark).summary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // 自启动 + 显示刷新率：设置项合并为一张卡片，位于 FPS 数据卡之下
                PrefGroup(modifier = cardInset) {
                    item {
                        PrefSwitchRow(
                            title = stringResource(R.string.fps_autostart),
                            checked = fpsAutostart,
                            enabled = controller != null && !isUnsupport && fpsCur != -1,
                            onCheckedChange = { v -> state.set(keyFpsAutoStart, v) },
                        )
                    }
                    item {
                        PrefSwitchRow(
                            title = stringResource(R.string.display_refresh_rate),
                            checked = displayRefreshRate,
                            enabled = controller != null,
                            onCheckedChange = { v ->
                                controller?.refreshRateDisplay = v
                                displayRefreshRate = v
                            },
                        )
                    }
                }
                // fpsRecover：持久化 -1 + 重置模式；开关可用性随 fpsCur==-1 自动失效
                // COUI 线是 accent 填充胶囊，Material 线保持 M3 Button
                if (isCoui) {
                    CouiFilledButton(
                        text = stringResource(R.string.restore_default_refresh_rate),
                        onClick = {
                            controller?.resetRefreshRateMode()
                            state.set(keyFpsCur, -1)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = controller != null,
                    )
                } else {
                    Button(
                        onClick = {
                            controller?.resetRefreshRateMode()
                            state.set(keyFpsCur, -1)
                        },
                        enabled = controller != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) { Text(stringResource(R.string.restore_default_refresh_rate)) }
                }
                if (isCoui) {
                    CouiText(
                        text = stringResource(R.string.fps_tips),
                        kind = CouiTextKind.Search,
                        color = CouiPageDefaults.themeColors(CouiTheme.isDark).summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 10.dp, end = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        stringResource(R.string.fps_tips),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 10.dp, end = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
