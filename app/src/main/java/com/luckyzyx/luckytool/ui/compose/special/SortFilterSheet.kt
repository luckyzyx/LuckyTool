package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.switch.Switch as CouiSwitch
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

/** Material 线：material3 `ModalBottomSheet` + `FilterChip` 的排序/筛选弹层；标签自带描边，折行时补 8dp 间距避免相邻描边贴成一段。 */
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
            FlowRow(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
 * COUI 线：面板由 [CouiModalBottomSheet] 承载，标题居中，反转是开关行，排序项是 COUI 筛选标签。
 *
 * 标签本身不带外边距，行内与行间都要在这里补：不补的话相邻胶囊贴在一起，整行读起来像一串连写的文字。
 * 排序项比其他入口多（6 项），用 FlowRow 折行而不是横向滚动，靠后的项不会被挤出屏幕外。
 * 标题用 [CouiTextKind.TopBar]：比小节标题更大，层级才不会反过来。
 * 关闭行为由面板负责：点遮罩或按返回键都走 [onDismiss]，内容再多也只在面板内部滚动。
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
    CouiModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            CouiText(
                text = stringResource(R.string.appinfo_sort_and_filter),
                kind = CouiTextKind.TopBar,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiText(
                    text = stringResource(R.string.appinfo_sort_by),
                    kind = CouiTextKind.ItemTitle,
                    modifier = Modifier.weight(1f),
                )
                CouiText(
                    text = stringResource(R.string.appinfo_reverse),
                    kind = CouiTextKind.ItemTitle,
                )
                CouiSwitch(
                    modifier = Modifier.padding(start = 8.dp),
                    checked = reverse,
                    onCheckedChange = null,
                    // 开关只上报“翻了一下”，具体取反由调用方持有
                    onDragCheckedChange = { onReverseChange() },
                    dark = CouiTheme.isDark,
                )
            }
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sortLabels.forEachIndexed { index, label ->
                    CouiFilterChip(
                        label = label,
                        selected = sortMode == index,
                        onClick = { onSortChange(index) },
                    )
                }
            }
            if (filterContent != null) {
                CouiDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    startIndent = 0.dp,
                    endIndent = 0.dp,
                )
                CouiText(
                    text = stringResource(R.string.appinfo_filter),
                    kind = CouiTextKind.ItemTitle,
                )
                Spacer(Modifier.size(8.dp))
                filterContent()
            }
        }
    }
}
