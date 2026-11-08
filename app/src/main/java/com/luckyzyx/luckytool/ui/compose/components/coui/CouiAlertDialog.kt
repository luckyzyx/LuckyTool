package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.dialog.Dialog
import com.itos.coui.theme.CouiTheme

/**
 * COUI 线的确认弹窗，默认居中摆放。
 *
 * 标题、正文与按钮都由 COUI 的 Dialog 绘制：正文超长时只有正文滚动，按钮区固定在窗口底部；
 * 按钮放不下时自动改成竖排。content 落进 Dialog 的正文槽，用来塞列表、输入框等自定义内容，
 * 它与 message 可以同时给，自定义内容排在 message 之后。
 *
 * 注意：主操作按钮的 style 若是 Primary，构造 CouiDialogAction 时就必须带 colors
 * （CouiDialogDefaults.primaryActionColors(accent)），该 data class 的 init 会直接校验，
 * 缺了会抛异常，这里无法替调用方补。
 */
@Composable
internal fun CouiAlertDialog(
    onDismissRequest: () -> Unit,
    confirmAction: CouiDialogAction,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    dismissAction: CouiDialogAction? = null,
    neutralAction: CouiDialogAction? = null,
    placement: CouiDialogPlacement = CouiDialogPlacement.Center,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmAction = confirmAction,
        // 显式传当前主题深浅色：宿主可能覆盖过色板，不能让 Dialog 的默认参数自己判断。
        dark = CouiTheme.isDark,
        title = title,
        message = message,
        dismissAction = dismissAction,
        neutralAction = neutralAction,
        placement = placement,
        content = content,
    )
}
