package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AllowUntrustedTouch : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("allow_untrusted_touch", false)

        //Source UntrustedTouchController
        "com.android.server.input.UntrustedTouchController".toClass().resolve().apply {
            firstMethod { name = "isOplusTrustedApp" }.hook {
                if (isEnable) {
                    intercept(true)
                }
            }
            firstMethod { name = "showTipsDialog" }.hook {
                if (isEnable) intercept()
            }
        }
        //Source WindowStateExtImpl
        "com.android.server.wm.WindowStateExtImpl".toClass().resolve().apply {
            firstMethod { name = "isOplusTrustedWindow" }.hook {
                if (isEnable) {
                    intercept(true)
                }
            }
        }
        if (SDK >= A14) return
        //Source InputManager
        "android.hardware.input.InputManager".toClass().resolve().apply {
            firstMethod { name = "getBlockUntrustedTouchesMode" }.hook {
                if (isEnable) {
                    intercept(0)
                }
            }
        }
    }
}