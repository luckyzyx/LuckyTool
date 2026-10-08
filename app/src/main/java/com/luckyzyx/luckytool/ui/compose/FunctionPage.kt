package com.luckyzyx.luckytool.ui.compose

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.components.preference.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.components.preference.PrefIndexItem
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.components.preference.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.components.EdgeSwipeDismiss
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveList
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressivePageScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedTextField
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageContent
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.sendPrefsValue
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import java.util.Arrays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Function 子树类型安全路由（迁移方案 §7：ScopeRoute(scopeId, key, position) 等价物）
// 注意：FunctionRoute 是 shell 级 tab 路由（MainShell），此处是 Function tab 内部导航。

@Serializable
data object FunctionTreeRoute

@Serializable
data object SearchRoute

@Serializable
data class ScopeRoute(
    val pageKey: String,
    val title: String = "",
    val scrollKey: String = "",
    val scrollPosition: Int = -1,
)

/**
 * Function tab：Compose 功能树 + 全屏搜索 + 作用域页宿主（P3 终局，替代旧
 * XposedFragment/ComposeScopeFragment + function_nav.xml 子树）。
 */
@Composable
fun FunctionPage(activity: MainActivity, onShellBack: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val atTree = backStackEntry?.destination?.hasRoute<FunctionTreeRoute>() == true
    // 内部栈是否还有上一页可退：功能树根与跨 tab 直达的作用域页均为内部栈根（无上一页）。
    // 用它替代原来的 atRoot 判断，是因为跨 tab 直达会把功能树弹出内部栈，作用域页成为根。
    val canPopInternal = navController.previousBackStackEntry != null

    var showVersionInfo by remember { mutableStateOf(false) }

    // 跨 tab 跳转请求（OtherPage/SettingPage → 作用域页）：MainShell 负责切 tab，这里消费执行
    LaunchedEffect(Unit) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                activity.functionNavRequests.value = null
                // 跨 tab 直达作用域页：把功能树（内部栈根）一并弹出，让作用域页成为内部栈根，
                // 返回键据此直接交还 shell（回到来源 tab），而不是先退回功能树。
                navController.navigate(ScopeRoute(request.pageKey, request.title ?: "", "", -1)) {
                    popUpTo<FunctionTreeRoute> { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // 返回键：有内部上一页则退内部栈，否则交还 shell（功能树根 → 上一 tab/退出；跨 tab 直达 → 来源 tab）
    BackHandler {
        if (canPopInternal) navController.popBackStack() else onShellBack()
    }

    var showRestartMenu by remember { mutableStateOf(false) }

    if (showVersionInfo) {
        VersionInfoDialog(onDismiss = { showVersionInfo = false })
    }

    if (showRestartMenu) {
        RestartMenuUtils.RestartMenuDialog(activity) { showRestartMenu = false }
    }

    EdgeSwipeDismiss(
        enabled = LocalEnableSwipeDismiss.current && !atTree,
        onDismiss = { if (canPopInternal) navController.popBackStack() else onShellBack() },
    ) {
        NavHost(
            navController = navController,
            startDestination = FunctionTreeRoute,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<FunctionTreeRoute> {
                FunctionTreeScreen(
                    onOpenPage = { key, title -> navController.navigate(ScopeRoute(key, title)) },
                    onOpenSearch = { navController.navigate(SearchRoute) },
                    onShowVersionInfo = { showVersionInfo = true },
                    onShowRestartMenu = { showRestartMenu = true },
                )
            }
            composable<SearchRoute> {
                FunctionSearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { route -> navController.navigate(route) },
                )
            }
            composable<ScopeRoute> { entry ->
                val route = entry.toRoute<ScopeRoute>()
                ScopePageHost(
                    activity = activity,
                    route = route,
                    // 顶栏返回键与系统返回键一致：内部有上一页退内部栈，跨 tab 直达的根页交还 shell
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            onShellBack()
                        }
                    },
                    onNavigate = { key, title -> navController.navigate(ScopeRoute(key, title ?: "", "", -1)) },
                )
            }
        }
    }
}

