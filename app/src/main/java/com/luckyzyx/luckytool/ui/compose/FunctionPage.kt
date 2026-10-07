package com.luckyzyx.luckytool.ui.compose

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.components.preference.PrefIndexItem
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.components.preference.ScopeScreen
import com.luckyzyx.luckytool.ui.components.preference.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.sendPrefsValue
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
    val atRoot = backStackEntry?.destination?.hasRoute<FunctionTreeRoute>() == true

    var showVersionInfo by remember { mutableStateOf(false) }

    // 跨 tab 跳转请求（OtherPage/SettingPage → 作用域页）：MainShell 负责切 tab，这里消费执行
    LaunchedEffect(Unit) {
        activity.functionNavRequests.collect { request ->
            if (request != null) {
                activity.functionNavRequests.value = null
                navController.navigate(ScopeRoute(request.pageKey, request.title ?: "", "", -1)) {
                    launchSingleTop = true
                }
            }
        }
    }

    // 返回键：非根退回上一页，根交还 shell（切回上一 tab 或退出）
    BackHandler {
        if (atRoot) onShellBack() else navController.popBackStack()
    }

    if (showVersionInfo) {
        VersionInfoDialog(onDismiss = { showVersionInfo = false })
    }

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
                onShowRestartMenu = { RestartMenuUtils.showMainRestartMenu(activity) },
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
                onBack = { navController.popBackStack() },
                onNavigate = { key, title -> navController.navigate(ScopeRoute(key, title ?: "", "", -1)) },
            )
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
private fun buildIndex(context: Context, spec: ScopePageSpec): List<PrefIndexItem> {
    val state = PrefState.of(context.applicationContext, spec.prefsName)
    val builder = PrefScopeBuilder(state)
    builder.context = context.applicationContext
    builder.beginBuild()
    spec.content(builder)
    return builder.snapshotIndex()
}

private data class TreeRow(val pageKey: String, val title: String, val summary: String?)

/** 功能树：49 页固定顺序（ScopePageRegistry.treeOrder），空索引页隐藏 */
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_function)) },
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
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(rows, key = { it.pageKey }) { row ->
                ListItem(
                    headlineContent = { Text(row.title) },
                    supportingContent = {
                        row.summary?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                    },
                    trailingContent = {
                        Icon(painterResource(R.drawable.ic_baseline_chevron_right_24), contentDescription = null)
                    },
                    modifier = Modifier.clickable { onOpenPage(row.pageKey, row.title) },
                )
                HorizontalDivider()
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

    // 过滤规则对齐 SearchResultAdapter.getFilter：key/title/summary contains(ignoreCase)
    val filtered = remember(entries, query) {
        if (query.isBlank()) emptyList() else entries.filter { entry ->
            entry.item.key.contains(query, ignoreCase = true) ||
                entry.item.title?.contains(query, ignoreCase = true) == true ||
                entry.item.summary?.contains(query, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                active = true,
                onActiveChange = {},
                placeholder = { Text(stringResource(R.string.menu_search)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_baseline_close_24), contentDescription = stringResource(R.string.clear))
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {}
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { "${it.pageKey}/${it.item.key}" }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.item.title ?: entry.item.key) },
                        supportingContent = {
                            Column {
                                if (!entry.item.summary.isNullOrBlank()) {
                                    Text(entry.item.summary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    entry.pageTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        trailingContent = {
                            Icon(painterResource(R.drawable.ic_baseline_chevron_right_24), contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            // page DSL 命中 → 跳目标页；普通偏好命中 → 本页滚动定位
                            val targetSpec = entry.item.pageTarget
                                ?.let { ScopePageRegistry.pageTargetMap[it] }
                                ?.let { ScopePageRegistry[it] }
                            if (targetSpec != null) {
                                onOpen(ScopeRoute(targetSpec.pageKey, pageTitle(context, targetSpec)))
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
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 作用域页宿主：类型安全 ScopeRoute → 任意已注册 ScopePageSpec */
@Composable
private fun ScopePageHost(
    activity: MainActivity,
    route: ScopeRoute,
    onBack: () -> Unit,
    onNavigate: (pageKey: String, title: String?) -> Unit,
) {
    val context = LocalContext.current
    val spec = ScopePageRegistry[route.pageKey]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey })
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (spec?.restartEnabled == true) {
                        IconButton(
                            onClick = {
                                RestartMenuUtils.showRestartScopeDialog(activity, spec.scopes, true)
                            }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_baseline_refresh_24),
                                contentDescription = stringResource(R.string.menu_reboot),
                            )
                        }
                    }
                },
            )
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
            return@Scaffold
        }
        val state = remember(spec) { PrefState.of(context.applicationContext, spec.prefsName) }
        ScopeScreen(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            sendValue = { key, value -> context.sendPrefsValue(spec.packName, key, value) },
            scrollTarget = if (route.scrollKey.isNotBlank() && route.scrollPosition >= 0) {
                ScrollTarget(route.scrollKey, route.scrollPosition)
            } else {
                null
            },
            onNavigate = { target, title ->
                ScopePageRegistry.pageTargetMap[target]?.let { onNavigate(it, title) }
            },
            onRestart = if (spec.restartEnabled) ({ activity.restart() }) else null,
            onRefresh = spec.onRefresh,
            fullContent = spec.fullContent,
            content = spec.content,
        )
    }
}

/** 版本信息对话框：xposed_scope 各包版本表（对齐旧 showBottomDialog 内容，去 Markwon） */
@Composable
private fun VersionInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(context) {
        val pkgs = context.resources.getStringArray(R.array.xposed_scope).sorted()
        buildString {
            append("| name | package | version |\n")
            append("| :--- | :--- | :--- |\n")
            pkgs.forEach { pkg ->
                AppUtils(context).getAppVerInfo(pkg)?.let { info ->
                    append("| ${info.name} | $pkg | ${info.versionName}(${info.versionCode})[${info.versionCommit}] |\n")
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_versioninfo)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}
