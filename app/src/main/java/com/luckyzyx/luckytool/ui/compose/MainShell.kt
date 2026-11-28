package com.luckyzyx.luckytool.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.itos.coui.icons.CouiIcons
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.activity.SecondaryActivity
import com.luckyzyx.luckytool.ui.compose.components.GateDialog
import com.luckyzyx.luckytool.ui.compose.components.RootScaffold
import com.luckyzyx.luckytool.ui.compose.components.currentUiStyle
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.luckyzyx.luckytool.ui.compose.components.material.LocalBottomBarPresent
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.pages.HomePage
import com.luckyzyx.luckytool.ui.compose.pages.OtherPage
import com.luckyzyx.luckytool.ui.compose.pages.SearchPage
import com.luckyzyx.luckytool.ui.compose.pages.SettingPage
import com.luckyzyx.luckytool.ui.compose.special.AppPermissionContent
import kotlin.system.exitProcess

/** 跨标签页打开作用域页时保留来源标签。 */
data class FunctionRequest(val pageKey: String, val title: String?)

internal enum class MainTab(val labelRes: Int) {
    Other(R.string.nav_other),
    Function(R.string.nav_function),
    Home(R.string.nav_home),
    Search(R.string.nav_search),
    Setting(R.string.nav_setting),
}

/**
 * 页签图标。COUI 图标是资源型向量，要在组合期按当前日夜配置解析，所以不能挂在枚举常量上。
 */
@Composable
internal fun MainTab.icon(): ImageVector = when (this) {
    MainTab.Other -> CouiIcons.Filled.Grid
    MainTab.Function -> CouiIcons.Outlined.SliderCustom
    MainTab.Home -> CouiIconsExt.Home
    MainTab.Search -> CouiIcons.Outlined.Search
    MainTab.Setting -> CouiIcons.Filled.Setting
}

/** 底栏页签图标的一对形态：未选中描边、选中实心。 */
internal class MainTabIcons(val unselected: ImageVector, val selected: ImageVector)

/**
 * 要表达选中态的底栏图标对。
 *
 * COUI 底栏在库内部画图标，染色固定成黑/白、没有逐项的透明度，改色调或渐变都没有入口，
 * 能区分选中态的只有换图形，所以未选中给描边、选中给实心。官方集里只有齿轮两态齐全，
 * 其余页签缺的那一半由 [CouiIconsExt] 补齐。
 */
@Composable
internal fun MainTab.iconPair(): MainTabIcons = when (this) {
    MainTab.Other -> MainTabIcons(CouiIconsExt.GridOutlined, CouiIcons.Filled.Grid)
    MainTab.Function -> MainTabIcons(CouiIcons.Outlined.SliderCustom, CouiIconsExt.SliderCustomFilled)
    MainTab.Home -> MainTabIcons(CouiIconsExt.Home, CouiIconsExt.HomeFilled)
    MainTab.Search -> MainTabIcons(CouiIcons.Outlined.Search, CouiIconsExt.SearchFilled)
    MainTab.Setting -> MainTabIcons(CouiIcons.Outlined.Setting, CouiIcons.Filled.Setting)
}

@Composable
fun MainShell(activity: MainActivity) {
    val pagerState = rememberPagerState(initialPage = MainTab.Home.ordinal) { MainTab.entries.size }
    val mainPagerState = rememberMainPagerState(pagerState)
    val settledTab = MainTab.entries[pagerState.settledPage]
    LaunchedEffect(settledTab) {
        activity.isOnFunctionTab = settledTab == MainTab.Function
        activity.isOnHomeTab = settledTab == MainTab.Home
    }
    LaunchedEffect(activity) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                activity.functionNavRequests.value = null
                SecondaryActivity.openScope(activity, request.pageKey, request.title.orEmpty())
            }
        }
    }

    // 功能页数据（功能树行 + 全屏搜索索引）在 App 打开时就后台预取：切到「功能」标签页时
    // 不再现算 50 页偏好 DSL + PackageManager IPC，避免首帧空白再 pop-in 的割裂感（见 FunctionPageData）
    val treeIconSize = with(LocalDensity.current) { 48.dp.roundToPx() }
    LaunchedEffect(activity, treeIconSize) { FunctionPageData.preload(activity, treeIconSize) }

    // 权限门先于启动门：权限页自己就把 Root / 模块（Xposed）状态写在页面上，不需要再多一个弹窗，
    // 启动门只服务主界面。反过来还会把用户困死 —— 这些对话框没有跳过出口（确定 = 退出进程），
    // 未授 Root 的新设备会从权限页被丢到主页，4 项权限从此没有授予入口。
    // 门的状态一直保留在 MainActivity 上，权限齐全后本函数继续往下走，GateDialog 在主界面照常弹出。
    if (activity.isPermissionGateActive) {
        PermissionGateScreen(activity)
        return
    }

    // 标签页不是页面栈：主页之外的标签先返回主页；二级页是独立 Activity，返回由系统接管。
    MainPagerBackHandler(
        state = mainPagerState,
        enabled = activity.gateDialog == null,
    )

    val shellContent: @Composable (PaddingValues) -> Unit = { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MainTabs(
                mainPagerState = mainPagerState,
                isActive = activity.gateDialog == null,
            ) { tab, isCurrentPage ->
                when (tab) {
                    MainTab.Home -> HomePage(activity, isCurrentPage)
                    MainTab.Other -> OtherPage(activity, isCurrentPage)
                    MainTab.Function -> FunctionPage(
                        activity = activity,
                        onOpenPage = { key, title -> SecondaryActivity.openScope(activity, key, title) },
                    )
                    MainTab.Search -> SearchPage(
                        isCurrentPage = isCurrentPage,
                        onOpen = { route -> SecondaryActivity.openScope(activity, route) },
                    )
                    MainTab.Setting -> SettingPage(activity) {
                        SecondaryActivity.openTheme(activity)
                    }
                }
            }
            activity.gateDialog?.let { spec ->
                GateDialog(
                    spec = spec,
                    onConfirm = { exitProcess(0) },
                    onNeutral = { activity.gateDialog = null },
                )
            }
        }
    }
    // 页面各自处理 insets；根壳按当前外观样式绘制（见 UiStyle）
    RootScaffold(content = shellContent)
}

