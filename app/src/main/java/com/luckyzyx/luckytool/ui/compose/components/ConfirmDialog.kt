package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 二次确认对话框：一段正文 + 确认 / 取消
 *
 * 标题可选：只在需要点明操作对象时才给（例如「重启系统」「备份数据」），
 * 正文负责讲清「会发生什么」，两者不要重复同一句话。
 * 确认按钮默认是系统「确定」；动作本身就是按钮含义时传 [confirmLabel]（如「备份数据」）。
 *
 * 两条 UI 线各用自己的一套：Material 走 material3 `AlertDialog`，
 * Coui 走 [CouiAlertDialog]（确认是 Primary、取消放取消位）。
 * 取消不放中性位：COUI 的中性位固定按警示色渲染，与「点它只是关掉」的语义不符。
 *
 * 返回键与点击外部等同取消，都回调 [onDismiss]；是否真的关闭由调用方决定。
 *
 * @param message 正文，多段内容自己用换行拼好
 * @param onConfirm 确认回调，调用方在里面执行动作并收起对话框
 * @param onDismiss 取消 / 返回键 / 点击外部
 * @param title 标题，null 表示不显示标题
 * @param confirmLabel 确认按钮文案，null 取系统「确定」
 * @param dismissLabel 取消按钮文案，null 取系统「取消」
 */
@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    confirmLabel: String? = null,
    dismissLabel: String? = null,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> AlertDialog(
            onDismissRequest = onDismiss,
            title = title?.let { { Text(it) } },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(confirmLabel ?: stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel ?: stringResource(android.R.string.cancel))
                }
            },
        )

        UiMode.Coui -> CouiAlertDialog(
            onDismissRequest = onDismiss,
            title = title,
            message = message,
            confirmAction = CouiDialogAction(
                text = confirmLabel ?: stringResource(android.R.string.ok),
                onClick = onConfirm,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = dismissLabel ?: stringResource(android.R.string.cancel),
                onClick = onDismiss,
            ),
        )
    }
}
