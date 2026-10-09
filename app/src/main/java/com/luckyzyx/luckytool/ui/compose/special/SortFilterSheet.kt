package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

/**
 * 排序/筛选底部弹层（旧 SortFilterBottomSheetDialog 的 Compose 等价物）。
 *
 * 由 HideAppIntentPage 迁出为共享件：DarkModePage / HideAppIntentPage / MultiAppPage / ZoomWindowPage 复用，
 * 入口签名与四个调用点保持完全不变，主题差异在唯一的 `when (LocalUiMode.current)` 缝上分派：
 * - Material 线 → [SortFilterSheetMaterial]（原实现逐字搬运）；
 * - Miuix 线 → [SortFilterSheetMiuix]（同包 SortFilterSheetMiuix.kt，根部 host 的 OverlayBottomSheet）。
 */
@Composable
internal fun SortFilterSheet(
    reverse: Boolean,
    sortMode: Int,
    sortLabels: List<String>,
    onReverseChange: () -> Unit,
    onSortChange: (Int) -> Unit,
    filterContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> SortFilterSheetMiuix(
            reverse = reverse,
            sortMode = sortMode,
            sortLabels = sortLabels,
            onReverseChange = onReverseChange,
            onSortChange = onSortChange,
            filterContent = filterContent,
            onDismiss = onDismiss,
        )

        UiMode.Material -> SortFilterSheetMaterial(
            reverse = reverse,
            sortMode = sortMode,
            sortLabels = sortLabels,
            onReverseChange = onReverseChange,
            onSortChange = onSortChange,
            filterContent = filterContent,
            onDismiss = onDismiss,
        )
    }
}

/** Material 线：原 SortFilterSheet 实现逐字搬运（仅函数名与可见性调整）。 */
@Suppress("DEPRECATION") // HideAppIntentPage material 代码逐字搬运，上游 API 迁移前已弃用
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SortFilterSheetMaterial(
    reverse: Boolean,
    sortMode: Int,
    sortLabels: List<String>,
    onReverseChange: () -> Unit,
    onSortChange: (Int) -> Unit,
    filterContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            Text(
                stringResource(R.string.appinfo_sort_and_filter),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.appinfo_sort_by),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.size(4.dp))
            FilterChip(
                selected = reverse,
                onClick = onReverseChange,
                label = { Text(stringResource(R.string.appinfo_reverse)) },
            )
            FlowRow {
                sortLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = sortMode == index,
                        onClick = { onSortChange(index) },
                        label = { Text(label) },
                    )
                }
            }
            if (filterContent != null) {
                Spacer(Modifier.size(12.dp))
                Text(
                    stringResource(R.string.appinfo_filter),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.size(4.dp))
                filterContent()
            }
        }
    }
}

/**
 * 排序/筛选底部弹层的 Miuix 线呈现（调用方都在 `if (show) { … }` 里条件组合，故 `show = true`）。
 *
 * 映射说明（与 Material 线逐条等价）：
 * - `ModalBottomSheet` → 根部 host 承载的 `OverlayBottomSheet`（`renderInRootScaffold` 保持默认 true）；
 * - 标题/小节标题 → `OverlayBottomSheet(title = …)` + `MiuixPrefCategoryHeader`；
 * - reverse 的 `FilterChip` → `MiuixSwitchItem`（单一开关语义：点击即取反，等价于 `onReverseChange()`）；
 * - 排序 `FilterChip` 组 → `MiuixRadioItem`（单选语义：`sortMode == index` 选中，点击回调同 `onSortChange(index)`）；
 * - `filterContent` 槽位原样透传（由调用方按线提供各自的过滤控件），非空时才显示「筛选」小节标题。
 * 库无 Chip 组件，故用 preference 行件表达，属已登记的主题级差异。
 */
@Composable
internal fun SortFilterSheetMiuix(
    reverse: Boolean,
    sortMode: Int,
    sortLabels: List<String>,
    onReverseChange: () -> Unit,
    onSortChange: (Int) -> Unit,
    filterContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(
        show = true,
        title = stringResource(R.string.appinfo_sort_and_filter),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            MiuixPrefCategoryHeader(stringResource(R.string.appinfo_sort_by))
            MiuixSwitchItem(
                title = stringResource(R.string.appinfo_reverse),
                checked = reverse,
                onCheckedChange = { onReverseChange() },
            )
            sortLabels.forEachIndexed { index, label ->
                MiuixRadioItem(
                    title = label,
                    selected = sortMode == index,
                    onClick = { onSortChange(index) },
                )
            }
            if (filterContent != null) {
                MiuixPrefCategoryHeader(stringResource(R.string.appinfo_filter))
                filterContent()
            }
        }
    }
}
