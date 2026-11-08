package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog as CouiDialog
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 启动阻塞门对话框的规格：Xposed 未激活 / 无 root / 系统不支持
 *
 * @param title 标题；为 null 时不显示标题（Xposed 未激活的场景只显示正文）
 * @param message 正文
 * @param neutralLabel 中性按钮文案；非空时显示，点击只关闭对话框（系统不支持时提供的「忽略」）
 */
data class GateDialogSpec(
    val title: String? = null,
    val message: String,
    val neutralLabel: String? = null,
)

/**
 * 启动阻塞门对话框（不可取消）：Material / Coui 双线实现
 *
 * 用于应用起不来时把原因讲清楚：Xposed 未激活 / 无 root / 系统不支持。
 * 它是 Activity 生命周期里的阻塞门，由 [com.luckyzyx.luckytool.ui.compose.MainShell]
 * 直接组合，不挂在任何页面上。
 *
 * 呈现与应用其余对话框保持一致，因此同样按 `UiMode` 分线：
 * Material 线走 material3 `AlertDialog`；Coui 线走 COUI 弹窗，按钮行由库按自身规范绘制。
 *
 * 不可取消：返回键与点击外部都不关闭
 * （靠 [DialogProperties] 的 `dismissOnBackPress` / `dismissOnClickOutside`），只能通过按钮离开。
 * 也没有单独的 dismiss 回调：确定按钮由调用方直接 `exitProcess(0)`，
 * 中性按钮只需把门对话框置空，都不需要再上报一次"已关闭"。
 *
 * @param onConfirm 确定按钮：由调用方决定是否退出进程
 * @param onNeutral 中性按钮：只关闭对话框
 */
@Composable
fun GateDialog(
    spec: GateDialogSpec,
    onConfirm: () -> Unit,
    onNeutral: () -> Unit,
) {
    val title = spec.title
    val neutralLabel = spec.neutralLabel
    when (LocalUiMode.current) {
        UiMode.Material -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            title = title?.let { { Text(it) } },
            text = { Text(spec.message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = neutralLabel?.let { label ->
                {
                    TextButton(onClick = onNeutral) {
                        Text(label)
                    }
                }
            },
        )

        UiMode.Coui -> CouiGateDialog(
            title = title,
            message = spec.message,
            neutralLabel = neutralLabel,
            onConfirm = onConfirm,
            onNeutral = onNeutral,
        )
    }
}

/**
 * Coui 线：COUI 弹窗。
 *
 * 中性按钮放取消位而不是中性位：COUI 的中性位固定按警示色渲染，与本对话框「点它只是关掉」的语义不符；
 * 放到取消位后顺序仍是「中性左 / 确定右」，只有一个中性按钮时也不会被染成警示色。
 * 取消位为 null 时库只画一个铺满整行的确定按钮。
 */
@Composable
private fun CouiGateDialog(
    title: String?,
    message: String,
    neutralLabel: String?,
    onConfirm: () -> Unit,
    onNeutral: () -> Unit,
) {
    CouiDialog(
        // 不可取消：返回键与点击外部都不关闭，与其余线一致
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = title,
        message = message,
        confirmAction = CouiDialogAction(
            text = stringResource(android.R.string.ok),
            onClick = onConfirm,
            style = CouiDialogActionStyle.Primary,
            colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
        ),
        dismissAction = neutralLabel?.let { label ->
            CouiDialogAction(text = label, onClick = onNeutral)
        },
    )
}
