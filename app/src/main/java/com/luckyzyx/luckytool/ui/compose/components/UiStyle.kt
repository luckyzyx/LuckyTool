package com.luckyzyx.luckytool.ui.compose.components

import android.os.Build
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar as CouiNavigationBar
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold as CouiScaffold
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.luckyzyx.luckytool.ui.compose.MainTab
import com.luckyzyx.luckytool.ui.compose.icon
import com.luckyzyx.luckytool.ui.compose.iconPair
import com.luckyzyx.luckytool.ui.compose.isRenderEffectSupported
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiBadge
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiBadgedBox
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPageScaffold
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPullToRefreshBox
import com.luckyzyx.luckytool.ui.compose.components.material.LocalBottomBarPresent
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialFloatingBottomBar
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTopBarBackButton
import com.luckyzyx.luckytool.ui.compose.components.material.materialTopAppBarColors
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBar
import com.luckyzyx.luckytool.ui.shell.LocalEnableNavigationBadge
import com.luckyzyx.luckytool.ui.shell.LocalFloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 底栏当前是否被放在 COUI Scaffold 的底栏槽位里。
 *
 * 槽位会给底栏提供正文录制的背景采样源，悬浮底栏因此不必自建采样层；值为 false 时
 * （例如把底栏从标签外壳里单独拿出来组合）悬浮底栏拿不到采样源，要退回平铺样式。
 */
internal val LocalCouiTabsBarSlot = compositionLocalOf { false }

/**
 * 当前页顶栏的折叠比例（0 = 展开，1 = 完全收起）。
 *
 * 下拉刷新只在顶栏完全展开后接管下拉手势：列表到顶、顶栏还差一点没展开时，正向位移
 * 会先被下拉组件吃掉，顶栏在 postScroll 里拿不到剩余量，就永远展不开、只剩一个下拉动画。
 */
internal val LocalTopBarCollapsedFraction = compositionLocalOf { 0f }

/**
 * 系统栏与刘海的安全区，不含输入法。
 *
 * [WindowInsets.safeDrawing] 把输入法也算在安全区里：键盘一弹出，用到它的页面底部内边距
 * 就凭空多出一整块键盘高度，正文被顶上去却在上方留出一条空白。常驻的顶栏 / 底栏 / 页面正文
 * 只用这份稳定值；输入法带来的位移统一由标签外壳补偿，避免两处各减一次。
 */
internal val ChromeInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)

/**
 * 一套外观样式（主题）的实现对象。
 *
 * 页面与外壳只调用文件末尾的 [PageScaffold] / [RootScaffold] 与接口上的 [TabsShell]，
 * 由当前外观选中的实现对象去画骨架：新增样式时写一个新的实现对象、在 [currentUiStyle] 里接上分支即可，
 * 页面侧不用改。这与设置区块里开关 / 复选框按外观分派的写法是同一套思路。
 */
internal interface UiStyle {

    /**
     * 单个页面：大标题、返回键、动作区、悬浮按钮与正文内边距。
     *
     * [header] 由调用方统一持有并在两条外观线之间交接折叠状态，实现对象只负责把它接到自己的顶栏上。
     */
    @Composable
    fun PageScaffold(
        title: String,
        modifier: Modifier = Modifier,
        onBack: (() -> Unit)? = null,
        actions: @Composable RowScope.() -> Unit = {},
        floatingActionButton: @Composable () -> Unit = {},
        bottomBar: @Composable () -> Unit = {},
        collapseEnabled: Boolean = true,
        header: PageHeader,
        content: @Composable (PaddingValues) -> Unit,
    )

    /** 根壳：只铺容器底色，不处理系统栏 insets（各页面自己消费）。 */
    @Composable
    fun RootScaffold(content: @Composable (PaddingValues) -> Unit)

    /**
     * 页面容器底色。
     *
     * 需要自建容器而不是走 [RootScaffold] 的地方（例如权限阻塞态整屏外壳）取这里，
     * 保证与根壳同色；不要在页面里直接读两套色板。
     */
    @Composable
    fun background(): Color

