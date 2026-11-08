package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.switch.CouiSwitchDefaults
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPrefCardScope
import com.luckyzyx.luckytool.ui.compose.components.AppSwitchItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 卡片行 / 卡原语，支持分段卡片与独立卡片。
 *
 * **「设置区块」的唯一 API 是 `PrefScope` DSL**（`group()` / `category()` / `switch()` / `click()` / `list()` /
 * `custom()`，见 `ui/compose/components/PrefScope.kt`）—— 所有标准设置页与作用域页都走它。
 * 本文件只保留 `custom()` 逃生舱（无法走 DSL 的自定义控件里的单行 / 单卡）所需的原始行 / 卡原语：
 * [PrefGroup] + [PrefRow] + [PrefSwitchRow]（组内行，形状由所属 [PrefGroup] 决定）、
 * [PrefCard] + [PrefSwitchCard]（不属于任何分段组的独立卡片）、[PrefIcon]（行首图标）、
 * [PrefColorPreview]（行尾颜色预览色块）。
 *
 * UI 线分派：material 线用 `Segmented*` 原语 + `MaterialTheme`；
 * COUI 线用 `com.itos.coui.layout.Item` + `couiButtonFeedback`；分派开关统一读 [LocalUiMode]。
 */

/**
 * 分段卡片组：组内连续条目合并成一张连续卡片
 * （首条上端、尾条下端 16dp 外圆角，组内条目不设圆角与间距，显隐带弹性过渡）。
 *
 * 组容器由 [PrefCardScope] 的实现分派：material 线走 `MaterialGroup` 动态重放
 * （保留弹性显隐过渡），COUI 线走 [CouiPrefCardScope]（首条 16dp 组间距 + 24dp 圆角切片）。
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
    val uiMode = LocalUiMode.current
    val scope: PrefCardScope = when (uiMode) {
        // COUI 线按「卡片切片」逐条渲染：首条上圆角、尾条下圆角、组内直边
        UiMode.Coui -> CouiPrefCardScope()
        else -> MaterialPrefCardScope()
    }
    content(scope)
    scope.render(modifier = modifier)
}

/** COUI 线的分段卡片组：只收集条目，渲染时按可见顺序逐条产出卡片切片 */
private class CouiPrefCardScope : PrefCardScope {
    private val collected = PrefCardEntries()

    override fun item(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        collected.add(key, visible, content)
    }

    @Composable
    override fun render(modifier: Modifier) {
        CouiPrefCardGroup(collected.entries, modifier)
    }
}

/**
 * COUI 线的分段卡片组容器：每行是一片带圆角与卡片底色的切片，行间由切片内的 CouiDivider 分隔。
 *
 * 整张卡片由一个 Column 切成多片而非一个 CouiGroup：条目各自带显隐动画，
 * 切片化后单条的进场/退场只影响自己所占的高度与圆角，不会牵动整卡。
 */
@Composable
private fun CouiPrefCardGroup(entries: List<PrefCardEntry>, modifier: Modifier = Modifier) {
    if (entries.none { it.visible }) return
    val visibleCount = entries.count { it.visible }
    Column(modifier = modifier.fillMaxWidth()) {
        var visibleIndex = 0
        entries.forEachIndexed { rawIndex, entry ->
            val index = visibleIndex
            if (entry.visible) visibleIndex++
            // 首条留出与上一区块的组间距，其余条目直接贴合，视觉上是一张连续卡片
            val gap = if (index == 0) CouiPageDefaults.groupSpacing else 0.dp
            key(entry.key ?: rawIndex) {
                AnimatedVisibility(
                    visible = entry.visible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    CouiScopeCard(
                        index = index,
                        count = visibleCount,
                        highlighted = false,
                        modifier = Modifier.padding(top = gap),
                    ) {
                        entry.content()
                    }
                }
            }
        }
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
    if (LocalUiMode.current == UiMode.Coui) {
        val colors = CouiPageDefaults.themeColors()
        val dark = CouiTheme.isDark
        val source = remember { MutableInteractionSource() }
        // COUI Item 只提供单击：带长按的行改用 combinedClickable 自行挂手势，点击仍走同一交互源
        val gesture = if (onLongClick != null) {
            Modifier
                .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = { onClick?.invoke() },
                )
        } else {
            Modifier
        }
        val summarySlot: (@Composable () -> Unit)? = summary?.let { text ->
            { CouiRowSummary(text, enabled, colors) }
        }
        CouiItem(
            modifier = modifier.then(gesture),
            icon = leading,
            onClick = if (onLongClick == null) onClick else null,
            enabled = enabled,
            title = { CouiRowTitle(title, enabled, colors) },
            summary = summarySlot,
            trailing = { trailing?.invoke() },
        )
    } else {
        // Material 线专用：COUI 线已在上面的 CouiItem 分支返回，这里交给 M3 列表行渲染
        AppListItem(
            modifier = modifier,
            onClick = onClick,
            onLongClick = onLongClick,
            enabled = enabled,
            headlineContent = { Text(title) },
            // Material 线专用：标题与说明由 M3 列表行自绘文本槽，COUI 线走 CouiRowTitle / CouiRowSummary
            supportingContent = summary?.let { { Text(it) } },
            leadingContent = leading,
            trailingContent = trailing,
        )
    }
}

