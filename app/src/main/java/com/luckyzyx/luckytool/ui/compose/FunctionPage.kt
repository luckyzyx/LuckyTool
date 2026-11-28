package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.R as LucideR
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.FunctionTopBarActions
import com.luckyzyx.luckytool.ui.compose.components.LocalPageTopInset
import com.luckyzyx.luckytool.ui.compose.components.PageScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.components.TopBarActionButton
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowItem
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowMenu
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.NoteTone
import com.luckyzyx.luckytool.ui.compose.components.PrefNote
import com.luckyzyx.luckytool.ui.compose.components.currentPrefStyle
import com.luckyzyx.luckytool.ui.compose.components.dialogListMaxHeight
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTextField
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
import java.util.Arrays

/** 功能标签页只展示功能树；搜索是独立的一级标签页，功能页由二级页宿主 PageActivity 承载。 */
@Composable
fun FunctionPage(
    activity: MainActivity,
    onOpenPage: (pageKey: String, title: String) -> Unit,
) {
    // 面板外壳自己要按 visible 常驻组合才能跑退场动画，所以状态留在这里、外壳不包 if
    val showVersionInfo = remember { mutableStateOf(false) }
    var showRestartMenu by remember { mutableStateOf(false) }

    VersionInfoSheet(visible = showVersionInfo)

    if (showRestartMenu) {
        RestartMenuUtils.RestartMenuDialog(activity) { showRestartMenu = false }
    }

    FunctionTreeScreen(
        onOpenPage = onOpenPage,
        onShowVersionInfo = { showVersionInfo.value = true },
        onShowRestartMenu = { showRestartMenu = true },
    )
}

