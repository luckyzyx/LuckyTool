package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAlwaysAllowAppStartList : YukiBaseHooker() {
    override fun onHook() {
        val isEnable = preferences(ModulePrefs).getBoolean("enable_always_allow_app_start_dialog", false)
        if (!isEnable) return

        var controller: Any? = null

        dataChannel.wait<ArrayList<Int>>("remove_always_allow_app_start_list") {
            it.forEachIndexed { _, i ->
                controller?.asResolver()?.firstMethod {
                    name = "onUserRemoved"
                    parameters(Int::class)
                }?.invoke(i)
            }
            YLog.debug("cleaning $it always start app list")
        }

        //Source OplusSecurityPermissionManager
        "com.android.server.am.OplusSecurityPermissionManager".toClass().resolve().apply {
            firstMethod { name = "init" }.hook {
                after {
                    controller =
                        firstField { type = "com.android.server.am.OplusActivityStartController" }
                            .of(instance).get()
                }
            }
        }
    }
}