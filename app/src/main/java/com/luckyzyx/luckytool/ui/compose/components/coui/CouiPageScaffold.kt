package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiCollapsingAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarColors
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.itos.coui.topappbar.TopAppBarSurface
import com.luckyzyx.luckytool.ui.compose.components.ChromeInsets
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * COUI 线的页面外壳。
 *
 * COUI 的 Scaffold 自己录制正文背景源，并把顶栏 / 底栏 / 悬浮按钮放在采样层之外，
 * 所以这里只需要接线标题、返回键、动作区，以及大标题折叠时的正文内边距补偿：
 * 顶栏收起过程中正文要跟随 padding 变化，否则内容会被顶栏盖住。
 *
 * 折叠连接挂在正文外层，页面内容只消费传入的 PaddingValues。
 */
@Composable
internal fun CouiPageScaffold(
    title: String,
    scrollBehavior: CouiTopAppBarScrollBehavior,
    restoreCollapsedFraction: Float? = null,
    onCollapsedFractionChanged: (Float) -> Unit = {},
    collapseEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = scrollBehavior
    val density = LocalDensity.current
    val state = behavior.state
    // 列表留白跟「逻辑滚动位置」走，顶栏可见位置也用它：库的 offsetPx 是弹簧，
    // 拿弹簧当栏高会让 Scaffold 每个弹簧帧都改写 contentPadding，整列跟着重排。
    // 逻辑位置没有公开读取入口，这里在滚动连接外面套一层，按消费量自己记账；
    // 库的状态范围还停在默认的 116dp，喂给它的量要先夹到应用侧的 58dp 行程里，
    // 否则顶栏折叠完库还会继续吞 58dp 滚动，列表要多滑一次才开始动。
    val rangePx = with(density) {
        (CouiCollapsingAppBarDefaults.titleOnlyHeight - CouiCollapsingAppBarDefaults.collapsedTitleHeight).toPx()
    }
    var logicalFraction by rememberSaveable(collapseEnabled) {
        mutableStateOf(
            if (collapseEnabled) state.collapsedFraction.coerceIn(0f, 1f) else 1f
        )
    }
    // 回写真实折叠比例：库的 collapsedFraction 分母仍停在 116dp，而栏的折叠行程是
    // 58dp，跨外观交接与下拉刷新的展开判定都要用 58dp 行程上的比例。
    val currentOnFractionChanged = rememberUpdatedState(onCollapsedFractionChanged)
    LaunchedEffect(Unit) {
        snapshotFlow { logicalFraction }.collect { currentOnFractionChanged.value(it) }
    }
    var settleTarget by remember { mutableStateOf<Float?>(null) }
    val trackingConnection = remember(behavior, rangePx) {
        object : NestedScrollConnection {
            // 吸附动画被打断时，栏位先落到吸附目标：库的逻辑位置在 onPostFling 里
            // 已经同步到那里，这里跟过去，两边才继续共享同一个逻辑位置。
            private fun syncSettledFraction() {
                settleTarget?.let { target ->
                    logicalFraction = (target / rangePx).coerceIn(0f, 1f)
                }
                settleTarget = null
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                syncSettledFraction()
                val remaining = (1f - logicalFraction) * rangePx
                val delta = available.y.coerceIn(-remaining, 0f)
                if (delta == 0f) return Offset.Zero
                val consumed = behavior.nestedScrollConnection.onPreScroll(Offset(0f, delta), source)
                if (consumed.y != 0f) {
                    logicalFraction = ((logicalFraction * rangePx - consumed.y) / rangePx).coerceIn(0f, 1f)
                }
                return consumed
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                syncSettledFraction()
                val remaining = logicalFraction * rangePx
                val delta = available.y.coerceIn(0f, remaining)
                if (delta == 0f) return Offset.Zero
                val next = behavior.nestedScrollConnection.onPostScroll(consumed, Offset(0f, delta), source)
                if (next.y != 0f) {
                    logicalFraction = ((logicalFraction * rangePx - next.y) / rangePx).coerceIn(0f, 1f)
                }
                return next
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                syncSettledFraction()
                // 不调库的 onPostFling：它会按自己的 116dp 范围吸附，全折叠时吸到 116，
                // 下一次展开又得多吞 58dp。这里按应用侧行程定目标，再把库的逻辑位置
                // 一次同步过去，库的弹簧继续跑着供外观交接读取。
                val current = logicalFraction * rangePx
                if (current <= 0f || current >= rangePx) return Velocity.Zero
                val target = if (current < rangePx * .5f) 0f else rangePx
                val delta = current - target
                if (delta > 0f) {
                    behavior.nestedScrollConnection.onPostScroll(
                        consumed = Offset.Zero,
                        available = Offset(0f, delta),
                        source = NestedScrollSource.UserInput,
                    )
                } else {
                    behavior.nestedScrollConnection.onPreScroll(
                        available = Offset(0f, delta),
                        source = NestedScrollSource.UserInput,
                    )
                }
                settleTarget = target
                return Velocity.Zero
            }
        }
    }
    val settleAnim = remember { Animatable(0f) }
    LaunchedEffect(settleTarget) {
        val target = settleTarget ?: return@LaunchedEffect
        settleAnim.snapTo(logicalFraction * rangePx)
        // 新动画 API 的 animateTo 不再回调每帧数值，这里观察 Animatable 的 state 同步给栏高与留白。
        launch {
            snapshotFlow { settleAnim.value }
                .collect { value -> logicalFraction = (value / rangePx).coerceIn(0f, 1f) }
        }
        // 与库 settle 同参数的临界阻尼弹簧，视觉上衔接得上。
        settleAnim.animateTo(target, spring(dampingRatio = 1f, stiffness = 632f))
        settleTarget = null
    }
    // 从另一种外观切回来时标题应保持收起：COUI 没有对外的状态写入入口，
    // 这里按折叠比例往滚动连接里补一次等效滚动，让可见栏位与列表留白一次到位。
    LaunchedEffect(behavior, restoreCollapsedFraction) {
        if (!collapseEnabled) return@LaunchedEffect
        val fraction = restoreCollapsedFraction ?: return@LaunchedEffect
        val targetPx = fraction.coerceIn(0f, 1f) * rangePx
        val currentPx = logicalFraction * rangePx
        when {
            targetPx > currentPx -> trackingConnection.onPreScroll(
                available = Offset(0f, currentPx - targetPx),
                source = NestedScrollSource.UserInput,
            )
            targetPx < currentPx -> trackingConnection.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(0f, currentPx - targetPx),
                source = NestedScrollSource.UserInput,
            )
        }
    }
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors(dark)
    val topBarColors = CouiTopAppBarDefaults.colors(titleColor = pageColors.title, dark = dark)
    Scaffold(
        modifier = modifier,
        // COUI 的页面底色与卡片色是两档：外壳铺页面底色，列表卡片才浮得起来。
        containerColor = pageColors.background,
        windowInsets = ChromeInsets,
        topBar = {
            CouiCollapsingTitleBar(
                // 用 COUI 的语义字体而不是 M3 Text：顶栏只提供 LocalCouiTextStyle，
                // M3 Text 读不到它，标题会退回 Material 的正文字号。HomeTitle 与顶栏折叠时用来算缩放的基准一致。
                title = {
                    CouiText(
                        text = title,
                        kind = CouiTextKind.HomeTitle,
                        color = topBarColors.titleColor,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (onBack != null) CouiTopBarBackButton(onClick = onBack)
                },
                actions = actions,
                fraction = logicalFraction.coerceIn(0f, 1f),
                colors = topBarColors,
                windowInsets = ChromeInsets.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = { floatingActionButton() },
    ) { innerPadding ->
        // 顶栏高度只随逻辑滚动位置变化，不再逐弹簧帧抖动；直接读 Scaffold
        // 报告的栏高就是列表应留出的顶部空间，取整像素避免半像素触发重排。
        val topPaddingState = remember(innerPadding, density) {
            derivedStateOf {
                with(density) { innerPadding.calculateTopPadding().roundToPx().toDp() }
            }
        }
        val bottomPaddingState = remember(innerPadding, density) {
            derivedStateOf {
                with(density) { innerPadding.calculateBottomPadding().roundToPx().toDp() }
            }
        }
        val contentPadding = remember(topPaddingState, bottomPaddingState) {
            object : PaddingValues {
                override fun calculateTopPadding() = topPaddingState.value
                override fun calculateLeftPadding(layoutDirection: LayoutDirection) = 0.dp
                override fun calculateRightPadding(layoutDirection: LayoutDirection) = 0.dp
                override fun calculateBottomPadding() = bottomPaddingState.value
            }
        }
        // 顶栏高度只能靠帧收敛，切回来后的第一帧里正文若还用展开态的留白，
        // 列表就会先被大标题压住再跳一次；所以先把留白放到交接过来的收起位置。
        val restoreFraction = restoreCollapsedFraction?.coerceIn(0f, 1f) ?: 0f
        var restoreSettled by remember(restoreFraction) { mutableStateOf(restoreFraction <= 0f) }
        if (!restoreSettled && logicalFraction >= restoreFraction - 0.01f) {
            SideEffect { restoreSettled = true }
        }
        val restoredPadding = if (restoreSettled) {
            contentPadding
        } else {
            val restoredTop = remember(contentPadding, density, restoreFraction) {
                with(density) {
                    (innerPadding.calculateTopPadding() -
                        (CouiCollapsingAppBarDefaults.titleOnlyHeight -
                            CouiCollapsingAppBarDefaults.collapsedTitleHeight) * restoreFraction)
                        .coerceAtLeast(0.dp)
                }
            }
            remember(contentPadding, restoredTop) {
                contentPadding.withTop(minOf(contentPadding.calculateTopPadding(), restoredTop))
            }
        }
        Box(
            if (collapseEnabled) Modifier.nestedScroll(trackingConnection) else Modifier
        ) {
            content(restoredPadding)
        }
    }
}

/** 与库顶栏标题节奏一致的缓动：只影响标题缩放/位移，不影响栏高。 */
private val CouiTitleEasing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/** 与库顶栏一致的按钮区水平留白与标题间距。 */
private val CouiTitleBarPadding = 12.dp
private val CouiTitleBarGap = 8.dp
/** 折叠大标题栏的模糊几何：采样半径按像素换算，延伸段让模糊渐变淡入正文。 */
private val CouiTitleBarBlurExtension = 8.dp
private const val CouiTitleBarBlurRadiusPx = 27f
/** 浅色顶栏遮罩顶部透明度：对齐 Material 底栏玻璃的 0.65，让正文滚过顶栏时透出。 */
private const val CouiTitleBarTopAlpha = 0.65f

/**
 * 应用侧折叠大标题栏：布局计算与库的 AppBarLayout 一致，背景使用库的
 * TopAppBarSurface 渐变模糊，正文滚过顶栏时能透出。
 *
 * API 33 及以上走 RenderEffect 离屏合成的渐变模糊，不在每帧弹簧动画里
 * 重跑大半径高斯模糊；低版本回退渐进模糊。折叠偏移按 [fraction] 换算后
 * 传给 topExtension，背景层随顶栏上移、渐变曲线不随当前栏高重拉伸。
 */
@Composable
private fun CouiCollapsingTitleBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    fraction: Float,
    colors: CouiTopAppBarColors,
    windowInsets: WindowInsets,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val titleStyle = rememberCouiTextStyle(CouiTextKind.HomeTitle)
    val progress = CouiTitleEasing.transform(fraction.coerceIn(0f, 1f))
    val collapsedScale = with(density) {
        CouiCollapsingAppBarDefaults.collapsedTitleSize.toPx() / titleStyle.fontSize.toPx()
    }
    val scale = 1f + (collapsedScale - 1f) * progress
    val topExtensionOffset =
        (CouiCollapsingAppBarDefaults.titleOnlyHeight - CouiCollapsingAppBarDefaults.collapsedTitleHeight) *
            fraction.coerceIn(0f, 1f)
    TopAppBarSurface(
        modifier = Modifier.fillMaxWidth(),
        referenceBlurRadius = with(density) { CouiTitleBarBlurRadiusPx.toDp() },
        bottomExtension = CouiTitleBarBlurExtension,
        gradientSpeed = 3f,
        // 深色沿用库预设的顶部 0.4 透明度；浅色顶部 alpha 为 1.0 会完全遮住正文，
        // 这里对齐 Material 底栏玻璃的透明度，让滚过顶栏的内容透出来。
        gradientColor = colors.backgroundColor.copy(
            alpha = if (CouiTheme.isDark) 1f else CouiTitleBarTopAlpha
        ),
        topExtension = { topExtensionOffset },
    ) {
        Layout(
            modifier = Modifier.windowInsetsPadding(windowInsets),
            content = {
                Box { navigationIcon() }
                Row(verticalAlignment = Alignment.CenterVertically) { actions() }
                Box(
                    Modifier
                        .semantics { heading() }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(
                                if (direction == LayoutDirection.Rtl) 1f else 0f,
                                0f,
                            )
                            clip = true
                        },
                ) { title() }
            },
        ) { nodes, constraints ->
            require(constraints.hasBoundedWidth) { "顶栏需要有界宽度" }
            val width = constraints.maxWidth
            val expanded = CouiCollapsingAppBarDefaults.titleOnlyHeight.roundToPx()
            val collapsed = CouiCollapsingAppBarDefaults.collapsedTitleHeight.roundToPx()
            val range = expanded - collapsed
            val offset = fraction.coerceIn(0f, 1f) * range
            val height = constraints.constrainHeight((expanded - offset).roundToInt())
            val toolbarHeight = minOf(collapsed, height)
            val padding = CouiTitleBarPadding.roundToPx().coerceAtMost(width / 2)
            val gap = CouiTitleBarGap.roundToPx()
            val slotWidth = (width - 2 * padding).coerceAtLeast(0)
            val navigation = nodes[0].measure(
                Constraints(maxWidth = slotWidth, maxHeight = toolbarHeight)
            )
            val action = nodes[1].measure(
                Constraints(maxWidth = slotWidth - navigation.width, maxHeight = toolbarHeight)
            )
            val compactStart =
                (padding + navigation.width + if (navigation.width > 0) gap else 0).toFloat()
            val compactEnd = padding + action.width + if (action.width > 0) gap else 0
            val expandedStart = CouiCollapsingAppBarDefaults.titleStart.toPx()
            val curvedTitle = navigation.width > 0
            // 弧线偏移在两端的位置与变化率均为零，保留标题原有的起步和收尾节奏。
            val arcEnvelope = progress * (1f - progress)
            val arcOffset = if (curvedTitle) 0.8f * arcEnvelope * arcEnvelope else 0f
            val horizontalProgress = progress + arcOffset
            val start = expandedStart + (compactStart - expandedStart) * horizontalProgress
            val end = compactEnd.toFloat()
            // 用缩放后的可见宽度限制标题，避免收起时覆盖导航或操作按钮。
            val titleWidth = ((width - start - end).coerceAtLeast(0f) / scale).roundToInt()
            val titleNode = nodes[2].measure(
                Constraints(maxWidth = titleWidth, maxHeight = (height / scale).toInt())
            )
            layout(width, height) {
                val navigationY = (toolbarHeight - navigation.height) / 2
                navigation.placeRelative(padding, navigationY, zIndex = 1f)
                action.placeRelative(
                    width - padding - action.width,
                    (toolbarHeight - action.height) / 2,
                    zIndex = 1f,
                )
                val expandedY =
                    expanded - (CouiCollapsingAppBarDefaults.titleOnlyHeight -
                        CouiCollapsingAppBarDefaults.titleOnlyTop).toPx()
                val collapsedY = (collapsed - titleNode.height * scale) / 2f
                val titleY = if (curvedTitle) {
                    val verticalProgress = progress - arcOffset
                    (expandedY + (collapsedY - expandedY) * verticalProgress).coerceAtLeast(0f)
                } else {
                    (expandedY + (range + collapsedY - expandedY) * progress - offset)
                        .coerceIn(0f, (height - titleNode.height * scale).coerceAtLeast(0f))
                }
                titleNode.placeRelative(start.roundToInt(), titleY.roundToInt())
            }
        }
    }
}

/** 只改顶部留白，其余边沿继续走原 [PaddingValues]。 */
private fun PaddingValues.withTop(top: Dp): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        this@withTop.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        this@withTop.calculateRightPadding(layoutDirection)

    override fun calculateTopPadding() = top

    override fun calculateBottomPadding() = this@withTop.calculateBottomPadding()
}