    /**
     * 分组与分组之间的纵向间距。
     *
     * COUI 的分段卡片之间要留出一档，material 的分段卡片自带间距、这里为 0。
     */
    val groupSpacing: Dp

    /**
     * 列表里相邻两段之间的间距。
     *
     * COUI 的一行本身就是一张分段卡片，段间留白会把卡片切断，因此为 0；material 仍是 2dp。
     */
    val listRowSpacing: Dp

    /**
     * 一级标签外壳：底栏形态、正文采样源与两者的让位关系都由本实现安排。
     *
     * 页签项、图标与角标由实现按本线需要自己读；把底栏做成盖在正文之上的浮层时，
     * 传给 [content] 的 innerPadding 底部为 0，改由 shellInset 告诉页面尾部该留多少空白。
     */
    @Composable
    fun TabsShell(
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
        content: @Composable (innerPadding: PaddingValues, shellInset: Dp) -> Unit,
    )

    /**
     * 设置页列表的下拉刷新容器：指示器与手势两条线各有一套，回调与刷新状态只有一份。
     *
     * [enabled] 只在条线自己支持时生效（material 线用它把下拉让给还没展开的顶栏）；
     * COUI 线在容器内部按顶栏折叠比例自行判断，不看这个参数。
     */
    @Composable
    fun RefreshBox(
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable BoxScope.() -> Unit,
    )

    /** 列表的跟手回弹效果；null 表示走平台默认的拉伸效果。 */
    @Composable
    fun overscrollEffect(): OverscrollEffect?

    /**
     * 权限阻塞页的整屏外壳。
     *
     * 阻塞态没有标签栏与顶栏，只有一层页面底色加安全区；[content] 自带滚动，外壳不再包滚动。
     */
    @Composable
    fun GateShell(insets: WindowInsets, content: @Composable () -> Unit)

    /**
     * 顶栏图标动作键。
     *
     * [painter] 是调用点按本外观线选好的图标；触摸区、按压反馈与配色由实现对象决定。
     */
    @Composable
    fun TopBarIconAction(
        onClick: () -> Unit,
        contentDescription: String?,
        painter: Painter,
        modifier: Modifier = Modifier,
    )

    /** 顶栏「更多」溢出菜单：动作键 + 弹出列表，条目顺序即展示顺序 */
    @Composable
    fun TopBarOverflow(
        items: List<TopBarOverflowItem>,
        contentDescription: String?,
        modifier: Modifier = Modifier,
    )
}

/**
 * 顶栏折叠状态的交接物。
 *
 * 两条外观线的顶栏各有自己的折叠状态，两份状态都常驻在这里：运行时切换外观后，
 * 新骨架按上一副骨架的折叠比例恢复标题位置，否则切一次样式大标题就弹回展开态。
 */
@Stable
internal class PageHeader internal constructor(
    /** Material 顶栏状态，由 Material 骨架的滚动行为驱动。 */
    internal val materialState: TopAppBarState,
    /** COUI 顶栏的滚动行为，常驻以便切回 COUI 时标题仍是收起的样子。 */
    internal val couiBehavior: CouiTopAppBarScrollBehavior,
    /** 顶栏测量完成后按折叠比例还原 Material 顶栏位置的修饰符。 */
    internal val restoreModifier: Modifier,
    /** 从 Material 切到 COUI 时要重放一次的折叠比例；null 表示无需重放。 */
    internal val couiRestoreFraction: Float?,
    /** COUI 顶栏真实折叠比例（58dp 行程上的逻辑值），由 COUI 骨架持续回写。 */
    internal val couiCollapsedFraction: MutableState<Float>,
)

/** Material 线：Material 3 的 Scaffold 与可折叠大标题。 */
private object MaterialUiStyle : UiStyle {

