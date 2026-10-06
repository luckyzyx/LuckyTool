package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveLockScreenBottomSOSButton : YukiBaseHooker() {
    override fun onHook() {
        //Source OplusEmergencyButtonControllExImpl
        VariousClass(
            "com.oplus.systemui.keyguard.OplusEmergencyButtonControllExImpl", //C13
            "com.oplus.keyguard.OplusEmergencyButtonExImpl" //C14 C15
        ).toClass().resolve().apply {
            firstMethodOrNull { name = "disableShowEmergencyButton" }?.hook {
                intercept(true)
            } ?: firstMethodOrNull { name = "shouldUpdateEmergencyCallButton" }?.hook {
                before {
                    firstField { name = "mEmergencyButton" }.of(instance).get<View>()
                        ?.isVisible = false
                    result = true
                }
            } ?: firstMethod { name = "updateEmergencyCallButton" }.hook {
                before {
                    lastArg().set(false)
                }
            }
        }
    }
}