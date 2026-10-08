/*
 * LuckyTool 统一页面外壳：与 KernelSU 主题页（ThemeScreen）一致的 Expressive 外观。
 * 顶层大标题可折叠 AppBar + surfaceContainer 底色 + 16dp 内容边距。
 */
package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.components.preference.LocalScopeScrollBehavior
import com.luckyzyx.luckytool.ui.compose.components.miuix.BlurredBar
import com.luckyzyx.luckytool.ui.compose.components.miuix.rememberBlurBackdrop
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
// Miuix 线骨架用的库件（与 KernelSU SettingsMiuix.kt / 本仓库 ThemeScreenMiuix.kt 同一批）
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 宿主是否已经提供底部导航栏（MainShell 的 NavigationBar 已消费系统导航栏 inset）。
 * 为 true 时 [ExpressiveList] 不再重复叠加导航栏内边距，避免列表底部多出一段空白。
 */
val LocalBottomBarPresent = staticCompositionLocalOf { false }

/**
 * 悬浮底栏的高度（由 MainShell 提供）：内容列表在常规底部内边距之外再预留这么多，
 * 使条目可以滑到悬浮胶囊之下（模糊/玻璃效果因此有可采样的内容），同时最后一条不会被永久遮挡。
 */
val LocalShellBottomInset = staticCompositionLocalOf { 0.dp }

/** Miuix 线内容列表的水平内缩：卡片自带 16dp insideMargin，列表侧只留 12dp（契约 §4）。 */
private val MiuixListHorizontalPadding = 12.dp

/**
 * Miuix 线页面骨架下发给内容的真实内边距与 Miuix 顶栏滚动行为。
 * material 线恒为 null（[ExpressiveList] 因此走 m3 分支，取值与分派前完全一致）。
 */
@Immutable
private class MiuixPageScaffoldValues(
    val innerPadding: PaddingValues,
    val scrollBehavior: ScrollBehavior,
)

/** Miuix 线页面骨架的内部传递通道：由 [ExpressivePageScaffold] 提供、[ExpressiveList] 消费。 */
private val LocalMiuixPageScaffold = staticCompositionLocalOf<MiuixPageScaffoldValues?> { null }

/**
 * 统一页面外壳（无返回键时传 onBack = null，用于底部导航主页面）。
 *
 * UiMode 分派：Material 线走 m3 Expressive Scaffold（原样），Miuix 线走 [MiuixExpressivePageScaffold]
 * （KernelSU SettingsMiuix 骨架：模糊顶栏 + 卡片列表）。调用点签名与调用方式不变。
 *
 * 用法：
 * ```
 * val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
 * ExpressivePageScaffold(title = stringResource(R.string.nav_setting), scrollBehavior = scrollBehavior) { padding ->
 *     ExpressiveList(scrollBehavior = scrollBehavior, modifier = Modifier.padding(padding)) { ... }
 * }
 * ```
 */
@Composable
fun ExpressivePageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()),
    content: @Composable (PaddingValues) -> Unit,
) {
    // 切换外观时把同一份页面内容在两条骨架之间「搬移」（movable content）：页面自身的
    // remember 状态（列表滚动位置、页内临时状态）随内容走，只有外侧骨架被替换。
    // rememberUpdatedState 防止固化首次组合的 lambda（否则页内回调会引用过期取值）。
    val currentContent = rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf<PaddingValues> { innerPadding -> currentContent.value(innerPadding) }
    }
    // Miuix 线：换成 KernelSU 骨架。material 线分支保持原样（只加分支，取值未改）。
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixExpressivePageScaffold(
            title = title,
            modifier = modifier,
            onBack = onBack,
            actions = actions,
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            content = movableContent,
        )
        return
    }
    ExpressiveScaffold(
        modifier = modifier,
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    if (onBack != null) TopBarBackButton(onClick = onBack)
                },
                title = { Text(title) },
                actions = actions,
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        content = movableContent,
    )
}

