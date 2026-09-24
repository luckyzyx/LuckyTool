package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLowBatteryDialogWarning : Hooker {
    override fun onHook() {
        //Source OplusPowerNotificationWarnings
        VariousClass(
            "com.oplusos.systemui.notification.power.OplusPowerNotificationWarnings", //C13
            "com.oplus.systemui.statusbar.notification.power.OplusPowerNotificationWarnings" //C14
        ).toClass().resolve().optional(true).apply {
            firstMethodOrNull { name = "createSavePowerDialog" }?.hook {
                intercept()
            }
            firstMethodOrNull { name = "createSuperSavePowerDialog" }?.hook {
                intercept()
            }
            firstMethodOrNull { name = "showLowBatteryWarning" }?.hook {
                replaceToFalse()
            }
        }
    }
}