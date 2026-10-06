package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableLauncherIndicatorEntry : YukiBaseHooker() {
    override fun onHook() {
        //Source IndicatorEntry Companion
        "com.android.launcher3.search.IndicatorEntry\$Companion".toClass().resolve().apply {
            firstMethod { name = "isSupportIndicatorEntryMenu" }.hook {
                intercept(true)
            }
        }
    }
}