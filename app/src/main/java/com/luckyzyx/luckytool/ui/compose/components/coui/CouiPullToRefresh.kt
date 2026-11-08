package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.LocalTopBarCollapsedFraction
import kotlin.math.roundToInt

/** 触发刷新所需的下拉位移。 */
private val CouiRefreshThreshold = 64.dp

/** 下拉位移的阻尼：手指走 2px 指示器才走 1px，避免一拽就过阈值。 */
private const val CouiPullDragCoefficient = 0.5f

/** 指示器最多能拉到的位置相对阈值的倍数。 */
private const val CouiPullMaxFactor = 1.6f

/** 指示器自身直径，用于把它从顶部内侧滑出来。 */
private val CouiRefreshIndicatorSize = 24.dp

/**
 * COUI 线的下拉刷新容器。
 *
 * 用嵌套滚动的 preScroll 累积下拉量：只有内容已经在顶部、再往上滚也没得滚的时候才吃掉这段位移，
 * 所以列表还能正常上滑时不会被这里拦住。松手（preFling）时位移到阈值就调 onRefresh，
 * 没到阈值就把位移清零，不做回弹动画。
 *
 * 注意：
 * - isRefreshing 由调用方维护，为 true 期间指示器一直停在下拉满的位置；变回 false 后位移清零。
 * - 指示器只是叠在内容顶部，不参与布局，内容不会因为下拉被挤下去。
 * - 手势结束完全依赖嵌套滚动的 fling 回调，容器内部必须有可滚动的子内容（LazyColumn 等），
 *   否则没有滚动节点派发这些回调。
 */
@Composable
internal fun CouiPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { CouiRefreshThreshold.toPx() }
    val indicatorSizePx = with(density) { CouiRefreshIndicatorSize.toPx() }
    val pull = remember { mutableFloatStateOf(0f) }
    val atTop = remember { mutableStateOf(true) }
    val currentOnRefresh = rememberUpdatedState(onRefresh)
    // 顶栏还没完全展开时把手势让给顶栏，否则会顶住展开动作、误触发下拉动画。
    val canPullDown = rememberUpdatedState(LocalTopBarCollapsedFraction.current <= 0f)
    val connection = remember(thresholdPx) {
        CouiPullToRefreshConnection(
            pull = pull,
            atTop = atTop,
            thresholdPx = thresholdPx,
            canPullDown = canPullDown,
            onRefresh = currentOnRefresh,
        )
    }
    // 刷新开始后把指示器顶到阈值位置，刷新结束后收回。
    LaunchedEffect(isRefreshing) {
        pull.floatValue = if (isRefreshing) thresholdPx else 0f
    }
    val visiblePull = if (isRefreshing) maxOf(pull.floatValue, thresholdPx) else pull.floatValue
    Box(modifier = modifier.nestedScroll(connection)) {
        content()
        if (visiblePull > 0f) {
            CouiCircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (visiblePull - indicatorSizePx).roundToInt()) }
                    .size(CouiRefreshIndicatorSize),
            )
        }
    }
}

/**
 * 下拉位移的累积与松手判定。
 *
 * atTop 由上一次 onPostScroll 的剩余量推断：子内容吃完还能剩下向下的位移，说明它已经到顶；
 * 这一帧的 preScroll 据此决定要不要吃掉这次下拉。
 */
private class CouiPullToRefreshConnection(
    private val pull: MutableFloatState,
    private val atTop: MutableState<Boolean>,
    private val thresholdPx: Float,
    private val canPullDown: State<Boolean>,
    private val onRefresh: State<() -> Unit>,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        if (available.y < 0f) {
            // 反向上推时先把已累积的下拉量收回去，松手前不会一直保持长位移。
            if (pull.floatValue <= 0f) return Offset.Zero
            val consumed = (-available.y).coerceAtMost(pull.floatValue)
            pull.floatValue -= consumed
            return Offset(0f, -consumed)
        }
        if (available.y <= 0f || !atTop.value || !canPullDown.value) return Offset.Zero
        pull.floatValue = (pull.floatValue + available.y * CouiPullDragCoefficient)
            .coerceAtMost(thresholdPx * CouiPullMaxFactor)
        return Offset(0f, available.y)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) {
            atTop.value = pull.floatValue > 0f || available.y > 0f
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pull.floatValue >= thresholdPx && canPullDown.value) onRefresh.value()
        pull.floatValue = 0f
        return Velocity.Zero
    }
}
