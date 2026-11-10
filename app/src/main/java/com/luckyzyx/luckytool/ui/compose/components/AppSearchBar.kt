package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itos.coui.icons.CouiIcons
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextField
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 应用列表页与选择器共用的搜索栏：前置放大镜 + 受控输入 + 可选清除 + 可选排序筛选入口。
 *
 * 两条线各取自家输入框（Material 用 [OutlinedTextField]、COUI 用 [CouiTextField]），
 * 图标也各用自家图标集，只把尺寸、着色、图标语义和按钮位置统一在一处，
 * 免得每个页面各写一份、同一条线上出现两种笔画粗细。
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
    when (LocalUiMode.current) {
        UiMode.Material -> OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_search_24),
                    contentDescription = null,
                    modifier = Modifier.size(SearchIconSize),
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, enabled = enabled) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_close_24),
                                contentDescription = stringResource(R.string.appinfo_clear_search),
                                modifier = Modifier.size(SearchIconSize),
                            )
                        }
                    }
                    if (onFilterClick != null) {
                        IconButton(onClick = onFilterClick, enabled = enabled) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_filter_list_24),
                                contentDescription = stringResource(R.string.appinfo_sort_and_filter),
                                modifier = Modifier.size(SearchIconSize),
                                tint = if (filterActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            },
            modifier = modifier.fillMaxWidth(),
        )

        UiMode.Coui -> {
            val colors = CouiTheme.colorScheme
            CouiTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                placeholder = placeholder,
                leadingIcon = {
                    Icon(
                        imageVector = CouiIcons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(SearchIconSize),
                        tint = colors.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            CouiIconButton(
                                onClick = { onQueryChange("") },
                                contentDescription = stringResource(R.string.appinfo_clear_search),
                                enabled = enabled,
                            ) {
                                Icon(
                                    imageVector = CouiIcons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SearchIconSize),
                                    tint = colors.onSurfaceVariant,
                                )
                            }
                        }
                        if (onFilterClick != null) {
                            CouiIconButton(
                                onClick = onFilterClick,
                                contentDescription = stringResource(R.string.appinfo_sort_and_filter),
                                enabled = enabled,
                            ) {
                                Icon(
                                    imageVector = CouiIcons.Filled.ListFilter,
                                    contentDescription = null,
                                    modifier = Modifier.size(SearchIconSize),
                                    tint = if (filterActive) colors.accent else colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                modifier = modifier.fillMaxWidth(),
            )
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
