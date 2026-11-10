package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.menu.Menu as CouiMenu
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialDropdownItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialGroup
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialGroupScope
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialListItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialRadioItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTonalCard
import com.luckyzyx.luckytool.ui.compose.components.material.materialGroupColors
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 页面列表的外观分派层：同一份调用在 COUI 外观下渲染 COUI 分组卡片与行，
 * 在 Material 外观下渲染 material3 分段列表。
 *
 * 页面只依赖这里的 App* 名称，不再区分两条线的排版差异；形状、配色与间距全部由对应外观线
 * 自己决定。需要在行内自行拼内容的场景直接用 material 或 COUI 的原语，不经过本文件。
 */

/** 行在 COUI 分组卡片中的位置；null 表示独立成卡（行自己画卡片背景与圆角）。 */
private data class CouiRowSlot(val index: Int, val inGroup: Boolean)

private val LocalCouiRowSlot = compositionLocalOf<CouiRowSlot?> { null }

/**
 * 祖先是否已经画好卡片背景。[CouiSegmentCard] 这类分段容器在自己这一层铺底色与圆角，
 * 行容器据此不要再补一层同色卡片，否则会出现「外层分段卡 + 内层整卡」的双层底色。
 */
private val LocalCouiRowFramed = compositionLocalOf { false }

private val isCouiAppearance: Boolean
    @Composable get() = LocalUiMode.current == UiMode.Coui

/** COUI 分组卡片：一张卡片承载整组行，行间由 [CouiDivider] 切分。 */
@Composable
private fun CouiGroupCard(
    modifier: Modifier = Modifier,
    title: String = "",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CouiPageDefaults.themeColors()
    Column(modifier = modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)) {
                CouiText(text = title, kind = CouiTextKind.GroupTitle, color = colors.accent)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CouiPageDefaults.shape)
                .background(colors.card),
            content = content,
        )
    }
}

/**
 * COUI 行容器，按祖先状态决定行自己要不要画卡片：
 * - 分组内（[LocalCouiRowSlot] 的 inGroup 为 true）：透明行，首行之外画分隔线，末行的线由分组卡负责；
 * - 祖先已画好卡片（[LocalCouiRowFramed] 为 true）：只占满宽度，底色与分段线都归祖先；
 * - 其余情况：行自己成卡。
 */
@Composable
private fun CouiRowFrame(content: @Composable () -> Unit) {
    val slot = LocalCouiRowSlot.current
    val colors = CouiPageDefaults.themeColors()
    when {
        slot != null && slot.inGroup -> Column(modifier = Modifier.fillMaxWidth()) {
            if (slot.index > 0) CouiDivider()
            content()
        }
        // 分段卡只给首尾画圆角、行间画线，行内再铺一层卡片就会叠出两块圆角与两层底色
        LocalCouiRowFramed.current -> Column(modifier = Modifier.fillMaxWidth()) { content() }
        else -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CouiPageDefaults.shape)
                .background(colors.card),
        ) {
            content()
        }
    }
}

/** 分组：静态条目列表 */
@Composable
fun AppGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    visibleLen: Int = 0,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return
    if (!isCouiAppearance) {
        MaterialGroup(modifier = modifier, title = title, visibleLen = visibleLen, content = content)
        return
    }
    CouiGroupCard(modifier = modifier, title = title) {
        content.forEachIndexed { index, itemContent ->
            CompositionLocalProvider(LocalCouiRowSlot provides CouiRowSlot(index, inGroup = true)) {
                itemContent()
            }
        }
    }
}

/** 分组：条目可显隐，显隐时整行缩放进出 */
@Composable
fun AppGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: MaterialGroupScope.() -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialGroup(modifier = modifier, title = title, content = content)
        return
    }
    val entries = MaterialGroupScope().apply(content).entries
    if (entries.isEmpty()) return
    // 分隔线只跟可见行算位置：隐藏的行不占位，也不保留分组端点
    val visibleIndices = entries.indices.filter { entries[it].visible }
    CouiGroupCard(modifier = modifier, title = title) {
        entries.forEachIndexed { index, entry ->
            AnimatedVisibility(
                visible = entry.visible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                CompositionLocalProvider(
                    LocalCouiRowSlot provides CouiRowSlot(
                        index = visibleIndices.indexOf(index).coerceAtLeast(0),
                        inGroup = true,
                    ),
                ) {
                    entry.content()
                }
            }
        }
    }
}

/** 单条目形状提供者：Material 侧给分段圆角，COUI 侧把行标成组内条目 */
@Composable
fun AppItem(
    index: Int,
    count: Int,
    content: @Composable () -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialItem(index = index, count = count, content = content)
        return
    }
    if (LocalCouiRowSlot.current?.inGroup == true) {
        CompositionLocalProvider(LocalCouiRowSlot provides CouiRowSlot(index, inGroup = true)) {
            content()
        }
        return
    }
    // 没有分组容器承载（列表直接平铺行）：按 index/count 自绘分段卡片，
    // 与 CouiGroupCard 的观感一致，相邻行拼起来仍是一张连续卡片。
    CouiSegmentCard(index = index, count = count) { content() }
}

