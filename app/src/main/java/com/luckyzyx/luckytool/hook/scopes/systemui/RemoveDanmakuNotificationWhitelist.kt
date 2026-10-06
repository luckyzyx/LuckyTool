package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveDanmakuNotificationWhitelist : YukiBaseHooker() {
    override fun onHook() {
        //Source DanmakuHelper
        VariousClass(
            "com.oplusos.systemui.notification.helper.DanmakuHelper", //C13
//            "com.oplus.systemui.statusbar.notification.helper.HeadsUpHelper" //C14
        ).toClass().resolve().apply {
            firstMethod {
                name = "isSupportDanmaku"
            }.hook {
                intercept(true)
            }
        }
    }
}