package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTopBarIconButton
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 顶栏动作键与「更多」溢出菜单的唯一下发点。
 *
 * 调用点不再自己写 material3 的 IconButton/DropdownMenu，两条外观线都经本文件下发：
 * material3 [IconButton]（48dp 触摸区）+ Expressive [DropdownMenuPopup]。
 * COUI 线用无填充色的圆形点击面 + [couiButtonFeedback] 按压高光，溢出菜单是自绘弹层
 * （容器色、形状、文字色都取 COUI 语义色，见 [CouiTopBarOverflowMenu]）。
 */

/**
 * 溢出菜单的一项。
 *
 * @param label 已解析好的条目文案（调用点用 stringResource 解析后传入）。
 * @param onClick 条目点击。
 */
@Immutable
class TopBarOverflowItem(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * 顶栏动作键。
 *
 * @param onClick 点击回调。
 * @param contentDescription 无障碍描述。
 * @param painter 图标：资源图用 painterResource，矢量图用 rememberVectorPainter。
 */
@Composable
fun TopBarActionButton(
    onClick: () -> Unit,
    contentDescription: String?,
    painter: Painter,
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        CouiTopBarActionButton(
            onClick = onClick,
            contentDescription = contentDescription,
            painter = painter,
            modifier = modifier,
        )
        return
    }
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painter, contentDescription)
    }
}

/** 矢量图标版：按钮触摸区域与按压效果沿用组件默认值。 */
@Composable
fun TopBarActionButton(
    onClick: () -> Unit,
    contentDescription: String?,
    materialIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    TopBarActionButton(
        onClick = onClick,
        contentDescription = contentDescription,
        painter = rememberVectorPainter(materialIcon),
        modifier = modifier,
    )
}

/**
 * 顶栏「更多」溢出菜单。
 *
 * @param items 条目列表，顺序即展示顺序；为空时调用点不应渲染本组件。
 * @param contentDescription 动作键的无障碍描述（通常是「更多」）。
 */
@Composable
fun TopBarOverflowMenu(
    items: List<TopBarOverflowItem>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        CouiTopBarOverflowMenu(
            items = items,
            contentDescription = contentDescription,
            modifier = modifier,
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription)
        }
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        shape = MenuDefaults.itemShape(index = index, count = items.size).shape,
                        onClick = {
                            expanded = false
                            item.onClick()
                        },
                    )
                }
            }
        }
    }
}

/**
 * COUI 线动作键：有 Mesh 渲染器时按 demo 顶栏按钮样式画材质按钮，否则退回
 * 48dp 纯点击面；两种形态的图标、按压反馈与语义统一由 [CouiTopBarIconButton] 处理。
 */
@Composable
private fun CouiTopBarActionButton(
    onClick: () -> Unit,
    contentDescription: String?,
    painter: Painter,
    modifier: Modifier = Modifier,
) {
    CouiTopBarIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = CouiTheme.colorScheme.onSurface,
        )
    }
}

/**
 * COUI 线溢出菜单：自己拼弹层，结构同 material3 的「动作键 + 弹出列表」。
 *
 * 不使用 COUI 的菜单组件：它内部走 Mesh，同样在单测里构造即失败。弹层底色取
 * [CouiTheme.colorScheme.menuSurface]、圆角取 [CouiPageDefaults.shape]、条目文字取 onSurface，
 * 与 COUI 的其余浮层保持同一观感；条目按下用 [couiButtonFeedback] 画高光，弹出位置贴动作键右下角。
 */
@Composable
private fun CouiTopBarOverflowMenu(
    items: List<TopBarOverflowItem>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    val positionProvider = remember(gapPx) { CouiOverflowMenuPosition(gapPx) }
    Box(modifier = modifier) {
        CouiTopBarActionButton(
            onClick = { expanded = true },
            contentDescription = contentDescription,
            painter = rememberVectorPainter(CouiIcons.Outlined.More),
        )
        if (expanded) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 160.dp)
                        // 弹层按 WRAP_CONTENT 测量，行内 fillMaxWidth 会把宽度撑到整个窗口；
                        // 先按内容最宽行定宽，行再填满这个宽度，才会收成菜单而不是铺满屏幕。
                        .width(IntrinsicSize.Max)
                        .heightIn(max = 320.dp)
                        .shadow(elevation = 8.dp, shape = CouiPageDefaults.shape)
                        .clip(CouiPageDefaults.shape)
                        .background(CouiTheme.colorScheme.menuSurface)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    items.forEach { item ->
                        CouiOverflowMenuItemRow(label = item.label) {
                            expanded = false
                            item.onClick()
                        }
                    }
                }
            }
        }
    }
}

/** COUI 溢出菜单的一行：按下画高光，行高按 COUI 的列表行最小高度对齐。 */
@Composable
private fun CouiOverflowMenuItemRow(label: String, onClick: () -> Unit) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CouiPageDefaults.rowMinHeight)
            .couiButtonFeedback(dark = dark, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = CouiPageDefaults.rowPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        CouiText(
            text = label,
            kind = CouiTextKind.Search,
            textAlign = TextAlign.Start,
            color = CouiTheme.colorScheme.onSurface,
        )
    }
}

/** 把溢出弹层贴在动作键正下方并与其右边缘对齐，两侧留 [gap] 不贴屏幕边。 */
private class CouiOverflowMenuPosition(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap)
        val maxY = (windowSize.height - popupContentSize.height - gap).coerceAtLeast(gap)
        val alignedX = if (layoutDirection == LayoutDirection.Rtl) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        return IntOffset(
            alignedX.coerceIn(gap, maxX),
            (anchorBounds.bottom + gap).coerceIn(gap, maxY),
        )
    }
}