/** 分段卡片的圆角：与 COUI 分组卡片的 24dp 圆角同一取值。 */
private val CouiSegmentCorner = 24.dp

/** 平铺行的分段卡片：组首圆上两角、组尾圆下两角、组内直边，行间由除末行外的切片绘制分隔线。 */
@Composable
private fun CouiSegmentCard(index: Int, count: Int, content: @Composable () -> Unit) {
    val colors = CouiPageDefaults.themeColors()
    val shape: Shape = when {
        count <= 1 -> CouiPageDefaults.shape
        index <= 0 -> RoundedCornerShape(topStart = CouiSegmentCorner, topEnd = CouiSegmentCorner)
        index >= count - 1 -> RoundedCornerShape(bottomStart = CouiSegmentCorner, bottomEnd = CouiSegmentCorner)
        else -> RectangleShape
    }
    CompositionLocalProvider(LocalCouiRowFramed provides true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.card),
        ) {
            content()
            if (index < count - 1) CouiDivider()
        }
    }
}

/** 通用行：标题 + 说明 + 首尾内容槽 */
@Composable
fun AppListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ListItemColors = materialGroupColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    /** COUI 行首图标槽边长，默认与 CouiItem 一致；Material 线不使用 */
    iconSize: Dp = CouiPageDefaults.iconSize,
) {
    if (!isCouiAppearance) {
        MaterialListItem(
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
            headlineContent = headlineContent,
            overlineContent = overlineContent,
            supportingContent = supportingContent,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
        )
        return
    }
    val title: @Composable () -> Unit = {
        Column {
            overlineContent?.invoke()
            headlineContent()
        }
    }
    CouiRowFrame {
        // CouiItem 只装点击；需要长按时按同一排版自建行，保证两条路径的间距与反馈一致
        if (onLongClick != null) {
            CouiLongPressItem(
                modifier = modifier,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                leading = leadingContent,
                title = title,
                summary = supportingContent,
                trailing = trailingContent,
                iconSize = iconSize,
            )
        } else {
            CouiItem(
                modifier = modifier,
                icon = leadingContent,
                iconSize = iconSize,
                title = title,
                summary = supportingContent,
                trailing = { trailingContent?.invoke() },
                onClick = onClick,
                enabled = enabled,
            )
        }
    }
}
/** COUI 行的长按形态：CouiItem 只装点击，需要长按时按同一排版自建行。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CouiLongPressItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
    leading: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    summary: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    iconSize: Dp = CouiPageDefaults.iconSize,
) {
    val dark = CouiTheme.isDark
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CouiPageDefaults.rowMinHeight)
            .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
            .padding(horizontal = CouiPageDefaults.rowPadding, vertical = CouiPageDefaults.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.padding(end = CouiPageDefaults.iconSpacing).size(iconSize),
                contentAlignment = Alignment.Center,
            ) { leading() }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CouiPageDefaults.summarySpacing),
        ) {
            title()
            summary?.invoke()
        }
        trailing?.invoke()
    }
}

/** 开关行：点击整行切换，行尾为 COUI 开关 */
@Composable
fun AppSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    colors: ListItemColors = materialGroupColors(),
    checked: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialSwitchItem(
            icon = icon,
            title = title,
            summary = summary,
            colors = colors,
            checked = checked,
            enabled = enabled,
            leading = leading,
            onCheckedChange = onCheckedChange,
        )
        return
    }
    val pageColors = CouiPageDefaults.themeColors()
    val dark = CouiTheme.isDark
    val source = remember { MutableInteractionSource() }
    CouiRowFrame {
        CouiItem(
            modifier = Modifier.toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
            icon = leading ?: icon?.let { vector ->
                { CouiIcon(vector, pageColors.title, CouiPageDefaults.iconSize) }
            },
            title = { CouiRowTitle(title, enabled, pageColors) },
            summary = summary?.let { text -> { CouiRowSummary(text, enabled, pageColors) } },
            trailing = {
                CouiSwitch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = null,
                    interactionSource = source,
                    onDragCheckedChange = { onCheckedChange(it) },
                    dark = dark,
                )
            },
        )
    }
}

/** 单选项行：点击整行选中，行首为 COUI 单选按钮 */
@Composable
fun AppRadioItem(
    title: String,
    summary: String? = null,
    colors: ListItemColors = materialGroupColors(),
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialRadioItem(
            title = title,
            summary = summary,
            colors = colors,
            selected = selected,
            enabled = enabled,
            onClick = onClick,
        )
        return
    }
    val pageColors = CouiPageDefaults.themeColors()
    val source = remember { MutableInteractionSource() }
    CouiRowFrame {
        CouiItem(
            modifier = Modifier.selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
            icon = { CouiRadioButton(selected = selected, enabled = enabled) },
            title = { CouiRowTitle(title, enabled, pageColors) },
            summary = summary?.let { text -> { CouiRowSummary(text, enabled, pageColors) } },
        )
    }
}

