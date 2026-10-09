package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialListItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPrefCardScope
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCardScope
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 卡片行 / 卡原语（KernelSU 风格分段卡片 / 独立卡片）。
 *
 * **「设置区块」的唯一 API 是 `PrefScope` DSL**（`category()` / `switch()` / `click()` / `list()` /
 * `custom()`，见 `ui/compose/components/PrefScope.kt`）—— 所有标准设置页与作用域页都走它。
 * 本文件只保留 `custom()` 逃生舱（无法走 DSL 的自定义控件里的单行 / 单卡）所需的原始行 / 卡原语：
 * [PrefGroup] + [PrefRow] + [PrefSwitchRow]（组内行，形状由所属 [PrefGroup] 决定）、
 * [PrefCard] + [PrefSwitchCard]（不属于任何分段组的独立卡片）、[PrefIconBadge]（左侧彩色徽标）。
 *
 * UI 线分派：material 线用 `Segmented*` 原语 + `MaterialTheme`；Miuix 线用 `miuix/MiuixPref*`
 * 原语 + `MiuixTheme`；分派开关统一读 [LocalUiMode]。
 */

/**
 * 分段卡片组：组内连续条目合并成一张连续卡片
 * （首条上端、尾条下端 16dp 外圆角，组内条目不设圆角与间距，显隐带弹性过渡）。
 *
 * 组容器由 [PrefCardScope] 的两个实现分派：material 线走 `MaterialGroup` 动态重放
 * （保留弹性显隐过渡），Miuix 线走 `MiuixPrefGroup`（首条 12dp 组间距 + 卡片圆角）。
 *
 * 用法：
 * ```
 * PrefGroup {
 *     item { PrefSwitchRow("标题", checked = a, onCheckedChange = { ... }) }
 *     item { PrefSwitchRow("标题2", checked = b, onCheckedChange = { ... }) }
 * }
 * ```
 */
@Composable
fun PrefGroup(
    modifier: Modifier = Modifier,
    content: PrefCardScope.() -> Unit,
) {
    val miuix = LocalUiMode.current == UiMode.Miuix
    val scope: PrefCardScope = if (miuix) MiuixPrefCardScope() else MaterialPrefCardScope()
    content(scope)
    scope.render(preferMiuix = miuix, modifier = modifier)
}

/** 分段卡片组内的普通条目（形状由所属 [PrefGroup] 决定） */
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
    if (LocalUiMode.current == UiMode.Miuix) {
        // Miuix 行自带 insideMargin(16dp)：不再套 material 线的外层 padding
        val endActions: (@Composable RowScope.() -> Unit)? = trailing?.let { content ->
            { content() }
        }
        MiuixListItem(
            title = title,
            modifier = modifier,
            summary = summary,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            startAction = leading,
            endActions = endActions,
        )
    } else {
        MaterialListItem(
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            headlineContent = { Text(title) },
            supportingContent = summary?.let { { Text(it) } },
            leadingContent = leading,
            trailingContent = trailing,
        )
    }
}

/** 左侧彩色徽标图标：固定色圆角底 + 白色矢量图标（功能树分类页 / 其他页 / 设置页通用） */
@Composable
fun PrefIconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
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
    if (LocalUiMode.current == UiMode.Miuix) {
        // 触感由 MiuixSwitchItem 内部补 VirtualKey（SwitchPreference 本身无触感）
        MiuixSwitchItem(
            title = title,
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            summary = summary,
            startAction = leading,
            enabled = enabled,
        )
    } else {
        Box(modifier = modifier) {
            MaterialSwitchItem(
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
 * 独立卡片（不属于任何分段组时使用）。
 * 外观与主题页一致：KernelSU MaterialListItem（surfaceBright 容器 + 16dp 外圆角）/
 * Miuix `MiuixPrefItem`（16dp 外圆角 + 12dp 水平内缩）。
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
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPrefItem(index = 0, count = 1) {
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
    } else {
        MaterialItem(index = 0, count = 1) {
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
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPrefItem(index = 0, count = 1) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    } else {
        MaterialItem(index = 0, count = 1) {
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
