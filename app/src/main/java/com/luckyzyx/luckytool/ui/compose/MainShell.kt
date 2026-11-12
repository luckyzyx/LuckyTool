package com.luckyzyx.luckytool.ui.compose

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luckyzyx.luckytool.R
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.ChromeInsets
import com.luckyzyx.luckytool.ui.compose.components.GateDialog
import com.luckyzyx.luckytool.ui.compose.components.LocalCouiTabsBarSlot
import com.luckyzyx.luckytool.ui.compose.components.RootScaffold
import com.luckyzyx.luckytool.ui.compose.components.TabsScaffold
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiBadge
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiBadgedBox
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialFloatingBottomBar
import com.luckyzyx.luckytool.ui.compose.components.material.LocalBottomBarPresent
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.pages.HomePage
import com.luckyzyx.luckytool.ui.compose.pages.OtherPage
import com.luckyzyx.luckytool.ui.compose.pages.SearchPage
import com.luckyzyx.luckytool.ui.compose.pages.SettingPage
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.compose.special.AppPermissionContent
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBar
import com.luckyzyx.luckytool.ui.shell.LocalFloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.LocalEnableNavigationBadge
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.couiNavigationFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar as CouiNavigationBar
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
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

@Composable
fun MainShell(activity: MainActivity) {
    val navController = rememberNavController()
    val pagerState = rememberPagerState(initialPage = MainTab.Home.ordinal) { MainTab.entries.size }
    val mainPagerState = rememberMainPagerState(pagerState)
    val settledTab = MainTab.entries[pagerState.settledPage]
    // 标签外壳是导航起点：栈顶回到 MainRoute 时一级标签才算「在场」
    val backStackEntry by navController.currentBackStackEntryAsState()
    val atMain = backStackEntry == null ||
        backStackEntry?.destination?.hasRoute<MainRoute>() == true
    LaunchedEffect(atMain, settledTab) {
        activity.isOnFunctionTab = atMain && settledTab == MainTab.Function
        activity.isOnHomeTab = atMain && settledTab == MainTab.Home
    }
    LaunchedEffect(activity, navController) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                activity.functionNavRequests.value = null
                navController.navigatePage(ScopeRoute(request.pageKey, request.title.orEmpty()))
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

    // 标签页不是页面栈：主页之外的标签先返回主页，页面返回由 NavHost 接管。
    MainPagerBackHandler(
        state = mainPagerState,
        enabled = atMain && activity.gateDialog == null,
    )

    val shellContent: @Composable (PaddingValues) -> Unit = { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AppNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                mainContent = {
                    MainTabs(
                        mainPagerState = mainPagerState,
                        isActive = atMain && activity.gateDialog == null,
                    ) { tab, isCurrentPage ->
                        when (tab) {
                            MainTab.Home -> HomePage(activity, isCurrentPage)
                            MainTab.Other -> OtherPage(activity, isCurrentPage)
                            MainTab.Function -> FunctionPage(
                                activity = activity,
                                onOpenPage = { key, title -> navController.navigatePage(ScopeRoute(key, title)) },
                            )
                            MainTab.Search -> SearchPage(
                                isCurrentPage = isCurrentPage,
                                onOpen = { route -> navController.navigatePage(route) },
                            )
                            MainTab.Setting -> SettingPage(activity) {
                                navController.navigatePage(ThemeRoute)
                            }
                        }
                    }
                },
                themeContent = {
                    ThemeScreen(onBack = { navController.popBackStack() })
                },
                scopeContent = { route ->
                    ScopePageHost(
                        activity = activity,
                        route = route,
                        onBack = { navController.popBackStack() },
                        onNavigate = { key, title ->
                            navController.navigatePage(ScopeRoute(key, title.orEmpty()))
                        },
                    )
                },
            )
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
    val uiMode = LocalUiMode.current
    val isCoui = uiMode == UiMode.Coui
    val enableBlur = LocalEnableBlur.current
    val floatingStyle = LocalFloatingBottomBarStyle.current
    val density = LocalDensity.current
    var floatingBarHeight by remember(floatingStyle, density) { mutableStateOf(76.dp) }
    val enableBadge = LocalEnableNavigationBadge.current
    val floatingBar = LocalEnableFloatingBottomBar.current
    val tabs = MainTab.entries
    val selectedIndex = mainPagerState.selectedPage
    val onSelect: (Int) -> Unit = { index -> if (isActive) mainPagerState.animateToPage(index) }
    val tabStateHolder = rememberSaveableStateHolder()

    // 只录制页面内容，底栏/胶囊必须在采样层之外，避免渲染树循环引用。
    // COUI 悬浮底栏的采样源由标签外壳的 COUI Scaffold 提供（正文整屏录入），这里只建 Material 的模糊源。
    val materialBackdropSurface = MaterialTheme.colorScheme.surfaceContainer
    val backdrop = if (!isCoui && floatingBar && enableBlur && isRenderEffectSupported()) {
        rememberLayerBackdrop {
            drawRect(materialBackdropSurface)
            drawContent()
        }
    } else {
        null
    }
    val barBlurBackdrop = if (enableBlur && isRenderEffectSupported()) backdrop else null
    val needBackdrop = barBlurBackdrop != null
    // 材质的浮动绘制器从 34 起可用，取不到就退回 Material 底栏。
    val couiRenderer = if (isCoui && Build.VERSION.SDK_INT >= 34) {
        remember { runCatching { CouiFloatingRenderer() }.getOrNull() }
    } else {
        null
    }
    // 底栏角标数据源：主页发现新版本。
    val showUpdateBadge = enableBadge && ShellBadgeState.updateAvailable
    val rootBottomBar: @Composable () -> Unit = {
        when (uiMode) {
            UiMode.Coui -> CouiTabsBottomBar(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                showUpdateBadge = showUpdateBadge,
                renderer = couiRenderer,
            )

            UiMode.Material -> {
                when {
                    // 悬浮胶囊模式：胶囊在内容区上方单独绘制，底栏槽位留空。
                    floatingBar -> Unit
                    else -> {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp,
                        ) {
                            tabs.forEach { tab ->
                                val selected = selectedIndex == tab.ordinal
                                // 角标数据源：主页发现新版本。
                                val showBadge = enableBadge &&
                                    tab == MainTab.Home &&
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
                                                Icon(tab.icon(), contentDescription = null, modifier = Modifier.size(24.dp))
                                            }
                                        } else {
                                            Icon(tab.icon(), contentDescription = null, modifier = Modifier.size(24.dp))
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
        }
    }

    val shellContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        // COUI 悬浮底栏占的是 Scaffold 的栏位，正文不必被栏位截短：正文按整屏铺开，页面列表
        // 在内容尾部按 LocalShellBottomInset 留白，滚动中的行从底栏底下穿过，胶囊才能透出下层 UI。
        val couiOverlayBar = isCoui && couiRenderer != null && LocalCouiTabsBarSlot.current
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = if (couiOverlayBar) 0.dp else innerPadding.calculateBottomPadding(),
        )
        // 输入法弹出时正文要让到键盘之上：可见高度由「正文底部留白」与「键盘高度」中较大的那个决定，
        // 两者相加会在键盘上方多留一条底栏高度的空白。底栏本身留在窗口底部（被键盘挡住）。
        val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val imeGap = (imeBottom - contentPadding.calculateBottomPadding()).coerceAtLeast(0.dp)
        CompositionLocalProvider(
            LocalBottomBarPresent provides true,
            LocalShellBottomInset provides when {
                // 悬浮底栏盖在页面之上：页面要多留一整个底栏高度，最后一条才不会被盖住
                couiOverlayBar -> innerPadding.calculateBottomPadding()
                floatingBar && !isCoui ->
                    (floatingBarHeight - innerPadding.calculateBottomPadding()).coerceAtLeast(0.dp)
                else -> 0.dp
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(bottom = imeGap)
                        .consumeWindowInsets(innerPadding)
                        .then(if (backdrop != null && needBackdrop) Modifier.layerBackdrop(backdrop) else Modifier),
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

                // 悬浮底栏位于背景采样层之外，页面按实际测量高度留出滚动空间。
                // COUI 底栏已经在底栏槽位里，这里不再叠一层。
                if (floatingBar && !isCoui) {
                    val barModifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { floatingBarHeight = with(density) { it.height.toDp() } }
                    MaterialFloatingBottomBar(
                        selectedIndex = selectedIndex,
                        onSelected = onSelect,
                        showUpdateBadge = enableBadge && ShellBadgeState.updateAvailable,
                        backdrop = barBlurBackdrop,
                        floatingStyle = floatingStyle,
                        modifier = barModifier,
                    )
                }
            }
        }
    }
    // 切换一级标签外壳时搬移整份标签内容：页内 remember 状态随内容走，不重建。
    val currentShellContent = rememberUpdatedState(shellContent)
    val movableShellContent = remember {
        movableContentOf<PaddingValues> { padding -> currentShellContent.value(padding) }
    }
    TabsScaffold(bottomBar = rootBottomBar, content = movableShellContent)
}

/** 底栏两端附件位占掉的宽度：每侧 56dp 圆 + 12dp 间距。 */
private val CouiBarAttachmentSpace = 136.dp

/**
 * Coui 线底栏：有背景渲染器时用悬浮胶囊底栏，取不到渲染器时退回同一套配色的平铺底栏。
 *
 * 渲染器只在 Android 14 及以上能构造成功，低版本与测试渲染环境里构造失败（返回 null），
 * 而悬浮底栏缺了渲染器就只能在构造期抛异常；因此这里必须留一条不依赖渲染器的路径，
 * 否则 Coui 线会掉回 Material 底栏，把 material3 组件带进 Coui 渲染树。
 */
@Composable
private fun CouiTabsBottomBar(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showUpdateBadge: Boolean,
    renderer: CouiFloatingRenderer?,
) {
    // 悬浮底栏要采样整块页面：采样源由标签外壳的 COUI Scaffold 底栏槽位提供，
    // 不在槽位里（例如单独组合标签外壳）拿不到，只能退回平铺底栏。
    if (renderer != null && LocalCouiTabsBarSlot.current) {
        // 胶囊宽度由 kit 按「可用宽 - 两端附件位」算出，附件位不挂也照样占位（合计 136dp），
        // 本应用恒有 5 个页签，扣完只剩屏宽六成、页签压到约 37dp。这里把这段预留补回再居中，
        // 胶囊回到整条可用宽度；多出来的部分落在屏幕外，不参与绘制与点击。
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 宽上限取 kit 自己的菜单上限，页签不足 5 个时胶囊按设计宽度居中，不会被推到一侧。
            val kitMenuMaxWidth = 24.dp + 70.dp * tabs.size + 8.dp * (tabs.size - 1)
            val barWidth = (maxWidth + CouiBarAttachmentSpace)
                .coerceAtMost(kitMenuMaxWidth + CouiBarAttachmentSpace + 32.dp)
            CouiNavigationBar(
                modifier = Modifier.requiredWidth(barWidth),
                renderer = renderer,
                // COUI 图标集没有选中/未选中成对的页签图标，两态共用同一个图形，
                // 选中态由底栏用文字透明度区分。
                items = tabs.map { tab ->
                    CouiNavigationItem(stringResource(tab.labelRes), tab.icon(), tab.icon())
                },
                selectedIndex = selectedIndex,
                dark = CouiTheme.isDark,
                onSelection = onSelect,
            )
        }
        return
    }
    CouiFlatTabsBottomBar(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        showUpdateBadge = showUpdateBadge,
    )
}

