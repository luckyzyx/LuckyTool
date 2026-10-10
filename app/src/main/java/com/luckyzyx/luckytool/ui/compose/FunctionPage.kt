package com.luckyzyx.luckytool.ui.compose

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SignalCellularAlt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.PrefIndexItem
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.components.EdgeSwipeDismiss
import com.luckyzyx.luckytool.ui.compose.components.PrefIconBadge
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPageScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTextField
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageContent
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppIconCache
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
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.util.Arrays
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

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
    // 内部栈是否还有上一页可退：功能树根是内部栈根（无上一页），作用域页/搜索页有上一页可退。
    // 跨 tab 直达不再走本子树（改由 MainShell 压入全屏作用域页），此处只服务功能树 → 作用域页的常规流程。
    val canPopInternal = navController.previousBackStackEntry != null

    var showVersionInfo by remember { mutableStateOf(false) }

    // 返回键：有内部上一页则退内部栈，否则交还 shell（功能树根 → 上一 tab/退出）
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
            // 预测性返回：当前页横滑右移，上一页从左侧滑入；不加淡入淡出，保证当前页不透明地在顶层滑走
            predictivePopEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(400),
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                )
            },
            predictivePopExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(400),
                    targetOffsetX = { fullWidth -> fullWidth },
                )
            },
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
    val pack = spec.displayPackName ?: if (spec.pageKey == "android_related") "android" else spec.packName
    return AppUtils(context).getAppLabel(pack).toString()
}

private data class TreeRow(
    val pageKey: String,
    val title: String,
    val summary: String?,
    val packName: String? = null,
)

/** 分类页彩色徽标配色：固定色（不随深浅色模式变化，与应用图标一致）+ 白色矢量图标 */
private data class CategoryBadge(val icon: ImageVector, val color: Color)

private val categoryBadges: Map<String, CategoryBadge> = mapOf(
    "android_related" to CategoryBadge(Icons.Filled.Android, Color(0xFF3DDC84)),
    "statusbar" to CategoryBadge(Icons.Filled.SignalCellularAlt, Color(0xFF4285F4)),
    "launcher" to CategoryBadge(Icons.Filled.Home, Color(0xFFFF9800)),
    "aod" to CategoryBadge(Icons.Filled.BrightnessMedium, Color(0xFF9C27B0)),
    "lock_screen" to CategoryBadge(Icons.Filled.Lock, Color(0xFF00BCD4)),
    "application" to CategoryBadge(Icons.Filled.Apps, Color(0xFF3F51B5)),
    "miscellaneous" to CategoryBadge(Icons.Filled.MoreHoriz, Color(0xFF607D8B)),
)

/** 功能树条目左侧图标：分类页用彩色圆角徽标 + 白色矢量图标，应用页用 App 图标（圆角裁剪），无图标回退通用应用图标 */
@Composable
private fun TreeRowLeading(pageKey: String, title: String, packName: String?) {
    val badgeShape = RoundedCornerShape(10.dp)
    val badge = categoryBadges[pageKey]
    if (badge != null) {
        PrefIconBadge(badge.icon, badge.color, contentDescription = title)
        return
    }
    // 图标走进程级缓存（AppIconCache）：取图标与光栅化都在后台线程，且按包名缓存复用。
    // 旧实现每行在组合期同步 getAppIcon（binder IPC）并在主线程光栅化，行每次滑入视口都要重做一遍，
    // 滑动越快单位时间新进视口的行越多 → 卡顿越明显。
    val context = LocalContext.current
    val iconState = remember(packName) {
        packName?.takeIf { it.isNotEmpty() }
            ?.let { AppIconCache.state(context.applicationContext, it) }
    }
    val appIcon = iconState?.value
    if (appIcon != null) {
        val painter = remember(appIcon) { BitmapPainter(appIcon) }
        Image(
            painter = painter,
            contentDescription = title,
            modifier = Modifier
                .size(40.dp)
                .clip(badgeShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(badgeShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 功能树：49 页固定顺序（ScopePageRegistry.treeOrder），spec 手动不可见、单 App 作用域未安装或空索引页隐藏 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionTreeScreen(
    onOpenPage: (pageKey: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    onShowVersionInfo: () -> Unit,
    onShowRestartMenu: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val rows by produceState(initialValue = emptyList<TreeRow>(), key1 = Unit) {
        value = try {
            withContext(Dispatchers.IO) {
                ScopePageRegistry.treeOrder.mapNotNull { key ->
                    val spec = ScopePageRegistry[key] ?: return@mapNotNull null
                    if (!spec.isVisible(context)) return@mapNotNull null
                    if (!ScopePageRegistry.isScopeAppPresent(context, spec)) return@mapNotNull null
                    val index = ScopePageRegistry.buildIndex(context, spec)
                    if (index.isEmpty()) return@mapNotNull null
                    val summary = index.mapNotNull { it.title }.take(3).joinToString(" · ").ifEmpty { null }
                    val packName = if (key in categoryBadges) null else (spec.displayPackName ?: spec.packName)
                    TreeRow(key, pageTitle(context, spec), summary, packName)
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
        val layoutDirection = LocalLayoutDirection.current
        val uiMode = LocalUiMode.current
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
            // 读取不会被列表追踪（旧症状：功能树一直白屏）
            val rowsNow = rows
            ScopeScreen(state = settings, modifier = scopeModifier) {
                // 整树合并为一张分段卡片（对齐主题页分组卡片），条目顺序与原 ListItem 完全一致
                rowsNow.forEach { row ->
                    click(
                        title = row.title,
                        summary = row.summary,
                        leading = { TreeRowLeading(row.pageKey, row.title, row.packName) },
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
        if (uiMode == UiMode.Miuix) {
            CompositionLocalProvider(LocalScopeTopInset provides padding.calculateTopPadding()) {
                scopeContent()
            }
        } else {
            scopeContent()
        }
    }
}

private data class SearchEntry(val pageKey: String, val pageTitle: String, val item: PrefIndexItem)

/** 全屏搜索：索引 = 全部已注册页 headless 构建，spec 手动不可见与单 App 作用域未安装页不入索引；过滤平移旧 SearchResultAdapter 规则 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionSearchScreen(onBack: () -> Unit, onOpen: (ScopeRoute) -> Unit) {
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

    val entries by produceState(initialValue = emptyList<SearchEntry>(), key1 = Unit) {
        value = try {
            withContext(Dispatchers.IO) {
                ScopePageRegistry.all().filter { it.isVisible(context) && ScopePageRegistry.isScopeAppPresent(context, it) }.flatMap { spec ->
                    ScopePageRegistry.buildIndex(context, spec).map { item -> SearchEntry(spec.pageKey, pageTitle(context, spec), item) }
                }
            }
        } catch (t: Throwable) {
            LogUtils.e("FunctionPage", "buildSearchEntries", t.toString(), true)
            emptyList()
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
            // 读取不会被列表追踪（旧症状：搜索结果不随输入刷新）
            val queryNow = query
            val entriesNow = entries
            val filteredNow = filtered
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
                    click(title = context.getString(R.string.search_index_loading))
                }
                if (queryNow.isNotBlank() && entriesNow.isNotEmpty() && filteredNow.isEmpty()) {
                    click(title = context.getString(R.string.search_no_results))
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

    MaterialPageScaffold(
        title = route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey },
        onBack = onBack,
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
            // 顶栏不再折叠（标题恒定展开、不随上滑吸顶），两条线都不再把列表滚动联动到顶栏，
            // 因此这里不接任何 nestedScrollConnection。
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
