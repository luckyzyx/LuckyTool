package com.luckyzyx.luckytool.hook.scopes.android

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAppUninstallButtonBlackList : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("remove_app_uninstall_button_blacklist", false)

        //Source OplusUninstallableConfigManager
        "com.android.server.pm.OplusUninstallableConfigManager".toClass().resolve().apply {
            firstMethod { name = "loadUninstallableConfig" }.hook {
                after {
                    if (!isEnable) return@after
                    val icon = firstField { name = "mHideUninstallIcon" }.of(instance).get()
                    icon?.asResolver()?.firstField { name = "mList" }?.get<ArraySet<String>>()?.clear()
                    val iconSoft = firstField { name = "mHideUninstallIconSoft" }.of(instance).get()
                    iconSoft?.asResolver()?.firstField { name = "mList" }?.get<ArraySet<String>>()
                        ?.clear()
                }
            }
        }
    }
}