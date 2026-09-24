package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveUSBConnectDialog : Hooker {
    override fun onHook() {
        //Source UsbService
        VariousClass(
            "com.coloros.systemui.notification.usb.UsbService", //A11
            "com.oplusos.systemui.notification.usb.UsbService",
            "com.oplus.systemui.usb.UsbService" //C14 C15
        ).toClass().resolve().optional(true).apply {
            (firstMethodOrNull { name = "onUsbConnected" }
                ?: firstMethod { name { it.contains("onUsbConnected") } }).hook {
                before {
                    val instance = instanceOrNull ?: args().first().any()
                    val context = args().last().cast<Context>() ?: return@before
                    firstMethod { name = "onUsbSelect" }.of(instance).invoke(1)
                    firstMethod { name = "updateAdbNotification" }.of(instance).invoke(context)
                    firstMethod { name = "updateUsbNotification" }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(Context::class.java)
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    firstMethod { name = "changeUsbConfig" }.let {
                        val contextIndex = it.self.parameterTypes.indexOf(Context::class.java)
                        if (contextIndex == 0) it.of(instance).invoke(context, 1)
                        else it.of(instance).invoke(1, context)
                    }
                    resultNull()
                }
            }
            firstMethod { name = "updateUsbNotification" }.hook {
                before {
                    firstField {
                        name { it.contains("NeedShowUsbDialog", true) }
                    }.of(instance).set(false)
                }
            }
        }
    }
}