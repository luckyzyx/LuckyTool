package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableStatusBarClockFormat : YukiBaseHooker() {
    override fun onHook() {
        //Source RmStatusbarClockPreferenceController
        "com.oplus.settings.feature.notification.controller.RmStatusbarClockPreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    intercept(0)
                }
            }
    }
}