package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/**
 * 筛选标签：可反复切换选中态。
 *
 * 选中时用半透明 accent 底 + accent 文字，未选中用 container 底 + onSurfaceVariant 文字；
 * 两种状态都保持同一胶囊轮廓，切换时只换颜色，不会跳尺寸。
 * 排版交给外层（一般放进可横向滚动的 Row），这里只负责单个标签。
 */
@Composable
internal fun CouiFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = CouiTheme.colorScheme
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> colors.accent
        else -> colors.onSurfaceVariant
    }
    CouiChipSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = if (selected) colors.accent.copy(alpha = 0.16f) else colors.container,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(4.dp))
        }
        CouiText(
            text = label,
            kind = CouiTextKind.ItemValue,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 建议标签：点一下就执行某个动作，本身不保持选中态。
 *
 * 底色始终是 container，和筛选标签区分开，避免用户以为它也是一组二选一的开关。
 */
@Composable
internal fun CouiSuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colorScheme
    CouiChipSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = colors.container,
    ) {
        CouiText(
            text = label,
            kind = CouiTextKind.ItemValue,
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 辅助标签：动作前可带一个图标，用于「添加 / 跳转」这类轻量入口。
 *
 * 图标尺寸由调用方决定，这里只在图标后留 4dp 间距；文字超长时省略而不是换行。
 */
@Composable
internal fun CouiAssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = CouiTheme.colorScheme
    val contentColor = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.38f)
    CouiChipSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = colors.container,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(4.dp))
        }
        CouiText(
            text = label,
            kind = CouiTextKind.ItemValue,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 标签的公共外壳：胶囊裁剪、底色、按压高光、点击语义与内边距。
 *
 * 高度至少 32dp，保证在密集列表里也点得准；内容水平居中排布。
 */
@Composable
private fun CouiChipSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    content: @Composable RowScope.() -> Unit,
) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(CouiCapsuleShape)
            .background(background)
            .couiButtonFeedback(dark = dark, interactionSource = interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}
