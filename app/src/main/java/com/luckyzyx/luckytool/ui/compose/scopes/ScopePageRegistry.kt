package com.luckyzyx.luckytool.ui.compose.scopes

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.PrefIndexItem
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusAlarmClockPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBatteryPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBeaconLinkPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBrowserPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCalendarPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCameraPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCloudServicePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusDirectUIPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusEngineerModePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusEyeProtectPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusFileManagerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGalleryPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGamesPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGesturePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusHealthPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusKeyboardPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusLinkerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMMSPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMarketPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMcsPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMetisPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMyDevicesPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusNfcPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusOSharePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusOTAPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPermissionControllerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPhoneManagerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPictorialPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusScreenshotPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSearchBoxPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSecuritypPermissionPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSettingsPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSmartSidebarPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSoundRecorderPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSpeechAssistPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusTeleServicePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusThemeStorePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusWeatherPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusWirelessSettingsPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.ADMPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.AlphaBackupProPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.ClawPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.GpsJoyStickPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.KsWebPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.AndroidRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.AodRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.ApplicationRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.CorePatchPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.DialogRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.FingerPrintRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LauncherBackupPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LauncherRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LockScreenRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.MiscellaneousPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.SoundRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarBatteryPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarControlCenterPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarIconOrderPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarIconPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarLayoutPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNetWorkSpeedPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNotifyPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNotifyRemovalPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarTilesPage
import com.luckyzyx.luckytool.ui.compose.special.AppPermissionPage
import com.luckyzyx.luckytool.ui.compose.special.BatteryInfoPage
import com.luckyzyx.luckytool.ui.compose.special.DarkModePage
import com.luckyzyx.luckytool.ui.compose.special.DonatePage
import com.luckyzyx.luckytool.ui.compose.special.ExtraDimPage
import com.luckyzyx.luckytool.ui.compose.special.ExtractOTAPage
import com.luckyzyx.luckytool.ui.compose.special.ForceFpsPage
import com.luckyzyx.luckytool.ui.compose.special.HideAppIntentPage
import com.luckyzyx.luckytool.ui.compose.special.MemcConfigPage
import com.luckyzyx.luckytool.ui.compose.special.MultiAppPage
import com.luckyzyx.luckytool.ui.compose.special.QuickEntryPage
import com.luckyzyx.luckytool.ui.compose.special.ZoomWindowPage
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.checkPackName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 页面顶栏动作：页面自己声明的菜单项，由 [ScopePageSpec.topActions] 声明、页面宿主渲染。
 *
 * 这些动作是对整页数据的一次性操作（打开系统设置、按 Intent 类型全选、清空、导入…），
 * 放在顶栏而不平铺成页面里的偏好卡片/选项行：混在正文里会与"页面选项"的视觉层级分不清，
 * 让人看不出哪个是设置项、哪个是动作。
 *
 * @param key           动作标识，页面在 [ScopePageSpec.actionHandler] 里据此分派
 * @param labelRes      动作标题（溢出菜单文本 / 图标按钮的 contentDescription）
 * @param icon          图标资源；0 = 无图标，只能进溢出菜单
 * @param showAsAction  true 且有图标时直接显示为顶栏图标按钮（空间足够就显示，不够则收进溢出菜单）
 */
class ScopePageAction(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val icon: Int = 0,
    val showAsAction: Boolean = false,
)

