package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnable32BitSupport : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("force_enable_32_bit_support", false)
        if (!isEnable) return

        //Source OplusPackageManagerHelper
        "com.android.server.pm.OplusPackageManagerHelper".toClass().resolve().apply {
            firstMethod { name = "allowInstall32BitApp" }.hook {
                intercept(true)
            }
        }
    }
}