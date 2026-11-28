package com.luckyzyx.luckytool.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.currentDialogStyle
import org.lsposed.lsparanoid.Obfuscate

/**
 * 捐赠二维码 Compose 对话框
 */
@Obfuscate
object DonateUtils {

    /**
     * 二维码弹窗：标题是渠道名，二维码与说明放进内容槽。
     *
     * 内容槽排在正文之后，二维码不会被说明挤到上面；横向留白由弹窗外壳补，
     * 这里只负责居中与两段内容的间距。标题为空时（type 不在 0..2）不传标题，
     * 避免画出一条空白标题。只有一个「确定」按钮，放确认位。
     */
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
        currentDialogStyle().Alert(
            onDismissRequest = onDismiss,
            confirmLabel = stringResource(android.R.string.ok),
            onConfirm = onDismiss,
            title = title.takeIf { it.isNotEmpty() },
            content = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    bitmap?.let { Image(it, contentDescription = title) }
                    Text(stringResource(R.string.donate_message))
                }
            },
        )
    }
}
