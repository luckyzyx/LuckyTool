package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSettingsBottomLaboratory : YukiBaseHooker() {
    override fun onHook() {
        //Source TopLevelLaboratoryPreferenceController
        "com.oplus.settings.feature.homepage.TopLevelLaboratoryPreferenceController".toClass()
            .resolve().apply {
                firstMethod { name = "getAvailabilityStatus" }.hook {
                    intercept(3)
                }
            }
    }
}