package com.luckyzyx.luckytool.hook.scopes.uiengine

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAodNotificationWhitelist : YukiBaseHooker() {
    override fun onHook() {
        //Source NotificationView -> BaseView
        "com.oplus.egview.widget.BaseView".toClass().resolve().apply {
            firstMethod { name = "isExpRegion" }.hook {
                intercept(true)
            }
        }
    }
}