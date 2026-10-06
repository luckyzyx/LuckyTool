package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ADBInstallConfirm : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_adb_install_confirm", false)

        //Source OplusPackageInstallInterceptManager
        VariousClass(
            "com.android.server.pm.ColorPackageInstallInterceptManager", //A11
            "com.android.server.pm.OplusPackageInstallInterceptManager"
        ).toClass().resolve().apply {
            firstMethod { name = "allowInterceptAdbInstallInInstallStage" }.hook {
                if (isEnable) {
                    intercept(false)
                }
            }
        }
    }
}