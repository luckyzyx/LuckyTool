package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSystemScreenshotDelay : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_system_screenshot_delay", false)

        //Source PhoneWindowManager
        "com.android.server.policy.PhoneWindowManager".toClass().resolve().apply {
            firstMethod {
                name = "getScreenshotChordLongPressDelay"
                returnType = Long::class
            }.hook {
                if (isEnable) {
                    intercept(0L)
                }
            }
        }
    }
}