/** 页标题：对齐旧功能树 root 标题（apps/others = App 标签；related/statusbar = 覆盖表） */
private fun pageTitle(context: Context, spec: ScopePageSpec): String {
    ScopePageRegistry.treeTitleRes[spec.pageKey]?.let { return context.getString(it) }
    val pack = if (spec.pageKey == "android_related") "android" else spec.packName
    return AppUtils(context).getAppLabel(pack).toString()
}

/**
 * headless 构建一页的搜索索引（等价旧 getAllPrefsItem：条件可见性已由 Kotlin if 应用，
 * 空索引 = 该页不可见）。构建是纯记录（emit 不渲染），可在任意线程运行。
 */
private fun buildIndex(context: Context, spec: ScopePageSpec): List<PrefIndexItem> = try {
    val state = PrefState.of(context.applicationContext, spec.prefsName)
    val builder = PrefScopeBuilder(state)
    builder.context = context.applicationContext
    builder.beginBuild()
    spec.content(builder)
    builder.snapshotIndex()
} catch (t: Throwable) {
    // 单页构建失败只丢弃该页，不得拖垮整棵功能树/搜索索引
    LogUtils.e("buildIndex", spec.pageKey, t.toString(), true)
    emptyList()
}

private data class TreeRow(val pageKey: String, val title: String, val summary: String?)

