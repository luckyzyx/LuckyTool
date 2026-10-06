package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayBottomGoogleSettings : YukiBaseHooker() {
    override fun onHook() {
        //Source GooglePreferenceController
        "com.oplus.settings.feature.homepage.controller.GooglePreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    intercept(0)
                }
            }
    }
}