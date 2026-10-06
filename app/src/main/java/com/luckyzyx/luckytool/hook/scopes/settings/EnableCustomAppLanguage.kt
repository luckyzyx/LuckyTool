package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableCustomAppLanguage : YukiBaseHooker() {
    override fun onHook() {
        //Source AppLocaleUtil
        "com.android.settings.applications.AppLocaleUtil".toClass().resolve().apply {
            firstMethod { name = "canDisplayLocaleUi" }.hook {
                intercept(true)
            }
        }
        //Source AppLocalePreferenceController
        "com.android.settings.applications.appinfo.AppLocalePreferenceController".toClass()
            .resolve().apply {
            firstMethod { name = "getAvailabilityStatus" }.hook {
                intercept(0)
            }
        }
    }
}