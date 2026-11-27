package com.luckyzyx.luckytool.ui.compose.pages

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import com.itos.coui.topappbar.CollapsingTopAppBar
import com.itos.coui.topappbar.rememberCouiCollapsingAppBarState
import com.itos.coui.layout.Scaffold as CouiScaffold
import com.itos.coui.search.SearchBar as CouiSearchBar
import com.itos.coui.search.rememberCouiSearchBarState
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.categoryBadges
import com.luckyzyx.luckytool.ui.compose.FunctionPageData
import com.luckyzyx.luckytool.ui.compose.pageTitle
import com.luckyzyx.luckytool.ui.compose.ScopeRoute
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeListState
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTextField
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.putString

/** 最近搜索历史的存放位置：只在本机 prefs 里，不参与 hook 侧读取。 */
private const val SearchHistoryPrefs = OtherPrefs
private const val SearchHistoryKey = "search_history"
private const val SearchHistoryLimit = 8

/** 读取最近搜索历史：按写入顺序保存，越靠前越新。 */
private fun readSearchHistory(context: Context): List<String> =
    context.getString(SearchHistoryPrefs, SearchHistoryKey)
        .split('\n')
        .filter { it.isNotBlank() }
        .distinct()
        .take(SearchHistoryLimit)

/** 写回最近搜索历史（去重、截断后整串覆盖）。 */
private fun writeSearchHistory(context: Context, terms: List<String>) {
    context.putString(
        SearchHistoryPrefs,
        SearchHistoryKey,
        terms.filter { it.isNotBlank() }.distinct().take(SearchHistoryLimit).joinToString("\n"),
    )
}

/**
 * 搜索页：独立的一级标签页。
 *
 * 索引与功能树同一份预取快照（FunctionPageData），进页面即有结果可搜；
 * 输入为空时展示最近搜索历史（点击即填入），命中结果点击后跳转目标页或在本页定位。
 */
