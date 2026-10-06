package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemovePasswordTimeoutVerification : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_72hour_password_verification", false)

        //Source LockSettingsStrongAuth -> StrongAuthTimeoutAlarmListener
        "com.android.server.locksettings.LockSettingsStrongAuth".toClass().resolve().apply {
            firstMethod {
                name = "rescheduleStrongAuthTimeoutAlarm"
                parameterCount = 2
            }.hook {
                if (isEnable) intercept()
            }
        }
    }
}