    @Composable
    override fun PageScaffold(
        title: String,
        modifier: Modifier,
        onBack: (() -> Unit)?,
        actions: @Composable RowScope.() -> Unit,
        floatingActionButton: @Composable () -> Unit,
        bottomBar: @Composable () -> Unit,
        collapseEnabled: Boolean,
        header: PageHeader,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(header.materialState)
        MaterialScaffold(
            modifier = modifier,
            topBar = {
                LargeFlexibleTopAppBar(
                    modifier = header.restoreModifier,
                    navigationIcon = {
                        if (onBack != null) MaterialTopBarBackButton(onClick = onBack)
                    },
                    title = { Text(title) },
                    actions = actions,
                    colors = materialTopAppBarColors(),
                    scrollBehavior = scrollBehavior,
                    windowInsets = ChromeInsets.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
                )
            },
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            contentWindowInsets = if (LocalBottomBarPresent.current) {
                ChromeInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            } else {
                ChromeInsets
            },
        ) { innerPadding ->
            Box(
                if (collapseEnabled) {
                    Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                } else {
                    Modifier
                }
            ) {
                content(innerPadding)
            }
        }
    }

    @Composable
    override fun RootScaffold(content: @Composable (PaddingValues) -> Unit) {
        // 页面各自处理 insets，根壳不再叠加一层
        Scaffold(
            containerColor = background(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            content = content,
        )
    }

    @Composable
    override fun background(): Color = MaterialTheme.colorScheme.surfaceContainer

    // material 的分段卡片自带段间留白，列表不需要额外的分组上沿留白
    override val groupSpacing: Dp = 0.dp

    override val listRowSpacing: Dp = 2.dp

    @Composable
    override fun TabsShell(
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
        content: @Composable (PaddingValues, Dp) -> Unit,
    ) {
        val floating = LocalEnableFloatingBottomBar.current
        val floatingStyle = LocalFloatingBottomBarStyle.current
        val showUpdateBadge = LocalEnableNavigationBadge.current && ShellBadgeState.updateAvailable
        val density = LocalDensity.current
        var capsuleHeight by remember(floatingStyle, density) { mutableStateOf(76.dp) }
        // 悬浮胶囊要采样正文才能透出下层 UI：采样源只套正文，胶囊必须画在采样层之外。
        val backdropSurface = MaterialTheme.colorScheme.surfaceContainer
        val backdrop = if (floating && LocalEnableBlur.current && isRenderEffectSupported()) {
            rememberLayerBackdrop {
                drawRect(backdropSurface)
                drawContent()
            }
        } else {
            null
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            bottomBar = {
                // 悬浮胶囊在正文之上单独画，底栏槽位留空
                if (!floating) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedIndex == tab.ordinal
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelect(tab.ordinal) },
                                icon = {
                                    val icon: @Composable () -> Unit = {
                                        Icon(tab.icon(), contentDescription = null, modifier = Modifier.size(24.dp))
                                    }
                                    if (showUpdateBadge && tab == MainTab.Home) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ) { Text("1") }
                                            },
                                        ) { icon() }
                                    } else {
                                        icon()
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
            ),
        ) { innerPadding ->
            Box(modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                ) {
                    // 胶囊盖在正文上：正文不必为栏位让高，页面按 shellInset 在末尾留白
                    content(
                        innerPadding,
                        if (floating) {
                            (capsuleHeight - innerPadding.calculateBottomPadding()).coerceAtLeast(0.dp)
                        } else {
                            0.dp
                        },
                    )
                }
                if (floating) {
                    MaterialFloatingBottomBar(
                        selectedIndex = selectedIndex,
                        onSelected = onSelect,
                        showUpdateBadge = showUpdateBadge,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { capsuleHeight = with(density) { it.height.toDp() } },
                        backdrop = backdrop,
                        floatingStyle = floatingStyle,
                    )
                }
            }
        }
    }

    @Composable
    override fun RefreshBox(
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            enabled = enabled,
            content = content,
        )
    }

    @Composable
    override fun overscrollEffect(): OverscrollEffect? = null

    @Composable
    override fun GateShell(insets: WindowInsets, content: @Composable () -> Unit) {
        Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentWindowInsets = insets) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) { content() }
        }
    }

    @Composable
    override fun TopBarIconAction(
        onClick: () -> Unit,
        contentDescription: String?,
        painter: Painter,
        modifier: Modifier,
    ) {
        MaterialTopBarActionButton(onClick, contentDescription, painter, modifier)
    }

    @Composable
    override fun TopBarOverflow(
        items: List<TopBarOverflowItem>,
        contentDescription: String?,
        modifier: Modifier,
    ) {
        MaterialTopBarOverflowMenu(items, contentDescription, modifier)
    }
}

