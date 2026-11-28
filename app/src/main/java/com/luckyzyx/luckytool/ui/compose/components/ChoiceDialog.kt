package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R

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

/** 列表高度上限：再按窗口可用高度夹一层，矮窗口里不会把按钮行顶出屏幕 */
private val ChoiceListMaxHeight: Dp = 360.dp

/** 空列表占位的最小高度，避免弹层塌成一条 */
private val ChoicePlaceholderMinHeight: Dp = 160.dp

/** 行首指示器形态：单选为单选钮、多选为勾选框、动作列表没有指示器 */
private enum class ChoiceIndicator {
    /** 无指示器（动作列表） */
    None,

    /** 单选钮（单选的选中态） */
    Radio,

    /** 勾选框（多选） */
    Checkbox,
}

/**
 * 一行的渲染模型：状态与数据加工在入口处完成一次，行渲染只读它。
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
 * 底部按钮模型：onConfirm 为 null 时只剩「取消」（动作列表形态）；
 * confirmEnabled = false 时「确定」照常占位但呈禁用态（单选未选中时不再「点了没反应」）；
 * showCancel = false 时连「取消」也不画，供「不可关闭」的调用方去掉整行动作区。
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
        ChoiceRowModel(item, checked = item.key == selection, indicator = ChoiceIndicator.Radio)
    }
    ChoiceDialogFrame(
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
    ChoiceDialogFrame(
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
    ChoiceDialogFrame(
        title = title,
        rows = rows,
        onDismiss = onDismiss,
        onRowClick = onRowClick,
        buttons = ChoiceButtons(onCancel = onDismiss, onConfirm = null, showCancel = showCancel),
        modifier = modifier,
        header = header,
    )
}

/**
 * 三处入口共用的弹窗外壳：标题、正文与按钮行都交给弹窗版式，
 * 「只剩取消」「一个按钮都不画」由版式自己消化，这里不做分支。
 */
@Composable
private fun ChoiceDialogFrame(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    buttons: ChoiceButtons,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    currentDialogStyle().Alert(
        onDismissRequest = onDismiss,
        confirmLabel = buttons.onConfirm?.let { stringResource(android.R.string.ok) },
        onConfirm = buttons.onConfirm,
        modifier = modifier,
        title = title,
        // showCancel = false 时不给次级位；与确认位同时为空即无按钮的展示型弹层
        secondaryLabel = if (buttons.showCancel) stringResource(android.R.string.cancel) else null,
        onSecondary = buttons.onCancel,
        confirmEnabled = buttons.confirmEnabled,
        // 正文是惰性列表：外壳若再套一层滚动会内外同向抢手势
        bodyScrollable = false,
    ) {
        ChoiceBody(header = header, rows = rows, onRowClick = onRowClick)
    }
}

/**
 * 弹层正文：调用方 header 在最上（例如搜索框），下面是空态或惰性行列表。
 *
 * 横向留白由弹窗外壳给（两条线同宽），这里不再补；列表高度按设计上限再夹窗口可用高度。
 */
@Composable
private fun ChoiceBody(
    header: (@Composable () -> Unit)?,
    rows: List<ChoiceRowModel>,
    onRowClick: (ChoiceItem) -> Unit,
) {
    header?.invoke()
    if (rows.isEmpty()) {
        ChoicePlaceholder {
            PrefNote(stringResource(R.string.choice_dialog_empty))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = dialogListMaxHeight(ChoiceListMaxHeight, chrome = 160.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(rows, key = { _, row -> row.item.key }) { _, row ->
                ChoiceRow(row = row, onRowClick = onRowClick)
            }
        }
    }
}

/**
 * 一行：整行自己给交互语义——单选归入单选组、多选可切换、动作列表是按钮，
 * 禁用时整行不可点。点击只上报 [onRowClick]，不在这里改状态。
 *
 * 读屏按 [ChoiceRowModel.indicator] 播报「单选 / 复选」；行尾放调用方的自定义动作。
 */
@Composable
private fun ChoiceRow(
    row: ChoiceRowModel,
    onRowClick: (ChoiceItem) -> Unit,
) {
    val enabled = row.item.enabled
    val onClick: () -> Unit = { onRowClick(row.item) }
    val interaction = when (row.indicator) {
        ChoiceIndicator.None -> Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )

        ChoiceIndicator.Radio -> Modifier.selectable(
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (row.indicator != ChoiceIndicator.None) {
            currentPrefStyle().ChoiceIndicator(
                radio = row.indicator == ChoiceIndicator.Radio,
                checked = row.checked,
                enabled = enabled,
            )
        }
        Column(Modifier.weight(1f)) {
            PrefRowTitle(row.item.title, enabled = enabled, maxLines = 2)
            row.item.summary?.let { summary ->
                PrefRowSummary(summary, maxLines = 2)
            }
        }
        row.actions?.invoke()
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
        PrefNote(text = countText)
        Row {
            currentPrefStyle().TextButton(
                text = stringResource(R.string.appinfo_select_all),
                onClick = onSelectAll,
                modifier = Modifier,
                enabled = true,
            )
            currentPrefStyle().TextButton(
                text = stringResource(R.string.appinfo_deselect_all),
                onClick = onClear,
                modifier = Modifier,
                enabled = true,
            )
        }
    }
}

/** 各入口共用的空态占位（本文件私有，不与 AppPickerDialog 的 PickerPlaceholder 混用） */
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