@Composable
fun SearchPage(isCurrentPage: Boolean, onOpen: (ScopeRoute) -> Unit) {
    val context = LocalContext.current
    val isCoui = LocalUiMode.current == UiMode.Coui
    var searchExpanded by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(readSearchHistory(context)) }

    // 默认先浏览历史与分类，点击搜索框才请求焦点和输入法。
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            searchExpanded = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    val snapshot by FunctionPageData.snapshot.collectAsStateWithLifecycle()
    val entries = snapshot?.searchEntries.orEmpty()
    val categories = snapshot?.rows.orEmpty().filter { it.pageKey in categoryBadges }
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

    val recordHistory: (String) -> Unit = { term ->
        if (term.isNotBlank()) {
            val next = listOf(term) + history.filterNot { it == term }
            history = next.take(SearchHistoryLimit)
            writeSearchHistory(context, history)
        }
    }

    val resultsContent: @Composable (PaddingValues) -> Unit = { padding ->
        val settings = remember { PrefState.of(context, SettingsPrefs) }
        val layoutDirection = LocalLayoutDirection.current
        // 顶栏高度经 LocalScopeTopInset 放进列表 contentPadding：视口铺满整屏，
        // 滚动中的行才能进入顶栏下方；水平/底部内边距仍留在视口外。
        val scopeModifier = if (isCoui) {
            // COUI Scaffold 的 PaddingValues 会随顶栏测量更新：在布局阶段读取整份占位。
            // 列表视口从搜索框下方开始，不能再把顶栏高度仅当作列表内部留白。
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).clipToBounds()
        } else {
            Modifier.fillMaxSize().consumeWindowInsets(padding).padding(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = padding.calculateBottomPadding(),
            )
        }
        val topInset = if (isCoui) 0.dp else padding.calculateTopPadding()
        val scopeContent: @Composable () -> Unit = {
            // 页面级状态在 @Composable 作用域内先读取：只在 ScopeScreen 的 content lambda 内
            // 读取不会被列表追踪，后果是搜索结果不随输入刷新
            val queryNow = query
            val entriesNow = entries
            val filteredNow = filtered
            val historyNow = history
            val categoriesNow = categories
            val indexLoading = snapshot == null
            val categoriesTitle = stringResource(R.string.search_categories)
            val historyEmptyText = stringResource(R.string.search_history_empty)
            // 行参数在非 Composable 的构建 lambda 内求值（那里没有 Composer），字符串先在这里取一次
            val searchIndexLoadingText = stringResource(R.string.search_index_loading)
            val searchNoResultsText = stringResource(R.string.search_no_results)
            val historyTitleText = stringResource(R.string.search_history)
            val historyClearText = stringResource(R.string.search_history_clear)
            CompositionLocalProvider(LocalScopeTopInset provides topInset) {
                ScopeScreen(state = settings, modifier = scopeModifier) {
                // 搜索框不是「选项行」，独占一张卡片（否则会与紧随其后的结果行拼在一起）
                if (!isCoui) custom(key = "search_field", standalone = true) {
                    // 顶栏折叠/展开由列表滚动驱动：滚动一开始就清掉搜索框焦点并收键盘，
                    // 否则仍聚焦的字段会在布局变化时被平台恢复输入法。
                    val scopeListState = LocalScopeListState.current
                    val focusManager = LocalFocusManager.current
                    LaunchedEffect(scopeListState, focusManager, keyboardController) {
                        if (scopeListState == null) return@LaunchedEffect
                        snapshotFlow { scopeListState.isScrollInProgress }
                            .collect { scrolling ->
                                if (scrolling) {
                                    // freeFocus 只释放 Captured 焦点，对普通聚焦的输入框无效，
                                    // 必须用 FocusManager.clearFocus 才能真正清焦并收键盘。
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                }
                            }
                    }
                    MaterialTextField(
                        value = queryNow,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.menu_search)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { recordHistory(queryNow) },
                        ),
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
                if (queryNow.isBlank()) {
                    // 空查询时给最近搜索历史留出入口：点一条即填入，右侧按钮只删这一条
                    category(historyTitleText)
                    if (historyNow.isEmpty()) {
                        // 无操作回调的独立提示卡片，沿用分组行的圆角、字号和内边距。
                        click(title = historyEmptyText, itemKey = "search_history_empty")
                    } else {
                        historyNow.forEach { term ->
                            click(
                                title = term,
                                trailing = {
                                    when (LocalUiMode.current) {
                                        UiMode.Material -> IconButton(
                                            onClick = {
                                                history = history.filterNot { it == term }
                                                writeSearchHistory(context, history)
                                            },
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.ic_baseline_close_24),
                                                contentDescription = stringResource(R.string.clear),
                                            )
                                        }

                                        UiMode.Coui -> CouiIconButton(
                                            onClick = {
                                                history = history.filterNot { it == term }
                                                writeSearchHistory(context, history)
                                            },
                                            contentDescription = stringResource(R.string.clear),
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.ic_baseline_close_24),
                                                contentDescription = null,
                                                tint = CouiTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                onClick = { query = term; searchExpanded = true },
                            )
                        }
                        click(
                            title = historyClearText,
                            onClick = {
                                history = emptyList()
                                writeSearchHistory(context, history)
                            },
                        )
                    }
                    category(categoriesTitle)
                    if (indexLoading) {
                        click(title = searchIndexLoadingText)
                    }
                    categoriesNow.forEach { row ->
                        click(
                            title = row.title,
                            trailing = {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            },
                            onClick = { onOpen(ScopeRoute(row.pageKey, row.title)) },
                        )
                    }
                } else {
                    if (entriesNow.isEmpty()) {
                        click(title = searchIndexLoadingText)
                    }
                    if (entriesNow.isNotEmpty() && filteredNow.isEmpty()) {
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
                                recordHistory(entry.item.title ?: entry.item.key)
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
        scopeContent()
    }
    if (isCoui) {
        val searchState = rememberCouiSearchBarState(searchExpanded)
        val headerState = rememberCouiCollapsingAppBarState(canScroll = { false })
        val density = LocalDensity.current
        val closeSearch = {
            searchExpanded = false
            query = ""
        }
        BackHandler(enabled = isCurrentPage && searchExpanded, onBack = closeSearch)
        CouiScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // 标题和搜索始终留在同一棵布局树，共用库的展开/取消动画状态。
                CollapsingTopAppBar(
                    state = headerState,
                    searchState = searchState,
                    title = stringResource(R.string.nav_search),
                ) {
                    CouiSearchBar(
                        state = searchState,
                        query = query,
                        onQueryChange = { query = it },
                        onExpandedChange = { expanded ->
                            searchExpanded = expanded
                            if (!expanded) query = ""
                        },
                        placeholder = stringResource(R.string.menu_search),
                        cancelText = stringResource(android.R.string.cancel),
                        clearDescription = stringResource(R.string.clear),
                        onSearch = { recordHistory(query.trim()) },
                        enabled = isCurrentPage,
                        shadowProgress = headerState.collapsedFraction,
                    )
                }
            },
            content = { padding ->
                val contentPadding = remember(headerState, padding, density) {
                    headerState.contentPadding(padding, density)
                }
                resultsContent(contentPadding)
            },
        )
    } else {
        PageScaffold(title = stringResource(R.string.nav_search), content = resultsContent)
    }

}
