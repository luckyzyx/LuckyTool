package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.currentPrefStyle

/**
 * 排序/筛选底部弹层。
 *
 * DarkModePage / HideAppIntentPage / MultiAppPage / ZoomWindowPage 共用这一份入口：
 * 面板外壳、独立开关、筛选片与分隔线都交给样式位去画，这里只排内容、上报点击。
 * 反向开关与排序选中态都由调用方持有，本函数不缓存任何状态。
 * [visible] 常驻传入：面板何时组合、靠什么收起由样式位决定，这里只持有这个状态。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SortFilterSheet(
    reverse: Boolean,
    sortMode: Int,
    sortLabels: List<String>,
    onReverseChange: () -> Unit,
    onSortChange: (Int) -> Unit,
    filterContent: (@Composable () -> Unit)? = null,
    visible: MutableState<Boolean>,
) {
    currentPrefStyle().Sheet(
        visible = visible,
        title = stringResource(R.string.appinfo_sort_and_filter),
        modifier = Modifier,
        closeLabel = stringResource(R.string.close),
        doneLabel = stringResource(R.string.sheet_done),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            currentPrefStyle().Title(
                text = stringResource(R.string.appinfo_sort_by),
                enabled = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 撑开标题后面的空隙，把开关推到行尾
            Spacer(Modifier.weight(1f))
            currentPrefStyle().Title(
                text = stringResource(R.string.appinfo_reverse),
                enabled = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            currentPrefStyle().Switch(
                checked = reverse,
                // 轻点与拖动都只上报「切换动作」，具体取反由调用方持有
                onCheckedChange = { onReverseChange() },
                modifier = Modifier.padding(start = 8.dp),
                enabled = true,
            )
        }
        // 排序项最多 6 项，折行而不是横向滚动，靠后的项不会被挤出屏幕外
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sortLabels.forEachIndexed { index, label ->
                currentPrefStyle().FilterChip(
                    label = label,
                    selected = sortMode == index,
                    onClick = { onSortChange(index) },
                    modifier = Modifier,
                    enabled = true,
                )
            }
        }
        if (filterContent != null) {
            // 分隔线把排序段与筛选段分开，横向内缩交给样式位
            currentPrefStyle().Divider(Modifier.padding(top = 12.dp, bottom = 8.dp))
            currentPrefStyle().Title(
                text = stringResource(R.string.appinfo_filter),
                enabled = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            filterContent()
        }
    }
}
