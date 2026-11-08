package com.luckyzyx.luckytool.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * 页面路由。类型安全导航的目的地：路由是数据类，参数随路由一起保存与恢复，
 * 重建 Activity 后回到同一页面。
 */
@Serializable
sealed interface AppRoute

/** 一级标签外壳（首页 / 功能 / 搜索 / 其他 / 设置都在它内部切换）。 */
@Serializable
data object MainRoute : AppRoute

/** 主题设置页。 */
@Serializable
data object ThemeRoute : AppRoute

/**
 * 任意已注册的功能页。
 *
 * [scrollKey] / [scrollPosition] 是搜索命中普通偏好时的页内定位参数：
 * 进页面后滚到该条目（[scrollPosition] 为条目序号），不为空时才生效。
 */
@Serializable
data class ScopeRoute(
    val pageKey: String,
    val title: String = "",
    val scrollKey: String = "",
    val scrollPosition: Int = -1,
) : AppRoute

/**
 * 入栈一个页面。
 *
 * 同一个页面已经停在栈顶时只保留一份：连点图标或列表行不会叠出多层相同的返回。
 */
internal fun NavHostController.navigatePage(route: AppRoute) {
    val topEntry = currentBackStackEntry
    val topDestination = topEntry?.destination
    // singleTop 只对「同一个页面」生效：目的地类相同还不够（所有 ScopeRoute 共用一个目的地），
    // 不同参数的功能页必须正常入栈，否则 A→B 会把 A 原地替换，返回时直接跳回主界面
    val topIsSamePage = when (route) {
        is MainRoute -> topDestination?.hasRoute<MainRoute>() == true
        is ThemeRoute -> topDestination?.hasRoute<ThemeRoute>() == true
        is ScopeRoute -> topDestination?.hasRoute<ScopeRoute>() == true &&
            topEntry.toRoute<ScopeRoute>() == route
    }
    navigate(route) { launchSingleTop = topIsSamePage }
}

/** 一次转场的时长：入栈与出栈共用，保持前进/后退手感一致。 */
private const val PageTransitionMillis = 260

/**
 * 导航宿主：唯一的 Activity 内部由一个 NavHost 承载全部页面。
 *
 * 前进时新页面从行尾侧滑入、旧页面向对侧退让一小段；后退时反向播放。
 * 系统返回键由 NavHost 自己接管；预测性返回手势在拖动过程中用同一组转场，
 * 当前页不透明地在顶层滑走（预测性转场不加淡入淡出）。
 * 页内横向拖拽的侧滑返回挂在每个非根目的地上，见 [SwipeBackPage]。
 */
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
    themeContent: @Composable () -> Unit,
    scopeContent: @Composable (ScopeRoute) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = MainRoute,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(tween(PageTransitionMillis)) { width -> width } +
                fadeIn(tween(PageTransitionMillis))
        },
        exitTransition = {
            slideOutHorizontally(tween(PageTransitionMillis)) { width -> -width / 4 } +
                fadeOut(tween(PageTransitionMillis))
        },
        popEnterTransition = {
            slideInHorizontally(tween(PageTransitionMillis)) { width -> -width / 4 } +
                fadeIn(tween(PageTransitionMillis))
        },
        popExitTransition = {
            slideOutHorizontally(tween(PageTransitionMillis)) { width -> width } +
                fadeOut(tween(PageTransitionMillis))
        },
        // 预测性返回拖动过程：只做位移，不加淡入淡出，保证当前页不透明地滑走
        predictivePopEnterTransition = {
            slideInHorizontally(tween(PageTransitionMillis)) { width -> -width / 4 }
        },
        predictivePopExitTransition = {
            slideOutHorizontally(tween(PageTransitionMillis)) { width -> width }
        },
    ) {
        // 根目的地不接侧滑返回：一级标签之间不是页面栈
        composable<MainRoute> { mainContent() }
        composable<ThemeRoute> {
            SwipeBackPage(onBack = { navController.popBackStack() }) { themeContent() }
        }
        composable<ScopeRoute> { entry ->
            val route = entry.toRoute<ScopeRoute>()
            SwipeBackPage(onBack = { navController.popBackStack() }) { scopeContent(route) }
        }
    }
}

/**
 * 侧滑返回：页面跟随手指横向位移，越过宽度 30% 或快速甩动即出栈，否则回弹。
 *
 * 只在偏好里开启侧滑返回时接管手势；页面内横向可滚动控件
 * 会先消费拖拽，因此滑块一类控件不受影响。
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
