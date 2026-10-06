package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemovePowerMenuSOSButton : YukiBaseHooker() {
    override fun onHook() {
        //Source GlobalActionsUtils
        VariousClass(
            "com.oplusos.systemui.controls.GlobalActionsUtils", //C13
            "com.oplusos.systemui.common.util.GlobalActionsUtils" //C14 C15
        ).toClass().resolve().apply {
            firstMethodOrNull { name = "isShowSosButton" }?.hook {
                intercept(false)
            }
        }

        //Source OplusShutdownView
        VariousClass(
            "com.oplusos.systemui.controls.OplusShutdownView", //C13
            "com.oplus.systemui.shutdown.OplusShutdownView"
        ).toClass().resolve().apply {
            firstMethod { name = "isShowEmergency" }.hook {
                intercept(false)
            }
        }
    }
}