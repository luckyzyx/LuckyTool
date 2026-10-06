package com.luckyzyx.luckytool.hook.scopes.otherapp

import android.app.Activity
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookAlphaBackupPro : YukiBaseHooker() {
    override fun onHook() {
        val isPro = preferences(ModulePrefs).getBoolean("remove_check_license", false)
        if (!isPro) return
        //Source HomeActivity
        "com.ruet_cse_1503050.ragib.appbackup.pro.activities.HomeActivity".toClass().resolve().apply {
            firstMethod { name = "onCreate" }.hook {
                before {
                    instance<Activity>().intent.putExtra("licenseState", "valid_licence")
                }
            }
        }
    }
}