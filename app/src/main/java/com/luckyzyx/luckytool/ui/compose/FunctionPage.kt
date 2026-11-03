package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.FunctionTopBarActions
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.components.TopBarActionButton
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowItem
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowMenu
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPageScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTextField
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageContent
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.sendPrefsValue
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close2
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.util.Arrays
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/** 功能标签页只展示功能树；搜索和作用域页由应用的同一个导航栈管理。 */
@Composable
fun FunctionPage(
    activity: MainActivity,
    onOpenPage: (pageKey: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
) {
    var showVersionInfo by remember { mutableStateOf(false) }
    var showRestartMenu by remember { mutableStateOf(false) }

    if (showVersionInfo) {
        VersionInfoDialog(onDismiss = { showVersionInfo = false })
    }

    if (showRestartMenu) {
        RestartMenuUtils.RestartMenuDialog(activity) { showRestartMenu = false }
    }

    FunctionTreeScreen(
        onOpenPage = onOpenPage,
        onOpenSearch = onOpenSearch,
        onShowVersionInfo = { showVersionInfo = true },
        onShowRestartMenu = { showRestartMenu = true },
    )
}

/** 功能树：50 页固定顺序（ScopePageRegistry.treeOrder），spec 手动不可见、单 App 作用域未安装或空索引页隐藏（selfRendered 特殊页除外） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionTreeScreen(
    onOpenPage: (pageKey: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    onShowVersionInfo: () -> Unit,
    onShowRestartMenu: () -> Unit,
) {
    val context = LocalContext.current
    // 以 Configuration 作 key：语言/字号等变化后需要重建行数据里的页面标题
    val rows by produceState(initialValue = emptyList<FunctionRow>(), key1 = LocalConfiguration.current) {
        value = try {
            withContext(Dispatchers.IO) {
                ScopePageRegistry.treeOrder.mapNotNull { key ->
                    val spec = ScopePageRegistry[key] ?: return@mapNotNull null
                    if (!spec.isVisible(context)) return@mapNotNull null
                    if (!ScopePageRegistry.isScopeAppPresent(context, spec)) return@mapNotNull null
                    val index = ScopePageRegistry.buildIndex(context, spec)
                    // 自带整页渲染的特殊页（暗色模式列表/意图/自定义浮窗等）不声明共享行、索引恒空，
                    // 空索引不代表不可见；判据与 ScopePageRegistry.hasVisibleContent 一致
                    if (index.isEmpty() && !spec.selfRendered) return@mapNotNull null
                    val packName = rowIconPackName(key, spec)
                    val summary = rowSummary(index)
                    FunctionRow(key, pageTitle(context, spec), summary, packName)
                }
            }
        } catch (t: Throwable) {
            // buildIndex 内部已逐页兜底，此处再兜底 isVisible/图标/标题等外层自定义逻辑，
            // 避免任一页面构建异常导致整棵功能树为空（白屏）
            LogUtils.e("FunctionPage", "buildTree", t.toString(), true)
            emptyList()
        }
    }

    MaterialPageScaffold(
        title = stringResource(R.string.nav_function),
        actions = {
            FunctionTopBarActions(
                onSearch = onOpenSearch,
                onRestart = onShowRestartMenu,
                onVersionInfo = onShowVersionInfo,
            )
        },
    ) { padding ->
        FunctionList(
            rows = rows,
            padding = padding,
            onOpenPage = onOpenPage,
        )
    }
}

/** 全屏搜索：索引 = 全部已注册页 headless 构建，spec 手动不可见与单 App 作用域未安装页不入索引；过滤按 key/title/summary 与页标题做不区分大小写的包含匹配 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FunctionSearchScreen(onBack: () -> Unit, onOpen: (ScopeRoute) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    // 进入搜索页即聚焦输入框并拉起输入法：打开就能直接打字，不必先点一下搜索框。
    // 焦点请求只在这里发一次（搜索框被 LazyColumn 回收重建时不会重复抢焦点/弹输入法）。
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // 等首帧布局完成（focus 节点挂载）后再请求焦点：节点未挂载时 requestFocus 会抛未初始化
        withFrameNanos { }
        searchFocusRequester.requestFocus()
        // 个别 ROM 上 requestFocus 不会自动弹出输入法，这里显式再请求一次
        keyboardController?.show()
    }

    // 索引与功能树同一份预取快照（FunctionPageData），进搜索页即有结果，不再现场构建
    val snapshot by FunctionPageData.snapshot.collectAsStateWithLifecycle()
    val entries = snapshot?.searchEntries.orEmpty()
    // 图标预热尺寸必须与功能树读取时一致（AppIconCache 的键含 size，不一致会白热一份）
    val iconSize = with(LocalDensity.current) { 48.dp.roundToPx() }
    LaunchedEffect(context, iconSize) { FunctionPageData.revalidate(context, iconSize) }

    // 过滤规则：key/title/summary 做 contains(ignoreCase)，并补充页标题匹配
    val filtered = remember(entries, query) {
        val q = query.trim()
        if (q.isEmpty()) emptyList() else entries.filter { entry ->
            entry.item.key.contains(q, ignoreCase = true) ||
                entry.item.title?.contains(q, ignoreCase = true) == true ||
                entry.item.summary?.contains(q, ignoreCase = true) == true ||
                entry.pageTitle.contains(q, ignoreCase = true)
        }
    }

    MaterialPageScaffold(
        title = stringResource(R.string.menu_search),
        onBack = onBack,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val uiMode = LocalUiMode.current
        val settings = remember { PrefState.of(context, SettingsPrefs) }
        val scopeModifier = if (uiMode == UiMode.Miuix) {
            Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
        } else {
            Modifier.fillMaxSize().padding(padding)
        }
        val scopeContent: @Composable () -> Unit = {
            // 页面级状态在 @Composable 作用域内先读取：只在 ScopeScreen 的 content lambda 内
            // 读取不会被列表追踪，后果是搜索结果不随输入刷新
            val queryNow = query
            val entriesNow = entries
            val filteredNow = filtered
            // 行参数在非 Composable 的构建 lambda 内求值（那里没有 Composer），字符串先在这里取一次
            val searchIndexLoadingText = stringResource(R.string.search_index_loading)
            val searchNoResultsText = stringResource(R.string.search_no_results)
            ScopeScreen(state = settings, modifier = scopeModifier) {
                // 搜索框不是「选项行」，独占一张卡片（否则会与紧随其后的结果行拼在一起）
                custom(key = "search_field", standalone = true) {
                    MaterialTextField(
                        value = queryNow,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.menu_search)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        focusRequester = searchFocusRequester,
                        leadingContent = {
                            Icon(
                                painterResource(R.drawable.ic_baseline_search_24),
                                contentDescription = null,
                            )
                        },
                        trailingContent = if (queryNow.isNotEmpty()) {
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
                if (queryNow.isNotBlank() && entriesNow.isEmpty()) {
                    click(title = searchIndexLoadingText)
                }
                if (queryNow.isNotBlank() && entriesNow.isNotEmpty() && filteredNow.isEmpty()) {
                    click(title = searchNoResultsText)
                }
                filteredNow.forEach { entry ->
                    click(
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
        if (uiMode == UiMode.Miuix) {
            CompositionLocalProvider(LocalScopeTopInset provides padding.calculateTopPadding()) {
                scopeContent()
            }
        } else {
            scopeContent()
        }
    }
}

/** 作用域页宿主：类型安全 ScopeRoute → 任意已注册 ScopePageSpec（MainShell 跨 tab 全屏直达也复用） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScopePageHost(
    activity: MainActivity,
    route: ScopeRoute,
    onBack: () -> Unit,
    onNavigate: (pageKey: String, title: String?) -> Unit,
) {
    val context = LocalContext.current
    val spec = ScopePageRegistry[route.pageKey]
    var showRestartScope by remember { mutableStateOf(false) }

    // 有图标且声明 showAsAction 的直接显示为图标按钮，其余收进溢出菜单
    val topActions = spec?.topActions.orEmpty()
    val overflowActions = topActions.filter { !(it.showAsAction && it.icon != 0) }

    MaterialPageScaffold(
        title = route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey },
        onBack = onBack,
        actions = {
            if (spec?.restartEnabled == true) {
                TopBarActionButton(
                    onClick = { showRestartScope = true },
                    contentDescription = stringResource(R.string.menu_reboot),
                    materialIcon = Icons.Filled.PowerSettingsNew,
                    miuixIcon = MiuixIcons.Close2,
                )
            }
            topActions.forEach { action ->
                if (action.showAsAction && action.icon != 0) {
                    TopBarActionButton(
                        onClick = { spec?.actionHandler?.invoke(action.key) },
                        contentDescription = stringResource(action.labelRes),
                        painter = painterResource(action.icon),
                    )
                }
            }
            if (overflowActions.isNotEmpty()) {
                TopBarOverflowMenu(
                    items = overflowActions.map { action ->
                        TopBarOverflowItem(
                            label = stringResource(action.labelRes),
                            onClick = { spec?.actionHandler?.invoke(action.key) },
                        )
                    },
                    contentDescription = stringResource(R.string.menu_more),
                )
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
            // 作用域页的刷新由 PrefState.revision + per-key flow 覆盖（写入 → bump → 构建 lambda 重跑），
            // 不再向页面注入 onRestart，也不再重建 Activity；需要重启作用域时用顶栏菜单。
            //
            // 页面内容分派（唯一分派点见 ScopePageContent）：
            // 页面为当前主题线提供了槽位（contentMiuix/contentMaterial）时由页面自有布局渲染整页，
            // 否则逐字回落到共享渲染层 ScopeScreen —— ScopeScreen 自带 LazyColumn 并渲染全部条目，
            // 直接作为页面内容：不能再包一层 ExpressiveList（同向嵌套 LazyColumn 会以无限高度约束测量而崩溃）。
            // 水平内边距此处不叠加（material 线由作用域内容自带，Miuix 线由列表级 12dp contentPadding 提供）。
            //
            // 骨架内边距传递：
            // - Material 线：innerPadding 整块交给内容，包含大标题折叠后的实际顶栏高度。
            // - Miuix 线：骨架给的是真实 innerPadding，其中 top = 顶栏高度；top 经 LocalScopeTopInset 交给
            //   列表充当 contentPadding.top（内容滚动到模糊顶栏之下），这里只保留
            //   start/end/bottom 外置 padding（横屏 displayCutout 水平内缩与底部 inset 不能丢）。
            // 两条外观线的默认折叠行为均由页面骨架统一连接，内容不重复接入同一条滚动连接。
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
            }
            val pageContent: @Composable () -> Unit = {
                ScopePageContent(
                    spec = spec,
                    state = state,
                    modifier = pageContentModifier,
                    sendValue = sendPrefsValue,
                    scrollTarget = jumpTarget,
                    onNavigate = navigatePage,
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

/** 版本信息对话框：xposed_scope 各包版本表（Markdown 表格经 Markwon 渲染） */
@Composable
private fun VersionInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    // 数组资源走 Compose API，避免 LocalContext 取值在配置变化后陈旧
    val pkgs = stringArrayResource(R.array.xposed_scope)
    val markdown = remember(context) {
        val list = ArrayList<String>().apply {
            add("| name | package | version |")
            add("| ------ | ------ | ------ |")
        }
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
