package com.luckyzyx.luckytool.ui.compose.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.HomeTopBarActions
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.AppCard
import com.luckyzyx.luckytool.ui.compose.components.material.materialBottomInset
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.DeviceInfoEntry

@Immutable
internal data class HomeDashboardState(
    val moduleActive: Boolean,
    val moduleVersion: String,
    val rootVersion: String? = null,
    val frameworkVersion: String? = null,
    val deviceInfo: List<DeviceInfoEntry>? = null,
    val updateVersion: String? = null,
    val showNotice: Boolean = false,
)

/** 主页只负责呈现快照与转发操作，读取服务和弹窗状态由页面持有。 */
@Composable
internal fun HomeDashboard(
    state: HomeDashboardState,
    onRestart: () -> Unit,
    onAbout: () -> Unit,
    onUpdate: () -> Unit,
    onStatusLongClick: (() -> Unit)?,
    onCopyDeviceInfo: () -> Unit,
    onDonate: () -> Unit,
    onDonateMethods: () -> Unit,
    onOpenNotice: () -> Unit,
) {
    val listState = rememberLazyListState()
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    val isCoui = LocalUiMode.current == UiMode.Coui
    val colors = if (isCoui) couiHomeColors() else homeColors()
    val bottomPadding = materialBottomInset(24.dp)
    PageScaffold(
        title = stringResource(R.string.app_name),
        actions = { HomeTopBarActions(onRestart = onRestart, onAbout = onAbout) },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // COUI 线用跟手回弹，Material 线保持平台默认的拉伸效果。
                overscrollEffect = if (isCoui) rememberCouiOverscrollEffect() else rememberOverscrollEffect(),
            ) {
                item(key = "status") {
                    HomeStatusCard(state, colors, onStatusLongClick)
                }
                if (state.updateVersion != null) {
                    item(key = "update") {
                        HomeActionCard(
                            title = stringResource(R.string.check_update_hint),
                            summary = state.updateVersion,
                            symbol = HomeSymbol.Update,
                            colors = colors,
                            onClick = onUpdate,
                            accent = true,
                        )
                    }
                }
                item(key = "device") {
                    HomeDeviceCard(
                        entries = state.deviceInfo,
                        expanded = detailsExpanded,
                        onExpandedChange = { detailsExpanded = !detailsExpanded },
                        colors = colors,
                        onCopy = onCopyDeviceInfo,
                    )
                }
                item(key = "support") {
                    HomeActionCard(
                        title = stringResource(R.string.donate_tv_title),
                        summary = "忆清鸣 · luckyzyx",
                        symbol = HomeSymbol.Support,
                        colors = colors,
                        onClick = onDonate,
                        onLongClick = onDonateMethods,
                    )
                }
                if (state.showNotice) {
                    item(key = "notice") {
                        HomeText(
                            text = stringResource(R.string.authorized),
                            role = HomeTextRole.Caption,
                            color = colors.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onOpenNotice)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStatusCard(state: HomeDashboardState, colors: HomeColors, onLongClick: (() -> Unit)?) {
    val isCoui = LocalUiMode.current == UiMode.Coui
    val tonal = state.moduleActive
    // COUI 语义色没有容器阶：启用态改用强调色标题，底色仍是卡片色。
    val accentForeground = if (tonal) colors.accent else colors.foreground
    val foreground = if (isCoui) {
        accentForeground
    } else {
        if (tonal) MaterialTheme.colorScheme.onPrimaryContainer else colors.foreground
    }
    // COUI 启用态只提亮标题，正文行仍然用摘要色。
    val secondary = if (tonal && !isCoui) foreground else colors.secondary
    // COUI 的字号来自 CouiTextKind，不叠加 Material 侧的字号覆盖。
    val statusStyle = if (isCoui) null else homeStatusTextStyle(headline = true)
    val lineStyle = if (isCoui) null else homeStatusTextStyle()
    // 版本查询返回「标题：值」，这里只保留值，标题由字符串资源统一给出。
    val root = state.rootVersion?.let(::runtimeInfoValue)?.takeUnless { it.isBlank() || it == "—" } ?: "—"
    val framework = state.frameworkVersion?.let(::runtimeInfoValue)?.takeUnless { it.isBlank() || it == "—" } ?: "Xposed"
    HomeCard(
        background = if (isCoui) colors.card else if (tonal) MaterialTheme.colorScheme.primaryContainer else colors.card,
        foreground = foreground,
        onLongClick = onLongClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeIcon(
                    symbol = if (state.moduleActive) HomeSymbol.Active else HomeSymbol.Inactive,
                    tint = if (state.moduleActive) (if (tonal) foreground else colors.accent) else colors.error,
                    modifier = Modifier.size(28.dp),
                )
                HomeText(
                    text = stringResource(if (state.moduleActive) R.string.module_isactivated else R.string.module_is_disabled),
                    role = HomeTextRole.Status,
                    color = foreground,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = statusStyle,
                )
            }
            Spacer(Modifier.height(2.dp))
            HomeText(
                "${stringResource(R.string.version)}: ${state.moduleVersion}",
                HomeTextRole.Body,
                secondary,
                style = lineStyle,
            )
            if (!state.moduleActive) {
                Spacer(Modifier.height(2.dp))
                HomeText(
                    stringResource(R.string.home_module_inactive_summary),
                    HomeTextRole.Body,
                    secondary,
                    style = lineStyle,
                )
            }
            Spacer(Modifier.height(2.dp))
            HomeText(
                "${stringResource(R.string.root_source)} $root",
                HomeTextRole.Body,
                secondary,
                style = lineStyle,
            )
            Spacer(Modifier.height(2.dp))
            HomeText(
                "${stringResource(R.string.framework_version)} $framework",
                HomeTextRole.Body,
                secondary,
                style = lineStyle,
            )
        }
    }
}

/** 版本查询返回带标题的说明，只移除首个字段分隔符之前的标题。 */
private fun runtimeInfoValue(text: String): String {
    val separator = text.indexOfFirst { it == ':' || it == '：' }
    return if (separator < 0) text else text.substring(separator + 1).trim()
}

@Composable
private fun HomeDeviceCard(
    entries: List<DeviceInfoEntry>?,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    colors: HomeColors,
    onCopy: () -> Unit,
) {
    val primary = entries?.filter { it.labelRes in listOf(R.string.model, R.string.system, R.string.version) }
    val details = entries?.filterNot { it.labelRes in listOf(R.string.model, R.string.system, R.string.version) }
    HomeCard(colors.card, colors.foreground, onLongClick = onCopy) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            if (primary == null) {
                HomeText(stringResource(R.string.home_info_loading), HomeTextRole.Body, colors.secondary)
            } else {
                primary.forEachIndexed { index, entry ->
                    if (index > 0) Spacer(Modifier.height(20.dp))
                    HomeDeviceRow(entry, colors)
                }
            }
            AnimatedVisibility(visible = expanded && !details.isNullOrEmpty()) {
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    details.orEmpty().forEach { HomeDeviceRow(it, colors) }
                }
            }
        }
        if (!details.isNullOrEmpty()) {
            val label = stringResource(if (expanded) R.string.home_device_details_hide else R.string.home_device_details_show)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .semantics { stateDescription = label }
                    .clickable(role = Role.Button, onClick = onExpandedChange)
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeText(label, HomeTextRole.Body, colors.secondary, Modifier.weight(1f))
                HomeIcon(if (expanded) HomeSymbol.Collapse else HomeSymbol.Expand, colors.secondary, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun HomeDeviceRow(entry: DeviceInfoEntry, colors: HomeColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        HomeIcon(
            symbol = when (entry.labelRes) {
                R.string.model -> HomeSymbol.Phone
                R.string.system -> HomeSymbol.System
                R.string.version -> HomeSymbol.Version
                R.string.product, R.string.flash -> HomeSymbol.Hardware
                else -> HomeSymbol.Info
            },
            tint = colors.secondary,
            modifier = Modifier.padding(top = 2.dp).size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val label = stringResource(entry.labelRes)
            HomeText(
                label,
                HomeTextRole.Title,
                colors.foreground,
            )
            HomeText(entry.value, HomeTextRole.Body, colors.secondary)
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    summary: String,
    symbol: HomeSymbol,
    colors: HomeColors,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    accent: Boolean = false,
) {
    HomeCard(colors.card, colors.foreground, onClick, onLongClick) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeIcon(symbol, if (accent) colors.accent else colors.foreground, Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HomeText(title, HomeTextRole.Title, if (accent) colors.accent else colors.foreground)
                HomeText(summary, HomeTextRole.Body, colors.secondary)
            }
            HomeIcon(HomeSymbol.Forward, colors.secondary, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HomeCard(
    background: Color,
    foreground: Color,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = background,
        contentColor = foreground,
        // COUI 外观下的卡片由 AppCard 的 COUI 分支用 clip + card 色背景绘制，这里只钉住形状来源。
        shape = if (LocalUiMode.current == UiMode.Coui) CouiPageDefaults.shape else null,
        onClick = onClick,
        onLongClick = onLongClick,
        content = content,
    )
}

private data class HomeColors(val card: Color, val foreground: Color, val secondary: Color, val accent: Color, val error: Color)

/** Material 线的取色入口，只在 [LocalUiMode] 为 Material 时调用；COUI 线走 [couiHomeColors]。 */
@Composable
private fun homeColors(): HomeColors =
    with(MaterialTheme.colorScheme) { HomeColors(surfaceBright, onSurface, onSurfaceVariant, primary, error) }

/**
 * COUI 语义色：卡片、标题、摘要与强调色都取 [CouiPageDefaults]；
 * 页面底色由 COUI 页面骨架绘制，这里不重复上色。
 *
 * COUI 色板没有 error 角色，警示态改用 onSurface 满强度表达：满强度前景色是这套色板里
 * 能表达「需要注意」的最高层级，不用 accent 是因为强调色在本页已经用来标可点项（更新卡、
 * 启用态标题），混用会让失效态看着仍像可点。当前色板下 onSurface 与 page.title 同值，
 * 所以失效卡片只看图标颜色分辨不出异常，状态语义由同卡的标题文案承担。
 */
@Composable
private fun couiHomeColors(): HomeColors {
    val page = CouiPageDefaults.themeColors()
    return HomeColors(page.card, page.title, page.summary, page.accent, CouiTheme.colorScheme.onSurface)
}

private enum class HomeTextRole { Status, Title, Body, Caption }

/** COUI 排版按现有层级对应：卡片标题走 ItemTitle，正文走 Search（与 a7 的 Body 同为 15/400），页脚提示走 GroupFooter。 */
private val HomeTextRole.couiKind: CouiTextKind
    get() = when (this) {
        HomeTextRole.Status -> CouiTextKind.ItemTitle
        HomeTextRole.Title -> CouiTextKind.ItemTitle
        HomeTextRole.Body -> CouiTextKind.Search
        HomeTextRole.Caption -> CouiTextKind.GroupFooter
    }

private val StatusHeadlineSize = 22.sp
private val StatusLineSize = 15.sp

/**
 * Material 线的排版映射，只在 Material 分支调用。
 * COUI 线的字号与行高由 [HomeTextRole.couiKind] 交给 CouiText，不经这里，
 * 所以 COUI 渲染路径上不会读到 MaterialTheme 的排版。
 */
@Composable
private fun homeTextStyle(role: HomeTextRole): TextStyle = with(MaterialTheme.typography) {
    when (role) {
        HomeTextRole.Status -> titleLarge
        HomeTextRole.Title -> titleMedium
        HomeTextRole.Body -> bodyMedium.copy(fontWeight = FontWeight.Normal)
        HomeTextRole.Caption -> bodySmall
    }
}

@Composable
private fun HomeText(
    text: String,
    role: HomeTextRole,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    style: TextStyle? = null,
) {
    // COUI 线用 CouiText 绘制，style 只服务 Material 线。
    if (LocalUiMode.current == UiMode.Coui) {
        CouiText(
            modifier = modifier.fillMaxWidth(),
            text = text,
            kind = role.couiKind,
            color = color,
            // 仪表盘文案在不传对齐时按起始对齐：COUI 的 Search 默认居中，与这里的左对齐版式不符。
            textAlign = textAlign ?: TextAlign.Start,
        )
        return
    }
    val resolved = (style ?: homeTextStyle(role)).copy(textDirection = TextDirection.ContentOrLtr)
    Text(text = text, style = resolved, color = color, modifier = modifier.fillMaxWidth(), textAlign = textAlign)
}

/** 状态卡片的文字排版：无额外字距、自然行高，与卡片内其他文案保持同一节奏。 */
@Composable
private fun homeStatusTextStyle(headline: Boolean = false): TextStyle =
    homeTextStyle(if (headline) HomeTextRole.Status else HomeTextRole.Body).copy(
        fontSize = if (headline) StatusHeadlineSize else StatusLineSize,
        letterSpacing = TextUnit.Unspecified,
        lineHeight = TextUnit.Unspecified,
    )

private enum class HomeSymbol { Active, Inactive, Phone, System, Version, Hardware, Info, Support, Update, Forward, Expand, Collapse }

@Composable
private fun HomeIcon(symbol: HomeSymbol, tint: Color, modifier: Modifier) {
    val vector: ImageVector = when (symbol) {
        HomeSymbol.Active -> Icons.Rounded.CheckCircleOutline
        HomeSymbol.Inactive -> Icons.Rounded.WarningAmber
        HomeSymbol.Phone -> Icons.Rounded.PhoneAndroid
        HomeSymbol.System -> Icons.Rounded.Settings
        HomeSymbol.Version -> Icons.Rounded.Update
        HomeSymbol.Hardware -> Icons.Rounded.Memory
        HomeSymbol.Info -> Icons.Rounded.Info
        HomeSymbol.Support -> Icons.Rounded.FavoriteBorder
        HomeSymbol.Update -> Icons.Rounded.SystemUpdateAlt
        HomeSymbol.Forward -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        HomeSymbol.Expand -> Icons.Rounded.ExpandMore
        HomeSymbol.Collapse -> Icons.Rounded.ExpandLess
    }
    Icon(vector, contentDescription = null, tint = tint, modifier = modifier)
}