/**
 * 不依赖渲染器的 Coui 底栏：整条铺满宽度、页签均分，选中项用强调色、未选中用次级文字色。
 *
 * 系统底部安全区由底栏自己消费（与悬浮底栏一致），正文侧因此不必再为底栏留白；
 * 每个页签是整块可点的胶囊区，按下时长按悬停反馈由 [couiNavigationFeedback] 画；
 * 角标只叠在主页页签的图标右上角。
 */
@Composable
private fun CouiFlatTabsBottomBar(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    showUpdateBadge: Boolean,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(ChromeInsets.only(WindowInsetsSides.Bottom))
            .height(64.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = selectedIndex == tab.ordinal
            val tint = if (selected) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(CouiCapsuleShape)
                    .couiNavigationFeedback(dark, interactionSource)
                    .selectable(
                        selected = selected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(tabs.indexOf(tab)) },
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tabIcon: @Composable () -> Unit = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = tint,
                    )
                }
                if (showUpdateBadge && tab == MainTab.Home) {
                    // 角标底色是强调色、内容色是对比色，由 CouiBadge 自己取，调用点只给数字。
                    CouiBadgedBox(badge = { CouiBadge(count = 1) }) { tabIcon() }
                } else {
                    tabIcon()
                }
                Spacer(Modifier.height(2.dp))
                CouiText(
                    text = stringResource(tab.labelRes),
                    kind = CouiTextKind.GroupFooter,
                    color = tint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
            // 阻塞页独立成屏，行级水平内缩由自己提供（宿主列表不在场）
            selfInset = true,
            // 4 项齐全：撤掉阻塞门回到主界面（不重启 Activity）
            onAllGranted = { activity.refreshPermissionGate() },
            onExit = { exitProcess(0) },
        )
    }
    val gateInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    when (LocalUiMode.current) {
        UiMode.Material -> Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentWindowInsets = gateInsets,
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) { gateContent() }
        }

        UiMode.Coui -> CouiPermissionGateShell(insets = gateInsets, content = gateContent)
    }
}

/**
 * Coui 线的权限阻塞壳：铺页面底色，并把系统栏 / 挖孔安全区让出来。
 *
 * 权限页自带滚动，这里不再给正文加滚动；安全区用内边距表达，留白与 Material 线一致。
 */
@Composable
private fun CouiPermissionGateShell(
    insets: WindowInsets,
    content: @Composable () -> Unit,
) {
    val pageColors = CouiPageDefaults.themeColors(CouiTheme.isDark)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColors.background)
            .windowInsetsPadding(insets),
    ) {
        content()
    }
}

