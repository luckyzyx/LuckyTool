package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/**
 * 文字按钮：透明底 + accent 文字的胶囊按钮。
 *
 * 用于弹窗的次要动作、行末的轻量操作。禁用态文字降到 onSurfaceVariant 低透明度，
 * 底色仍透明，不会在页面上留下灰块。
 */
@Composable
internal fun CouiTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colorScheme
    CouiPillButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = Color.Transparent,
    ) {
        CouiText(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = if (enabled) colors.accent else colors.onSurfaceVariant.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 强调按钮（纯文字版）：accent 底 + onAccent 文字的胶囊按钮。
 *
 * 一屏只放一个主操作；禁用态降低底色不透明度而不是换成灰色，避免和次要按钮的观感混同。
 */
@Composable
internal fun CouiFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CouiTheme.colorScheme
    CouiFilledButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        CouiText(
            text = text,
            kind = CouiTextKind.ItemTitle,
            color = colors.onAccent.copy(alpha = if (enabled) 1f else 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 强调按钮（内容槽版）：内容由调用方给，可放图标 + 文字。
 *
 * 内容按水平居中等分排布在胶囊里，RowScope 里可直接用 weight 让图标与文字对齐。
 */
@Composable
internal fun CouiFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = CouiTheme.colorScheme
    CouiPillButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = colors.accent.copy(alpha = if (enabled) 1f else 0.35f),
        content = content,
    )
}

/**
 * 图标按钮：40dp 方形点击面 + 圆形按压高光，内容（图标）由调用方给。
 *
 * 点击面固定 40dp，图标建议 24dp；contentDescription 为重读屏文案，
 * 若内容自带语义（图标自身已声明），这里传 null 就不会重复播报。
 */
@Composable
internal fun CouiIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val description = contentDescription
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .couiButtonFeedback(dark = dark, interactionSource = interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (description != null) {
                    Modifier.semantics { this.contentDescription = description }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 胶囊按钮的公共骨架：裁剪、底色、按压高光、点击语义与内边距都在这里，
 * 三个按钮只负责决定底色和内容。
 */
@Composable
private fun CouiPillButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    backgroundColor: Color,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CouiCapsuleShape)
            .background(backgroundColor)
            // 先裁剪再画按压高光，高光不会溢到圆角之外。
            .couiButtonFeedback(dark = dark, interactionSource = interactionSource, enabled = enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}
