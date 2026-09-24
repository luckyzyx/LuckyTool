package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveFlashlightOpenNotification : Hooker {
    override fun onHook() {
        //Source FlashlightNotification
        VariousClass(
            "com.oplusos.systemui.flashlight.FlashlightNotification", //C13
            "com.oplus.systemui.statusbar.notification.flashlight.FlashlightNotification", //C14
            "com.oplus.systemui.notification.flashlight.FlashlightNotification" //C15.0.1
        ).toClass().resolve().apply {
            firstMethod {
                // ColorOS 17 的 R8 将 boolean 入口重命名，保留旧系统入口。
                name { it == "sendNotification" || it == "sendNotification\$1" }
                parameters(Boolean::class)
            }.hook {
                intercept()
            }
        }
    }
}