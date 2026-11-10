package com.luckyzyx.luckytool.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import org.lsposed.lsparanoid.Obfuscate

/**
 * 捐赠二维码 Compose 对话框
 */
@Obfuscate
object DonateUtils {

    @Composable
    fun DonateQRDialog(type: Int, onDismiss: () -> Unit) {
        val title = when (type) {
            0 -> stringResource(R.string.qq)
            1 -> stringResource(R.string.wechat)
            2 -> stringResource(R.string.alipay)
            else -> ""
        }
        val base64Str = when (type) {
            0 -> Base64CodeUtils.qqCode
            1 -> Base64CodeUtils.wechatCode
            2 -> Base64CodeUtils.alipayCode
            else -> null
        }
        val bitmap = remember(type) { base64Str?.let { base64ToBitmap(it) }?.asImageBitmap() }
        when (LocalUiMode.current) {
            UiMode.Material -> MaterialDonateQRDialog(title, bitmap, onDismiss)
            UiMode.Coui -> CouiDonateQRDialog(title, bitmap, onDismiss)
        }
    }

    /** Material 线：material3 弹窗，标题 + 二维码 + 说明，单个确定按钮 */
    @Composable
    private fun MaterialDonateQRDialog(
        title: String, bitmap: ImageBitmap?, onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    bitmap?.let { Image(it, contentDescription = title) }
                    Text(stringResource(R.string.donate_message))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }

    /**
     * Coui 线：COUI 弹窗。
     *
     * 二维码与说明都放进正文槽（内容槽排在 message 之后，用 message 传说明会把二维码挤到文字下面），
     * 正文槽不带内边距，左右留白由这里补；它自身已是纵向滚动容器，不再套 verticalScroll。
     * 标题为空时（type 不在 0..2）不传标题，避免画出一条空白标题。
     */
    @Composable
    private fun CouiDonateQRDialog(
        title: String, bitmap: ImageBitmap?, onDismiss: () -> Unit
    ) {
        val colors = CouiTheme.colorScheme
        CouiAlertDialog(
            onDismissRequest = onDismiss,
            title = title.takeIf { it.isNotEmpty() },
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(colors.accent),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                bitmap?.let { Image(it, contentDescription = title) }
                CouiText(
                    text = stringResource(R.string.donate_message),
                    kind = CouiTextKind.Search,
                    textAlign = TextAlign.Start,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