/**
 * 一个 Compose 作用域页的声明：页面注册键、prefs 文件名、宿主包名与显示包名、作用域包名列表、
 * 「重启作用域」菜单开关、页面级可见性、下拉刷新回调，以及页面内容 DSL（或自带的两线呈现槽位）。
 *
 * @param pageKey        页面注册键（如 statusbar_clock），[ScopePageRegistry] 以它为查表键
 * @param prefsName      SharedPreferences 文件名（ModulePrefs 等）
 * @param packName       宿主包名（sendPrefsValue 通知目标）
 * @param displayPackName 功能树标题/图标来源包名（默认 = packName；通知目标与显示 App 不同时单独指定）
 * @param scopes         Xposed 作用域包名列表（重启作用域对话框用）
 * @param restartEnabled 是否显示"重启作用域"菜单
 * @param isVisible      页面级可见性：false 时功能树、搜索与子页入口均不出现此页
 * @param onRefresh      下拉刷新回调（null = 无下拉刷新；如 OTA 提取、捐赠数据页）
 * @param content        页面内容 DSL（每次重组重跑，条件可见性写 Kotlin if）
 * @param contentMiuix   页面自带的 Miuix 呈现（null = 走共享渲染层 [ScopeScreen]）
 * @param contentMaterial 页面自带的 material 呈现（null = 走共享渲染层 [ScopeScreen]）
 *
 * 页面需要自带主题布局时，提供一个或两个**槽位**（[contentMiuix] / [contentMaterial]）：
 * 槽位非空时由页面自有布局渲染整页内容（见 [ScopePageContent]），共享层只负责状态、动作与搜索跳转。
 * 行清单仍只写一份 —— 放在 [content] 里；槽位布局遍历已构建好的 `builder.entries` 渲染
 * （共享层已跑过 [content]，**不要**在槽位里再 `apply` 一次，否则条目翻倍）。
 */
class ScopePageSpec(
    val pageKey: String,
    val prefsName: String,
    val packName: String,
    /**
     * 功能树标题/图标来源包名（默认 = [packName]）。
     * 仅当「通知目标」与「显示 App」不同时单独指定，例如手势体感页通知目标是 systemui、
     * 但功能树应显示 com.oplus.gesture 的名称与图标。
     */
    val displayPackName: String? = null,
    val scopes: Array<String>,
    val restartEnabled: Boolean,
    /**
     * 页面级可见性：
     * false 时功能树、搜索与子页入口均不出现此页；默认恒可见。
     */
    val isVisible: Context.() -> Boolean = { true },
    /**
     * 自带整页渲染的**特殊页**（[fullContent] 或主题槽位）：入口可见性只由 [isVisible] 与作用域决定，
     * 不看 DSL 行索引 —— 这类页面不声明共享行、索引恒空，空索引不能作为「该页不可见」的依据。
     * 必须由页面自己声明（不要用 [fullContent] 非空之类的结构特征反推）。
     */
    val selfRendered: Boolean = false,
    val onRefresh: (suspend () -> Unit)? = null,
    val fullContent: (@Composable LazyItemScope.(PrefScopeBuilder) -> Unit)? = null,
    /**
     * 顶栏动作清单：有图标且 [ScopePageAction.showAsAction] 的直接显示为
     * 图标按钮，其余收进顶栏溢出菜单。动作处理函数由页面在组合期登记到 [actionHandler]。
     */
    val topActions: List<ScopePageAction> = emptyList(),
    val contentMiuix: (@Composable ScopeContentScope.(PrefScopeBuilder) -> Unit)? = null,
    val contentMaterial: (@Composable ScopeContentScope.(PrefScopeBuilder) -> Unit)? = null,
    /**
     * 共享行声明。72 个既有页面用尾随 lambda 传入，因此**必须保持最后一个参数**；
     * 无槽位时由 [ScopeScreen] 渲染，有槽位时由页面布局渲染同一批条目。
     */
    val content: PrefScopeBuilder.() -> Unit,
) {
    /**
     * 顶栏动作处理函数：页面内容在组合期登记（范式同 `refresh`，事件期读取），
     * 顶栏点击时以 [ScopePageAction.key] 调用。用普通 var 而非快照状态，避免只读回调触发重组。
     */
    var actionHandler: ((String) -> Unit)? = null
}

/**
 * 页面级内容作用域（中性）：页面自带布局（[ScopePageSpec.contentMiuix] / [ScopePageSpec.contentMaterial]）
 * 通过它拿到只读能力与动作入口，**不含任何 material/miuix 主题类型** —— 页面代码只依赖本接口与
 * [PrefScopeBuilder] 公共 API，因此同一套页面逻辑可以在两条主题线上复用。
 */
interface ScopeContentScope {
    /** 宿主应用 Context（与共享层注入 [PrefScopeBuilder] 的同一个 applicationContext） */
    val context: Context

    /** 本页偏好状态（与共享层同一实例，条件可见性判断用它） */
    val state: PrefState

