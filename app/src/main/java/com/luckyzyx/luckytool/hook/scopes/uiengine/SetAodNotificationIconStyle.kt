package com.luckyzyx.luckytool.hook.scopes.uiengine

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object SetAodNotificationIconStyle : YukiBaseHooker() {

    override fun onHook() {
        val mode = preferences(ModulePrefs).getString("set_aod_notification_icon_style", "0")
        if (mode == "0") return

        //Source ProductFlavorOption
        "com.oplus.egview.util.ProductFlavorOption".toClass().resolve().apply {
            firstMethod {
                name = if (SDK >= A14) "isFlavorTwoDeviceExp" else "isFlavorTwoDevice"
            }.hook {
                when (mode) {
                    "1" -> {
                        intercept(true)
                    }
                    "2" -> {
                        intercept(false)
                    }
                }
            }
        }
    }
}