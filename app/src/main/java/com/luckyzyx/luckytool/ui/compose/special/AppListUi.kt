package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveSwitch
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField

/**
 * 应用列表页（ZoomWindowPage / MultiAppPage）共享的双线组件。
 *
 * 与 prefCard（`components/PrefCards.kt`）同一分派哲学：分派统一读 [LocalUiMode]，
 * 只在渲染体里分派到 Miuix 或 Material 原语，取值与两线旧实现逐字一致，
 * 避免每个页面各写一份 Material / Miuix 变体。
 */

/**
 * 应用行：图标 + 应用名 + 包名 + 只读开关，整行点击切换（VirtualKey 触感 → [onToggle]）。
 *
 * 与 prefCard 的通用 `PrefRow` 不同：Material 线需要 2 行截断（长应用名/包名）+ 共享
 * `interactionSource` 的只读开关，故保留为专用双线行，写值语义与旧 `AppToggleRowMiuix`
 * 及各页 Material 内联行完全一致。
 */
@Composable
internal fun AppToggleRow(
    info: AppInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixListItem(
            title = info.name,
            summary = info.packageName,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onToggle(!enabled)
            },
            startAction = {
                info.icon?.let { d ->
                    Image(
                        remember(info) { d.toBitmap().asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
            endActions = {
                MiuixSwitch(checked = enabled, onCheckedChange = null)
            },
        )
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        SegmentedListItem(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onToggle(!enabled)
            },
            interactionSource = interactionSource,
            headlineContent = {
                Text(info.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(info.packageName, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                info.icon?.let { d ->
                    Image(
                        remember(info) { d.toBitmap().asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
            trailingContent = {
                ExpressiveSwitch(
                    checked = enabled,
                    onCheckedChange = null,
                    interactionSource = interactionSource,
                )
            },
        )
    }
}

/**
 * 应用搜索框：受控输入 + 搜索图标 + 排序筛选按钮。
 *
 * Material 线 `OutlinedTextField`（placeholder 语义）；Miuix 线 `TextField`
 * （`useLabelAsPlaceholder` 保留原 placeholder 语义）。
 */
@Composable
internal fun AppSearchField(
    query: String,
    enabled: Boolean,
    onQueryChange: (String) -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Name / PackageName",
            useLabelAsPlaceholder = true,
            enabled = enabled,
            singleLine = true,
            leadingIcon = {
                MiuixIcon(
                    painterResource(R.drawable.ic_baseline_search_24),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                MiuixIconButton(onClick = onSortClick) {
                    MiuixIcon(
                        painterResource(R.drawable.baseline_filter_list_24),
                        contentDescription = null,
                    )
                }
            },
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text("Name / PackageName") },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.ic_baseline_search_24),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                IconButton(onClick = onSortClick) {
                    Icon(
                        painterResource(R.drawable.baseline_filter_list_24),
                        contentDescription = null,
                    )
                }
            },
            modifier = modifier.fillMaxWidth(),
        )
    }
}