@Composable
internal fun MainTabs(
    mainPagerState: MainPagerState,
    isActive: Boolean,
    pageContent: @Composable (tab: MainTab, isCurrentPage: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val tabs = MainTab.entries
    val selectedIndex = mainPagerState.selectedPage
    val onSelect: (Int) -> Unit = { index -> if (isActive) mainPagerState.animateToPage(index) }
    val tabStateHolder = rememberSaveableStateHolder()

    val shellBody: @Composable (PaddingValues, Dp) -> Unit = { innerPadding, shellInset ->
        // 底栏形态、正文采样源与「底栏是否盖住正文」都由外观样式决定（见 UiStyle.TabsShell）：
        // 底栏盖在正文上时 innerPadding 底部为 0，页面改按 shellInset 在末尾留白。
        // 输入法弹出时正文要让到键盘之上：可见高度由「正文底部留白」与「键盘高度」中较大的那个决定，
        // 两者相加会在键盘上方多留一条底栏高度的空白。底栏本身留在窗口底部（被键盘挡住）。
        val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val imeGap = (imeBottom - innerPadding.calculateBottomPadding()).coerceAtLeast(0.dp)
        CompositionLocalProvider(
            LocalBottomBarPresent provides true,
            LocalShellBottomInset provides shellInset,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = imeGap)
                    .consumeWindowInsets(innerPadding),
            ) {
                MainPager(
                    state = mainPagerState,
                    gesturesEnabled = isActive,
                    pageKey = { tabs[it].name },
                ) { index ->
                    val tab = tabs[index]
                    val isCurrentPage = isActive && index == mainPagerState.pagerState.settledPage &&
                        !mainPagerState.pagerState.isScrollInProgress
                    tabStateHolder.SaveableStateProvider(tab.name) {
                        pageContent(tab, isCurrentPage)
                    }
                }
            }
        }
    }
    // 切换一级标签外壳时搬移整份标签内容：页内 remember 状态随内容走，不重建。
    val currentShellBody = rememberUpdatedState(shellBody)
    val movableShellBody = remember {
        movableContentOf<Pair<PaddingValues, Dp>> { body -> currentShellBody.value(body.first, body.second) }
    }
    currentUiStyle().TabsShell(
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = Modifier,
        content = { innerPadding, shellInset -> movableShellBody(innerPadding to shellInset) },
    )
}


/**
 * 权限阻塞态：只渲染权限页内容（无底栏/抽屉），吞掉系统返回键、无跳过出口。
 * 由 [MainShell] 提前 return 后组合，因此根骨架一并被跳过；本函数自建容器提供
 * 同一套容器底色与 insets，弹层由 Compose Dialog 自己承载。
 */
@Composable
private fun PermissionGateScreen(activity: MainActivity) {
    // 阻塞态没有可返回的目的地：系统返回键无效
    BackHandler { }
    val gateContent: @Composable () -> Unit = {
        AppPermissionContent(
            activity = activity,
            blocking = true,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            // 4 项齐全：撤掉阻塞门回到主界面（不重启 Activity）
            onAllGranted = { activity.refreshPermissionGate() },
            onExit = { exitProcess(0) },
        )
    }
    val gateInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    currentUiStyle().GateShell(insets = gateInsets) { gateContent() }
}