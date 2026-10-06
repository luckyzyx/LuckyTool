package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveChargingCompleted : YukiBaseHooker() {
    override fun onHook() {
        //Source OplusPowerNotificationWarnings
        VariousClass(
            "com.coloros.systemui.notification.power.ColorosPowerNotificationWarnings", //A11
            "com.oplusos.systemui.notification.power.OplusPowerNotificationWarnings",
            "com.oplus.systemui.statusbar.notification.power.OplusPowerNotificationWarnings" //C14
        ).toClass().resolve().apply {
            firstMethod { name = "showChargeErrorDialog";parameterCount = 1 }.hook {
                before {
                    if ((firstArg().get<Int>() ?: 0) == 7) result = null
                }
            }
        }
    }
}