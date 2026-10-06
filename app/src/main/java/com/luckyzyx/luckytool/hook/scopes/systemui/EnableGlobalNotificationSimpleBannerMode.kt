package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableGlobalNotificationSimpleBannerMode : YukiBaseHooker() {
    override fun onHook() {
        //Source FullScreenBannerHelper -> simple_banner_switch_state
        VariousClass(
            "com.oplusos.systemui.notification.helper.FullScreenBannerHelper", //C12.1
            "com.oplus.systemui.statusbar.notification.helper.FullScreenBannerHelper", //C13 C14 C15
            "com.oplus.systemui.notification.interruption.fullscreenbanner.FullScreenBannerHelper" //C15.0.1
        ).toClass().resolve().apply {
            firstMethod {
                name = "isSimpleBannerEnable"
                returnType = Boolean::class
            }.hook {
                intercept(true)
            }
        }
    }
}