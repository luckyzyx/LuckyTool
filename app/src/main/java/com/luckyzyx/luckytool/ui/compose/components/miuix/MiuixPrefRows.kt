package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 行首图标（对齐 t10 契约的 iconStartAction 冻结形态）。
 *
 * 无图标时返回 null，交给库内默认行为（不占用 startAction 槽位）。
 */
@Composable
private fun iconStartAction(icon: ImageVector?, enabled: Boolean): (@Composable () -> Unit)? {
    if (icon == null) return null
    return {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
            tint = if (enabled) {
                MiuixTheme.colorScheme.onBackground
            } else {
                MiuixTheme.colorScheme.disabledOnSecondaryVariant
            },
        )
    }
}

/**
 * 通用设置条目：整行可点、可选摘要、可选首尾槽位与底部扩展。
 *
 * `onLongClick` 非空时由本函数自带**唯一**手势处理器（`combinedClickable`；库内 `BasicComponent`
 * 只有 `onClick`、没有长按参数），并把 `BasicComponent` 的 `onClick` 置空，使短按与长按由同一
 * 处理器仲裁——避免「外层长按检测 + 内层 clickable」两套检测器争用同一批指针事件；共享
 * `interactionSource` 保证按压反馈（`HoldDownObserver`）仍然生效。短按与长按互斥，`enabled = false`
 * 时两者都不触发，与 material 侧 `SegmentedListItem` 一致；不产生额外触感。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiuixListItem(
    title: String?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
) {
    if (onLongClick == null) {
        BasicComponent(
            modifier = modifier,
            title = title,
            summary = summary,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            onClick = onClick,
            enabled = enabled,
        )
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        BasicComponent(
            modifier = modifier.combinedClickable(
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            ),
            title = title,
            summary = summary,
            startAction = startAction,
            endActions = endActions,
            bottomAction = bottomAction,
            onClick = null,
            enabled = enabled,
            interactionSource = interactionSource,
        )
    }
}

/**
 * 开关条目：整行可点、尾部开关。
 *
 * 库内 `SwitchPreference` 无触感，这里补齐 `HapticFeedbackType.VirtualKey`，
 * 与 material 侧 `SegmentedSwitchItem`（每次切换一次 VirtualKey）一致。
 */
@Composable
fun MiuixSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current
    SwitchPreference(
        checked = checked,
        onCheckedChange = { newChecked ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onCheckedChange(newChecked)
        },
        title = title,
        modifier = modifier,
        summary = summary,
        startAction = startAction ?: iconStartAction(icon, enabled),
        enabled = enabled,
    )
}

/** 箭头条目：整行可点、尾部箭头由库内 `ArrowPreference` 绘制（不手绘）。 */
@Composable
fun MiuixArrowItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    startAction: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    ArrowPreference(
        title = title,
        modifier = modifier,
        summary = summary,
        startAction = startAction ?: iconStartAction(icon, enabled),
        onClick = onClick,
        enabled = enabled,
    )
}

/**
 * 下拉条目：弹层由根部 Miuix Scaffold 的 popup host 承载。
 *
 * 使用 `OverlayDropdownPreference`（`renderInRootScaffold` 保持默认 true，不传 `popupHost`）。
 * 库内没有 `onClick` 参数，[onClick] 映射到 `onExpandedChange` 的展开时刻；
 * 展开触感由库内 `HapticFeedbackType.ContextClick` 提供（不叠加 VirtualKey，避免双震），
 * 选中回调则补 `HapticFeedbackType.VirtualKey`，与 material 侧 `SegmentedDropdownItem` 一致。
 */
@Composable
fun MiuixDropdownItem(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    startAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    OverlayDropdownPreference(
        items = items,
        selectedIndex = selectedIndex,
        title = title,
        modifier = modifier,
        summary = summary,
        startAction = startAction ?: iconStartAction(icon, enabled),
        enabled = enabled,
        onExpandedChange = { expanded -> if (expanded) onClick?.invoke() },
        onSelectedIndexChange = { index ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onItemSelected(index)
        },
    )
}

/**
 * 单选项条目：整行可点选中。
 *
 * 库内 `RadioButtonPreference` 自带 `ToggleOn` / `ToggleOff` 触感，此处不叠加 VirtualKey
 * （叠加会双震）；该触感类型差异登记为允许差异。
 */
@Composable
fun MiuixRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    RadioButtonPreference(
        title = title,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
    )
}

/**
 * 多选项条目：整行可点切换，复选框在尾部（契约 §10.4：Miuix 线复选框一律 `CheckboxLocation.End`）。
 *
 * 库内 `CheckboxPreference` 无触感，这里补齐 `HapticFeedbackType.VirtualKey`，
 * 与 material 侧 `SegmentedCheckboxItem` 一致。
 */
@Composable
fun MiuixCheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current
    CheckboxPreference(
        title = title,
        checked = checked,
        onCheckedChange = { newChecked ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onCheckedChange(newChecked)
        },
        modifier = modifier,
        summary = summary,
        checkboxLocation = CheckboxLocation.End,
        enabled = enabled,
    )
}

/** 滑条条目：直接委托库内 `SliderPreference`（滑条渲染在行底部，尾部显示 [valueText]）。 */
@Composable
fun MiuixSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    valueText: String? = null,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        title = title,
        summary = summary,
        valueText = valueText,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}