/** 下拉选择行：行尾显示当前项，点击在触点处展开 COUI 菜单 */
@Composable
fun AppDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = materialGroupColors(),
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialDropdownItem(
            icon = icon,
            title = title,
            summary = summary,
            items = items,
            colors = colors,
            enabled = enabled,
            leading = leading,
            onClick = onClick,
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected,
        )
        return
    }
    val pageColors = CouiPageDefaults.themeColors()
    val dark = CouiTheme.isDark
    val source = remember { MutableInteractionSource() }
    val anchor = rememberCouiMenuAnchor()
    var expanded by remember { mutableStateOf(false) }
    val hasItems = items.isNotEmpty()
    val safeIndex = if (hasItems) selectedIndex.coerceIn(0, items.lastIndex) else -1
    val menuItems = items.mapIndexed { index, text ->
        CouiMenuItem(value = index, text = text, selected = index == safeIndex)
    }
    CouiRowFrame {
        Box {
            CouiItem(
                modifier = Modifier
                    .couiMenuAnchor(anchor)
                    .couiButtonFeedback(
                        dark = dark,
                        interactionSource = source,
                        enabled = enabled,
                        list = true,
                        // 菜单展开期间保持按压底色，行在弹层打开时不失去高亮
                        hoverLocked = expanded,
                    )
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                    ) {
                        onClick?.invoke()
                        anchor.open { expanded = true }
                    },
                icon = leading ?: icon?.let { vector ->
                    { CouiIcon(vector, pageColors.title, CouiPageDefaults.iconSize) }
                },
                title = { CouiRowTitle(title, enabled, pageColors) },
                summary = summary?.let { text -> { CouiRowSummary(text, enabled, pageColors) } },
                trailing = {
                    CouiRowValue(
                        text = if (hasItems && safeIndex >= 0) items[safeIndex] else "",
                        enabled = enabled,
                        colors = pageColors,
                    )
                    CouiIcon(
                        vector = CouiIconsExt.Popup,
                        tint = couiTrailingIconTint(dark),
                        size = 14.dp,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                },
            )
            CouiMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                items = menuItems,
                onItemClick = { item ->
                    expanded = false
                    if (item.value in items.indices) onItemSelected(item.value)
                },
                anchor = anchor,
            )
        }
    }
}

/** 卡片：内容自带的排版不变，只换容器颜色与圆角来源 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    shape: Shape? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    if (!isCouiAppearance) {
        MaterialTonalCard(
            modifier = modifier,
            containerColor = containerColor.takeOrElse { MaterialTheme.colorScheme.surfaceBright },
            contentColor = contentColor.takeOrElse {
                contentColorFor(containerColor.takeOrElse { MaterialTheme.colorScheme.surfaceBright })
            },
            shape = shape ?: MaterialTheme.shapes.large,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interactionSource,
            content = content,
        )
        return
    }
    val pageColors = CouiPageDefaults.themeColors()
    val resolvedShape = shape ?: CouiPageDefaults.shape
    val background = containerColor.takeOrElse { pageColors.card }
    val interaction = when {
        onLongClick != null -> Modifier.combinedClickable(
            enabled = enabled,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
        )
        onClick != null -> Modifier.clickable(
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
        )
        else -> Modifier
    }
    val resolvedContentColor = contentColor.takeOrElse {
        if (background == pageColors.card) pageColors.title else Color.Unspecified
    }
    val container = modifier.clip(resolvedShape).background(background).then(interaction)
    CompositionLocalProvider(LocalCouiContentColor provides resolvedContentColor) {
        Column(modifier = container) {
            content()
        }
    }
}

/** COUI 图标：统一按 COUI 图标的 24dp 盒子与着色方式绘制 */
@Composable
private fun CouiIcon(
    vector: ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp = CouiPageDefaults.iconSize,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Icon(
        imageVector = vector,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
/** 开关：外观分派，COUI 外观下用 COUI 开关的轨道与把手 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!isCouiAppearance) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }
    CouiSwitch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
    )
}

/** 滑条：外观分派，COUI 外观下把 M3 的 SliderState 映射成 COUI 的值式滑条 */
@Composable
fun AppSlider(
    state: SliderState,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    if (!isCouiAppearance) {
        Slider(
            state = state,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }
    CouiSlider(
        modifier = modifier,
        value = state.value,
        onValueChange = {
            state.value = it
            onValueChange(it)
        },
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        onValueChangeFinished = { onValueChangeFinished?.invoke() },
    )
}
