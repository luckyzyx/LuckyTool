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
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilterChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiModalBottomSheet
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 排序/筛选底部弹层。
 *
 * DarkModePage / HideAppIntentPage / MultiAppPage / ZoomWindowPage 共用这一份入口：
 * 四个调用点传同一组参数，具体呈现按 [LocalUiMode] 分线 ——
 * Material 线是 [SortFilterSheetMaterial]，COUI 线是 [CouiSortFilterSheet]。
 * 两条线的反向开关与排序选中态都由调用方持有，这里只上报点击。
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
        UiMode.Material -> SortFilterSheetMaterial(
            reverse = reverse,
            sortMode = sortMode,
            sortLabels = sortLabels,
            onReverseChange = onReverseChange,
            onSortChange = onSortChange,
            filterContent = filterContent,
            onDismiss = onDismiss,
        )

        UiMode.Coui -> CouiSortFilterSheet(
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

/** Material 线：material3 `ModalBottomSheet` + `FilterChip` 的排序/筛选弹层。 */
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
 * COUI 线：面板由 [CouiModalBottomSheet] 承载，标签用 COUI 筛选标签。
 *
 * 关闭行为与 Material 线一致：点遮罩或按返回键都走 [onDismiss]；面板本身已经带了
 * 最大高度与竖向滚动，内容再多也只滚面板内部，不会把整块内容顶出屏幕。
 * 左右留白在这里补，弹层自带的上下留白与导航栏内边距不再重复叠加。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiSortFilterSheet(
    reverse: Boolean,
    sortMode: Int,
    sortLabels: List<String>,
    onReverseChange: () -> Unit,
    onSortChange: (Int) -> Unit,
    filterContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val colors = CouiTheme.colorScheme
    CouiModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            CouiText(
                text = stringResource(R.string.appinfo_sort_and_filter),
                kind = CouiTextKind.GroupTitle,
                color = colors.onSurface,
            )
            Spacer(Modifier.size(8.dp))
            CouiText(
                text = stringResource(R.string.appinfo_sort_by),
                kind = CouiTextKind.PreferenceTitle,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            CouiFilterChip(
                label = stringResource(R.string.appinfo_reverse),
                selected = reverse,
                onClick = onReverseChange,
            )
            FlowRow {
                sortLabels.forEachIndexed { index, label ->
                    CouiFilterChip(
                        label = label,
                        selected = sortMode == index,
                        onClick = { onSortChange(index) },
                    )
                }
            }
            if (filterContent != null) {
                Spacer(Modifier.size(12.dp))
                CouiText(
                    text = stringResource(R.string.appinfo_filter),
                    kind = CouiTextKind.PreferenceTitle,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                filterContent()
            }
        }
    }
}
