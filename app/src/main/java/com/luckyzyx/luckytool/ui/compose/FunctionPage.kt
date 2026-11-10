package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.FunctionTopBarActions
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.components.TopBarActionButton
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowItem
import com.luckyzyx.luckytool.ui.compose.components.TopBarOverflowMenu
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
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
import java.util.Arrays

/** 功能标签页只展示功能树；搜索是独立的一级标签页，作用域页由同一个导航栈管理。 */
@Composable
fun FunctionPage(
    activity: MainActivity,
    onOpenPage: (pageKey: String, title: String) -> Unit,
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
        onShowVersionInfo = { showVersionInfo = true },
        onShowRestartMenu = { showRestartMenu = true },
    )
}

/** 功能树：50 页固定顺序（ScopePageRegistry.treeOrder），spec 手动不可见、单 App 作用域未安装或空索引页隐藏（selfRendered 特殊页除外） */
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

    PageScaffold(
        title = route.title.ifBlank { spec?.let { pageTitle(context, it) } ?: route.pageKey },
        onBack = onBack,
        collapseEnabled = false,
        actions = {
            if (spec?.restartEnabled == true) {
                TopBarActionButton(
                    onClick = { showRestartScope = true },
                    contentDescription = stringResource(R.string.menu_reboot),
                    materialIcon = Icons.Filled.PowerSettingsNew,
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
                when (LocalUiMode.current) {
                    UiMode.Material -> Text(
                        "Unknown page: ${route.pageKey}",
                        color = MaterialTheme.colorScheme.error,
                    )

                    // COUI 的色板没有 error 角色：错误态用 onSurface 满强度表达，
                    // 整屏只有这一行字，用常态正文色也不会被误读成普通提示
                    UiMode.Coui -> CouiText(
                        text = "Unknown page: ${route.pageKey}",
                        kind = CouiTextKind.Search,
                        textAlign = TextAlign.Start,
                        color = CouiTheme.colorScheme.onSurface,
                    )
                }
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
                ScopePageRegistry.pageTargetMap[target]?.let { onNavigate(it, title) }
            }
            // 作用域页的刷新由 PrefState.revision + per-key flow 覆盖（写入 → bump → 构建 lambda 重跑），
            // 不再向页面注入 onRestart，也不再重建 Activity；需要重启作用域时用顶栏菜单。
            //
            // 页面内容分派（唯一分派点见 ScopePageContent）：
            // 页面为当前主题线提供了内容槽位时由页面自有布局渲染整页，
            // 否则逐字回落到共享渲染层 ScopeScreen —— ScopeScreen 自带 LazyColumn 并渲染全部条目，
            // 直接作为页面内容：不能再包一层 ExpressiveList（同向嵌套 LazyColumn 会以无限高度约束测量而崩溃）。
            // 水平内边距此处不叠加（由作用域内容自带）。
            //
            // 骨架内边距传递：顶栏高度（padding 的 top）经 LocalScopeTopInset 交给作用域列表的
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
                CompositionLocalProvider(LocalScopeTopInset provides topInset) {
                    ScopePageContent(
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

        UiMode.Coui -> CouiAlertDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.menu_versioninfo),
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                style = CouiDialogActionStyle.Primary,
                // Primary 样式必须显式给按钮配色，缺 colors 构造即抛异常
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            content = {
                // 正文槽本身已经是纵向滚动容器，这里不再套 verticalScroll：
                // 两层同向滚动会先被内层吃掉手势，长表格滚不到底
                AndroidView(
                    factory = { ctx -> android.widget.TextView(ctx) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CouiDialogDefaults.contentPadding),
                    update = { tv ->
                        tv.setTextIsSelectable(true)
                        markwon.setMarkdown(tv, markdown)
                    },
                )
            },
        )
    }
}