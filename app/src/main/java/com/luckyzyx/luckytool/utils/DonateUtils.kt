package com.luckyzyx.luckytool.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import org.lsposed.lsparanoid.Obfuscate
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 捐赠二维码 Compose 对话框（旧 DialogDonateLayoutBinding 对话框的等价物）
 */
@Obfuscate
object DonateUtils {

    @Composable
    fun DonateQRDialog(type: Int, onDismiss: () -> Unit) {
        val context = LocalContext.current
        val title = when (type) {
            0 -> context.getString(R.string.qq)
            1 -> context.getString(R.string.wechat)
            2 -> context.getString(R.string.alipay)
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
            UiMode.Miuix -> MiuixDonateQRDialog(title, bitmap, onDismiss)
            UiMode.Material -> AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        bitmap?.let { Image(it, contentDescription = title) }
                        Text(context.getString(R.string.donate_message))
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(context.getString(android.R.string.ok)) }
                }
            )
        }
    }
}

/**
 * 捐赠二维码对话框（Miuix 线）。
 *
 * 弹层走 `overlay.OverlayDialog`，由根部 Miuix Scaffold 的 popup host 承载
 * （`renderInRootScaffold` 保持默认 true，不自装 host）。
 */
@Composable
private fun MiuixDonateQRDialog(title: String, bitmap: ImageBitmap?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            bitmap?.let { Image(it, contentDescription = title) }
            MiuixText(text = context.getString(R.string.donate_message))
            Row(modifier = Modifier.fillMaxWidth()) {
                MiuixTextButton(
                    text = context.getString(android.R.string.ok),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
