package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.EdgeSwipeDismiss
import com.luckyzyx.luckytool.ui.compose.pages.HomePage
import com.luckyzyx.luckytool.ui.compose.pages.LogPage
import com.luckyzyx.luckytool.ui.compose.pages.OtherPage
import com.luckyzyx.luckytool.ui.compose.pages.SettingPage
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.shell.LocalEnableNavigationBadge
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import kotlinx.serialization.Serializable

// 类型安全路由（迁移方案 P2：MainShell 五个 tab，官方 recommended pattern）

@Serializable
data object HomeRoute

@Serializable
data object OtherRoute

@Serializable
data object FunctionRoute

@Serializable
data object LogRoute

@Serializable
data object SettingRoute

/** 主题与配色页（迁移 KernelSU colorpalette，Material 实现） */
@Serializable
data object ThemeRoute

/** 跨 tab 跳转请求：pageKey = ScopePageRegistry 页键，title = 目标页标题（可空） */
data class FunctionRequest(val pageKey: String, val title: String?)

private data class ShellTab(val labelRes: Int, val iconRes: Int, val route: Any)

@Composable
fun MainShell(activity: MainActivity) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onFunctionTab = currentDestination?.hasRoute<FunctionRoute>() == true
    val onHomeTab = currentDestination?.hasRoute<HomeRoute>() == true
    // 主题页为全屏子页：隐藏底部导航，避免与页内 ExpressiveScaffold 顶栏叠加
    val onThemePage = currentDestination?.hasRoute<ThemeRoute>() == true

    // 回传当前 tab 状态（MainActivity.onResume 显示恢复 / checkOs 判断）
    LaunchedEffect(onFunctionTab) { activity.isOnFunctionTab = onFunctionTab }
    LaunchedEffect(onHomeTab) { activity.isOnHomeTab = onHomeTab }

    // Function tab 由子树处理返回键：关闭 Compose 侧自动返回，避免双处理
    LaunchedEffect(onFunctionTab) { navController.enableOnBackPressed(!onFunctionTab) }

    // 跨 tab 跳转请求（Compose 页面 → Function 子树作用域页）：只切 tab，FunctionPage 消费执行
    LaunchedEffect(Unit) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                navController.navigate(FunctionRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!onThemePage) {
                NavigationBar {
                    val tabs = listOf(
                        ShellTab(R.string.nav_other, R.drawable.ic_baseline_dashboard_24, OtherRoute),
                        ShellTab(R.string.nav_function, R.drawable.ic_baseline_extension_24, FunctionRoute),
                        ShellTab(R.string.nav_home, R.drawable.ic_baseline_home_24, HomeRoute),
                        ShellTab(R.string.nav_log, R.drawable.ic_baseline_assignment_24, LogRoute),
                        ShellTab(R.string.nav_setting, R.drawable.ic_baseline_settings_24, SettingRoute),
                    )
                    val enableBadge = LocalEnableNavigationBadge.current
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hasRoute(tab.route::class) == true
                        // 角标数据源：主页发现新版本（对齐 KernelSU 导航角标开关）
                        val showBadge = enableBadge &&
                            tab.route == HomeRoute &&
                            ShellBadgeState.updateAvailable
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (showBadge) {
                                    BadgedBox(badge = { Badge { Text("1") } }) {
                                        Icon(painterResource(tab.iconRes), contentDescription = null)
                                    }
                                } else {
                                    Icon(painterResource(tab.iconRes), contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable<HomeRoute> { HomePage(activity) }
            composable<OtherRoute> { OtherPage(activity) }
            composable<FunctionRoute> {
                FunctionPage(
                    activity = activity,
                    onShellBack = { navController.popBackStack() },
                )
            }
            composable<LogRoute> { LogPage() }
            composable<SettingRoute> {
                SettingPage(
                    activity = activity,
                    onOpenTheme = { navController.navigate(ThemeRoute) { launchSingleTop = true } },
                )
            }
            composable<ThemeRoute> {
                EdgeSwipeDismiss(
                    enabled = LocalEnableSwipeDismiss.current,
                    onDismiss = { navController.popBackStack() },
                ) {
                    ThemeScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