/**
 * Miuix 线页面骨架（KernelSU SettingsMiuix.kt:75-107 定式，与本仓库 ThemeScreenMiuix.kt:279-326 同款）：
 * MiuixScrollBehavior → 模糊采样层 → BlurredBar{TopAppBar} → Scaffold(popupHost = {}) → 内容列表。
 *
 * 与调用点的契约（8 处调用点冻结、零改动）：content 拿到的仍是真实骨架内边距（top = 顶栏实测高度），
 * 因此把内边距当 `Modifier.padding(padding)` 用的调用点保持正确 —— LogPage 的居中 Box，
 * 以及 FunctionPage 里自带 LazyColumn 的 ScopeScreen（不能再套 ExpressiveList）。
 * 列表页的 [ExpressiveList] 另经 [LocalMiuixPageScaffold] 取到同一份内边距与 Miuix 滚动行为，
 * 自行抵消外层已施加的顶部偏移，从而复刻 KernelSU「列表铺到顶栏之下」的滚动观感；
 * 自带 LazyColumn 的内容（ScopeScreen）经 [LocalScopeScrollBehavior] 拿到同一个 MiuixScrollBehavior
 * 并接上 `nestedScroll`，因此作用域/特殊页的列表同样能驱动顶栏折叠。
 *
 * 硬约束：popupHost 保持空槽位（全应用唯一 Miuix popup host 由根部 Miuix Scaffold 的默认槽位提供，
 * MainShell.kt:187-191）；采样层录制盒只包内容、绝不含绘制该图层的顶栏（MainShell.kt:160-163）。
 */
@Composable
private fun MiuixExpressivePageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    title = title,
                    color = barColor,
                    navigationIcon = {
                        if (onBack != null) MiuixTopBarBackButton(onClick = onBack)
                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        // 空槽位：弹层冒泡到根部 Miuix Scaffold 的默认 popupHost（各页不得自装 host）
        popupHost = {},
        // KernelSU 写法为 systemBars.add(displayCutout)；本仓库 Compose 版本没有 WindowInsets.add，
        // 用同一语义的 union（= miuix basic/Scaffold.kt:90 自己的默认值写法）
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalMiuixPageScaffold provides remember(innerPadding, scrollBehavior) {
                MiuixPageScaffoldValues(innerPadding, scrollBehavior)
            },
            // 同一份 scrollBehavior 下发给「自带 LazyColumn、不能再套 ExpressiveList」的内容
            // （ScopeScreen 路径）：让 70 个作用域/特殊页的列表也能折叠顶栏。
            LocalScopeScrollBehavior provides scrollBehavior,
        ) {
            // 模糊采样层录制盒：只录制本页内容；顶栏绘制在该层之内，不能放进盒内（渲染树循环引用）
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                content(innerPadding)
            }
        }
    }
}

