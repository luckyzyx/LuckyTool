package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationCleanupButton : YukiBaseHooker() {
    override fun onHook() {
        //Source ClearAllController
        VariousClass(
            "com.oplusos.systemui.notification.ClearAllController", //C12 C13
            "com.oplus.systemui.statusbar.notification.ClearAllController", //C14 C15
            "com.oplus.systemui.notification.clearall.ClearAllController" //C15.0.1
        ).toClass().resolve().apply {
            firstMethod { name = "setVisible";parameterCount = 3 }.hook {
                before {
                    arg(1).set(false)
                    lastArg().set(false)
                }
            }
        }
    }
}