/** 功能树：49 页固定顺序（ScopePageRegistry.treeOrder），空索引页隐藏 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionTreeScreen(
    onOpenPage: (pageKey: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    onShowVersionInfo: () -> Unit,
    onShowRestartMenu: () -> Unit,
) {
    val context = LocalContext.current
    val rows by produceState(initialValue = emptyList<TreeRow>(), key1 = Unit) {
        value = withContext(Dispatchers.IO) {
            ScopePageRegistry.treeOrder.mapNotNull { key ->
                val spec = ScopePageRegistry[key] ?: return@mapNotNull null
                val index = buildIndex(context, spec)
                if (index.isEmpty()) return@mapNotNull null
                val summary = index.mapNotNull { it.title }.take(3).joinToString(" · ").ifEmpty { null }
                TreeRow(key, pageTitle(context, spec), summary)
            }
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    ExpressivePageScaffold(
        title = stringResource(R.string.nav_function),
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    painterResource(R.drawable.ic_baseline_search_24),
                    contentDescription = stringResource(R.string.menu_search),
                )
            }
            IconButton(onClick = onShowRestartMenu) {
                Icon(
                    painterResource(R.drawable.ic_baseline_refresh_24),
                    contentDescription = stringResource(R.string.menu_reboot),
                )
            }
            IconButton(onClick = onShowVersionInfo) {
                Icon(
                    painterResource(R.drawable.ic_baseline_extension_24),
                    contentDescription = stringResource(R.string.menu_versioninfo),
                )
            }
        },
    ) { padding ->
        ExpressiveList(
            scrollBehavior = scrollBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // 整树合并为一张分段卡片（对齐主题页分组卡片），条目顺序与原 ListItem 完全一致
            item(key = "function_tree") {
                PrefGroup {
                    rows.forEach { row ->
                        item(key = row.pageKey) {
                            PrefRow(
                                title = row.title,
                                summary = row.summary,
                                trailing = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                },
                                onClick = { onOpenPage(row.pageKey, row.title) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SearchEntry(val pageKey: String, val pageTitle: String, val item: PrefIndexItem)

/** 全屏搜索：索引 = 全部已注册页 headless 构建，过滤平移旧 SearchResultAdapter 规则 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionSearchScreen(onBack: () -> Unit, onOpen: (ScopeRoute) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val entries by produceState(initialValue = emptyList<SearchEntry>(), key1 = Unit) {
        value = withContext(Dispatchers.IO) {
            ScopePageRegistry.all().flatMap { spec ->
                buildIndex(context, spec).map { item -> SearchEntry(spec.pageKey, pageTitle(context, spec), item) }
            }
        }
    }

    // 过滤规则对齐 SearchResultAdapter.getFilter：key/title/summary contains(ignoreCase)，并补充页标题匹配
    val filtered = remember(entries, query) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else entries.filter { entry ->
            entry.item.key.contains(q, ignoreCase = true) ||
                entry.item.title?.contains(q, ignoreCase = true) == true ||
                entry.item.summary?.contains(q, ignoreCase = true) == true ||
                entry.pageTitle.contains(q, ignoreCase = true)
        }
    }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    ExpressivePageScaffold(
        title = stringResource(R.string.menu_search),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
    ) { padding ->
        ExpressiveList(
            scrollBehavior = scrollBehavior,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(key = "search_field") {
                PrefGroup {
                    item {
                        SegmentedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(stringResource(R.string.menu_search)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.ic_baseline_search_24),
                                    contentDescription = null,
                                )
                            },
                            trailingContent = if (query.isNotEmpty()) {
                                {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(
                                            painterResource(R.drawable.ic_baseline_close_24),
                                            contentDescription = stringResource(R.string.clear),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
            if (query.isNotBlank() && entries.isEmpty()) {
                item(key = "search_index_loading") {
                    PrefGroup {
                        item {
                            PrefRow(title = stringResource(R.string.search_index_loading))
                        }
                    }
                }
            }
            if (query.isNotBlank() && entries.isNotEmpty() && filtered.isEmpty()) {
                item(key = "search_no_results") {
                    PrefGroup {
                        item {
                            PrefRow(title = stringResource(R.string.search_no_results))
                        }
                    }
                }
            }
            filtered.forEach { entry ->
                item(key = "result_${entry.pageKey}/${entry.item.key}") {
                    PrefGroup {
                        item {
                            PrefRow(
                                title = entry.item.title ?: entry.item.key,
                                summary = buildString {
                                    if (!entry.item.summary.isNullOrBlank()) {
                                        append(entry.item.summary)
                                        append('\n')
                                    }
                                    append(entry.pageTitle)
                                },
                                trailing = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    // page DSL 命中 → 跳目标页；普通偏好命中 → 本页滚动定位
                                    val targetSpec = entry.item.pageTarget
                                        ?.let { ScopePageRegistry.pageTargetMap[it] }
                                        ?.let { ScopePageRegistry[it] }
                                    if (targetSpec != null) {
                                        onOpen(
                                            ScopeRoute(
                                                targetSpec.pageKey,
                                                pageTitle(context, targetSpec)
                                            )
                                        )
                                    } else {
                                        onOpen(
                                            ScopeRoute(
                                                entry.pageKey,
                                                entry.pageTitle,
                                                entry.item.key,
                                                entry.item.slot,
                                            )
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 作用域页宿主：类型安全 ScopeRoute → 任意已注册 ScopePageSpec */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopePageHost(
    activity: MainActivity,
    route: ScopeRoute,
    onBack: () -> Unit,
    onNavigate: (pageKey: String, title: String?) -> Unit,
) {
    val context = LocalContext.current
    val spec = ScopePageRegistry[route.pageKey]
    var showRestartScope by remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    ExpressivePageScaffold(
        title = route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey },
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = {
            if (spec?.restartEnabled == true) {
                IconButton(
                    onClick = { showRestartScope = true }
                ) {
                    Icon(
                        painterResource(R.drawable.ic_baseline_refresh_24),
                        contentDescription = stringResource(R.string.menu_reboot),
                    )
                }
            }
        },
    ) { padding ->
        if (spec == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Unknown page: ${route.pageKey}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val state = remember(spec) { PrefState.of(context.applicationContext, spec.prefsName) }
            val uiMode = LocalUiMode.current
            val sendPrefsValue: (key: String, value: Any) -> Unit = { key, value ->
                context.sendPrefsValue(spec.packName, key, value)
            }
            val jumpTarget = if (route.scrollKey.isNotBlank() && route.scrollPosition >= 0) {
                ScrollTarget(route.scrollKey, route.scrollPosition)
            } else {
                null
            }
            val navigatePage: (target: String, title: String?) -> Unit = { target, title ->
                ScopePageRegistry.pageTargetMap[target]?.let { onNavigate(it, title) }
            }
            val restartScope: (() -> Unit)? = if (spec.restartEnabled) ({ activity.restart() }) else null

            // 页面内容分派（唯一分派点见 ScopePageContent）：
            // 页面为当前主题线提供了槽位（contentMiuix/contentMaterial）时由页面自有布局渲染整页，
            // 否则逐字回落到共享渲染层 ScopeScreen —— ScopeScreen 自带 LazyColumn 并渲染全部条目，
            // 直接作为页面内容：不能再包一层 ExpressiveList（同向嵌套 LazyColumn 会以无限高度约束测量而崩溃）。
            // 水平内边距此处不叠加（material 线由作用域内容自带，Miuix 线由列表级 12dp contentPadding 提供）。
            //
            // 骨架内边距传递：
            // - material 线：innerPadding 整块交给内容（与今天逐字一致，m3 顶栏 64dp 由 innerPadding 承担）。
            // - Miuix 线：骨架给的是真实 innerPadding，其中 top = 顶栏高度；top 经 LocalScopeTopInset 交给
            //   列表充当 contentPadding.top（内容滚动到模糊顶栏之下，KernelSU 式 scroll-under），这里只保留
            //   start/end/bottom 外置 padding（横屏 displayCutout 水平内缩与底部 inset 不能丢）。
            // Miuix 线不接 m3 顶栏的 nestedScrollConnection：Miuix 骨架走的是自家 MiuixScrollBehavior
            //（MiuixExpressivePageScaffold 自带 scrollBehavior，并经 LocalScopeScrollBehavior 下发，
            // ScopeScreen 已在列表上接好）。m3 的 exitUntilCollapsed 行为只在真实 m3 TopAppBar 被布局时
            // 才会写入 state.heightOffsetLimit（否则 limit 恒为 -Float.MAX_VALUE），Miuix 线没有 m3 顶栏，
            // 接上去会让 ExitUntilCollapsedScrollBehavior.onPreScroll 把向上滚动的 delta 全部吃掉，
            // 表现为页面完全无法上滑（内容下移不可见）。
            val pageContentModifier = if (uiMode == UiMode.Miuix) {
                val layoutDirection = LocalLayoutDirection.current
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    )
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            }
            val pageContent: @Composable () -> Unit = {
                ScopePageContent(
                    spec = spec,
                    state = state,
                    modifier = pageContentModifier,
                    sendValue = sendPrefsValue,
                    scrollTarget = jumpTarget,
                    onNavigate = navigatePage,
                    onRestart = restartScope,
                )
            }
            if (uiMode == UiMode.Miuix) {
                CompositionLocalProvider(LocalScopeTopInset provides padding.calculateTopPadding()) {
                    pageContent()
                }
            } else {
                pageContent()
            }
        }
    }

    if (showRestartScope) {
        RestartMenuUtils.RestartScopeDialog(context, spec?.scopes ?: emptyArray(), isSystem = true) {
            showRestartScope = false
        }
    }
}

