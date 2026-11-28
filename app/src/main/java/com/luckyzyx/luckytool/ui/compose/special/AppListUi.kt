package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.AppSwitch
import com.luckyzyx.luckytool.ui.compose.components.PrefRowSummary
import com.luckyzyx.luckytool.ui.compose.components.PrefRowTitle
import com.luckyzyx.luckytool.ui.compose.components.currentPrefStyle

/**
 * 应用列表页（ZoomWindowPage / MultiAppPage / DarkModePage）共享的组件。
 *
 * 控件外形与配色交给样式位（[currentPrefStyle]），三个页面共用同一份状态与回调，
 * 不必各写一份行 / 搜索框。
 */

/**
 * 搜索框与下方列表之间的间距。
 *
 * 搜索框与 LazyColumn 是同一 Column 的兄弟节点，Compose 不替它们保留间距：
 * 不留这层空白，两者在视觉上会直接"贴"在一起。
 * 取 12dp 而不用分节间距：搜索框与列表属于同一组内容，只需分开、不需要分成两组。
 */
internal val AppListContentGap = 12.dp

/**
 * 整页加载罩：loading 时用居中的圆形进度条顶替页面内容，否则原样显示内容。
 *
 * 页面根容器都在 LazyItemScope 里用 `fillParentMaxHeight()` 取满视口高度，宽度
 * 由罩子自己补满，进度条因此对齐页面中心；loading 期间内容不参与组合，陈旧的
 * 行也不会再响应点击。进度条由样式位按外观线画，这里只负责居中与取满宽度。
 */
@Composable
internal fun AppListLoadingContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (visible) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            currentPrefStyle().Progress(Modifier)
        }
    } else {
        content()
    }
}

/**
 * 应用行：图标 + 应用名 + 包名 + 只读开关，整行点击切换（VirtualKey 触感 → [onToggle]）。
 *
 * 文字与开关都用行层原语：应用名 / 包名限 2 行省略，开关与整行共用 `interactionSource`，
 * 因此按压时开关的高光与整行同步。
 *
 * 行尾开关在两条线上都只显示状态、不接手势（`onCheckedChange = null`）：点击与触感由整行统一处理，
 * 开关再挂手势会与整行点击重复触发。
 */
@Composable
internal fun AppToggleRow(
    info: AppInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    AppListItem(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onToggle(!enabled)
        },
        interactionSource = interactionSource,
        headlineContent = { PrefRowTitle(info.name, maxLines = 2) },
        supportingContent = { PrefRowSummary(info.packageName, maxLines = 2) },
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
            AppSwitch(
                checked = enabled,
                onCheckedChange = null,
                interactionSource = interactionSource,
            )
        },
    )
}
