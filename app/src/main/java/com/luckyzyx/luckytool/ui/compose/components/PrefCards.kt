package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.switch.CouiSwitchDefaults
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPreferenceGroup
import com.luckyzyx.luckytool.ui.compose.components.AppSwitchItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 卡片行 / 卡原语，支持分段卡片与独立卡片。
 *
 * **「设置区块」的唯一 API 是 `PrefScope` DSL**（`group()` / `switch()` / `click()` / `list()` /
 * `custom()`，见 `ui/compose/components/PrefScope.kt`）—— 所有标准设置页与作用域页都走它。
 * 走不了 DSL 的页面（`custom()` 里的自定义内容）用 [PrefGroup]：它与 DSL 的 `group()` 共用同一份
 * 标题 / 卡片 / 行分隔线实现，页面因此不必再自判内缩、自画标题。
 * 本文件其余部分是 `custom()` 逃生舱里的原始行 / 卡原语：
 * [PrefRow] + [PrefSwitchRow]（[PrefGroup] 内的行，形状由所属卡片决定）、
 * [PrefCard] + [PrefSwitchCard]（不属于任何分段组的独立卡片）、[PrefIcon]（行首图标）、
 * [PrefColorPreview]（行尾颜色预览色块）。
 *
 * UI 线分派：material 线用 `Segmented*` 原语 + `MaterialTheme`；
 * COUI 线用 `com.itos.coui.layout.Item` + `couiButtonFeedback`；分派开关统一读 [LocalUiMode]。
 *
 * **版式三层归属**（页面 / 分组 / 行各管一段，跨层补就会叠内缩、叠卡片）：
 * 1. 页面：只决定分组的先后与分组之外的内容，整页不判外观线；需要与卡片左右对齐时用 [pageInset]；
 * 2. 分组：唯一入口是 DSL 的 `group()` 与本文件的 [PrefGroup]（两者都落到 [PrefSectionRows]），
 *    标题 / 说明 / 卡片 / 行间分隔线 / 分组上沿留白都在那里给；
 * 3. 行：DSL 页面用 `switch()` / `list()` / `click()`，`custom()` 里用 [PrefRow] / [PrefSwitchRow]，
 *    行本身不再判外观线内缩。
 *
 * 特殊组件（滑条、图表、自定义卡）放 `custom()` / `fullContent` —— 那是 DSL 的正规逃生舱：
 * 只放内容与交互，卡片、分组标题、分组间距仍由上面分层给。
 */

/**
 * 设置区块：页面自绘的行 + 一套统一的分组版式（内缩 / 标题 / 说明 / 卡片 / 行间分隔线）。
 *
 * 一份实现、两个作者面：DSL 页面在 `PrefScopeBuilder` 作用域写 `group(title, summary) { ... }`
 * （见 `PrefScope.kt`），走不了 DSL 的页面（`custom()` / `fullContent` 里的组合式内容、应用权限页）
 * 写本函数。两者最后都落到 [PrefSectionRows]，所以标题、卡片、行间分隔线、行级显隐只有一份，
 * 页面不必再自己画分组标题、判断外观线内缩、或用 Spacer 分隔分组：
 *
 * ```
 * PrefGroup(title = "标题", summary = "说明", footer = "底部说明") {
 *     item { PrefSwitchRow("开关", checked = a, onCheckedChange = { ... }) }
 *     // 显隐不定的行给显式 key：缺省 key 是条目序号，前面的行增减会让后面的行跟着换 key
 *     item(key = "advanced", visible = advanced) { PrefSwitchRow("高级", ...) }
 * }
 * ```
 *
 * 版式归属：水平内缩与分组上沿留白都由本入口给（COUI 线卡片自带内缩，material 线这里补 16dp），
 * 调用方不要再加水平 padding（会叠成 32dp），也不要在相邻分组之间插 Spacer。
 *
 * @param title 分组标题，null / 空表示无标题（不占标题行）
 * @param summary 标题下方的分组说明
 * @param footer 卡片下方的分组说明
 */
@Composable
fun PrefGroup(
    title: String? = null,
    summary: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable PrefCardScope.() -> Unit,
) {
    val collected = PrefCardEntries()
    content(
        object : PrefCardScope {
            override fun item(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
                collected.add(key, visible, content)
            }
        },
    )
    PrefSectionRows(
        title = title,
        summary = summary,
        footer = footer,
        modifier = modifier,
        rows = collected.entries.map { entry ->
            PrefCardRow(key = entry.key, visible = entry.visible) { entry.content() }
        },
    )
}

