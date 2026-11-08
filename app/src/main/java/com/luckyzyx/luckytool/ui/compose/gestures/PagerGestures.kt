package com.luckyzyx.luckytool.ui.compose.gestures

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

/** 分页落位弹簧：不给超调，避免页面越过目标再回弹。 */
internal val PagerSnapSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 460f,
)

/**
 * 只让页面内容处理纵向速度，横向速度留给分页器自己。
 * 用于拖拽已由 [crossAxisPagerDrag] 接管的分页器，避免两套手势同时改页面位置。
 */
internal object VerticalScrollOnlyConnection : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        Velocity(available.x, 0f)
}

/** 判定横向拖拽所需的甩动速度，低于它的松手按位置就近落位。 */
private val PagerMinimumFlingVelocity = 400.dp

/**
 * 直接用指针事件拖动分页器，而不是交给分页器自己的手势判定。
 *
 * 好处是横向拖拽能抢在子级列表的惯性与回弹之前生效：列表还在滑（或正在从顶端回弹）时，
 * 一次横向拖动就能翻页；纵向拖动仍然整颗让给子级，分页器不介入。
 * 使用时要把分页器的 userScrollEnabled 关掉、pageNestedScrollConnection 换成
 * [VerticalScrollOnlyConnection]，否则两套手势会互相打架。
 */
internal fun Modifier.crossAxisPagerDrag(
    pagerState: PagerState,
    scope: CoroutineScope,
    enabled: Boolean = true,
    rtl: Boolean = false,
    onIntercepted: (() -> Unit)? = null,
): Modifier {
    if (!enabled) return this
    return pointerInput(pagerState, onIntercepted, rtl) {
            val touchSlop = viewConfiguration.touchSlop
            val minimumVelocity = PagerMinimumFlingVelocity.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val tracker = VelocityTracker()
                tracker.addPointerInputChange(down)
                var pointerId = down.id
                var accumulated = Offset.Zero
                var dragging = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.fastFirstOrNull { it.id == pointerId } ?: break
                    if (change.isConsumed) break
                    if (!change.pressed) {
                        val replacement = event.changes.fastFirstOrNull { it.pressed }
                        if (replacement != null) {
                            pointerId = replacement.id
                            tracker.resetTracking()
                            tracker.addPointerInputChange(replacement)
                            continue
                        }
                        tracker.addPointerInputChange(change)
                        if (dragging) {
                            change.consume()
                            val sign = pagerState.scrollSign(rtl)
                            val velocity = tracker.calculateVelocity().x * sign
                            scope.launch { pagerState.settle(velocity, minimumVelocity) }
                        }
                        break
                    }
                    tracker.addPointerInputChange(change)
                    val delta = change.position - change.previousPosition
                    accumulated += delta
                    if (!dragging) {
                        val dx = abs(accumulated.x)
                        val dy = abs(accumulated.y)
                        if (dx > touchSlop && dx > dy) {
                            dragging = true
                            onIntercepted?.invoke()
                            change.consume()
                            // 把越过触摸阈值的那一段也算进拖拽，手感才跟手。
                            val overSlop = accumulated.x - sign(accumulated.x) * touchSlop
                            pagerState.dispatchRawDelta(overSlop * pagerState.scrollSign(rtl))
                        } else if (dy > touchSlop) {
                            // 纵向手势交给页面内容，分页器退出本次判定。
                            break
                        }
                    } else {
                        change.consume()
                        pagerState.dispatchRawDelta(delta.x * pagerState.scrollSign(rtl))
                    }
                }
            }
        }
}

/** 拖拽方向到分页偏移的符号：RTL 或反向布局时取反。 */
private fun PagerState.scrollSign(rtl: Boolean): Float =
    if (rtl.xor(layoutInfo.reverseLayout)) 1f else -1f

/** 按松手速度决定翻到哪一页，速度不够则就近落位。 */
private suspend fun PagerState.settle(velocity: Float, minimumVelocity: Float) {
    val position = currentPage + currentPageOffsetFraction
    val target = when {
        velocity > minimumVelocity -> ceil(position).toInt()
        velocity < -minimumVelocity -> floor(position).toInt()
        else -> position.roundToInt()
    }
    if (pageCount == 0) return
    animateScrollToPage(target.coerceIn(0, pageCount - 1), animationSpec = PagerSnapSpring)
}

/**
 * 记录子级列表是否正在惯性滚动，并让下一次横向手势只停住惯性、不翻页。
 *
 * [isChildFlinging] 由子级的 fling 事件维护；[haltFling] 置位后子级的惯性会在下一帧被取消，
 * 于是第一次横向滑动只把列表停住，用户再滑一次才翻页。
 */
internal class ChildFlingTracker : NestedScrollConnection {
    var isChildFlinging by mutableStateOf(false)
        internal set

    var haltFling by mutableStateOf(false)
        internal set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0f) {
            haltFling = false
            isChildFlinging = false
        } else if (source == NestedScrollSource.SideEffect && available.y != 0f &&
            isChildFlinging && haltFling
        ) {
            isChildFlinging = false
            // 消费惯性位移只会让惯性少走一帧，取消这次滚动才能真正停住。
            throw CancellationException("child fling halted by pager gesture")
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        haltFling = false
        isChildFlinging = available.y != 0f
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val halted = if (haltFling) Velocity(0f, available.y) else Velocity.Zero
        isChildFlinging = false
        haltFling = false
        return halted
    }
}

/**
 * 子级惯性滚动中按下并横向拖动时，吃掉这次手势，只把惯性停住。
 * 子级没有惯性时不做任何事，横向滑动仍由分页器自己处理。
 */
internal fun Modifier.iosStyleMomentumHalt(
    tracker: ChildFlingTracker,
    enabled: Boolean = true,
    onHalted: (() -> Unit)? = null,
): Modifier {
    if (!enabled) return this
    return nestedScroll(tracker).pointerInput(tracker, onHalted) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (!tracker.isChildFlinging) return@awaitEachGesture
            tracker.haltFling = true
            onHalted?.invoke()
            var halting = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.fastFirstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = abs(change.position.x - down.position.x)
                val dy = abs(change.position.y - down.position.y)
                if (!halting && dx > touchSlop && dx > dy * 2f) halting = true
                if (halting) {
                    change.consume()
                } else if (dy > touchSlop && dy > dx) {
                    tracker.haltFling = false
                    break
                }
            }
        }
    }
}