    /** 页面自有列表使用的滚动状态：宿主已用它执行搜索跳转 */
    val listState: LazyListState

    /** 搜索跳转命中的槽位（无命中为 null）：页面卡片据此高亮，语义与共享层一致 */
    val highlightedSlot: Int?

    /** 搜索跳转目标（只读；跳转与高亮已由宿主完成） */
    val scrollTarget: ScrollTarget?

    /** 写值：落盘 + 通知宿主（等价共享层行的写值路径） */
    fun sendValue(key: String, value: Any)

    /** 页面入口跳转（page DSL 项的等价动作；无宿主回调时为空操作） */
    fun navigate(target: String, title: String? = null)

    /** 下拉刷新动作（`spec.onRefresh` 非空时有效） */
    suspend fun refresh()
}

/** [ScopeContentScope] 的唯一委托体：两条主题线的实现类都委托到它，行为单点、不复制共享层逻辑。 */
private class ScopeContentScopeImpl(
    override val context: Context,
    override val state: PrefState,
    override val listState: LazyListState,
    private val highlighted: () -> Int?,
    private val target: () -> ScrollTarget?,
    private val onSendValue: (key: String, value: Any) -> Unit,
    private val onNavigate: ((target: String, title: String?) -> Unit)?,
    private val onRefresh: (suspend () -> Unit)?,
) : ScopeContentScope {
    override val highlightedSlot: Int? get() = highlighted()
    override val scrollTarget: ScrollTarget? get() = target()
    override fun sendValue(key: String, value: Any) = onSendValue(key, value)
    override fun navigate(target: String, title: String?) {
        onNavigate?.invoke(target, title)
    }

    override suspend fun refresh() {
        onRefresh?.invoke()
    }
}

/** material 线页面作用域实现（对外签名与 [MiuixScopeContentScope] 逐字一致，行为委托同一实现体）。 */
internal class MaterialScopeContentScope(delegate: ScopeContentScope) :
    ScopeContentScope by delegate

/** miuix 线页面作用域实现（对外签名与 [MaterialScopeContentScope] 逐字一致，行为委托同一实现体）。 */
internal class MiuixScopeContentScope(delegate: ScopeContentScope) : ScopeContentScope by delegate