/**
 * 分组渲染的唯一实现：[PrefGroup] 与 DSL 的 `group()` 都走这里，行由 [PrefCardRow] 描述。
 *
 * COUI 线一个分组就是一张卡片（[CouiPrefGroup] + [CouiRowsColumn]）。material 线用同一套分段圆角
 * （[MaterialPreferenceGroup] 内部就是 `MaterialGroup`），差别只在容器：DSL 页面的 material 线由
 * [ScopeScreen] 把行留在 `LazyColumn` 里逐条渲染（需要 `animateItem()` 的逐行动画），这里则是整块 `Column`。
 *
 * [PrefCardRow.background] 只在 COUI 线生效，用于搜索命中的行底色。
 */
@Composable
internal fun PrefSectionRows(
    rows: List<PrefCardRow>,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    footer: String? = null,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        CouiPrefGroup(title = title, summary = summary, footer = footer, modifier = modifier) {
            CouiRowsColumn(rows = rows)
        }
    } else {
        Column(modifier = modifier) {
            if (!title.isNullOrEmpty()) PrefCategoryHeader(title = title, summary = summary)
            MaterialPreferenceGroup(
                entries = rows.map { PrefCardEntry(key = it.key, visible = it.visible, content = it.content) },
                modifier = Modifier.pageInset(),
            )
            if (!footer.isNullOrEmpty()) PrefGroupFooter(footer)
        }
    }
}

/**
 * 页内非分组内容的水平内缩：搜索栏、独立按钮、滑条卡、面板里的搜索框，都靠它把左右边界对到 [PrefGroup] 的卡片上。
 *
 * 全应用只有这一处给「内容对齐卡片边界」的数值 —— material 线分段卡与 COUI 库 Group 同为 16dp，两条线因此共用同一个数。
 * **分组内容不要用它**：内缩已由 [PrefGroup] / DSL 的 `group()` 给，再套一层会叠成 32dp。
 */
internal val PageInset = 16.dp

/** 见 [PageInset]。 */
internal fun Modifier.pageInset(): Modifier = padding(horizontal = PageInset)

/** 分组卡片内的普通条目（形状由所属卡片决定，见 [PrefGroup] / DSL 的 `group()`） */
@Composable
fun PrefRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        val colors = CouiPageDefaults.themeColors()
        val dark = CouiTheme.isDark
        val source = remember { MutableInteractionSource() }
        // COUI Item 只提供单击：带长按的行改用 combinedClickable 自行挂手势，点击仍走同一交互源
        val gesture = if (onLongClick != null) {
            Modifier
                .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = { onClick?.invoke() },
                )
        } else {
            Modifier
        }
        val summarySlot: (@Composable () -> Unit)? = summary?.let { text ->
            { CouiRowSummary(text, enabled, colors) }
        }
        CouiItem(
            modifier = modifier.then(gesture),
            icon = leading,
            onClick = if (onLongClick == null) onClick else null,
            enabled = enabled,
            title = { CouiRowTitle(title, enabled, colors) },
            summary = summarySlot,
            trailing = { trailing?.invoke() },
        )
    } else {
        // Material 线专用：COUI 线已在上面的 CouiItem 分支返回，这里交给 M3 列表行渲染
        AppListItem(
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            headlineContent = { Text(title) },
            // Material 线专用：标题与说明由 M3 列表行自绘文本槽，COUI 线走 CouiRowTitle / CouiRowSummary
            supportingContent = summary?.let { { Text(it) } },
            leadingContent = leading,
            trailingContent = trailing,
        )
    }
}

