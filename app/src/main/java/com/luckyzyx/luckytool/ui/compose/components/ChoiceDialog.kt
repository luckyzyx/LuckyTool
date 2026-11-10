package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.itos.coui.checkbox.Checkbox as CouiCheckbox
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog as CouiDialog
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.shape.CouiShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 选择对话框的一行。
 *
 * @param key 行的稳定标识：既是 LazyColumn 的 key，也是单选/多选对外回调的值。
 * @param title 主标题。
 * @param summary 副标题，null 时不占行高。
 * @param enabled 为 false 时整行不可点、指示器呈禁用色。
 */
data class ChoiceItem(
    val key: String,
    val title: String,
    val summary: String? = null,
    val enabled: Boolean = true,
)

/** Material 线列表高度上限：其余线的正文自己收敛高度，这个常量只给 Material 线用 */
private val ChoiceListMaxHeight: Dp = 360.dp

/** 空列表占位的最小高度，避免弹层塌成一条 */
private val ChoicePlaceholderMinHeight: Dp = 160.dp

/** 行尾指示器形态：各条线各自映射到本线原语 */
private enum class ChoiceIndicator {
    /** 无指示器（动作列表） */
    None,

    /** 选中指示（单选：选中时显示勾） */
    Selected,

    /** 勾选框（多选） */
    Checkbox,
}

/**
 * 一行的渲染模型：状态与数据加工在入口处完成一次，各条渲染线只读它。
 *
 * @param actions 行尾自定义动作槽（多选 Pin 之类），与勾选框同处行尾。
 */
private class ChoiceRowModel(
    val item: ChoiceItem,
    val checked: Boolean,
    val indicator: ChoiceIndicator,
    val actions: (@Composable () -> Unit)? = null,
)

/**
 * 底部按钮行：onConfirm 为 null 时只渲染「取消」（动作列表形态）；
 * confirmEnabled = false 时「确定」照常占位但呈禁用态（单选未选中时不再「点了没反应」）；
 * showCancel = false 时整行动作区都不画，供「不可关闭」的调用方去掉按钮。
 */
private class ChoiceButtons(
    val onCancel: () -> Unit,
    val onConfirm: (() -> Unit)?,
    val confirmEnabled: Boolean = true,
    val showCancel: Boolean = true,
)

/**
 * 单选对话框：整行点击即选中，底部「取消 | 确定」。
 *
 * 选择是组件内部的临时态：点行只改临时态，只有点「确定」才通过 [onConfirm] 回写；
 * 取消或点弹层外部只回调 [onDismiss]，绝不会顺带回写。
 */
@Composable
fun SingleChoiceDialog(
    title: String,
    items: List<ChoiceItem>,
    selectedKey: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 记忆键只留 selectedKey：列表重建（items 换新实例）不该重置用户刚点的临时选中
    var selection by remember(selectedKey) { mutableStateOf(selectedKey) }
    val haptic = LocalHapticFeedback.current
    val onRowClick: (ChoiceItem) -> Unit = { item ->
        if (item.enabled && selection != item.key) {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            selection = item.key
        }
    }
    val rows = items.map { item ->
        ChoiceRowModel(item, checked = item.key == selection, indicator = ChoiceIndicator.Selected)
    }
    ChoiceDialogShell(
        title = title,
        rows = rows,
        onDismiss = onDismiss,
        onRowClick = onRowClick,
        buttons = ChoiceButtons(
            onCancel = onDismiss,
            // onConfirm 始终非空：未选中任何一项时由 confirmEnabled 把按钮灰掉
            onConfirm = { selection?.let(onConfirm) },
            confirmEnabled = selection != null,
        ),
        modifier = modifier,
    )
}

/**
 * 多选对话框：整行点击切换勾选，底部「取消 | 确定」。
 *
 * [itemActions] 是行尾自定义动作槽（如 Pin 图标按钮），它的点击不会冒泡成整行切换：
 * 内部按钮自带 clickable，指针事件在子层被消费，行点击收不到。
 *
 * [header] 渲染在列表上方、正文内（不随 LazyColumn 滚动），供调用方放搜索框；
 * [showSelectAll] 额外画一行「全选 / 清空 + 已选 x/y」——选中态是组件内部临时态，调用方改不了，
 * 所以这行只能由组件自己画，作用范围是当前传入的 [items]（调用方过滤后即「全选可见项」）。
 */
