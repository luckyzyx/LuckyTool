package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAccessibilityDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source SpecialPermRiskConfirmHelper
        "com.oplus.settings.applications.specialaccess.SpecialPermRiskConfirmHelper".toClass()
            .resolve().apply {
                firstMethod {
                    name = "show"
                    returnType = Boolean::class
                }.intercept {
                    val runnale = lastArg() as? Runnable
                    runnale?.run()
                    true
                }
            }
    }
}