/**
 * 页面内容的中性宿主与**唯一分派点**（[ScopeScreen] 的调用处）。
 *
 * - 页面为当前主题线提供了槽位（Miuix → [ScopePageSpec.contentMiuix]，Material → [ScopePageSpec.contentMaterial]）
 *   时，整页内容由页面自有布局渲染；共享层只做与 [ScopeScreen] 同语义的三件事：订阅 revision、构建
 *   [PrefScopeBuilder]（行声明与条件可见性只写一份）、执行搜索跳转/高亮，并沿用下拉刷新包装。
 *   页面布局若要让内容在模糊顶栏下滚动，把 Miuix 线的 `LocalScopeTopInset` 用作自己
 *   LazyColumn 的 `contentPadding.top`，并把 [ScopeContentScope.listState] 交给该 LazyColumn。
 * - 未提供槽位时（72 个既有页面），逐字回落到今天的 [ScopeScreen] 路径。
 *
 * [modifier] 由调用方的页面骨架给出：Miuix 线保留 start/end/bottom 外置 padding（top 归 0，顶栏高度经
 * `LocalScopeTopInset` 交给列表充当 `contentPadding.top`），material 线是骨架的整块 innerPadding。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScopePageContent(
    spec: ScopePageSpec,
    state: PrefState,
    modifier: Modifier,
    sendValue: (key: String, value: Any) -> Unit,
    scrollTarget: ScrollTarget? = null,
    onNavigate: ((target: String, title: String?) -> Unit)? = null,
) {
    val contentSlot = when (LocalUiMode.current) {
        UiMode.Miuix -> spec.contentMiuix
        UiMode.Material -> spec.contentMaterial
    }
    if (contentSlot == null) {
        // 未提供槽位：与今天逐字一致的共享渲染层路径
        ScopeScreen(
            state = state,
            modifier = modifier,
            sendValue = sendValue,
            scrollTarget = scrollTarget,
            onNavigate = onNavigate,
            onRefresh = spec.onRefresh,
            fullContent = spec.fullContent,
            content = spec.content,
        )
        return
    }

    // ---- 槽位路径：页面自有布局（共享层负责状态/动作，不绘制任何行）----
    // 订阅 revision：任何偏好写入都会重组本页 → 构建 lambda 重跑 → 条件可见性自动重求值
    // 必须读取 .value：仅调用 collectAsStateWithLifecycle() 并丢弃返回值，快照订阅不会建立
    state.revision.collectAsStateWithLifecycle().value

    val builder = remember(state) { PrefScopeBuilder(state) }
    builder.sendValue = sendValue
    builder.navigate = onNavigate
    builder.context = LocalContext.current.applicationContext
    builder.beginBuild()
    spec.content(builder)
    builder.computeSegments()

    val listState = rememberLazyListState()
    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        val targetSlot = if (target.key.isNotBlank()) {
            // 按索引 key 解析槽位（条目在构建后位置可能因条件可见性变化而偏移）
            builder.entries.firstOrNull { it.indexKey == target.key }?.slot ?: return@LaunchedEffect
        } else {
            target.position.takeIf { it >= 0 } ?: return@LaunchedEffect
        }
        listState.animateScrollToItem(targetSlot)
        builder.highlightSlot.value = targetSlot
        delay(2500.milliseconds)
        if (builder.highlightSlot.value == targetSlot) builder.highlightSlot.value = null
    }

    val scopeImpl = ScopeContentScopeImpl(
        context = LocalContext.current.applicationContext,
        state = state,
        listState = listState,
        highlighted = { builder.highlightSlot.value },
        target = { scrollTarget },
        onSendValue = sendValue,
        onNavigate = onNavigate,
        onRefresh = spec.onRefresh,
    )
    val scope: ScopeContentScope = when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixScopeContentScope(scopeImpl)
        UiMode.Material -> MaterialScopeContentScope(scopeImpl)
    }

    val page: @Composable (Modifier) -> Unit = { pageModifier ->
        Box(pageModifier) { contentSlot(scope, builder) }
    }
    val onRefresh = spec.onRefresh
    if (onRefresh != null) {
        var refreshing by remember { mutableStateOf(false) }
        val refreshScope = rememberCoroutineScope()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshScope.launch {
                    refreshing = true
                    try {
                        onRefresh()
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = modifier,
        ) {
            page(Modifier.fillMaxSize())
        }
    } else {
        page(modifier)
    }
}

/** Compose 页注册表：ComposeScopeFragment 按 page_key 查表渲染 */
object ScopePageRegistry {
    private val pages = LinkedHashMap<String, ScopePageSpec>()

    fun register(spec: ScopePageSpec) {
        pages[spec.pageKey] = spec
    }

    operator fun get(key: String): ScopePageSpec? = pages[key]

    /** 全部已注册页（搜索索引用） */
    fun all(): List<ScopePageSpec> = pages.values.toList()

    /**
     * headless 构建一页的搜索索引（条件可见性已由 DSL 的 Kotlin if 应用）。
     * 构建是纯记录（emit 不渲染），可在任意线程运行；功能树、全屏搜索与 DSL page() 子页入口可达性判断共用。
     * 注意：空索引**不等于**该页不可见——见 [hasVisibleContent]。
     */
    fun buildIndex(context: Context, spec: ScopePageSpec): List<PrefIndexItem> = try {
        val state = PrefState.of(context.applicationContext, spec.prefsName)
        val builder = PrefScopeBuilder(state)
        builder.context = context.applicationContext
        builder.beginBuild()
        spec.content(builder)
        builder.snapshotIndex()
    } catch (t: Throwable) {
        // 单页构建失败只丢弃该页，不得拖垮整棵功能树/搜索索引
        LogUtils.e("buildIndex", spec.pageKey, t.toString(), true)
        emptyList()
    }

    /**
     * 单 App 作用域存在性检查：
     * 恰有一个作用域且不是 system、且该包未安装 → 整页不可见（功能树/搜索/子页入口共用）。
     */
    fun isScopeAppPresent(context: Context, spec: ScopePageSpec): Boolean {
        val scopes = spec.scopes
        return !(scopes.size == 1 && scopes.first() != "system") || context.checkPackName(scopes.first())
    }

