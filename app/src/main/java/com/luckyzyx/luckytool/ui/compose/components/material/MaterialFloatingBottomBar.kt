package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.MainTab
import com.luckyzyx.luckytool.ui.compose.components.ChromeInsets
import com.luckyzyx.luckytool.ui.compose.components.animation.DampedDragAnimation
import com.luckyzyx.luckytool.ui.theme.isInDarkTheme
import com.luckyzyx.luckytool.ui.shell.FloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.compose.icon
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop

/** 悬浮胶囊的水平内外边距与 item 间距：只影响胶囊宽度，按底栏 item 高度比例取值。 */
private val FloatingBarHorizontalPadding = 12.dp
private val FloatingBarItemSpacing = 8.dp

@Composable
internal fun MaterialFloatingBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    showUpdateBadge: Boolean,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    floatingStyle: FloatingBottomBarStyle = FloatingBottomBarStyle.Classic,
) {
    val iosLayout = floatingStyle == FloatingBottomBarStyle.IosLike
    val containerHeight = if (iosLayout) 56.dp else 52.dp
    val itemHeight = 48.dp
    val iconSize = if (iosLayout) 24.dp else 22.dp
    val horizontalPadding = if (iosLayout) 4.dp else FloatingBarHorizontalPadding
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomGap = if (iosLayout) {
        if (navBarPadding > 0.dp) 8.dp else 28.dp
    } else {
        if (navBarPadding > 0.dp) 26.dp else 36.dp
    }
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDark = isInDarkTheme
    val scope = rememberCoroutineScope()
    val onSelectedUpdated by rememberUpdatedState(onSelected)
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    var barWidth by remember { mutableFloatStateOf(0f) }
    val paddingPx = with(density) { horizontalPadding.toPx() }
    val tabWidth = ((barWidth - paddingPx * 2) / MainTab.entries.size).coerceAtLeast(0f)
    val currentTabWidth by rememberUpdatedState(tabWidth)
    val dragOffset = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            val fraction = if (barWidth > 0f) (dragOffset.value / barWidth).coerceIn(-1f, 1f) else 0f
            with(density) { 4.dp.toPx() } * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }

    fun settlePanel() {
        scope.launch { dragOffset.animateTo(0f, spring(1f, 300f, 0.5f)) }
    }

    val motion = remember(scope, density, isLtr, floatingStyle) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..MainTab.entries.lastIndex.toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { it.x in 0f..barWidth },
            onDragStarted = { position ->
                if (currentTabWidth > 0f) {
                    val x = if (isLtr) position.x else barWidth - position.x
                    val index = ((x - paddingPx) / currentTabWidth).toInt().coerceIn(0, MainTab.entries.lastIndex)
                    updateValue(index.toFloat())
                }
            },
            onDragStopped = {
                val index = targetValue.roundToInt().coerceIn(0, MainTab.entries.lastIndex)
                if (index != currentIndex) {
                    currentIndex = index
                    onSelectedUpdated(index)
                }
                updateValue(index.toFloat())
                settlePanel()
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                settlePanel()
            },
            onDrag = { _, delta ->
                if (currentTabWidth > 0f && delta.x != 0f) {
                    updateValue(targetValue + delta.x / currentTabWidth * if (isLtr) 1f else -1f)
                    scope.launch { dragOffset.snapTo(dragOffset.value + delta.x) }
                }
            },
        )
    }
    LaunchedEffect(selectedIndex, motion) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            motion.animateToValue(selectedIndex.toFloat())
        }
    }
    val activate: (Int) -> Unit = { index ->
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
        motion.animateToValue(index.toFloat())
    }
    val colors = MaterialTheme.colorScheme
    val surface = colors.surfaceContainer

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(ChromeInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(bottom = bottomGap),
        contentAlignment = Alignment.Center,
    ) {
        // 悬浮底栏宽度规则在两条外观线间保持一致，切换外观时布局不变。
        val width = if (iosLayout) {
            val sidePadding = 60.dp.coerceAtMost(
                ((maxWidth - itemHeight * MainTab.entries.size - 8.dp) / 2).coerceAtLeast(0.dp),
            )
            maxWidth - sidePadding * 2
        } else {
            (itemHeight * MainTab.entries.size +
                FloatingBarItemSpacing * (MainTab.entries.size - 1) +
                FloatingBarHorizontalPadding * 2).coerceAtMost(maxWidth)
        }
        Box(
            Modifier.width(width),
            contentAlignment = Alignment.CenterStart,
        ) {
            MaterialFloatingTabs(
                currentIndex = currentIndex,
                activate = activate,
                showUpdateBadge = showUpdateBadge,
                tint = colors.onSurface,
                iconSize = iconSize,
                modifier = Modifier
                    .onSizeChanged { barWidth = it.width.toFloat() }
                    .graphicsLayer { translationX = panelOffset }
                    .dropShadow(CircleShape, Shadow(radius = 10.dp, color = Color.Black, alpha = if (isDark) 0.2f else 0.1f))
                    .then(
                        if (backdrop == null) Modifier.background(surface, CircleShape)
                        else Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = { blur(25.dp.toPx(), 25.dp.toPx()) },
                            onDrawSurface = { drawRect(surface.copy(alpha = 0.65f)) },
                        )
                    )
                    .then(motion.modifier)
                    .selectableGroup()
                    .height(containerHeight)
                    .padding(horizontal = horizontalPadding, vertical = (containerHeight - itemHeight) / 2),
            )
            if (tabWidth > 0f) {
                Box(
                    Modifier.padding(horizontal = horizontalPadding)
                        .graphicsLayer {
                            val offset = motion.value * tabWidth
                            translationX = (if (isLtr) offset else -offset) + panelOffset
                        }
                        .testTag("materialFloatingIndicator")
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.15f), CircleShape)
                        .height(itemHeight)
                        .width(with(density) { tabWidth.toDp() }),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // 选中图标随滑块裁切显色，滑动途中保留经过图标的局部着色。
                    MaterialFloatingTabs(
                        currentIndex = currentIndex,
                        activate = activate,
                        showUpdateBadge = showUpdateBadge,
                        tint = colors.primary,
                        iconSize = iconSize,
                        interactive = false,
                        modifier = Modifier.clearAndSetSemantics {}
                            .wrapContentWidth(Alignment.Start, unbounded = true)
                            .requiredWidth(with(density) { (barWidth - paddingPx * 2).toDp() })
                            .height(itemHeight)
                            .graphicsLayer {
                                val offset = motion.value * tabWidth
                                translationX = if (isLtr) -offset else offset
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialFloatingTabs(
    currentIndex: Int,
    activate: (Int) -> Unit,
    showUpdateBadge: Boolean,
    tint: Color,
    iconSize: Dp,
    modifier: Modifier,
    interactive: Boolean = true,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        MainTab.entries.forEach { tab ->
            Box(
                Modifier.weight(1f).height(48.dp).then(
                    if (!interactive) Modifier else Modifier
                        .semantics(mergeDescendants = true) {
                            selected = currentIndex == tab.ordinal
                            role = Role.Tab
                            onClick { activate(tab.ordinal); true }
                        }
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar) {
                                if (event.type == KeyEventType.KeyUp) activate(tab.ordinal)
                                true
                            } else false
                        }
                        .focusable()
                ),
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(badge = {
                    if (showUpdateBadge && tab == MainTab.Home) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { Text("1") }
                    }
                }) {
                    Icon(
                        tab.icon(),
                        contentDescription = stringResource(tab.labelRes),
                        modifier = Modifier.size(iconSize),
                        tint = tint,
                    )
                }
            }
        }
    }
}
