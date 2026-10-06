package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AllowDisablingSystemApps : YukiBaseHooker() {
    override fun onHook() {
        //Source AppButtonsPreferenceControllerAdaptor
        "com.oplus.settings.adaptor.AppButtonsPreferenceControllerAdaptor".toClass().resolve().apply {
            firstMethod { name = "setUninstallButtonEnabled" }.hook {
                before {
                    firstArg().set(true)
                }
            }
        }
    }
}