    /** 可达性探测栈（线程内防环）：page() 目标链理论上是树，防御性保护未来出现回链 */
    private val reachabilityProbeStack: ThreadLocal<ArrayDeque<String>> =
        ThreadLocal.withInitial { ArrayDeque() }

    /**
     * 一页是否有可见内容（判据：可见子条目数 <= 0 时隐藏该页入口）。
     *
     * [ScopePageSpec.selfRendered] 的特殊页（暗色模式列表 / 意图 / 自定义浮窗 / 应用分身 / MEMC 等）
     * 自带整页渲染、行清单画在自己的布局里，DSL content 恒为空、索引恒为空；它们的可见性**只看
     * [ScopePageSpec.isVisible] 与作用域**，这里不再碰索引，否则父页入口整行会被误隐藏。
     * 普通页仍按空索引判不可见（条件可见性把行全关掉时不该留下一个空页入口）。
     */
    fun hasVisibleContent(context: Context, spec: ScopePageSpec): Boolean =
        spec.selfRendered || buildIndex(context, spec).isNotEmpty()

    /**
     * 一页是否可进入（下面三级判断集中在此，供 DSL page() 子页入口使用）：
     * spec.isVisible（作用域判断通常也写在这里面）&& 单 App 作用域已安装
     * && 存在可见条目（可见子条目数 <= 0 时隐藏）。
     * [ScopePageSpec.selfRendered] 的特殊页到第三步直接为真——它们只靠 isVisible + 作用域，不看索引。
     * 目标页不可达时父页不发射入口行。未注册的 pageKey 视为可达（不拦截未知目标）。
     */
    fun isPageReachable(context: Context, pageKey: String): Boolean {
        val spec = pages[pageKey] ?: return true
        if (!spec.isVisible(context)) return false
        if (!isScopeAppPresent(context, spec)) return false
        val stack = reachabilityProbeStack.get() ?: return true
        if (pageKey in stack) return true
        stack.addLast(pageKey)
        return try {
            hasVisibleContent(context, spec)
        } finally {
            stack.removeLast()
        }
    }

    /** 功能树页面顺序（功能树按此固定排序，共 51 页） */
    val treeOrder: List<String> = listOf(
        "android_related",
        "statusbar",
        "launcher",
        "aod",
        "lock_screen",
        "application",
        "miscellaneous",
        "oplus_screenshot",
        "oplus_battery",
        "oplus_alarm_clock",
        "oplus_settings",
        "oplus_wireless_settings",
        "oplus_tele_service",
        "oplus_mms",
        "oplus_browser",
        "oplus_camera",
        "oplus_gallery",
        "oplus_games",
        "theme_store",
        "oplus_market",
        "oplus_cloud_service",
        "oplus_ota",
        "oplus_pictorial",
        "oplus_gesture",
        "oplus_speech_assist",
        "oplus_direct_ui",
        "oplus_search_box",
        "oplus_weather",
        "oplus_calendar",
        "oplus_smart_sidebar",
        "oplus_metis",
        "oplus_phone_manager",
        "oplus_health",
        "oplus_sound_recorder",
        "oplus_eye_protect",
        "oplus_beacon_link",
        "oplus_nfc",
        "oplus_oshare",
        "oplus_permission_controller",
        "oplus_linker",
        "oplus_securityp_permission",
        "oplus_file_manager",
        "oplus_engineer_mode",
        "oplus_my_devices",
        "oplus_mcs",
        "claw",
        "oplus_keyboard",
        "alpha_backup_pro",
        "ks_web",
        "adm",
        "gps_joy_stick",
    )

    /**
     * 树标题覆盖表（pageKey → 字符串资源）：登记过的页用固定字符串资源作功能树标题。
     * 其余页标题 = AppUtils.getAppLabel(packName)（标题即该页宿主 App 的名称）；
     * android_related 特殊：标题取 getAppLabel("android")（见 FunctionPage.pageTitle）。
     */
    val treeTitleRes: Map<String, Int> = mapOf(
        "statusbar" to R.string.StatusBar,
        "launcher" to R.string.Desktop,
        "launcher_backup" to R.string.launcher_backup,
        "aod" to R.string.AodRelated,
        "lock_screen" to R.string.LockScreen,
        "application" to R.string.Application,
        "miscellaneous" to R.string.Miscellaneous,
        // 排序页不是功能树 root，登记标题只为顶栏兜底标题（经 page() 进入时用入口行标题）
        "statusbar_icon_order" to R.string.statusbar_icon_order,
    )