/** COUI 线：COUI 的 Scaffold 与可折叠大标题，底栏自带采样背景源。 */
private object CouiUiStyle : UiStyle {

    @Composable
    override fun PageScaffold(
        title: String,
        modifier: Modifier,
        onBack: (() -> Unit)?,
        actions: @Composable RowScope.() -> Unit,
        floatingActionButton: @Composable () -> Unit,
        bottomBar: @Composable () -> Unit,
        collapseEnabled: Boolean,
        header: PageHeader,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        CouiPageScaffold(
            title = title,
            scrollBehavior = header.couiBehavior,
            restoreCollapsedFraction = header.couiRestoreFraction,
            onCollapsedFractionChanged = { header.couiCollapsedFraction.value = it },
            collapseEnabled = collapseEnabled,
            modifier = modifier,
            onBack = onBack,
            actions = actions,
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            content = content,
        )
    }

    @Composable
    override fun RootScaffold(content: @Composable (PaddingValues) -> Unit) {
        // 根壳不再套 COUI 的 Scaffold：标签外壳的 Scaffold 已经为底栏录制了一整屏背景源，
        // 再叠一层就是两次全屏离屏绘制
        Box(Modifier.fillMaxSize().background(background())) {
            content(PaddingValues())
        }
    }

    @Composable
    override fun background(): Color = CouiPageDefaults.themeColors(CouiTheme.isDark).background

    override val groupSpacing: Dp = CouiPageDefaults.groupSpacing

    override val listRowSpacing: Dp = 0.dp

    @Composable
    override fun TabsShell(
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier,
        content: @Composable (PaddingValues, Dp) -> Unit,
    ) {
        // 材质的浮动绘制器从 34 起可用，取不到就退回不依赖渲染器的平铺底栏。
        val renderer = if (Build.VERSION.SDK_INT >= 34) {
            remember { runCatching { CouiFloatingRenderer() }.getOrNull() }
        } else {
            null
        }
        // 底栏放进 COUI Scaffold 的底栏槽位：正文整屏录进背景采样源，底栏在采样层之外读它，
        // 悬浮底栏才有整块页面可采样。底栏自己的高度由 Scaffold 通过正文 Padding 报告，
        // 纵向安全区只由栏位消费，这里只统一消费横向安全区。
        val pageColors = CouiPageDefaults.themeColors(CouiTheme.isDark)
        CompositionLocalProvider(LocalCouiTabsBarSlot provides true) {
            CouiScaffold(
                modifier = modifier,
                containerColor = pageColors.background,
                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                bottomBar = { CouiTabsBar(renderer, selectedIndex, onSelect) },
            ) { innerPadding ->
                if (renderer != null) {
                    // 悬浮底栏盖在正文之上：正文底部不再让出栏位，页面按 shellInset 在末尾留白
                    val layoutDirection = LocalLayoutDirection.current
                    content(
                        PaddingValues(
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            top = innerPadding.calculateTopPadding(),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                            bottom = 0.dp,
                        ),
                        innerPadding.calculateBottomPadding(),
                    )
                } else {
                    content(innerPadding, 0.dp)
                }
            }
        }
    }

    @Composable
    override fun RefreshBox(
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        // COUI 线的下拉手势由容器内部的嵌套滚动接管，本身就会在顶栏未展开时让位
        CouiPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            content = content,
        )
    }

    @Composable
    override fun overscrollEffect(): OverscrollEffect? = rememberCouiOverscrollEffect()

    @Composable
    override fun GateShell(insets: WindowInsets, content: @Composable () -> Unit) {
        // 权限页自带滚动，这里只铺页面底色并把系统栏 / 挖孔安全区让出来（留白用内边距表达）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background())
                .windowInsetsPadding(insets),
        ) {
            content()
        }
    }

    @Composable
    override fun TopBarIconAction(
        onClick: () -> Unit,
        contentDescription: String?,
        painter: Painter,
        modifier: Modifier,
    ) {
        CouiTopBarActionButton(onClick, contentDescription, painter, modifier)
    }

    @Composable
    override fun TopBarOverflow(
        items: List<TopBarOverflowItem>,
        contentDescription: String?,
        modifier: Modifier,
    ) {
        CouiTopBarOverflowMenu(items, contentDescription, modifier)
    }
}

