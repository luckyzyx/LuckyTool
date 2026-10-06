package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AppSplashScreen : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("disable_splash_screen", false)

        //Source StartingSurfaceController
        "com.android.server.wm.StartingSurfaceController".toClass().resolve().apply {
            firstMethod {
                name = "showStartingWindow"
                parameterCount = 5
            }.hook {
                if (isEnable) intercept()
            }
        }
    }
}