/** Miuix 线返回按钮（KernelSU AboutMiuix.kt:130-144 同款：RTL 时图标水平镜像）。 */
@Composable
private fun MiuixTopBarBackButton(onClick: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    IconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = MiuixIcons.Back,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

/**
 * 抵消调用点在列表外侧施加的顶部内边距：测量时按 [topPx] 补回高度，布局时再向上位移同样距离，
 * 使列表铺到页面顶部（顶栏之下）而内容仍从 contentPadding 起算。
 *
 * 只用于 Miuix 线；m3 线由 m3 Scaffold 的 contentPadding 承担同一语义（见 [MiuixExpressiveList]）。
 * 不用负 padding：依赖未定义的负 Dp 语义且会破坏约束。
 */
private fun Modifier.modifierTopInsetCompensation(topPx: Int): Modifier = if (topPx <= 0) {
    this
} else {
    layout { measurable, constraints ->
        // Constraints.Infinity 参与加法会溢出成负数，这里先夹到安全上限
        val room = (Constraints.Infinity - topPx).coerceAtLeast(0)
        val placeable = measurable.measure(
            constraints.copy(
                minHeight = constraints.minHeight.coerceAtMost(room) + topPx,
                maxHeight = constraints.maxHeight.coerceAtMost(room) + topPx,
            )
        )
        layout(placeable.width, (placeable.height - topPx).coerceAtLeast(0)) {
            placeable.place(0, -topPx)
        }
    }
}

/**
 * 统一内容列表：横向 16dp 边距（与 SegmentedColumn 卡片对齐）、条目间距 8dp、
 * 底部预留导航栏 / 手写笔工具栏高度，并把滚动联动到折叠 AppBar。
 *
 * UiMode 分派：Material 线保持上面的取值；Miuix 线走 [MiuixExpressiveList]（KernelSU 卡片列表骨架）。
 * 调用点签名与调用方式不变。
 */
@Composable
fun ExpressiveList(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: LazyListScope.() -> Unit,
) {
    val miuixPage = LocalMiuixPageScaffold.current
    if (miuixPage != null) {
        MiuixExpressiveList(
            page = miuixPage,
            modifier = modifier,
            state = state,
            content = content,
        )
        return
    }
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val captionBar = WindowInsets.captionBar.asPaddingValues()
    // 宿主已有底部导航栏（已消费导航栏 inset）时不再重复叠加，避免底部空白
    val bottomInset = if (LocalBottomBarPresent.current) {
        0.dp
    } else {
        navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = state,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 4.dp,
            bottom = 16.dp + bottomInset + LocalShellBottomInset.current,
        ),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Miuix 线内容列表（KernelSU SettingsMiuix.kt:97-107 的 LazyColumn 模板）。
 *
 * 与 material 线的两点有意差异（契约 §4 已登记为允许的像素差异）：
 * 1. 水平内缩固定 12dp（Miuix 卡片自带 16dp insideMargin，不能再叠加，否则缩进变 32dp）；
 *    调用点的 `horizontalPadding` 参数因此在本分支不参与；
 * 2. 条目间距由 Miuix 行自带（组内 2dp / 组间 12dp，见契约 §4.2），故 verticalArrangement 取
 *    [Arrangement.Top]，避免与外层默认的 8dp 叠加成 10dp / 20dp；调用点的 `verticalArrangement`
 *    参数同理不参与。
 *
 * 顶部：调用点把骨架内边距以 `Modifier.padding(padding)` 施加在列表外侧（7 处调用点均如此），
 * 这里先抵消那层顶部偏移，再把真实顶栏高度作为 contentPadding.top —— 列表铺到顶栏之下、
 * 内容从顶栏底部起算，即 KernelSU 的 scroll-under 观感（模糊顶栏下始终有内容可采样）。
 */
@Composable
private fun MiuixExpressiveList(
    page: MiuixPageScaffoldValues,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val innerPadding = page.innerPadding
    val layoutDirection = LocalLayoutDirection.current
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val captionBar = WindowInsets.captionBar.asPaddingValues()
    // 宿主已有底部导航栏（已消费导航栏 inset）时不再重复叠加，避免底部空白
    val bottomInset = if (LocalBottomBarPresent.current) {
        0.dp
    } else {
        navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
    }
    // miuix Scaffold 的 contentPadding.top 即顶栏实测高度（含其内部 window inset 内边距）
    val topInset = innerPadding.calculateTopPadding()
    val topInsetPx = with(LocalDensity.current) { topInset.roundToPx() }
    LazyColumn(
        modifier = modifier
            .modifierTopInsetCompensation(topInsetPx)
            .fillMaxHeight()
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(page.scrollBehavior.nestedScrollConnection)
            .padding(horizontal = MiuixListHorizontalPadding),
        state = state,
        contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            end = innerPadding.calculateEndPadding(layoutDirection),
            top = topInset,
            bottom = 16.dp + bottomInset + LocalShellBottomInset.current,
        ),
        verticalArrangement = Arrangement.Top,
        overscrollEffect = null,
        content = content,
    )
}