    /** DSL page(target=...) 的驼峰 target 串 → pageKey：target 沿用历史 nav 参数名（如 statusBarClock），这里翻成页面注册键（如 statusbar_clock） */
    val pageTargetMap: Map<String, String> = mapOf(
        "statusBarClock" to "statusbar_clock",
        "statusBar" to "statusbar",
        "oplusAlarmClock" to "oplus_alarm_clock",
        "oplusBattery" to "oplus_battery",
        "oplusBeaconLink" to "oplus_beacon_link",
        "oplusBrowser" to "oplus_browser",
        "oplusCalendar" to "oplus_calendar",
        "oplusCamera" to "oplus_camera",
        "oplusCloudService" to "oplus_cloud_service",
        "oplusDirectUI" to "oplus_direct_ui",
        "oplusEngineerMode" to "oplus_engineer_mode",
        "oplusEyeProtect" to "oplus_eye_protect",
        "oplusFileManager" to "oplus_file_manager",
        "oplusGallery" to "oplus_gallery",
        "oplusGames" to "oplus_games",
        "oplusGesture" to "oplus_gesture",
        "oplusHealth" to "oplus_health",
        "oplusLinker" to "oplus_linker",
        "oplusMarket" to "oplus_market",
        "oplusMMS" to "oplus_mms",
        "oplusMcs" to "oplus_mcs",
        "oplusMyDevices" to "oplus_my_devices",
        "oplusNfc" to "oplus_nfc",
        "oplusOShare" to "oplus_oshare",
        "oplusOta" to "oplus_ota",
        "oplusPermissionController" to "oplus_permission_controller",
        "oplusPhoneManager" to "oplus_phone_manager",
        "oplusPictorial" to "oplus_pictorial",
        "oplusScreenshot" to "oplus_screenshot",
        "oplusSearchBox" to "oplus_search_box",
        "oplusSecuritypPermission" to "oplus_securityp_permission",
        "oplusSettings" to "oplus_settings",
        "oplusSmartSidebar" to "oplus_smart_sidebar",
        "oplusSoundRecorder" to "oplus_sound_recorder",
        "oplusSpeechAssist" to "oplus_speech_assist",
        "oplusTeleService" to "oplus_tele_service",
        "themeStore" to "theme_store",
        "oplusWeather" to "oplus_weather",
        "oplusWirelessSettings" to "oplus_wireless_settings",
        "statusBarNotify" to "statusbar_notify",
        "statusBarIcon" to "statusbar_icon",
        // 新页没有历史驼峰 target 名：入口行 target 直接用 pageKey，自映射让 page() 跳转与搜索索引都能解析
        "statusbar_icon_order" to "statusbar_icon_order",
        "statusBarControlCenter" to "statusbar_control_center",
        "statusBarLayout" to "statusbar_layout",
        "statusBarBattery" to "statusbar_battery",
        "statusBarNetWorkSpeed" to "statusbar_network_speed",
        "statusBarTiles" to "statusbar_tiles",
        "statusBarNotifyRemoval" to "statusbar_notify_removal",
        "androidRelated" to "android_related",
        "launcher" to "launcher",
        "launcherBackup" to "launcher_backup",
        "lockScreen" to "lock_screen",
        "application" to "application",
        "miscellaneous" to "miscellaneous",
        "dialogRelated" to "dialog_related",
        "fingerPrintRelated" to "finger_print_related",
        "soundRelated" to "sound_related",
        "aod" to "aod",
        "corePatch" to "core_patch",
        "alphaBackupPro" to "alpha_backup_pro",
        "claw" to "claw",
        "ksWeb" to "ks_web",
        "adm" to "adm",
        "gpsJoyStick" to "gps_joy_stick",
        "oplus_keyboard" to "oplus_keyboard",
        "multiAppFragment" to "multi_app",
        "zoomWindowFragment" to "zoom_window",
        "darkModeFragment" to "dark_mode",
        "forceFpsFragment" to "force_fps",
        "extractOTAFragment" to "extract_ota",
        "memcConfigFragment" to "memc_config",
        "hideAppIntentFragment" to "hide_app_intent",
        "batteryInfoFragment" to "battery_info",
        "donateFragment" to "donate",
        "systemQuickEntry" to "quick_entry",
        // 极暗模式页没有历史驼峰 target 名：入口行 target 直接用 pageKey，自映射让跳转与搜索索引都能解析
        "extra_dim" to "extra_dim",
    )