@Composable
fun MultiChoiceDialog(
    title: String,
    items: List<ChoiceItem>,
    selectedKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    itemActions: (@Composable (item: ChoiceItem) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    showSelectAll: Boolean = false,
) {
    // 记忆键只留 selectedKeys：列表重建（items 换新实例）不该重置用户刚点的临时勾选
    var selection by remember(selectedKeys) { mutableStateOf(selectedKeys) }
    val haptic = LocalHapticFeedback.current
    val onRowClick: (ChoiceItem) -> Unit = { item ->
        if (item.enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            selection = if (item.key in selection) selection - item.key else selection + item.key
        }
    }
    val actionSlot = itemActions
    val rows = items.map { item ->
        val actions: (@Composable () -> Unit)? =
            if (actionSlot != null) {
                { actionSlot(item) }
            } else null
        ChoiceRowModel(
            item = item,
            checked = item.key in selection,
            indicator = ChoiceIndicator.Checkbox,
            actions = actions,
        )
    }
    val allEnabledKeys = items.filter { it.enabled }.map { it.key }.toSet()
    val headerContent: (@Composable () -> Unit)? =
        if (!showSelectAll && header == null) null else {
            {
                // 顺序：调用方 header（搜索框）贴标题，批量行紧跟其后
                header?.invoke()
                if (showSelectAll) {
                    ChoiceSelectAllRow(
                        // 只数当前 items 里还存在的键：搜索过滤后 selection 里可能有不可见的项，
                        // 直接取 selection.size 会算出「已选 5 / 3」这种荒唐值
                        selectedCount = items.count { it.key in selection },
                        totalCount = items.size,
                        onSelectAll = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selection = selection + allEnabledKeys
                        },
                        onClear = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            selection = emptySet()
                        },
                    )
                }
            }
        }
    ChoiceDialogShell(
        title = title,
        rows = rows,
        header = headerContent,
        onDismiss = onDismiss,
        onRowClick = onRowClick,
        buttons = ChoiceButtons(
            onCancel = onDismiss,
            onConfirm = { onConfirm(selection) },
        ),
        modifier = modifier,
    )
}

/**
 * 动作列表对话框：整行点击直接回调 [onSelect]，底部只有「取消」。
 *
 * 组件自身不关闭：要不要在动作后关闭由调用方决定（需要关闭就自己调 onDismiss）。
 * [header] 渲染在列表上方、正文内（不随 LazyColumn 滚动），供调用方放搜索框。
 */
@Composable
fun ActionListDialog(
    title: String,
    items: List<ChoiceItem>,
    onDismiss: () -> Unit,
    onSelect: (ChoiceItem) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    /** 不可关闭的场景（例如 release 版下载源选择）传 false：整行动作区不画，而不是渲染一个点了没反应的「取消」 */
    showCancel: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val onRowClick: (ChoiceItem) -> Unit = { item ->
        if (item.enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onSelect(item)
        }
    }
    val rows = items.map { item ->
        ChoiceRowModel(item, checked = false, indicator = ChoiceIndicator.None)
    }
    ChoiceDialogShell(
        title = title,
        rows = rows,
        onDismiss = onDismiss,
        onRowClick = onRowClick,
        buttons = ChoiceButtons(onCancel = onDismiss, onConfirm = null, showCancel = showCancel),
        modifier = modifier,
        header = header,
    )
}

/** 按当前外观线分派：状态、行模型与回调都在入口处算好，各条线只负责画 */
@Composable
private fun ChoiceDialogShell(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    buttons: ChoiceButtons,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialChoiceDialog(
            title = title,
            rows = rows,
            onDismiss = onDismiss,
            onRowClick = onRowClick,
            buttons = buttons,
            modifier = modifier,
            header = header,
        )

        UiMode.Coui -> CouiChoiceDialog(
            title = title,
            rows = rows,
            onDismiss = onDismiss,
            onRowClick = onRowClick,
            buttons = buttons,
            modifier = modifier,
            header = header,
        )
    }
}