/** 设置入口的单色图标，使用当前外观的中性前景色和默认图标尺寸。 */
@Composable
fun PrefIcon(icon: ImageVector) {
    if (LocalUiMode.current == UiMode.Coui) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CouiTheme.colorScheme.onSurface,
        )
    } else {
        // Material 线专用：图标前景色取 M3 主题；COUI 线在上一分支已返回
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 行尾颜色预览的边长：与行尾图片缩略图 [PrefImagePreview] 的行高同取 40dp */
private val ColorPreviewSize = 40.dp

/** 行尾颜色预览的圆角：正方形 + 6dp 圆角（与 [PrefImagePreview] 的圆角一致） */
private val ColorPreviewCornerRadius = 6.dp

/**
 * 行尾颜色预览色块：正方形 + 圆角，用于颜色偏好条目右侧。
 *
 * 形态对齐 [PrefImagePreview]：同一 40dp 边长、同一 6dp 圆角，只是没有按图片比例拉宽，
 * 因此是正方形 + 圆角而不是圆点、胶囊或宽扁的矩形。
 *
 * 描边不能取主题色的 `outline`：主题色本身可以选到白色，此时列表行与弹层都是浅色，
 * 白色描边等于没有描边，而这两个颜色偏好的默认值正是 `#FFFFFFFF`，色块会整块看不见。
 */
@Composable
fun PrefColorPreview(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ColorPreviewCornerRadius)

    Box(
        modifier
            .size(ColorPreviewSize)
            .clip(shape)
            .background(color)
            .border(1.dp, contrastBorder(color), shape)
    )
}

/**
 * 与 [color] 形成对比的描边色（[PrefColorPreview] 与颜色选择器的预览色块共用）。
 *
 * 只按颜色自身取反、不跟主题色走：浅色用深描边，深色用浅描边；半透明色一律用深描边，
 * 因为它底下透出的是浅色内容（选择器里是浅灰棋盘格）。这样无论主题色与所选颜色怎么组合，
 * 色块本体与轮廓至少有一个看得清 —— 「白色主题色 + 白色颜色」原先是两者都看不见的组合。
 */
internal fun contrastBorder(color: Color): Color =
    if (color.alpha < 0.5f || color.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }

/** 分段卡片组内的开关条目：整行点击切换，尾部为 Expressive 开关 */
@Composable
fun PrefSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        val colors = CouiPageDefaults.themeColors()
        val dark = CouiTheme.isDark
        val source = remember { MutableInteractionSource() }
        val summarySlot: (@Composable () -> Unit)? = summary?.let { text ->
            { CouiRowSummary(text, enabled, colors) }
        }
        // 整行 toggleable 承载点击切换，尾部开关只取视觉与拖动，两者共用同一个交互源
        CouiItem(
            modifier = modifier
                .couiButtonFeedback(dark = dark, interactionSource = source, enabled = enabled, list = true)
                .toggleable(
                    value = checked,
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
            icon = leading,
            title = { CouiRowTitle(title, enabled, colors) },
            summary = summarySlot,
            trailing = {
                CouiSwitch(
                    modifier = Modifier.padding(start = 16.dp),
                    checked = checked,
                    onCheckedChange = null,
                    dark = dark,
                    colors = CouiSwitchDefaults.colors(colors.accent, dark),
                    enabled = enabled,
                    interactionSource = source,
                    onDragCheckedChange = onCheckedChange,
                )
            },
        )
    } else {
        Box(modifier = modifier) {
            AppSwitchItem(
                title = title,
                summary = summary,
                checked = checked,
                enabled = enabled,
                leading = leading,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

/**
 * `custom()` 逃生舱里行内容的标准水平内缩包装，与 DSL 的 switch()/list()/click() 等分派一致：
 * material 线套 16dp 内缩（material 列表无水平 contentPadding，行需自带）；
 * COUI 线不套（CouiItem 自带 16dp 行内缩）。
 */
@Composable
fun PrefRowInset(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (LocalUiMode.current == UiMode.Coui) {
                    Modifier
                } else {
                    Modifier.padding(horizontal = 16.dp)
                },
            ),
        content = content,
    )
}

/**
 * 独立卡片（不属于任何分段组时使用）。
 * 外观与主题页一致：Material（surfaceBright 容器 + 16dp 外圆角）/
 * COUI 整卡切片（`CouiPageDefaults.shape` 24dp 圆角 + 卡片底色）。
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
    if (LocalUiMode.current == UiMode.Coui) {
        // 单条即整张卡：调用方的 modifier 交给卡片（横向内缩与底色都在卡上），行内不再叠
        CouiScopeCard(index = 0, count = 1, highlighted = false, modifier = modifier) {
            PrefRow(
                title = title,
                summary = summary,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                trailing = trailing,
            )
        }
    } else {
        AppItem(index = 0, count = 1) {
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
    if (LocalUiMode.current == UiMode.Coui) {
        CouiScopeCard(index = 0, count = 1, highlighted = false, modifier = modifier) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                summary = summary,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    } else {
        AppItem(index = 0, count = 1) {
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
