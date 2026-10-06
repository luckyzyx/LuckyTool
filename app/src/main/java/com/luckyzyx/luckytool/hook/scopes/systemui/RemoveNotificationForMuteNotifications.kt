package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationForMuteNotifications : YukiBaseHooker() {
    override fun onHook() {
        //Source NoDisturbController
        VariousClass(
            "com.oplusos.systemui.statusbar.controller.NoDisturbController",
            "com.oplus.systemui.statusbar.controller.NoDisturbController" //C14
        ).toClass().resolve().apply {
            firstMethod { name = "checkBlockBannerStatus" }.hook {
                intercept(false)
            }
        }
    }
}