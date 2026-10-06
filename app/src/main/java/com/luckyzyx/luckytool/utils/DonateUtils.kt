package com.luckyzyx.luckytool.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.databinding.DialogDonateLayoutBinding
import org.lsposed.lsparanoid.Obfuscate

@Suppress("unused")
@Obfuscate
object DonateUtils {
    private val CNU = "Null"
    private val CQQ = "QQ"
    private val CQQHB = "QQHB"
    private val CWC = "WeChat"
    private val CAP = "AliPay"
    private val CPP = "PayPal"

    fun showQRCode(context: Context, type: Int) {
        val binding = DialogDonateLayoutBinding.inflate(context.layoutInflater)
        MaterialAlertDialogBuilder(context, dialogCentered).apply {
            setTitle(
                when (type) {
                    0 -> context.getString(R.string.qq)
                    1 -> context.getString(R.string.wechat)
                    2 -> context.getString(R.string.alipay)
                    else -> ""
                }
            )
            setView(binding.root)
        }.show()
        val base64Str = when(type) {
            0 -> Base64CodeUtils.qqCode
            1 -> Base64CodeUtils.wechatCode
            2 -> Base64CodeUtils.alipayCode
            else -> null
        } ?: return
        binding.donateImage.setImageBitmap(base64ToBitmap(base64Str))
        binding.donateMessage.text = context.getString(R.string.donate_message)
    }
}