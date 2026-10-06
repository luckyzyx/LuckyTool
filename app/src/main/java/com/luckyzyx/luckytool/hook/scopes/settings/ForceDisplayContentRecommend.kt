package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayContentRecommend : YukiBaseHooker() {
    override fun onHook() {
        //Source RecommendController
        VariousClass(
            "com.oplus.settings.feature.othersettings.controller.RecommendController", //C13 C14
            "com.oplus.settings.feature.spfunction.RecommendController" //C14.1
        ).toClass().resolve().apply {
            firstMethod { name = "getAvailabilityStatus" }.hook {
                intercept(0)
            }
        }
    }
}