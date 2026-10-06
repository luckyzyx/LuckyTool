package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFlashlightOpenNotification : YukiBaseHooker() {
    override fun onHook() {
        //Source FlashlightNotification
        VariousClass(
            "com.oplusos.systemui.flashlight.FlashlightNotification", //C13
            "com.oplus.systemui.statusbar.notification.flashlight.FlashlightNotification", //C14
            "com.oplus.systemui.notification.flashlight.FlashlightNotification" //C15.0.1
        ).toClass().resolve().apply {
            firstMethod {
                name { it.startsWith("sendNotification") }
                parameterCount = 1
            }.hook {
                intercept()
            }
        }
    }
}