/** 当前外观对应的实现对象：新增样式只在这里加分支，并在 [UiMode] 里加条目。 */
@Composable
internal fun currentUiStyle(): UiStyle = when (LocalUiMode.current) {
    UiMode.Coui -> CouiUiStyle
    UiMode.Material -> MaterialUiStyle
}

/**
 * 页面骨架的统一入口。
 *
 * 切换外观时把同一份页面内容在两条骨架之间「搬移」（movable content）：页面自身的
 * remember 状态（列表滚动位置、页内临时状态）随内容走，只有外侧骨架被替换。
 */
@Composable
internal fun PageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    collapseEnabled: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    // rememberUpdatedState 防止固化首次组合的 lambda（否则页内回调会引用过期取值）
    val currentContent = rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf<PaddingValues> { innerPadding -> currentContent.value(innerPadding) }
    }
    val uiMode = LocalUiMode.current
    val materialState = rememberTopAppBarState()
    val couiBehavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // 库的 collapsedFraction 分母停在 116dp，COUI 骨架回写 58dp 行程上的真实比例。
    val couiCollapsedFraction = remember { mutableStateOf(0f) }
    var previousUiMode by remember { mutableStateOf(uiMode) }
    var pendingCollapsedFraction by remember(uiMode) {
        mutableStateOf(
            if (previousUiMode == uiMode) {
                null
            } else if (previousUiMode == UiMode.Coui) {
                // 记下上一副骨架的折叠比例，交给新骨架的顶栏恢复
                couiCollapsedFraction.value
            } else {
                materialState.collapsedFraction
            }
        )
    }
    var pendingContentOffset by remember(uiMode) {
        mutableStateOf(if (previousUiMode == uiMode) 0f else materialState.contentOffset)
    }
    // COUI 没有对外的状态写入入口，只能按折叠比例重放一次滚动把它收起来
    val couiRestoreFraction = remember(uiMode) {
        if (previousUiMode == UiMode.Material) materialState.collapsedFraction else null
    }
    SideEffect { previousUiMode = uiMode }
    // 顶栏测量后按折叠比例换算偏移量，避免切换外观时重置标题位置
    val restoreModifier = Modifier.onSizeChanged {
        val limit = materialState.heightOffsetLimit
        if (!limit.isFinite() || limit == -Float.MAX_VALUE) return@onSizeChanged
        val fraction = pendingCollapsedFraction
        if (fraction != null) {
            materialState.heightOffset = limit * fraction.coerceIn(0f, 1f)
            materialState.contentOffset = pendingContentOffset
            pendingCollapsedFraction = null
        } else if (!collapseEnabled) {
            // 非导航页不允许展开：测出高度上限后直接把标题压到收起位置
            materialState.heightOffset = limit
        }
    }
    // 下拉刷新组件用这个值判断顶栏是否已完全展开，两条线都只在展开后才接管下拉手势
    val topBarCollapsedFraction = if (uiMode == UiMode.Coui) {
        couiCollapsedFraction.value
    } else {
        materialState.collapsedFraction
    }
    CompositionLocalProvider(LocalTopBarCollapsedFraction provides topBarCollapsedFraction) {
        currentUiStyle().PageScaffold(
            title = title,
            modifier = modifier,
            onBack = onBack,
            actions = actions,
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            collapseEnabled = collapseEnabled,
            header = PageHeader(
                materialState = materialState,
                couiBehavior = couiBehavior,
                restoreModifier = restoreModifier,
                couiRestoreFraction = couiRestoreFraction,
                couiCollapsedFraction = couiCollapsedFraction,
            ),
            content = movableContent,
        )
    }
}

/** 根壳的统一入口：除页面骨架之外的最外层容器。 */
@Composable
internal fun RootScaffold(content: @Composable (PaddingValues) -> Unit) {
    // 两条外观线的根壳都只是铺底色、内边距为零：固定在同一处铺色（颜色问样式位）而不换容器，
    // 避免切换外观时导航宿主因调用点变化被销毁重建。
    val background = currentUiStyle().background()
    Box(Modifier.fillMaxSize().background(background)) {
        content(PaddingValues())
    }
}

