package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 备份/恢复结束后的结果对话框：标题 + 成功/失败计数正文；失败条目不为空时提供
 * 「查看失败信息」按钮，点击切换到失败条目明细，明细返回回到计数页。
 *
 * 两条 UI 线各用自己的一套：Material 走 material3 [AlertDialog]，Coui 走 [CouiAlertDialog]；
 * 关闭按钮是主操作（Primary），「查看失败信息」放在次级位。
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
        val detailsTitle = stringResource(R.string.data_result_failed_entries)
        val detailsText = failedKeys.joinToString("\n")
        when (LocalUiMode.current) {
            UiMode.Material -> AlertDialog(
                onDismissRequest = { showDetails = false },
                title = { Text(detailsTitle) },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = dialogBodyMaxHeight(chrome = 160.dp))
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(detailsText)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetails = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            )

            UiMode.Coui -> CouiAlertDialog(
                onDismissRequest = { showDetails = false },
                title = detailsTitle,
                message = detailsText,
                confirmAction = CouiDialogAction(
                    text = stringResource(android.R.string.ok),
                    onClick = { showDetails = false },
                    style = CouiDialogActionStyle.Primary,
                    colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
                ),
            )
        }
    } else {
        when (LocalUiMode.current) {
            UiMode.Material -> AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = if (failedKeys.isNotEmpty()) {
                    {
                        TextButton(onClick = { showDetails = true }) {
                            Text(viewFailuresLabel)
                        }
                    }
                } else {
                    null
                },
            )

            UiMode.Coui -> CouiAlertDialog(
                onDismissRequest = onDismiss,
                title = title,
                message = message,
                confirmAction = CouiDialogAction(
                    text = stringResource(android.R.string.ok),
                    onClick = onDismiss,
                    style = CouiDialogActionStyle.Primary,
                    colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
                ),
                dismissAction = if (failedKeys.isNotEmpty()) CouiDialogAction(
                    text = viewFailuresLabel,
                    onClick = { showDetails = true },
                ) else null,
            )
        }
    }
}