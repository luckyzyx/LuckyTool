package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

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
 * 启动阻塞门对话框（不可取消）：Material / Miuix 双线实现
 *
 * 用于应用起不来时把原因讲清楚：Xposed 未激活 / 无 root / 系统不支持。
 * 它是 Activity 生命周期里的阻塞门，由 [com.luckyzyx.luckytool.ui.compose.MainShell]
 * 直接组合，不挂在任何页面上。
 *
 * 呈现与应用其余对话框保持一致，因此同样按 `UiMode` 双线：
 * Miuix 线走 `overlay.OverlayDialog`，它由根部 Miuix Scaffold 的 popup host 承载，
 * 所以必须组合在 [com.luckyzyx.luckytool.ui.compose.MainShell] 内；
 * Material 线走 material3 `AlertDialog`。
 *
 * 不可取消：返回键与点击外部都不关闭
 * （Material 线靠 [DialogProperties] 的 `dismissOnBackPress` / `dismissOnClickOutside`，
 * Miuix 线的 `onDismissRequest` 传空实现），只能通过按钮离开。
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
        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = title,
            summary = spec.message,
            onDismissRequest = {},
            content = {
                Row {
                    neutralLabel?.let { label ->
                        MiuixTextButton(
                            text = label,
                            onClick = onNeutral,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(20.dp))
                    }
                    MiuixTextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )

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
    }
}
