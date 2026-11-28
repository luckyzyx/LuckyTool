package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * 启动阻塞门对话框的规格：Xposed 未激活 / 无 root / 系统不支持
 *
 * 三个门都必须给标题：标题只放结论（未激活 / 无 root / 系统不支持），正文放原因与后续动作，
 * 这样三个门的结构、高度与排版完全一致。
 *
 * @param title 标题（结论）
 * @param message 正文（原因与后续动作，不重复标题里的结论）
 * @param neutralLabel 中性按钮文案；非空时显示，点击只关闭对话框（系统不支持时提供的「忽略」）
 */
data class GateDialogSpec(
    val title: String,
    val message: String,
    val neutralLabel: String? = null,
)

/**
 * 启动阻塞门对话框（不可取消）
 *
 * 用于应用起不来时把原因讲清楚：Xposed 未激活 / 无 root / 系统不支持。
 * 它是 Activity 生命周期里的阻塞门，由 com.luckyzyx.luckytool.ui.compose.MainShell
 * 直接组合，不挂在任何页面上。
 *
 * 呈现与应用其余对话框保持一致：外壳走 DialogStyle，与其余弹窗同一套按钮行与配色。
 *
 * 不可取消：返回键与点击外部都不关闭，只能通过按钮离开。
 * 没有单独的 dismiss 回调：确定按钮由调用方直接 exitProcess(0)，
 * 中性按钮只需把门对话框置空，都不需要再上报一次「已关闭」。
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
    val neutralLabel = spec.neutralLabel
    currentDialogStyle().Alert(
        onDismissRequest = {},
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = onConfirm,
        title = spec.title,
        message = spec.message,
        secondaryLabel = neutralLabel,
        onSecondary = if (neutralLabel != null) onNeutral else null,
        dismissible = false,
    )
}