/** kit 在菜单两侧预留的附件位：每侧 56dp 圆 + 12dp 间距，不挂附件也照样占位。 */
private val CouiBarAttachmentSpace = 136.dp

/** 底栏单个页签的目标宽度：kit 的设计基准 70dp 铺满时过于扁平，取更紧凑的一档。 */
private val CouiBarItemWidth = 52.dp

/**
 * COUI 底栏：能构造出背景渲染器时用悬浮胶囊，否则退回同一套配色的平铺底栏。
 *
 * 悬浮底栏缺了渲染器就只能在构造期抛异常，所以必须留一条不依赖渲染器的路径，
 * 否则低版本会掉回 Material 底栏，把 material3 组件带进 COUI 渲染树。
 */
@Composable
private fun CouiTabsBar(
    renderer: CouiFloatingRenderer?,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val tabs = MainTab.entries
    val showUpdateBadge = LocalEnableNavigationBadge.current && ShellBadgeState.updateAvailable
    if (renderer != null && LocalCouiTabsBarSlot.current) {
        // kit 的菜单宽 = 可用宽 - 136dp（两端附件位），5 个页签只剩每项约 37dp。
        // 这里按页签目标宽度反推菜单宽，再把这 136dp 加回去：kit 扣完正好落在目标宽度上，
        // 胶囊宽度因此不随屏宽浮动，页签也拿到设计宽度；溢出的部分落在屏幕外，不参与绘制。
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 窄屏兜底：菜单不超过整条可用宽（屏宽减去 kit 左右各 16dp 的内边距）。
            val menuWidth = (24.dp + CouiBarItemWidth * tabs.size + 8.dp * (tabs.size - 1))
                .coerceAtMost(maxWidth - 32.dp)
            CouiNavigationBar(
                modifier = Modifier.requiredWidth(menuWidth + CouiBarAttachmentSpace + 32.dp),
                renderer = renderer,
                // 两态在图形上就分开（未选中描边、选中实心），文字透明度只是附加区分。
                items = tabs.map { tab ->
                    val icons = tab.iconPair()
                    CouiNavigationItem(stringResource(tab.labelRes), icons.unselected, icons.selected)
                },
                selectedIndex = selectedIndex,
                dark = CouiTheme.isDark,
                onSelection = onSelect,
            )
        }
        return
    }
    CouiFlatTabsBar(tabs, selectedIndex, onSelect, showUpdateBadge)
}

/**
 * 不依赖渲染器的 COUI 底栏：整条铺满宽度、页签均分，选中项用实心图标 + 强调色，
 * 未选中用描边图标 + 次级文字色。
 *
 * 系统底部安全区由底栏自己消费，正文侧因此不必再为底栏留白；每个页签是整块可点的胶囊区，
 * 按下与长按悬停反馈由 couiNavigationFeedback 画；角标只叠在主页页签的图标右上角。
 */
@Composable
private fun CouiFlatTabsBar(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showUpdateBadge: Boolean,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(ChromeInsets.only(WindowInsetsSides.Bottom))
            .height(64.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = selectedIndex == tab.ordinal
            val tint = if (selected) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CouiCapsuleShape)
                    .couiNavigationFeedback(dark, interactionSource)
                    .selectable(
                        selected = selected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(tab.ordinal) },
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tabIcon: @Composable () -> Unit = {
                    val icons = tab.iconPair()
                    Icon(
                        imageVector = if (selected) icons.selected else icons.unselected,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = tint,
                    )
                }
                if (showUpdateBadge && tab == MainTab.Home) {
                    // 角标底色是强调色、内容色是对比色，由 CouiBadge 自己取，调用点只给数字。
                    CouiBadgedBox(badge = { CouiBadge(count = 1) }) { tabIcon() }
                } else {
                    tabIcon()
                }
                Spacer(Modifier.height(2.dp))
                CouiText(
                    text = stringResource(tab.labelRes),
                    kind = CouiTextKind.GroupFooter,
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}