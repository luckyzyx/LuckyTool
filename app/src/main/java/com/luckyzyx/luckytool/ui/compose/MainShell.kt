package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.EdgeSwipeDismiss
import com.luckyzyx.luckytool.ui.compose.components.material.LocalBottomBarPresent
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixBarItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixBottomBar
import com.luckyzyx.luckytool.ui.compose.pages.HomePage
import com.luckyzyx.luckytool.ui.compose.pages.LogPage
import com.luckyzyx.luckytool.ui.compose.pages.OtherPage
import com.luckyzyx.luckytool.ui.compose.pages.SettingPage
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBar
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBarBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableNavigationBadge
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

/** 主题与配色页（迁移 KernelSU colorpalette，Material / Miuix 双实现） */
@Serializable
data object ThemeRoute

/** 跨 tab 跳转请求：pageKey = ScopePageRegistry 页键，title = 目标页标题（可空） */
data class FunctionRequest(val pageKey: String, val title: String?)

/** 底栏条目：drawable 供 Material 底栏，ImageVector 供 Miuix 底栏 */
private data class ShellTab(
    val labelRes: Int,
    val iconRes: Int,
    val mIcon: ImageVector,
    val route: Any,
)

/** 悬浮胶囊底栏占位高度（对齐 FloatingBottomBar 的 64.dp 胶囊 + 12.dp 余量） */
private val FloatingBarInset = 76.dp