/** 设置入口的单色图标：COUI 线用 [painter]（Lucide 资源），Material 线用 [icon]；二者至少提供一个。 */
@Composable
fun PrefIcon(icon: ImageVector? = null, painter: Painter? = null) {
    if (LocalUiMode.current == UiMode.Coui) {
        val resolved = painter ?: icon?.let { rememberVectorPainter(it) }
        if (resolved != null) {
            Icon(
                painter = resolved,
                contentDescription = null,
                tint = CouiTheme.colorScheme.onSurface,
            )
        }
    } else {
        // Material 线专用：图标前景色取 M3 主题；COUI 线在上一分支已返回
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** 行尾颜色预览的边长：与行尾图片缩略图 [PrefImagePreview] 的行高同取 40dp */
private val ColorPreviewSize = 40.dp

/** 行尾颜色预览的圆角：正方形 + 6dp 圆角（与 [PrefImagePreview] 的圆角一致） */
private val ColorPreviewCornerRadius = 6.dp

/**
 * 行尾颜色预览色块：正方形 + 圆角，用于颜色偏好条目右侧。
 *
 * 形态对齐 [PrefImagePreview]：同一 40dp 边长、同一 6dp 圆角，只是没有按图片比例拉宽，
 * 因此是正方形 + 圆角而不是圆点、胶囊或宽扁的矩形。
 *
 * 描边不能取主题色的 `outline`：主题色本身可以选到白色，此时列表行与弹层都是浅色，
 * 白色描边等于没有描边，而这两个颜色偏好的默认值正是 `#FFFFFFFF`，色块会整块看不见。
 */
@Composable
fun PrefColorPreview(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ColorPreviewCornerRadius)

    Box(
        modifier
            .size(ColorPreviewSize)
            .clip(shape)
            .background(color)
            .border(1.dp, contrastBorder(color), shape)
    )
}

/**
 * 与 [color] 形成对比的描边色（[PrefColorPreview] 与颜色选择器的预览色块共用）。
 *
 * 只按颜色自身取反、不跟主题色走：浅色用深描边，深色用浅描边；半透明色一律用深描边，
 * 因为它底下透出的是浅色内容（选择器里是浅灰棋盘格）。这样无论主题色与所选颜色怎么组合，
 * 色块本体与轮廓至少有一个看得清 —— 「白色主题色 + 白色颜色」原先是两者都看不见的组合。
 */
internal fun contrastBorder(color: Color): Color =
    if (color.alpha < 0.5f || color.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }

/** 分段卡片组内的开关条目：整行点击切换，尾部为 Expressive 开关 */
@Composable
fun PrefSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        val colors = CouiPageDefaults.themeColors()
        val dark = CouiTheme.isDark
        val source = remember { MutableInteractionSource() }
        val summarySlot: (@Composable () -> Unit)? = summary?.let { text ->
            { CouiRowSummary(text, enabled, colors) }
        }
        // 整行 toggleable 承载点击切换，尾部开关只取视觉与拖动，两者共用同一个交互源
        CouiItem(
            modifier = modifier
                .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
                .toggleable(
                    value = checked,
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
            icon = leading,
            title = { CouiRowTitle(title, enabled, colors) },
            summary = summarySlot,
            trailing = {
                CouiSwitch(
                    modifier = Modifier.padding(start = 16.dp),
                    checked = checked,
                    onCheckedChange = null,
                    dark = dark,
                    colors = CouiSwitchDefaults.colors(colors.accent, dark),
                    enabled = enabled,
                    interactionSource = source,
                    onDragCheckedChange = onCheckedChange,
                )
            },
        )
    } else {
        Box(modifier = modifier) {
            AppSwitchItem(
                title = title,
                summary = summary,
                checked = checked,
                enabled = enabled,
                leading = leading,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/**
 * `custom()` 逃生舱里行内容的标准水平内缩包装，与 DSL 的 switch()/list()/click() 等分派一致：
 * material 线套 16dp 内缩（material 列表无水平 contentPadding，行需自带）；
 * COUI 线不套（CouiItem 自带 16dp 行内缩）。
 */
@Composable
fun PrefRowInset(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (LocalUiMode.current == UiMode.Coui) {
                    Modifier
                } else {
                    Modifier.padding(horizontal = 16.dp)
                },
            ),
        content = content,
    )
}

/**
 * 独立卡片（不属于任何分段组时使用）。
 * 外观与主题页一致：Material（surfaceBright 容器 + 16dp 外圆角）/
 * COUI 整卡（`CouiPageDefaults.shape` 24dp 圆角 + 卡片底色，不成组，故不套库的 Group）。
 */
@Composable
fun PrefCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        // 单条即整张卡：调用方的 modifier 交给卡片（横向内缩与底色都在卡上），行内不再叠
        CouiCard(modifier = modifier) {
            PrefRow(
                title = title,
                summary = summary,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                trailing = trailing,
            )
        }
    } else {
        AppItem(index = 0, count = 1) {
            PrefRow(
                title = title,
                modifier = modifier,
                summary = summary,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                trailing = trailing,
            )
        }
    }
}

/** 独立开关卡片（不属于任何分段组时使用） */
@Composable
fun PrefSwitchCard(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        CouiCard(modifier = modifier) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                summary = summary,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    } else {
        AppItem(index = 0, count = 1) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
