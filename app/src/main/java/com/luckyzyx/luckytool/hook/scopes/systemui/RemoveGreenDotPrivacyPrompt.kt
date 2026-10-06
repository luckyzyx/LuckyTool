package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveGreenDotPrivacyPrompt : YukiBaseHooker() {
    override fun onHook() {
        //Source ViewState
        VariousClass(
            "com.oplusos.systemui.statusbar.events.ViewState", //C13
            "com.oplus.systemui.privacy.ViewState" //C14 C15
        ).toClass().resolve().apply {
            firstMethod { name = "shouldShowDot" }.hook {
                intercept(false)
            }
        }

        //Source ViewState
        "com.android.systemui.statusbar.events.ViewState".toClass().resolve().apply {
            firstMethod { name = "shouldShowDot" }.hook {
                intercept(false)
            }
        }
    }
}