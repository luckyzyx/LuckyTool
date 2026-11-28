package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itos.coui.icons.CouiIcons
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 应用列表页与选择器共用的搜索栏：前置放大镜 + 受控输入 + 可选清除 + 可选排序筛选入口。
 *
 * 输入框本体由样式位画（[currentPrefStyle] 的 TextField），本文件只留两条线的图标资源集与行尾按钮：
 * 尺寸、着色、图标语义和按钮位置统一在一处，免得每个页面各写一份、同一条线上出现两种笔画粗细。
 *
 * - [onFilterClick] 为 null 时不出筛选入口（选择器只搜索、不排序时用）。
 * - 输入非空才出清除按钮，点击即回调空串，清空动作由页面自己落库。
 * - [filterActive] 只影响着色：弹层打开时强调色，收起时次级色。
 */
@Composable
internal fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filterActive: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
) {
    currentPrefStyle().TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        leadingIcon = { SearchLeadingIcon() },
        placeholder = placeholder,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    SearchActionIcon(
                        onClick = { onQueryChange("") },
                        contentDescription = stringResource(R.string.appinfo_clear_search),
                        enabled = enabled,
                        couiIcon = CouiIcons.Filled.Close,
                        materialIcon = R.drawable.ic_baseline_close_24,
                    )
                }
                if (onFilterClick != null) {
                    SearchActionIcon(
                        onClick = onFilterClick,
                        contentDescription = stringResource(R.string.appinfo_sort_and_filter),
                        enabled = enabled,
                        couiIcon = CouiIcons.Filled.ListFilter,
                        materialIcon = R.drawable.baseline_filter_list_24,
                        active = filterActive,
                    )
                }
            }
        },
    )
}

/**
 * 输入框前置放大镜。
 *
 * 图标资源集按外观线选：COUI 线用 CouiIcons 矢量图标并显式给次级前景色，
 * material 线用 drawable 绘制资源、沿用输入框前导图标的默认着色。
 */
@Composable
private fun SearchLeadingIcon() {
    when (LocalUiMode.current) {
        UiMode.Coui -> Icon(
            imageVector = CouiIcons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(SearchIconSize),
            tint = CouiTheme.colorScheme.onSurfaceVariant,
        )

        UiMode.Material -> Icon(
            painter = painterResource(R.drawable.ic_baseline_search_24),
            contentDescription = null,
            modifier = Modifier.size(SearchIconSize),
        )
    }
}

/**
 * 行尾动作键（清空与筛选），两个按钮共用一套尺寸与触感。
 *
 * [active] 时换成该线自己的强调色，表示筛选弹层正开着；样式位的 IconButton 只有 danger 一档着色，
 * 表达不了这个高亮态，所以按钮形态与着色仍按线取：COUI 线无填充圆形点击面，material 线 M3 图标按钮。
 * 图标资源集同 [SearchLeadingIcon]，两份都给，各线用自己那份。
 */
@Composable
private fun SearchActionIcon(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean,
    couiIcon: ImageVector,
    materialIcon: Int,
    active: Boolean = false,
) {
    when (LocalUiMode.current) {
        UiMode.Coui -> {
            val colors = CouiTheme.colorScheme
            CouiIconButton(
                onClick = onClick,
                contentDescription = contentDescription,
                enabled = enabled,
            ) {
                Icon(
                    imageVector = couiIcon,
                    contentDescription = null,
                    modifier = Modifier.size(SearchIconSize),
                    tint = if (active) colors.accent else colors.onSurfaceVariant,
                )
            }
        }

        UiMode.Material -> {
            val colors = MaterialTheme.colorScheme
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(
                    painter = painterResource(materialIcon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(SearchIconSize),
                    tint = if (active) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 搜索栏内图标的绘制尺寸。
 *
 * 输入框本身有 48dp 起的高度，图标按 24dp 画会让整条看起来头重脚轻；
 * 20dp 与 COUI 输入框的前后图标留白（8dp）配起来比例更稳。
 */
private val SearchIconSize = 20.dp

/**
 * 应用列表页搜索栏的统一提示语。
 *
 * 这些页面搜的都是应用名或包名，同一句文案只写一处，免得换个页面就换个说法。
 */
internal const val AppSearchPlaceholder = "Name / PackageName"
