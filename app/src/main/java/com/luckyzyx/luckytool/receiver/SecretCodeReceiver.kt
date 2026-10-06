package com.luckyzyx.luckytool.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.ui.activity.MainActivity
import org.lsposed.lsparanoid.Obfuscate

@Suppress("PrivatePropertyName")
@Obfuscate
class SecretCodeReceiver : BroadcastReceiver() {
    private val SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE"
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == SECRET_CODE_ACTION) {
            val code = intent.data?.host ?: return
            if (code == "582598665") Intent().apply {
                setClass(context, classOf<MainActivity>())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                context.startActivity(this)
            }
        }
    }
}