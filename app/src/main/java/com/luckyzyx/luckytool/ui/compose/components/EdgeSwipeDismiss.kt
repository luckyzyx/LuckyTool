package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 边缘横移返回（迁移 KernelSU 的 swipe dismiss 能力，纯 Compose 实现）。
 *
 * 只有在 [edgeWidth] 边缘区内按下、且手势以横向滑动开始（横向位移超过 touchSlop 且大于纵向位移）
 * 时才接管指针；纵向滑动与边缘区外的滑动一律交还给子级，
 * 因此页内的横向滚动容器（如主题页色板 LazyRow）与纵向列表都不会被误伤。
 *
 * 两点实现约束：
 * 1. 手势过程只写普通状态 —— `AwaitPointerEventScope` 是 restricted suspension 作用域，
 *    不能在内部调用 Animatable 的挂起方法，回弹动画交给组合作用域的协程驱动；
 * 2. 无论 [enabled] 与否都保持同一层级的 Box 容器结构（仅条件挂载手势 modifier），
 *    否则开关切换会重建子树、导致内部 NavHost 状态丢失。
 *
 * [edgeWidth] 默认 14.dp：LuckyTool 各页面容器统一使用 16.dp 横向内边距，
 * 热区小于该内边距即可保证不会与卡片内的滑杆、色板 LazyRow 等横向手势竞争。
 */
@Composable
fun EdgeSwipeDismiss(
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    edgeWidth: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var releaseJob by remember { mutableStateOf<Job?>(null) }
    val edgePx = with(LocalDensity.current) { edgeWidth.toPx() }

    LaunchedEffect(enabled) {
        if (!enabled) {
            releaseJob?.cancel()
            offsetX = 0f
        }
    }

    val gestureModifier = if (enabled) {
        Modifier.pointerInput(edgePx, onDismiss) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                // 边缘区外按下：不参与本次手势
                if (down.position.x > edgePx) {
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    return@awaitEachGesture
                }

                val slop = viewConfiguration.touchSlop
                val widthPx = size.width.toFloat()
                var started = false
                var total = 0f

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUpIgnoreConsumed()) break

                    val delta = change.positionChangeIgnoreConsumed()
                    if (!started) {
                        if (abs(delta.x) > slop && abs(delta.x) > abs(delta.y)) {
                            started = true
                            total = 0f
                        } else if (abs(delta.y) > slop) {
                            // 纵向手势：交还子级
                            break
                        } else {
                            continue
                        }
                    }

                    change.consume()
                    total = (total + delta.x).coerceAtLeast(0f)
                    offsetX = total
                }

                releaseJob?.cancel()
                if (started && total > widthPx * 0.3f) {
                    // 已越过阈值：直接返回，无需回落动画（页面随即出栈）
                    onDismiss()
                    offsetX = 0f
                } else if (offsetX > 0f) {
                    val start = offsetX
                    releaseJob = scope.launch {
                        animate(
                            initialValue = start,
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ) { value, _ -> offsetX = value }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .then(gestureModifier),
    ) {
        content()
    }
}