    init {
        register(StatusBarClockPage.spec)
        register(StatusBarRelatedPage.spec)
        // P3 apps 批
        register(OplusAlarmClockPage.spec)
        register(OplusBatteryPage.spec)
        register(OplusBeaconLinkPage.spec)
        register(OplusBrowserPage.spec)
        register(OplusCalendarPage.spec)
        register(OplusCameraPage.spec)
        register(OplusCloudServicePage.spec)
        register(OplusDirectUIPage.spec)
        register(OplusEngineerModePage.spec)
        register(OplusEyeProtectPage.spec)
        register(OplusFileManagerPage.spec)
        register(OplusGalleryPage.spec)
        register(OplusGamesPage.spec)
        register(OplusGesturePage.spec)
        register(OplusHealthPage.spec)
        register(OplusKeyboardPage.spec)
        register(OplusLinkerPage.spec)
        register(OplusMarketPage.spec)
        register(OplusMMSPage.spec)
        register(OplusMcsPage.spec)
        register(OplusMetisPage.spec)
        register(OplusMyDevicesPage.spec)
        register(OplusNfcPage.spec)
        register(OplusOSharePage.spec)
        register(OplusOTAPage.spec)
        register(OplusPermissionControllerPage.spec)
        register(OplusPhoneManagerPage.spec)
        register(OplusPictorialPage.spec)
        register(OplusScreenshotPage.spec)
        register(OplusSearchBoxPage.spec)
        register(OplusSecuritypPermissionPage.spec)
        register(OplusSettingsPage.spec)
        register(OplusSmartSidebarPage.spec)
        register(OplusSoundRecorderPage.spec)
        register(OplusSpeechAssistPage.spec)
        register(OplusTeleServicePage.spec)
        register(OplusThemeStorePage.spec)
        register(OplusWeatherPage.spec)
        register(OplusWirelessSettingsPage.spec)
        // P3 statusbar 批
        register(StatusBarBatteryPage.spec)
        register(StatusBarControlCenterPage.spec)
        register(StatusBarIconPage.spec)
        register(StatusBarIconOrderPage.spec)
        register(StatusBarLayoutPage.spec)
        register(StatusBarNetWorkSpeedPage.spec)
        register(StatusBarNotifyPage.spec)
        register(StatusBarNotifyRemovalPage.spec)
        register(StatusBarTilesPage.spec)
        register(AndroidRelatedPage.spec)
        register(AodRelatedPage.spec)
        register(ApplicationRelatedPage.spec)
        register(CorePatchPage.spec)
        register(DialogRelatedPage.spec)
        register(FingerPrintRelatedPage.spec)
        register(LauncherRelatedPage.spec)
        register(LauncherBackupPage.spec)
        register(LockScreenRelatedPage.spec)
        register(MiscellaneousPage.spec)
        register(SoundRelatedPage.spec)
        register(ADMPage.spec)
        register(AlphaBackupProPage.spec)
        register(ClawPage.spec)
        register(GpsJoyStickPage.spec)
        register(KsWebPage.spec)
        // P4 special 批
        register(AppPermissionPage.spec)
        register(BatteryInfoPage.spec)
        register(DonatePage.spec)
        register(ExtractOTAPage.spec)
        register(ForceFpsPage.spec)
        register(ExtraDimPage.spec)
        register(QuickEntryPage.spec)
        register(DarkModePage.spec)
        register(HideAppIntentPage.spec)
        register(MemcConfigPage.spec)
        register(MultiAppPage.spec)
        register(ZoomWindowPage.spec)
    }
}
