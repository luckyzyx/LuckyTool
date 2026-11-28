package com.luckyzyx.luckytool.ui.compose.components.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/** 底栏里的一个标签：[icon] 是显示图标，[label] 同时作为无障碍描述。 */
internal data class LiquidGlassItem(
    val label: String,
    val icon: ImageVector,
)

private val LocalIosTabScale = staticCompositionLocalOf { { 1f } }

private val iosIndicatorSpecular: Highlight = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

// 与 HighlightStyle.kt 的 LIGHT_REF 保持同步。
private const val LIGHT_REF_X = 0.5f
private const val LIGHT_REF_Y = 0.7f
private const val GRAVITY_DIR_THRESHOLD_SQ = 0.01f // |g_xy| > 0.1，约 6° 倾斜

// 重力方向角量化步进（3°）：小于一个步进的方向变化肉眼不可辨，不值得一次重绘。
private val GRAVITY_ANGLE_STEP_RAD = (3.0 * PI / 180.0).toFloat()

/**
 * 返回屏幕平面内重力方向角（弧度，已量化到 3° 步进）的 [State]。
 *
 * 只返回 State 而不在组合期读值：传感器以约 50Hz 无节流写入 tilt 快照状态，
 * 若在组合期读取，失效会上浮到整个调用方作用域造成每 tick 重组。消费方应把
 * `State.value` 的读取下沉到 drawBackdrop 的 highlight lambda 内（绘制阶段求值），
 * 让传感器更新只触发 draw 失效；derivedStateOf 的结构相等去重再把 draw 失效频率
 * 从传感器速率降到方向真正跨过量化步进时。
 */
@Composable
private fun rememberQuantizedGravityAngle(): State<Float> {
    val tiltState = rememberDeviceTilt()
    return remember(tiltState) {
        derivedStateOf {
            val tilt = tiltState.value
            val gx = tilt.gravityX
            val gy = tilt.gravityY
            val gMagSq = gx * gx + gy * gy
            if (gMagSq > GRAVITY_DIR_THRESHOLD_SQ) {
                (atan2(gy, gx) / GRAVITY_ANGLE_STEP_RAD).roundToInt() * GRAVITY_ANGLE_STEP_RAD
            } else {
                // 近水平放置时屏幕平面内的重力分量方向不稳定，固定指向 (0, -1)
                (-PI / 2).toFloat()
            }
        }
    }
}

/** 由量化后的重力方向角计算 `dualPeak` 高光的主光源位置，并在其上叠加 [extraDegrees] 的额外旋转。 */
private fun gravityRotatedHighlight(
    base: Highlight,
    gravityAngleRad: Float,
    extraDegrees: Float,
): Highlight {
    val baseStyle = base.style as BloomStroke
    val basePrimary = baseStyle.primaryLight
    val rad = gravityAngleRad + (extraDegrees * PI / 180.0).toFloat()
    return base.copy(
        style = baseStyle.copy(
            primaryLight = basePrimary.copy(
                position = LightPosition(
                    x = LIGHT_REF_X + cos(rad),
                    y = LIGHT_REF_Y + sin(rad),
                    z = basePrimary.position.z,
                ),
            ),
        ),
    )
}

/**
 * 液态玻璃悬浮底栏。
 *
 * [backdrop] 为背景采样源：为 `null` 或 [isBlurActive] 为 `false` 时退化为纯色胶囊，
 * 几何与选中动画不变，因此切换模糊开关不会让底栏跳动。
 * 点击标签、拖动滑块或滑动后松手都会通过 [onSelected] 回报新的下标，调用方只需把它
 * 回写进 [selectedIndex]；[badge] 按标签下标提供角标，返回 `null` 表示该项无角标。
 */
