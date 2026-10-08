package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 数值编辑行（迁移自 KernelSU `ui/component/miuix/SuperEditArrow.kt`）。
 *
 * 行本身是 [ArrowPreference]（摘要显示当前值），点击后弹出 Miuix [OverlayDialog]，
 * 由数字键盘输入框改值，确认后经 [onValueChange] 回传。
 *
 * @param modifier 应用于行的修饰符。
 * @param title 行标题，同时作为对话框标题。
 * @param titleColor 标题颜色。
 * @param defaultValue 当前值，同时作为对话框初值。
 * @param summaryColor 摘要颜色。
 * @param startAction 起始槽位。
 * @param insideMargin 行内边距。
 * @param enabled 是否可用。
 * @param onValueChange 确认后的回调。
 */
@Composable
fun SuperEditArrow(
    modifier: Modifier = Modifier,
    title: String,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    defaultValue: Int = -1,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    enabled: Boolean = true,
    onValueChange: ((Int) -> Unit)? = null,
) {
    val showDialog = remember { mutableStateOf(false) }

    ArrowPreference(
        title = title,
        titleColor = titleColor,
        summary = defaultValue.toString(),
        summaryColor = summaryColor,
        startAction = startAction,
        modifier = modifier,
        insideMargin = insideMargin,
        onClick = {
            showDialog.value = true
        },
        holdDownState = showDialog.value,
        enabled = enabled,
    )

    EditDialog(
        title = title,
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false },
        dialogTextFieldValue = defaultValue,
        onValueChange = {
            onValueChange?.invoke(it)
        },
    )
}

@Composable
private fun EditDialog(
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    dialogTextFieldValue: Int,
    onValueChange: (Int) -> Unit,
) {
    // 基准仓库用 `FilterNumber`（依赖 `BaseFieldFilter`）做输入过滤与关闭复位；
    // 本仓无该基础设施，改为「受控 String + 纯函数过滤」，语义保持一致：
    // 每次打开（或外部值变化）取当前值作初值，关闭时复位。
    var inputValue by remember(show, dialogTextFieldValue) {
        mutableStateOf(dialogTextFieldValue.toString())
    }

    OverlayDialog(
        show = show,
        title = title,
        onDismissRequest = {
            onDismissRequest()
            inputValue = dialogTextFieldValue.toString()
        },
        content = {
            TextField(
                modifier = Modifier.padding(bottom = 16.dp),
                value = inputValue,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                onValueChange = { inputValue = filterNumberInput(inputValue, it) },
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = {
                        onDismissRequest()
                        inputValue = dialogTextFieldValue.toString()
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        onDismissRequest()
                        val parsed = inputValue.toIntOrNull() ?: 0
                        onValueChange(parsed)
                        inputValue = parsed.toString()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
}

/**
 * 数字输入过滤（对齐 KernelSU `FilterNumber.filterInputNumber` 的可观察语义）：
 * 只允许可选前导负号与十进制数字，越界或含非法字符时保留上一次有效值。
 */
private fun filterNumberInput(previous: String, input: String): String {
    if (input.isEmpty()) return ""
    val negative = input.startsWith('-')
    val digits = if (negative) input.substring(1) else input
    if (digits.isEmpty()) return if (negative) "-" else ""
    if (digits.any { !it.isDigit() }) return previous
    val magnitude = digits.toLongOrNull() ?: return previous
    val signed = if (negative) -magnitude else magnitude
    return if (signed in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) input else previous
}
