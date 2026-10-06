package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAccessDeviceLogDialog : YukiBaseHooker() {

    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_access_device_log_dialog", false)

        //Source LogcatManagerService
        "com.android.server.logcat.LogcatManagerService".toClass().resolve().apply {
            firstMethod { name = "processNewLogAccessRequest" }.hook {
                before {
                    if (!isEnable) return@before
                    val client = firstArg().get() ?: return@before
                    firstMethod { name = "onAccessApprovedForClient" }.of(instance).invoke(client)
                    result = null
                }
            }
        }
    }
}