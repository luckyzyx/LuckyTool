package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookMediaProjectionManager : YukiBaseHooker() {
    override fun onHook() {
        val isEnable =
            preferences(ModulePrefs).getBoolean("enable_record_calls_on_third_party_apps", false)

        //Source MediaProjectionManagerServiceExtImpl
        "android.media.projection.MediaProjectionManagerServiceExtImpl".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "isOplusApp";parameterCount = 1 }.hook {
                after {
                    if (!isEnable) return@after
                    val packageName = firstArg().get<String>() ?: ""
                    if (packageName == "com.oplus.audiomonitor") result = true
                }
            }
        }
    }
}