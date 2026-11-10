package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.shape.CouiShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme

/**
 * COUI 线的单行 / 多行输入框（String 版）。
 *
 * 只用 BasicTextField 做输入与光标，外观自己画：surface 底色 + 描边胶囊（多行时换成圆角矩形），
 * 描边颜色未聚焦是 outlineVariant、聚焦是 accent。placeholder 叠在输入层下面，输入为空时才画。
 *
 * 注意：
 * - 不允许改名成 OutlinedTextField 之类的 Material 语义，调用方按 COUI 的观感取用。
 * - 错误态没有专门的错误色角色，CouiColorScheme 里只有配色字段，所以用 onSurface 满强度描边
 *   把「和常态不一样」这件事表达出来，不引入 Material 的 error 配色。
 */
@Composable
internal fun CouiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = CouiTheme.colorScheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textStyle = rememberCouiTextStyle(CouiTextKind.ItemTitle).copy(
        color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
    )
    CouiTextFieldFrame(
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        focused = focused,
        showPlaceholder = value.isEmpty(),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            maxLines = maxLines,
            interactionSource = source,
            cursorBrush = SolidColor(colors.accent),
        )
    }
}

/**
 * COUI 线的单行 / 多行输入框（TextFieldValue 版）。
 *
 * 与 String 版共用外观和布局，区别只在能拿到光标位置与选区，适合需要接管光标、
 * 做输入过滤或选中替换的场景。singleLine / maxLines 的语义与 String 版完全一致。
 */
@Composable
internal fun CouiTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = CouiTheme.colorScheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textStyle = rememberCouiTextStyle(CouiTextKind.ItemTitle).copy(
        color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
    )
    CouiTextFieldFrame(
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        focused = focused,
        showPlaceholder = value.text.isEmpty(),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            maxLines = maxLines,
            interactionSource = source,
            cursorBrush = SolidColor(colors.accent),
        )
    }
}

/**
 * 输入框外观：标题行、描边容器、前后图标槽、辅助文案。
 *
 * 输入控件本身由调用方作为 slot 传进来，占满图标之间的剩余宽度；
 * 多行时胶囊半径会变成一个角半径固定的圆角矩形，否则长文本会把圆角撑成拱形。
 */
@Composable
private fun CouiTextFieldFrame(
    modifier: Modifier,
    label: String?,
    placeholder: String?,
    supportingText: String?,
    isError: Boolean,
    singleLine: Boolean,
    focused: Boolean,
    showPlaceholder: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    field: @Composable () -> Unit,
) {
    val colors = CouiTheme.colorScheme
    val shape: Shape = if (singleLine) CouiCapsuleShape else CouiShape(16.dp, 2.5f)
    val outlineColor = when {
        isError -> colors.onSurface
        focused -> colors.accent
        else -> colors.outlineVariant
    }
    val hintColor = if (isError) colors.onSurface else colors.onSurfaceVariant
    Column(modifier = modifier) {
        if (label != null) {
            CouiText(
                text = label,
                kind = CouiTextKind.ItemSummary,
                color = hintColor,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(colors.surface)
                .border(1.dp, outlineColor, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(8.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                // placeholder 先画、输入层后画：输入为空时占位文案留在原位，光标叠在它上面。
                if (showPlaceholder && placeholder != null) {
                    CouiText(
                        text = placeholder,
                        kind = CouiTextKind.ItemSummary,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                field()
            }
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
        if (supportingText != null) {
            CouiText(
                text = supportingText,
                kind = CouiTextKind.ItemSummary,
                color = hintColor,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