@Composable
fun MainShell(activity: MainActivity) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onFunctionTab = currentDestination?.hasRoute<FunctionRoute>() == true
    val onHomeTab = currentDestination?.hasRoute<HomeRoute>() == true
    // 主题页为全屏子页：隐藏底部导航，避免与页内 ExpressiveScaffold 顶栏叠加
    val onThemePage = currentDestination?.hasRoute<ThemeRoute>() == true

    val uiMode = LocalUiMode.current
    val isMiuix = uiMode == UiMode.Miuix
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val enableBadge = LocalEnableNavigationBadge.current

    val showBar = !onThemePage
    // 悬浮底栏：Miuix 外观线 + 开关开启（主题页自身不显示底栏）
    val floatingBar = isMiuix && showBar && enableFloatingBottomBar

    // 回传当前 tab 状态（MainActivity.onResume 显示恢复 / checkOs 判断）
    LaunchedEffect(onFunctionTab) { activity.isOnFunctionTab = onFunctionTab }
    LaunchedEffect(onHomeTab) { activity.isOnHomeTab = onHomeTab }

    // Function tab 由子树处理返回键：关闭 Compose 侧自动返回，避免双处理
    LaunchedEffect(onFunctionTab) { navController.enableOnBackPressed(!onFunctionTab) }

    // 跨 tab 跳转请求（Compose 页面 → Function 子树作用域页）：只切 tab，FunctionPage 消费执行
    // 注意：这里不 popUpTo 来源 tab，而是把 Function 压到当前栈顶，保留来源 tab（Other/Setting）
    // 在返回栈中，这样从作用域页返回时直接回到来源 tab，而不是先落到功能树再落到 Home。
    LaunchedEffect(Unit) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                navController.navigate(FunctionRoute) {
                    launchSingleTop = true
                }
            }
        }
    }

    val tabs = listOf(
        ShellTab(R.string.nav_other, R.drawable.ic_baseline_dashboard_24, Icons.Rounded.Dashboard, OtherRoute),
        ShellTab(R.string.nav_function, R.drawable.ic_baseline_extension_24, Icons.Rounded.Extension, FunctionRoute),
        ShellTab(R.string.nav_home, R.drawable.ic_baseline_home_24, Icons.Rounded.Home, HomeRoute),
        ShellTab(R.string.nav_log, R.drawable.ic_baseline_assignment_24, Icons.Rounded.Assignment, LogRoute),
        ShellTab(R.string.nav_setting, R.drawable.ic_baseline_settings_24, Icons.Rounded.Settings, SettingRoute),
    )
    val selectedIndex = tabs.indexOfFirst { currentDestination?.hasRoute(it.route::class) == true }
    val onSelect: (Int) -> Unit = { index ->
        val tab = tabs.getOrNull(index)
        if (tab != null) {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Miuix 外观线：模糊采样源（普通底栏背景模糊与悬浮胶囊液态玻璃共用）。
    // 关键约束：该图层只录制页面内容（录制修饰符挂在 NavHost 上），录制盒内
    // 绝不能包含任何绘制该图层的组件（底栏/胶囊），否则渲染树循环引用 →
    // RenderThread prepareTree 无限递归 → 原生栈溢出崩溃（SIGSEGV）。
    val backdropSurface = if (isMiuix) MiuixTheme.colorScheme.surface else Color.Unspecified
    val backdrop = if (isMiuix) {
        rememberLayerBackdrop {
            if (backdropSurface != Color.Unspecified) drawRect(backdropSurface)
            drawContent()
        }
    } else {
        null
    }
    // 普通底栏背景模糊：仅在模糊开关开启且 RenderEffect 可用时使用采样层
    val barBlurBackdrop = if (isMiuix && enableBlur && isRenderEffectSupported()) backdrop else null
    // 是否存在实际消费者需要录制背板：普通底栏模糊 或 悬浮胶囊液态玻璃
    val needBackdrop = backdrop != null &&
        ((barBlurBackdrop != null && !floatingBar) || (floatingBar && enableFloatingBottomBarBlur))

    val containerColor = if (isMiuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    // 根脚手架按外观线分派（对齐 KernelSU MainActivity「UiMode.Miuix -> Scaffold { navDisplay() }」）：
    // Miuix 脚手架的 popupHost 默认槽位即 MiuixPopupHost()，是本应用唯一能承载
    // OverlayDropdownPreference / OverlayDialog 的挂载点 —— 库中 LocalPopupStates /
    // LocalRootPopupStates 均为 internal，应用层无法自行提供，只能由根部 Miuix Scaffold 提供。
    // 因此各页保留自己的空 popupHost 槽位（库内 Miuix 页面同样如此），弹层统一冒泡到此处渲染。
    val rootBottomBar: @Composable () -> Unit = {
            when {
                // 主题页为全屏子页：不显示底栏
                !showBar -> Unit
                // 悬浮胶囊模式：胶囊在内容区上方单独绘制，底栏槽位留空
                floatingBar -> Unit
                isMiuix -> {
                    MiuixBottomBar(
                        items = tabs.mapIndexed { index, tab ->
                            MiuixBarItem(
                                label = stringResource(tab.labelRes),
                                icon = tab.mIcon,
                                badge = if (enableBadge && tab.route == HomeRoute && ShellBadgeState.updateAvailable) {
                                    { Badge { Text("1") } }
                                } else {
                                    null
                                },
                            )
                        },
                        selectedIndex = selectedIndex,
                        onSelected = onSelect,
                        blurBackdrop = barBlurBackdrop,
                        backdrop = backdrop ?: rememberLayerBackdrop { },
                        floating = false,
                        floatingBlur = false,
                    )
                }

                else -> {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hasRoute(tab.route::class) == true
                            // 角标数据源：主页发现新版本（对齐 KernelSU 导航角标开关）
                            val showBadge = enableBadge &&
                                tab.route == HomeRoute &&
                                ShellBadgeState.updateAvailable
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelect(tabs.indexOf(tab)) },
                                icon = {
                                    if (showBadge) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                                ) { Text("1") }
                                            },
                                        ) {
                                            Icon(painterResource(tab.iconRes), contentDescription = null)
                                        }
                                    } else {
                                        Icon(painterResource(tab.iconRes), contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
    }

    val shellContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        CompositionLocalProvider(
            // 底部导航栏已消费系统导航栏 inset：告知页面列表不要重复叠加底部内边距
            LocalBottomBarPresent provides showBar,
            // 悬浮胶囊覆盖在内容之上：列表额外预留胶囊高度，条目仍可滑到胶囊之下
            LocalShellBottomInset provides if (floatingBar) FloatingBarInset else 0.dp,
        ) {
            // 采样层录制盒是 NavHost（见上）：本 Box 只做普通容器，
            // 悬浮胶囊位于本 Box 内、录制盒之外，避免渲染树循环引用
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(
                            if (backdrop != null && needBackdrop) {
                                Modifier.layerBackdrop(backdrop)
                            } else {
                                Modifier
                            }
                        ),
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

                // 悬浮底栏（Apple 风格胶囊）：覆盖在内容之上，透明底 + 液态玻璃
                if (floatingBar) {
                    MiuixBottomBar(
                        items = tabs.map { tab ->
                            MiuixBarItem(
                                label = stringResource(tab.labelRes),
                                icon = tab.mIcon,
                                badge = if (enableBadge && tab.route == HomeRoute && ShellBadgeState.updateAvailable) {
                                    { Badge { Text("1") } }
                                } else {
                                    null
                                },
                            )
                        },
                        selectedIndex = selectedIndex,
                        onSelected = onSelect,
                        blurBackdrop = barBlurBackdrop,
                        backdrop = backdrop ?: rememberLayerBackdrop { },
                        floating = true,
                        floatingBlur = enableFloatingBottomBarBlur,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    // 切换外观线会在两套 Scaffold 之间切换组合位置（Miuix Scaffold ↔ material3 Scaffold）：
    // 直接切换会销毁重建其内容，NavHost / NavController / 返回栈 / 页面内 remember 全部丢失。
    // movableContentOf 把同一份 shell 内容在两者之间「搬移」，槽表（状态）随内容走、只换外观容器；
    // shellContent 通过 rememberUpdatedState 间接化，避免固化首次组合的 lambda 实例
    //（否则底栏选中态、悬浮开关等捕获值会被冻结）。
    val currentShellContent = rememberUpdatedState(shellContent)
    val movableShellContent = remember {
        movableContentOf<PaddingValues> { innerPadding -> currentShellContent.value(innerPadding) }
    }

    if (isMiuix) {
        top.yukonga.miuix.kmp.basic.Scaffold(
            containerColor = containerColor,
            bottomBar = rootBottomBar,
            content = movableShellContent,
        )
    } else {
        Scaffold(
            containerColor = containerColor,
            bottomBar = rootBottomBar,
            content = movableShellContent,
        )
    }
}
