package com.luckyzyx.luckytool.ui.compose.components.style

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.luckyzyx.luckytool.ui.compose.components.DialogStyle
import com.luckyzyx.luckytool.ui.compose.components.dialogBodyMaxHeight

/** material 弹窗：正文是普通文字时自带滚动与高度上限，操作按钮固定在卡片底部 */
internal object MaterialDialogStyle : DialogStyle {

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
        val body: (@Composable () -> Unit)? = if (message == null && content == null) {
            null
        } else {
            {
                Column(
                    if (bodyScrollable) {
                        // 横屏/分屏窗口变矮时正文自己滚动，按钮行不会被顶出屏幕
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = dialogBodyMaxHeight(chrome = 160.dp))
                            .verticalScroll(rememberScrollState())
                    } else {
                        // 正文自带滚动：再套一层会内外同向抢手势
                        Modifier.fillMaxWidth()
                    },
                ) {
                    if (message != null) Text(message)
                    content?.invoke(this)
                }
            }
        }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = DialogProperties(
                dismissOnBackPress = dismissible,
                dismissOnClickOutside = dismissible,
            ),
            title = title?.let { { Text(it) } },
            text = body,
            // 没有确认位时给空槽：AlertDialog 的按钮槽不可省，留空即不画
            confirmButton = confirmLabel?.let { label ->
                {
                    TextButton(onClick = { onConfirm?.invoke() }, enabled = confirmEnabled) {
                        Text(label)
                    }
                }
            } ?: {},
            dismissButton = secondaryLabel?.let { label ->
                {
                    TextButton(onClick = onSecondary ?: onDismissRequest, enabled = secondaryEnabled) {
                        Text(label)
                    }
                }
            },
        )
    }

    @Composable
    override fun Busy(title: String, modifier: Modifier, message: String) {
        AlertDialog(
            onDismissRequest = {},
            modifier = modifier,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {},
        )
    }

    @Composable
    override fun BodyText(text: String, modifier: Modifier) {
        Text(text = text, modifier = modifier.padding(20.dp))
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
        AlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            title = { Text(title) },
            text = {
                Column {
                    // 长说明放进「可滚动 + 只吃剩余高度」区域，先测量输入框，
                    // 避免说明把输入框挤出对话框
                    message?.takeIf { it.isNotEmpty() }?.let {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .heightIn(max = dialogBodyMaxHeight(chrome = 200.dp))
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = hint?.let { { Text(it) } },
                        singleLine = maxLines == 1,
                        maxLines = maxLines,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(confirmLabel) }
            },
            dismissButton = dismissLabel?.let { label ->
                {
                    TextButton(onClick = onDismissRequest) { Text(label) }
                }
            },
        )
    }
}
