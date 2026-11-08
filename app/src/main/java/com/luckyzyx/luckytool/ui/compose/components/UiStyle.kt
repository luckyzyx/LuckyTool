package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold as CouiScaffold
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPageScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.LocalBottomBarPresent
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTopBarBackButton
import com.luckyzyx.luckytool.ui.compose.components.material.materialTopAppBarColors
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 底栏当前是否被放在 COUI Scaffold 的底栏槽位里。
 *
 * 槽位会给底栏提供正文录制的背景采样源，悬浮底栏因此不必自建采样层；值为 false 时
 * （例如单独组合标签外壳、不经过 [TabsScaffold]）悬浮底栏拿不到采样源，要退回平铺样式。
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
 * 页面与外壳只调用文件末尾的 [PageScaffold] / [RootScaffold] / [TabsScaffold]，
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

    /** 一级标签外壳：正文在上、底栏在下，底栏按自身高度占位。 */
    @Composable
    fun TabsScaffold(
        bottomBar: @Composable () -> Unit,
        content: @Composable (PaddingValues) -> Unit,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            content = content,
        )
    }

    @Composable
    override fun TabsScaffold(
        bottomBar: @Composable () -> Unit,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            bottomBar = bottomBar,
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
            ),
            content = content,
        )
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
        val pageColors = CouiPageDefaults.themeColors(CouiTheme.isDark)
        Box(Modifier.fillMaxSize().background(pageColors.background)) {
            content(PaddingValues())
        }
    }

    @Composable
    override fun TabsScaffold(
        bottomBar: @Composable () -> Unit,
        content: @Composable (PaddingValues) -> Unit,
    ) {
        // 底栏放进 COUI Scaffold 的底栏槽位：正文整屏录进背景采样源，底栏在采样层之外读它，
        // 悬浮底栏才有整块页面可采样。底栏自己的高度由 Scaffold 通过正文 Padding 报告，
        // 纵向安全区只由栏位消费，这里只统一消费横向安全区。
        val pageColors = CouiPageDefaults.themeColors(CouiTheme.isDark)
        CompositionLocalProvider(LocalCouiTabsBarSlot provides true) {
            CouiScaffold(
                containerColor = pageColors.background,
                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                bottomBar = bottomBar,
                content = content,
            )
        }
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
    // 两条外观线的根壳都只是铺底色、内边距为零：固定在同一处铺色而不走分支，
    // 避免切换外观时导航宿主因调用点变化被销毁重建。
    val background = if (LocalUiMode.current == UiMode.Coui) {
        CouiPageDefaults.themeColors(CouiTheme.isDark).background
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Box(Modifier.fillMaxSize().background(background)) {
        content(PaddingValues())
    }
}

/** 一级标签外壳的统一入口。 */
@Composable
internal fun TabsScaffold(
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    currentUiStyle().TabsScaffold(bottomBar = bottomBar, content = content)
}