@Composable
internal fun LiquidGlassBottomBar(
    items: List<LiquidGlassItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isBlurActive: Boolean,
    badge: (Int) -> (@Composable () -> Unit)? = { null },
    modifier: Modifier = Modifier,
) {
    val pillShape = remember { CircleShape }
    val accentColor = MaterialTheme.colorScheme.primary
    val tabContentColor = MaterialTheme.colorScheme.onSurface
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = if (isBlurActive) surfaceContainer.copy(alpha = 0.4f) else surfaceContainer
    // 暗色下阴影更实、滑块底色用白：从主题表面亮度推导，不额外要参数。
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).coerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelected)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val dampedDrag = remember(animationScope, tabsCount, density, isLtr) {
        LiquidDampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset ->
                offset.x in 0f..totalWidthPx
            },
            onDragStarted = { position -> updateValue(indexAt(position.x).toFloat()) },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(targetIndex)
                }
                updateValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        )
    }

    LaunchedEffect(selectedIndex, dampedDrag) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            dampedDrag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        if (currentIndex != index) {
            currentIndex = index
            onSelectedUpdated(index)
        }
        dampedDrag.animateToValue(index.toFloat())
    }

    // dampedDrag 必须在 remember 键里：density 变化（如 DPI 缩放调整）会重建 dampedDrag，
    // 若本实例存活，position lambda 捕获的仍是旧 dampedDrag（其 value 已冻结）与旧 panelOffset
    // 委托，按压高亮不再跟手；以 dampedDrag 身份为键重建可同时刷新这两处捕获。
    val interactiveHighlight = remember(animationScope, tabWidthPx, dampedDrag) {
        LiquidInteractiveHighlight(
            animationScope = animationScope,
            position = { layerSize, _ ->
                Offset(
                    x = if (isLtr) {
                        (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        layerSize.width - (dampedDrag.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = layerSize.height / 2f,
                )
            },
        )
    }

    // 只持有 State，不在组合期读值；在下方 highlight lambda（绘制阶段）内消费，
    // 使传感器更新只触发 draw 失效而非整个底栏作用域的重组。
    val gravityAngle = rememberQuantizedGravityAngle()

    val combinedBackdrop = backdrop?.let { rememberCombinedBackdrop(it, tabsBackdrop) }

    val navBarBottomPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPaddingValue = if (navBarBottomPadding != 0.dp) 8.dp + navBarBottomPadding else 28.dp
    val containerHeight = 56.dp
    val indicatorHeight = 48.dp
    val preferredHorizontalPadding = 60.dp

    val tabsContent: @Composable RowScope.() -> Unit = {
        val tabScale = LocalIosTabScale.current
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = index == selectedIndex
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val activationKey = event.key == Key.Enter ||
                                event.key == Key.NumPadEnter || event.key == Key.Spacebar
                        if (activationKey) {
                            if (event.type == KeyEventType.KeyUp) activateTab(index)
                            true
                        } else {
                            false
                        }
                    }
                    .focusable()
                    .weight(1f)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = tabScale()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = CenterHorizontally,
            ) {
                BadgedBox(badge = { badge(index)?.invoke() }) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(bottom = bottomPaddingValue)
                .fillMaxWidth(),
        ) {
            val horizontalPadding = preferredHorizontalPadding.coerceAtMost(
                ((maxWidth - 48.dp * tabsCount - 8.dp) / 2).coerceAtLeast(0.dp),
            )
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(horizontalPadding)
                        .height(containerHeight)
                        .pointerInput(Unit) {
                            detectTapGestures { }
                        },
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                        Row(
                            modifier = Modifier
                                .selectableGroup()
                                .onSizeChanged { coords ->
                                    totalWidthPx = coords.width.toFloat()
                                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                                }
                                .graphicsLayer { translationX = panelOffset }
                                .dropShadow(
                                    shape = pillShape,
                                    shadow = Shadow(
                                        radius = 10.dp,
                                        color = Color.Black,
                                        alpha = if (isDark) 0.2f else 0.1f,
                                    ),
                                )
                                .then(
                                    if (isBlurActive && backdrop != null) {
                                        Modifier.drawBackdrop(
                                            backdrop = backdrop,
                                            shape = { pillShape },
                                            effects = {
                                                padding = maxOf(padding, 40.dp.toPx())
                                                vibrancy()
                                                blur(
                                                    4.dp.toPx(),
                                                    4.dp.toPx(),
                                                )
                                                lens(
                                                    refractionHeight = 24.dp.toPx(),
                                                    refractionAmount = 24.dp.toPx(),
                                                )
                                            },
                                            highlight = {
                                                gravityRotatedHighlight(iosIndicatorSpecular, gravityAngle.value, extraDegrees = -45f)
                                                    .copy(alpha = 0.75f)
                                            },
                                            layerBlock = {
                                                val width = size.width.coerceAtLeast(1f)
                                                val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDrag.pressProgress)
                                                scaleX = s
                                                scaleY = s
                                            },
                                            onDrawSurface = { drawRect(containerColor) },
                                        )
                                    } else {
                                        Modifier.background(containerColor, pillShape)
                                    },
                                )
                                .then(
                                    if (isBlurActive) {
                                        interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                                    } else Modifier
                                )
                                .then(dampedDrag.modifier)
                                .height(containerHeight)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = tabsContent,
                        )
                    }

                    if (isBlurActive && backdrop != null) {
                        CompositionLocalProvider(
                            LocalIosTabScale provides { lerp(1f, 1.2f, dampedDrag.pressProgress) },
                            LocalContentColor provides accentColor,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clearAndSetSemantics {}
                                    .alpha(0f)
                                    .layerBackdrop(tabsBackdrop)
                                    .graphicsLayer { translationX = panelOffset }
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { pillShape },
                                        effects = {
                                            vibrancy()
                                            blur(4.dp.toPx(), 4.dp.toPx())
                                            lens(
                                                refractionHeight = 24.dp.toPx(),
                                                refractionAmount = 24.dp.toPx(),
                                            )
                                        },
                                        onDrawSurface = { drawRect(containerColor) },
                                    )
                                    .then(interactiveHighlight.modifier)
                                    .height(indicatorHeight)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                content = tabsContent,
                            )
                        }
                    }

                    if (tabWidthPx > 0f) {
                        val tabWidthDp = with(density) { tabWidthPx.toDp() }
                        if (isBlurActive && combinedBackdrop != null) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .graphicsLayer {
                                        val singleTabWidth = tabWidthPx
                                        val progressOffset = dampedDrag.value * singleTabWidth
                                        translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                                    }
                                    // testTag 必须挂在 graphicsLayer 之后：语义节点的 bounds 取自该节点自身坐标，
                                    // 只有放在位移修饰符内侧，读取 bounds 才能看到滑块被平移到选中项上。
                                    .testTag("floatingBarIndicator")
                                    .drawBackdrop(
                                        backdrop = combinedBackdrop,
                                        shape = { pillShape },
                                        effects = {
                                            val progress = dampedDrag.pressProgress
                                            lens(
                                                refractionHeight = 10.dp.toPx() * progress,
                                                refractionAmount = 14.dp.toPx() * progress,
                                                depthEffect = true,
                                                chromaticAberration = 0.5f,
                                            )
                                        },
                                        highlight = {
                                            gravityRotatedHighlight(iosIndicatorSpecular, gravityAngle.value, extraDegrees = 90f)
                                                .copy(alpha = dampedDrag.pressProgress)
                                        },
                                        layerBlock = {
                                            scaleX = dampedDrag.scaleX
                                            scaleY = dampedDrag.scaleY
                                            val v = dampedDrag.velocity / 10f
                                            scaleX /= 1f - (v * 0.75f).coerceIn(-0.2f, 0.2f)
                                            scaleY *= 1f - (v * 0.25f).coerceIn(-0.2f, 0.2f)
                                        },
                                        onDrawSurface = {
                                            val progress = dampedDrag.pressProgress
                                            drawRect(
                                                color = if (!isDark) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
                                                alpha = 1f - progress,
                                            )
                                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                        },
                                    )
                                    .innerShadow(shape = pillShape) {
                                        InnerShadow(
                                            radius = 8.dp * dampedDrag.pressProgress,
                                            color = Color.Black.copy(alpha = 0.15f),
                                            alpha = dampedDrag.pressProgress,
                                        )
                                    }
                                    .height(indicatorHeight)
                                    .width(tabWidthDp),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .graphicsLayer {
                                        val progressOffset = dampedDrag.value * tabWidthPx
                                        translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                                    }
                                    .testTag("floatingBarIndicator")
                                    .clip(pillShape)
                                    .background(accentColor.copy(alpha = 0.15f), pillShape)
                                    .height(indicatorHeight)
                                    .width(tabWidthDp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                CompositionLocalProvider(LocalContentColor provides accentColor) {
                                    Row(
                                        modifier = Modifier
                                            .clearAndSetSemantics {}
                                            .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                            .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                            .height(indicatorHeight)
                                            .graphicsLayer {
                                                val progressOffset = dampedDrag.value * tabWidthPx
                                                translationX = if (isLtr) -progressOffset else progressOffset
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        content = tabsContent,
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .width(horizontalPadding)
                        .height(containerHeight)
                        .pointerInput(Unit) {
                            detectTapGestures { }
                        },
                )
            }
        }
    }
}
