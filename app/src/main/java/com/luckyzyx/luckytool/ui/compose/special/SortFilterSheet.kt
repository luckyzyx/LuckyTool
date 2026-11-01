package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * 排序/筛选底部弹层。
 *
 * DarkModePage / HideAppIntentPage / MultiAppPage / ZoomWindowPage 共用这一份入口：
 * 四个调用点传同一组参数，主题差异只在唯一的 `when (LocalUiMode.current)` 缝上分派：
 * - Material 线 → [SortFilterSheetMaterial]（material3 `ModalBottomSheet` + `FilterChip`）；
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

/** Material 线：material3 `ModalBottomSheet` + `FilterChip` 的排序/筛选弹层，承载内容与 [SortFilterSheetMiuix] 相同，只是控件换成 material3 原语。 */
@Suppress("DEPRECATION") // rememberModalBottomSheetState() 自 material3 1.5 起弃用（改用 rememberBottomSheetState + 显式 enabledValues）；Material 线沿用旧重载，以免改动半展开状态的既有组合
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
 *
 * 内容必须可滚动：库的 `BottomSheetColumn` 只是 `heightIn(max = 窗口高 - topInset)` 的普通 Column，
 * 行数多到超过窗口高度时，末尾行会被父 Column 按剩余高度压扁——`BasicComponent` 的 16dp 内边距会先
 * 吃掉标题的高度（标题先整体消失），固定尺寸的复选框稍后才被裁掉，看上去就是「只剩勾选框、没有文字」。
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
                .verticalScroll(rememberScrollState())
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