/** Material 线：M3 AlertDialog + 分段卡片列表；高度上限由具名常量收敛 */
@Composable
private fun MaterialChoiceDialog(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    buttons: ChoiceButtons,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                header?.invoke()
                if (rows.isEmpty()) {
                    ChoicePlaceholder {
                        Text(
                            text = stringResource(R.string.choice_dialog_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = ChoiceListMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(rows, key = { _, row -> row.item.key }) { index, row ->
                            val summary = row.item.summary
                            val supporting: (@Composable () -> Unit)? =
                                if (summary != null) {
                                    { Text(summary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                } else null
                            AppItem(index = index, count = rows.size) {
                                AppListItem(
                                    onClick = { onRowClick(row.item) },
                                    enabled = row.item.enabled,
                                    headlineContent = {
                                        Text(
                                            text = row.item.title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    supportingContent = supporting,
                                    leadingContent = materialLeading(row),
                                    trailingContent = row.actions,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            buttons.onConfirm?.let { onConfirm ->
                TextButton(
                    onClick = onConfirm,
                    enabled = buttons.confirmEnabled,
                ) { Text(stringResource(android.R.string.ok)) }
            }
        },
        // showCancel = false 时不画取消按钮；传 null 而不是空 lambda，空 lambda 仍会占出一个空按钮行
        dismissButton = if (buttons.showCancel) {
            {
                TextButton(
                    onClick = buttons.onCancel,
                    // 动作列表形态没有「确定」：单个「取消」铺满按钮行走廊，而不是挤在行尾
                    modifier = if (buttons.onConfirm == null) Modifier.fillMaxWidth() else Modifier,
                ) { Text(stringResource(android.R.string.cancel)) }
            }
        } else null,
    )
}

/** Coui 线：COUI 弹窗自绘按钮行；正文槽本身可滚动，行用普通 Column 平铺而不是 LazyColumn */
@Composable
private fun CouiChoiceDialog(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    buttons: ChoiceButtons,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val onConfirm = buttons.onConfirm
    // 动作区一个按钮都不画（showCancel = false 且没有确认位）：COUI 弹窗的确认位是必填项，
    // 表达不了零按钮的弹层，改由裸 Dialog 承载一张 COUI 配色卡片
    if (!buttons.showCancel && onConfirm == null) {
        CouiButtonlessChoiceDialog(
            title = title,
            rows = rows,
            onDismiss = onDismiss,
            onRowClick = onRowClick,
            modifier = modifier,
            header = header,
        )
        return
    }
    CouiDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = title,
        // 动作列表形态只剩一个「取消」：放进必填的确认位，呈次级样式，语义仍是取消
        confirmAction = if (onConfirm == null) {
            CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = buttons.onCancel,
            )
        } else {
            CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onConfirm,
                enabled = buttons.confirmEnabled,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            )
        },
        // showCancel = false 时确认位之外不再有按钮；动作列表形态也只剩那一个「取消」
        dismissAction = if (!buttons.showCancel || onConfirm == null) null else CouiDialogAction(
            text = stringResource(android.R.string.cancel),
            onClick = buttons.onCancel,
        ),
    ) {
        CouiChoiceBody(header = header, rows = rows, onRowClick = onRowClick)
    }
}

/**
 * Coui 线：没有任何按钮的展示型弹层。
 *
 * COUI 弹窗的确认位必填，画不出「零按钮」，所以用裸 Dialog 承载一张 COUI 配色卡片：
 * 标题在上，行列表平铺在下，不画按钮区；点弹层外部要不要关闭由 [onDismiss] 自己决定。
 */
@Composable
private fun CouiButtonlessChoiceDialog(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val scheme = CouiTheme.colorScheme
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(CouiShape(24.dp, 2f))
                .background(scheme.container)
                .padding(vertical = 24.dp),
        ) {
            CouiText(
                text = title,
                kind = CouiTextKind.ItemTitle,
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = CouiDialogDefaults.contentPadding),
            )
            Spacer(Modifier.height(8.dp))
            // 行数可能超过一屏：正文自己滚动，最后几行才够得到
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                CouiChoiceBody(header = header, rows = rows, onRowClick = onRowClick)
            }
        }
    }
}

/** Coui 线的弹层正文：调用方 header、空态或平铺的行列表；左右留白由 header 与行自己补 */
@Composable
private fun CouiChoiceBody(
    header: (@Composable () -> Unit)?,
    rows: List<ChoiceRowModel>,
    onRowClick: (ChoiceItem) -> Unit,
) {
    // 正文槽没有内边距，标题与正文的左右留白由调用方自己补
    if (header != null) {
        Box(Modifier.padding(horizontal = CouiDialogDefaults.contentPadding)) { header() }
    }
    if (rows.isEmpty()) {
        ChoicePlaceholder {
            CouiText(
                text = stringResource(R.string.choice_dialog_empty),
                kind = CouiTextKind.Search,
                textAlign = TextAlign.Start,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                CouiChoiceRow(row = row, onRowClick = onRowClick)
            }
        }
    }
}

/**
 * Coui 线的一行：整行可点，行为与其余线一致（点击只上报，不自行改状态）。
 *
 * 行首指示器沿用本文件其余线的位置：单选为单选钮、多选为勾选框，动作列表无指示器；
 * 行尾放调用方的自定义动作。语义角色随指示器变化，读屏据此播报「单选 / 复选」。
 */
@Composable
private fun CouiChoiceRow(
    row: ChoiceRowModel,
    onRowClick: (ChoiceItem) -> Unit,
) {
    val enabled = row.item.enabled
    val scheme = CouiTheme.colorScheme
    val onClick: () -> Unit = { onRowClick(row.item) }
    val interaction = when (row.indicator) {
        ChoiceIndicator.None -> Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )

        ChoiceIndicator.Selected -> Modifier.selectable(
            selected = row.checked,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        )

        ChoiceIndicator.Checkbox -> Modifier.toggleable(
            value = row.checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .padding(horizontal = CouiDialogDefaults.contentPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (row.indicator) {
            ChoiceIndicator.None -> Unit
            ChoiceIndicator.Selected -> CouiRadioButton(selected = row.checked, enabled = enabled)
            ChoiceIndicator.Checkbox -> CouiCheckbox(checked = row.checked, enabled = enabled)
        }
        Column(Modifier.weight(1f)) {
            CouiText(
                text = row.item.title,
                kind = CouiTextKind.ItemTitle,
                color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            row.item.summary?.let { summary ->
                CouiText(
                    text = summary,
                    kind = CouiTextKind.ItemSummary,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        row.actions?.invoke()
    }
}

/** Material 线行首：单选为单选钮、多选为勾选框，动作列表无行首指示器 */
@Composable
private fun materialLeading(row: ChoiceRowModel): (@Composable () -> Unit)? = when (row.indicator) {
    ChoiceIndicator.None -> null

    ChoiceIndicator.Selected -> {
        { RadioButton(selected = row.checked, onClick = null, enabled = row.item.enabled) }
    }

    ChoiceIndicator.Checkbox -> {
        { Checkbox(checked = row.checked, onCheckedChange = null, enabled = row.item.enabled) }
    }
}

/**
 * 多选的「全选 / 清空 + 已选 x/y」行。选中态是组件内部临时态，调用方改不了，所以这行只能由组件自己画；
 * 全选只并入当前传入的 enabled 项，调用方已按搜索过滤，因此等价于「全选可见项」。
 */
@Composable
private fun ChoiceSelectAllRow(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
) {
    val countText =
        stringResource(R.string.appinfo_selected_count, selectedCount) + " / " + totalCount
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (LocalUiMode.current) {
            UiMode.Material -> Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            UiMode.Coui -> CouiText(
                text = countText,
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            when (LocalUiMode.current) {
                UiMode.Material -> {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.appinfo_select_all))
                    }
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.appinfo_deselect_all))
                    }
                }

                UiMode.Coui -> {
                    CouiTextAction(
                        text = stringResource(R.string.appinfo_select_all),
                        onClick = onSelectAll,
                    )
                    CouiTextAction(
                        text = stringResource(R.string.appinfo_deselect_all),
                        onClick = onClear,
                    )
                }
            }
        }
    }
}

/**
 * Coui 线的行内文字按钮：COUI 不提供公开的文字按钮，用可点文本承载同样的「点击即执行」语义。
 * 触控高度按文字行高加纵向留白补到可点范围，读屏角色为按钮。
 */
@Composable
private fun CouiTextAction(
    text: String,
    onClick: () -> Unit,
) {
    CouiText(
        text = text,
        kind = CouiTextKind.Search,
        textAlign = TextAlign.Start,
        color = CouiTheme.colorScheme.accent,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** 各条线共用的空态占位（本文件私有，不与 AppPickerDialog 的 PickerPlaceholder 混用） */
@Composable
private fun ChoicePlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChoicePlaceholderMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
