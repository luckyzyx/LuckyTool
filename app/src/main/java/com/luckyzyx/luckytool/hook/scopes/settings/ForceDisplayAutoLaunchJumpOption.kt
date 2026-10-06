package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayAutoLaunchJumpOption : YukiBaseHooker() {
    override fun onHook() {
        //Source AutoLaunchMgrPreferenceController
        "com.oplus.settings.feature.appmanager.controller.AutoLaunchMgrPreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    intercept(0)
                }
            }
    }
}