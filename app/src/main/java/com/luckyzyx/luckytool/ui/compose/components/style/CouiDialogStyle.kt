package com.luckyzyx.luckytool.ui.compose.components.style

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.InputDialog as CouiInputDialog
import com.itos.coui.shape.CouiShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.DialogStyle
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiProgressDialog

/** COUI 弹窗：按钮行由库按自身规范绘制，主操作是 Primary 样式 */
internal object CouiDialogStyle : DialogStyle {

    @Composable
    override fun Alert(
        onDismissRequest: () -> Unit,
        confirmLabel: String?,
        onConfirm: (() -> Unit)?,
        modifier: Modifier,
        title: String?,
        message: String?,
        secondaryLabel: String?,
        onSecondary: (() -> Unit)?,
        confirmEnabled: Boolean,
        secondaryEnabled: Boolean,
        dismissible: Boolean,
        bodyScrollable: Boolean,
        content: (@Composable ColumnScope.() -> Unit)?,
    ) {
        val confirmAction = confirmLabel?.let { label ->
            CouiDialogAction(
                text = label,
                onClick = onConfirm ?: onDismissRequest,
                enabled = confirmEnabled,
                style = CouiDialogActionStyle.Primary,
                // Primary 样式必须显式给按钮配色，构造时缺 colors 会直接抛异常
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            )
        }
        val dismissAction = secondaryLabel?.let { label ->
            CouiDialogAction(text = label, onClick = onSecondary ?: onDismissRequest, enabled = secondaryEnabled)
        }
        if (confirmAction == null && dismissAction == null) {
            // 确认位是 COUI 弹窗的必填项，画不出「零按钮」：改由裸 Dialog 承载一张配色卡片
            ButtonlessCard(
                onDismissRequest = { if (dismissible) onDismissRequest() },
                modifier = modifier,
                title = title,
                message = message,
                bodyScrollable = bodyScrollable,
                content = content,
            )
            return
        }
        CouiAlertDialog(
            // 库只在返回键/点击外部时回调 onDismissRequest，空实现即等于不可取消
            onDismissRequest = { if (dismissible) onDismissRequest() },
            modifier = modifier,
            title = title,
            message = message,
            // 只剩一个次级操作时占住必填的确认位，用默认（次级）样式而不是主操作样式
            confirmAction = confirmAction ?: dismissAction!!,
            dismissAction = if (confirmAction == null) null else dismissAction,
            // 库只给标题与正文加 24dp 内边距，content 槽是裸 ColumnScope：自定义内容自己补留白，
            // 否则两条外观线共用外壳时 COUI 侧内容会贴着卡片边缘（material 的 text 槽自带 24dp）
            content = if (content == null) {
                {}
            } else {
                {
                    Column(Modifier.padding(horizontal = CouiDialogDefaults.contentPadding)) {
                        content()
                    }
                }
            },
            // bodyScrollable 在 COUI 线不参与：库的正文槽本来就不包滚动，滚动由正文自己决定
        )
    }

    @Composable
    override fun Busy(title: String, modifier: Modifier, message: String) {
        CouiProgressDialog(title = title, modifier = modifier, message = message)
    }

    @Composable
    override fun BodyText(text: String, modifier: Modifier) {
        CouiText(
            modifier = modifier.padding(vertical = 16.dp),
            text = text,
            kind = CouiTextKind.Search,
            color = CouiTheme.colorScheme.onSurface,
        )
    }

    @Composable
    override fun Input(
        title: String,
        value: String,
        onValueChange: (String) -> Unit,
        onDismissRequest: () -> Unit,
        confirmLabel: String,
        onConfirm: () -> Unit,
        modifier: Modifier,
        message: String?,
        hint: String?,
        clearContentDescription: String?,
        dismissLabel: String?,
        keyboardType: KeyboardType,
        maxLines: Int,
    ) {
        // COUI 的输入弹窗固定单行，多行内容只能在输入框里整体编辑
        CouiInputDialog(
            value = TextFieldValue(value),
            onValueChange = { onValueChange(it.text) },
            onDismissRequest = onDismissRequest,
            confirmAction = CouiDialogAction(
                text = confirmLabel,
                onClick = onConfirm,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            // 库的清空键描述不接受 null，缺省给空串（清空键仍可用，只是没有朗读文本）
            clearContentDescription = clearContentDescription.orEmpty(),
            title = title,
            message = message,
            placeholder = hint,
            dismissAction = dismissLabel?.let { label ->
                CouiDialogAction(text = label, onClick = onDismissRequest)
            },
            dark = CouiTheme.isDark,
            accent = CouiTheme.colorScheme.accent,
        )
    }
}
/**
 * 没有任何按钮的展示型弹层：一张 COUI 配色卡片，标题在上、正文在下，不留按钮区。
 *
 * COUI 弹窗的确认位必填，表达不了零按钮，所以这里自己画卡片。[onDismissRequest] 已经过
 * dismissible 判断。[bodyScrollable] 与普通弹窗同一含义：正文自带滚动时不要再套一层滚动。
 */
@Composable
private fun ButtonlessCard(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    title: String?,
    message: String?,
    bodyScrollable: Boolean,
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    val scheme = CouiTheme.colorScheme
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(CouiShape(24.dp, 2f))
                .background(scheme.container)
                .padding(vertical = 24.dp),
        ) {
            if (title != null) {
                CouiText(
                    text = title,
                    kind = CouiTextKind.ItemTitle,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(horizontal = CouiDialogDefaults.contentPadding),
                )
                Spacer(Modifier.height(8.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (bodyScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    // 与普通弹窗的正文槽一致：内容左右留白由卡片给，调用方不再补
                    .padding(horizontal = CouiDialogDefaults.contentPadding),
            ) {
                if (message != null) {
                    CouiText(text = message, kind = CouiTextKind.ItemSummary, color = scheme.onSurfaceVariant)
                }
                content?.invoke(this)
            }
        }
    }
}
