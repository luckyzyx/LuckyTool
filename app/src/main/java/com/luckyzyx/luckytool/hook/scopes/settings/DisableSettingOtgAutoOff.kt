package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.Context
import android.os.customize.OplusCustomizeRestrictionManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableSettingOtgAutoOff : YukiBaseHooker() {
    override fun onHook() {
        //Source OtgConnectionOpenedPreferenceController
        VariousClass(
            "com.oplus.settings.feature.othersettings.controller.OtgConnectionOpenedPreferenceController",  //C14
            "com.oplus.settings.feature.spfunction.OtgConnectionOpenedPreferenceController"  //C14.1 C15
        ).toClass().resolve().apply {
            firstMethod { name = "isPreferenceSupport" }.hook {
                before {
                    val context = firstArg().get<Context>() ?: return@before
                    val isUSBOtgDisabled =
                        OplusCustomizeRestrictionManager.getInstance(context).isUSBOtgDisabled
                    result = !isUSBOtgDisabled
                }
            }
        }
    }
}