/** 版本信息对话框：xposed_scope 各包版本表（对齐旧 showBottomDialog，Markwon 渲染） */
@Composable
private fun VersionInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val markdown = remember(context) {
        val list = ArrayList<String>().apply {
            add("| name | package | version |")
            add("| ------ | ------ | ------ |")
        }
        val pkgs = context.resources.getStringArray(R.array.xposed_scope)
        Arrays.sort(pkgs)
        pkgs.forEach { pkg ->
            AppUtils(context).getAppVerInfo(pkg)?.let { info ->
                list.add("| ${info.name} | $pkg | ${info.versionName}(${info.versionCode})[${info.versionCommit}] |")
            }
        }
        formatStringAuto(list, "\n")
    }
    val markwon = remember(context) {
        Markwon.builder(context).usePlugin(TablePlugin.create(context)).build()
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(R.string.menu_versioninfo),
            onDismissRequest = onDismiss,
        ) {
            AndroidView(
                factory = { ctx -> android.widget.TextView(ctx) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                update = { tv ->
                    tv.setTextIsSelectable(true)
                    markwon.setMarkdown(tv, markdown)
                },
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                MiuixTextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        UiMode.Material -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.menu_versioninfo)) },
            text = {
                AndroidView(
                    factory = { ctx -> android.widget.TextView(ctx) },
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    update = { tv ->
                        tv.setTextIsSelectable(true)
                        markwon.setMarkdown(tv, markdown)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }
}