/** 功能树：50 页固定顺序（PageRegistry.treeOrder），spec 手动不可见、单 App 作用域未安装或空索引页隐藏（selfRendered 特殊页除外） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FunctionTreeScreen(
    onOpenPage: (pageKey: String, title: String) -> Unit,
    onShowVersionInfo: () -> Unit,
    onShowRestartMenu: () -> Unit,
) {
    val context = LocalContext.current
    // 以 Configuration 作 key：语言/字号等变化后需要重建行数据里的页面标题
    val configuration = LocalConfiguration.current
    val iconSize = with(LocalDensity.current) { 48.dp.roundToPx() }
    // 先渲染 App 启动时预取的旧快照，后台重算后替换：不再现场跑 50 页偏好 DSL + PackageManager IPC
    val snapshot by FunctionPageData.snapshot.collectAsStateWithLifecycle()
    LaunchedEffect(configuration, iconSize) {
        FunctionPageData.revalidate(context, iconSize)
    }
    val rows = snapshot?.rows?.map { FunctionRow(it.pageKey, it.title, it.summary, it.packName) }.orEmpty()

    PageScaffold(
        title = stringResource(R.string.nav_function),
        actions = {
            FunctionTopBarActions(
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

/** 功能页宿主：类型安全 PageRoute → 任意已注册 PageSpec（MainShell 跨 tab 全屏直达也复用） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageHost(
    route: PageRoute,
    onBack: () -> Unit,
    onNavigate: (pageKey: String, title: String?) -> Unit,
) {
    val context = LocalContext.current
    val spec = PageRegistry[route.pageKey]
    var showRestartScope by remember { mutableStateOf(false) }

    // 有图标且声明 showAsAction 的直接显示为图标按钮，其余收进溢出菜单
    val topActions = spec?.topActions.orEmpty()
    val overflowActions = topActions.filter { !(it.showAsAction && it.icon != 0) }

    PageScaffold(
        title = route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey },
        onBack = onBack,
        collapseEnabled = false,
        actions = {
            if (spec?.restartEnabled == true) {
                if (LocalUiMode.current == UiMode.Coui) {
                    TopBarActionButton(
                        onClick = { showRestartScope = true },
                        contentDescription = stringResource(R.string.menu_reboot),
                        painter = painterResource(LucideR.drawable.lucide_ic_power),
                    )
                } else {
                    TopBarActionButton(
                        onClick = { showRestartScope = true },
                        contentDescription = stringResource(R.string.menu_reboot),
                        materialIcon = Icons.Filled.PowerSettingsNew,
                    )
                }
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
                // 错误态文字：Material 用 error 色，COUI 的色板没有 error 角色，
                // 由样式位退回 onSurface 满强度（整屏只有这一行字，不会被误读成普通提示）
                PrefNote(text = "Unknown page: ${route.pageKey}", tone = NoteTone.Error)
            }
        } else {
            val state = remember(spec) { PrefState.of(context.applicationContext, spec.prefsName) }
            val sendPrefsValue: (key: String, value: Any) -> Unit = { key, value ->
                context.sendPrefsValue(spec.packName, key, value)
            }
            val jumpTarget = if (route.scrollKey.isNotBlank() && route.scrollPosition >= 0) {
                ScrollTarget(route.scrollKey, route.scrollPosition)
            } else {
                null
            }
            val navigatePage: (target: String, title: String?) -> Unit = { target, title ->
                PageRegistry.pageTargetMap[target]?.let { onNavigate(it, title) }
            }
            // 功能页的刷新由 PrefState.revision + per-key flow 覆盖（写入 → bump → 构建 lambda 重跑），
            // 不再向页面注入 onRestart，也不再重建 Activity；需要重启作用域时用顶栏菜单。
            //
            // 页面内容分派（唯一分派点见 PageContent）：
            // 页面为当前主题线提供了内容槽位时由页面自有布局渲染整页，
            // 否则逐字回落到共享渲染层 PageScreen —— PageScreen 自带 LazyColumn 并渲染全部条目，
            // 直接作为页面内容：不能再包一层 ExpressiveList（同向嵌套 LazyColumn 会以无限高度约束测量而崩溃）。
            // 水平内边距此处不叠加（由作用域内容自带）。
            //
            // 骨架内边距传递：顶栏高度（padding 的 top）经 LocalPageTopInset 交给作用域列表的
            // contentPadding，列表视口铺满整屏、滚动中的行才能进入顶栏下方；水平与底部内边距
            // 仍留在视口外。默认折叠行为由页面骨架统一连接，内容不重复接入同一条滚动连接。
            val layoutDirection = LocalLayoutDirection.current
            val pageContentModifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
            val topInset = padding.calculateTopPadding()
            val pageContent: @Composable () -> Unit = {
                CompositionLocalProvider(LocalPageTopInset provides topInset) {
                    PageContent(
                        spec = spec,
                        state = state,
                        modifier = pageContentModifier,
                        sendValue = sendPrefsValue,
                        scrollTarget = jumpTarget,
                        onNavigate = navigatePage,
                    )
                }
            }
            pageContent()
        }
    }

    if (showRestartScope) {
        RestartMenuUtils.RestartScopeDialog(context, spec?.scopes ?: emptyArray()) {
            showRestartScope = false
        }
    }
}

/**
 * 版本信息面板：xposed_scope 各包版本表（Markdown 表格经 Markwon 渲染）。
 *
 * visible 常驻传入，两种主题各自由自己的面板实现决定挂载与退场。
 */
@Composable
private fun VersionInfoSheet(visible: MutableState<Boolean>) {
    currentPrefStyle().Sheet(
        visible = visible,
        title = stringResource(R.string.menu_versioninfo),
        modifier = Modifier,
        closeLabel = stringResource(R.string.close),
        doneLabel = stringResource(R.string.sheet_done),
    ) {
        VersionInfoTable()
    }
}

/**
 * 版本表正文。
 *
 * 只在面板内容槽里组合：两种面板实现都会在不可见时跳过内容，所以每次进功能页不会白跑一遍包信息查询。
 *
 * 高度上限加纵向滚动是补 material 那一侧的：ModalBottomSheet 不替正文滚动，长表格会被裁掉；
 * COUI 面板本来就滚正文、视口还更小，这里的上限只在内层生效，不会和面板抢手势。
 */
@Composable
private fun VersionInfoTable() {
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
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = dialogListMaxHeight(designMax = 420.dp, chrome = 160.dp))
            .verticalScroll(rememberScrollState())
    ) {
        AndroidView(
            factory = { ctx -> android.widget.TextView(ctx) },
            modifier = Modifier.fillMaxWidth(),
            update = { tv ->
                tv.setTextIsSelectable(true)
                markwon.setMarkdown(tv, markdown)
            },
        )
    }
}