package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.luckyzyx.luckytool.R

/**
 * 备份/恢复结束后的结果对话框：标题 + 成功/失败计数正文；失败条目不为空时提供
 * 「查看失败信息」按钮，点击切换到失败条目明细，明细返回回到计数页。
 *
 * 外观由 DialogStyle 决定：关闭按钮是主操作，「查看失败信息」放在次级位。
 *
 * @param title 标题，例如「数据备份已完成」
 * @param message 计数正文，例如「备份完成：成功 12，失败 1」
 * @param failedKeys 失败条目，逐行展示在明细里
 * @param onDismiss 关闭整个对话框（明细页的返回不算关闭）
 */
@Composable
fun DataResultDialog(
    title: String,
    message: String,
    failedKeys: List<String>,
    onDismiss: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }
    val viewFailuresLabel = stringResource(R.string.data_result_view_failures)
    if (showDetails) {
        currentDialogStyle().Alert(
            onDismissRequest = { showDetails = false },
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = { showDetails = false },
            title = stringResource(R.string.data_result_failed_entries),
            message = failedKeys.joinToString("\n"),
        )
    } else {
        currentDialogStyle().Alert(
            onDismissRequest = onDismiss,
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = onDismiss,
            title = title,
            message = message,
            secondaryLabel = if (failedKeys.isNotEmpty()) viewFailuresLabel else null,
            onSecondary = if (failedKeys.isNotEmpty()) {
                { showDetails = true }
            } else {
                null
            },
        )
    }
}
