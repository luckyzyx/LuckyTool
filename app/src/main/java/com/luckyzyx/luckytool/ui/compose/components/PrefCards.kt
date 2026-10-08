package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MaterialPrefCardScope
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCardScope
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.PrefCardScope
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 共享卡片行件（KernelSU 风格分段卡片 / 独立卡片）。
 *
 * UI 线分派：material 线逐字保留原行为（`Segmented*` 原语 + `MaterialTheme`）；
 * Miuix 线改用 t11 的 `miuix/MiuixPref*` 原语 + `MiuixTheme`。分派开关统一读 [LocalUiMode]，
 * 每处只在渲染体里加一个分支，取值与公开签名不变 —— 15 处 `PrefGroup`/`prefGroup` 与 26 处
 * `PrefRow` 等既有调用点（含 `special/` 零 diff 的 9 处）无需改动。
 */

/**
 * 分段卡片组：组内连续条目自动合并成 KernelSU 风格分段卡片
 * （首条 16dp 外圆角、中间 4dp 内圆角、组内 2dp 间距，显隐带弹性过渡）。
 *
 * 组容器由 [PrefCardScope] 的两个实现分派：material 线走 `SegmentedColumn` 动态重放
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

/**
 * 在 LazyListScope 中放置一个分段卡片组。
 * [PrefGroup] 是 @Composable，不能直接写在 `ExpressiveList { }` 的列表作用域里，
 * 因此用本扩展把它包进一个 item。
 */
fun LazyListScope.prefGroup(
    key: Any? = null,
    modifier: Modifier = Modifier,
    content: PrefCardScope.() -> Unit,
) {
    item(key = key) {
        PrefGroup(modifier = modifier, content = content)
    }
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
        SegmentedListItem(
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

/** 分段卡片组内的开关条目：整行点击切换，尾部为 Expressive 开关 */
@Composable
fun PrefSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
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
            enabled = enabled,
        )
    } else {
        Box(modifier = modifier) {
            SegmentedSwitchItem(
                title = title,
                summary = summary,
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/** 分段卡片组内的取值条目：尾部显示当前值，点击弹出选择 */
@Composable
fun PrefValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    PrefRow(
        title = title,
        modifier = modifier,
        summary = summary,
        onClick = onClick,
        enabled = enabled,
        trailing = if (LocalUiMode.current == UiMode.Miuix) {
            {
                MiuixText(
                    text = value,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary,
                )
            }
        } else {
            {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

/**
 * 独立卡片（不属于任何分段组时使用）。
 * 外观与主题页一致：KernelSU SegmentedListItem（surfaceBright 容器 + 16dp 外圆角）/
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
        SegmentedItem(index = 0, count = 1) {
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
        SegmentedItem(index = 0, count = 1) {
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

/** 独立取值卡片（不属于任何分段组时使用） */
@Composable
fun PrefValueCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPrefItem(index = 0, count = 1) {
            PrefValueRow(
                title = title,
                value = value,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                onClick = onClick,
            )
        }
    } else {
        SegmentedItem(index = 0, count = 1) {
            PrefValueRow(
                title = title,
                value = value,
                modifier = modifier,
                summary = summary,
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

/** 分类标题（旧 PreferenceCategory 等价物；样式与 SegmentedColumn title 一致） */
@Composable
fun PrefCategoryHeader(title: String, modifier: Modifier = Modifier) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPrefCategoryHeader(title = title, modifier = modifier)
    } else {
        Text(
            text = title,
            modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
