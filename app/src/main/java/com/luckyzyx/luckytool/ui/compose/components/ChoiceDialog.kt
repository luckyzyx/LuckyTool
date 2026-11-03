package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixDialogScrollBody
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

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

/** Material 线列表高度上限：两条线都由正文自己收敛，这里的常量只给 Material 线用 */
private val ChoiceListMaxHeight: Dp = 360.dp

/** 空列表占位的最小高度，避免弹层塌成一条 */
private val ChoicePlaceholderMinHeight: Dp = 160.dp

/** 行尾指示器形态：两条线各自映射到本线原语 */
private enum class ChoiceIndicator {
    /** 无指示器（动作列表） */
    None,

    /** 选中指示（单选：选中时显示勾） */
    Selected,

    /** 勾选框（多选） */
    Checkbox,
}

/**
 * 一行的渲染模型：状态与数据加工在入口处完成一次，两条渲染线只读它。
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

/** 按当前外观线分派：状态、行模型与回调都在入口处算好，两条线只负责画 */
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
        UiMode.Miuix -> MiuixChoiceDialog(
            title = title,
            rows = rows,
            onDismiss = onDismiss,
            onRowClick = onRowClick,
            buttons = buttons,
            modifier = modifier,
            header = header,
        )

        UiMode.Material -> MaterialChoiceDialog(
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

/** Miuix 线：OverlayDialog（根部 popup host 承载）+ 分段卡片列表 + MiuixDialogScrollBody 按钮行 */
@Composable
private fun MiuixChoiceDialog(
    title: String,
    rows: List<ChoiceRowModel>,
    onDismiss: () -> Unit,
    onRowClick: (ChoiceItem) -> Unit,
    buttons: ChoiceButtons,
    modifier: Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        // showCancel = false（不可关闭场景）：整行动作区不画，而不是渲染一个点了没反应的「取消」按钮
        val actions: (@Composable () -> Unit)? = if (buttons.showCancel) {
            {
                val onConfirm = buttons.onConfirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 动作列表形态没有「确定」：单个「取消」也铺满整行，按内容宽度居中会显得又窄又飘
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = buttons.onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    if (onConfirm != null) {
                        Spacer(Modifier.width(20.dp))
                        MiuixTextButton(
                            text = stringResource(android.R.string.ok),
                            onClick = onConfirm,
                            enabled = buttons.confirmEnabled,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        } else {
            null
        }
        MiuixDialogScrollBody(
            modifier = modifier,
            // 列表本身是 LazyColumn，正文不能再套 verticalScroll
            scrollable = false,
            actions = actions,
        ) {
            header?.invoke()
            if (rows.isEmpty()) {
                ChoicePlaceholder {
                    MiuixText(
                        text = stringResource(R.string.choice_dialog_empty),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(rows, key = { _, row -> row.item.key }) { index, row ->
                        MiuixPrefItem(index = index, count = rows.size) {
                            MiuixListItem(
                                title = row.item.title,
                                summary = row.item.summary,
                                onClick = { onRowClick(row.item) },
                                enabled = row.item.enabled,
                                endActions = miuixEndActions(row),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miuix 线行尾：多选为「自定义动作 + 勾选框」，单选为选中勾，动作列表为空。
 *
 * 库内 BasicComponent 把 endActions 包在一个默认顶对齐的 Row 里，而动作按钮的触控区有 40dp、
 * 勾选框只有 26dp，顶对齐会让图标比勾选态高出半行；所以这里统一用垂直居中的 Row 收口，
 * 动作与指示器共用同一条水平中轴。
 */
@Composable
private fun miuixEndActions(row: ChoiceRowModel): (@Composable RowScope.() -> Unit)? {
    val actions = row.actions
    val hasIndicator = when (row.indicator) {
        ChoiceIndicator.None -> false
        ChoiceIndicator.Selected -> row.checked
        ChoiceIndicator.Checkbox -> true
    }
    if (actions == null && !hasIndicator) return null
    return {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke()
            when (row.indicator) {
                ChoiceIndicator.None -> Unit
                ChoiceIndicator.Selected -> if (row.checked) {
                    MiuixIcon(
                        imageVector = MiuixIcons.Basic.Check,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }

                ChoiceIndicator.Checkbox -> MiuixCheckbox(
                    state = ToggleableState(row.checked),
                    onClick = null,
                    enabled = row.item.enabled,
                )
            }
        }
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
                            MaterialItem(index = index, count = rows.size) {
                                MaterialListItem(
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
            UiMode.Miuix -> MiuixText(
                text = countText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )

            UiMode.Material -> Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            when (LocalUiMode.current) {
                UiMode.Miuix -> {
                    MiuixTextButton(
                        text = stringResource(R.string.appinfo_select_all),
                        onClick = onSelectAll,
                    )
                    MiuixTextButton(
                        text = stringResource(R.string.appinfo_deselect_all),
                        onClick = onClear,
                    )
                }

                UiMode.Material -> {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.appinfo_select_all))
                    }
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.appinfo_deselect_all))
                    }
                }
            }
        }
    }
}

/** 两条线共用的空态占位（本文件私有，不与 AppPickerDialog 的 PickerPlaceholder 混用） */
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
