package com.luckyzyx.luckytool.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 任意已注册的功能页。二级页迁移到独立 Activity 后不再是导航目的地，
 * 只是把打开一个作用域页所需的参数从主界面带到 SecondaryActivity。
 *
 * [scrollKey] / [scrollPosition] 是搜索命中普通偏好时的页内定位参数：
 * 进页面后滚到该条目（[scrollPosition] 为条目序号），不为空时才生效。
 */
data class ScopeRoute(
    val pageKey: String,
    val title: String = "",
    val scrollKey: String = "",
    val scrollPosition: Int = -1,
)

/**
 * 侧滑返回：页面跟随手指横向位移，越过宽度 30% 或快速甩动即出栈，否则回弹。
 *
 * 二级页迁移到独立 Activity 后暂时没有调用点；保留实现，待侧滑返回修复后
 * 接回二级页（出栈动作改成 finish()）。
 */
@Composable
private fun SwipeBackPage(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!LocalEnableSwipeDismiss.current) {
        content()
        return
    }
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val limit = constraints.maxWidth.toFloat()
        Box(
            Modifier
                .fillMaxSize()
                .offset {
                    val distance = if (isLtr) dragOffset.value else -dragOffset.value
                    IntOffset(distance.roundToInt(), 0)
                }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val forward = if (isLtr) delta else -delta
                        val next = (dragOffset.value + forward).coerceIn(0f, limit)
                        scope.launch { dragOffset.snapTo(next) }
                    },
                    onDragStopped = { velocity ->
                        val forwardVelocity = if (isLtr) velocity else -velocity
                        if (dragOffset.value > limit * 0.3f || forwardVelocity > 1500f) {
                            onBack()
                            dragOffset.snapTo(0f)
                        } else {
                            dragOffset.animateTo(0f)
                        }
                    },
                ),
        ) {
            content()
        }
    }
}
