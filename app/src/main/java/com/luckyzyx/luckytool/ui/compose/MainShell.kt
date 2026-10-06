package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.luckyzyx.luckytool.ui.compose.pages.HomePage
import com.luckyzyx.luckytool.ui.compose.pages.LogPage
import com.luckyzyx.luckytool.ui.compose.pages.OtherPage
import com.luckyzyx.luckytool.ui.compose.pages.SettingPage
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

private data class ShellTab(val labelRes: Int, val iconRes: Int, val route: Any)

@Composable
fun MainShell(activity: MainActivity) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onFunctionTab = currentDestination?.hasRoute<FunctionRoute>() == true
    val onHomeTab = currentDestination?.hasRoute<HomeRoute>() == true

    // 回传当前 tab 状态（MainActivity.onResume 显示恢复 / checkOs 判断）
    LaunchedEffect(onFunctionTab) { activity.isOnFunctionTab = onFunctionTab }
    LaunchedEffect(onHomeTab) { activity.isOnHomeTab = onHomeTab }

    // Function tab 由子树处理返回键：关闭 Compose 侧自动返回，避免双处理
    LaunchedEffect(onFunctionTab) { navController.enableOnBackPressed(!onFunctionTab) }

    // 跨 tab 跳转请求（Compose 页面 → 旧功能树页面）：先收养子树再导航
    LaunchedEffect(Unit) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                activity.setPendingFunctionNavigation(request.first, request.second)
                activity.functionNavRequests.value = null
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
            NavigationBar {
                val tabs = listOf(
                    ShellTab(R.string.nav_other, R.drawable.ic_baseline_dashboard_24, OtherRoute),
                    ShellTab(R.string.nav_function, R.drawable.ic_baseline_extension_24, FunctionRoute),
                    ShellTab(R.string.nav_home, R.drawable.ic_baseline_home_24, HomeRoute),
                    ShellTab(R.string.nav_log, R.drawable.ic_baseline_assignment_24, LogRoute),
                    ShellTab(R.string.nav_setting, R.drawable.ic_baseline_settings_24, SettingRoute),
                )
                tabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.route::class) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
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
                    functionNavController = activity.functionNavController,
                    onShellBack = { navController.popBackStack() },
                )
            }
            composable<LogRoute> { LogPage() }
            composable<SettingRoute> { SettingPage(activity) }
        }
    }
}
