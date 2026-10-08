package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 文本输入对话框（对应 material 线 `EditTextPreference` 的 M3 对话框形态）。
 *
 * 使用 `overlay.OverlayDialog`：弹层由根部 Miuix Scaffold 的 popup host 承载
 * （`renderInRootScaffold` 保持默认 true），不使用平台窗口版 `WindowDialog`。
 *
 * 文本状态由对话框内局部持有（每次 [show] 变 true 时以 [value] 重置），确认时由调用方
 * 提交，与 material 基线 `PrefScope.editText`（`var text by remember { mutableStateOf(current) }`
 * + 确认落盘）行为一致；[onValueChange] 提供实时镜像回调。
 */
@Composable
fun MiuixPrefTextDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    summary: String? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismiss,
        content = {
            var text by remember(show) { mutableStateOf(value) }
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                modifier = Modifier.padding(bottom = 16.dp),
                label = placeholder ?: "",
                useLabelAsPlaceholder = placeholder != null,
                keyboardOptions = keyboardOptions,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}
