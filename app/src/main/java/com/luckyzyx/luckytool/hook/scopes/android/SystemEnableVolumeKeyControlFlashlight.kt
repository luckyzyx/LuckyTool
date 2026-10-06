package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object SystemEnableVolumeKeyControlFlashlight : YukiBaseHooker() {
    override fun onHook() {
        if (getOSVersionCode < 27) return
        val isEnable = preferences(ModulePrefs).getBoolean("enable_volume_key_control_flashlight", false)

        //Source OplusScreenOffTorchHelper
        "com.android.server.power.OplusScreenOffTorchHelper".toClassOrNull()?.resolve()?.apply {
            firstMethod {
                name = "getInstance"
                parameters(Context::class)
            }.hook {
                after {
                    if (!isEnable) return@after
                    val context = firstArg().get<Context>() ?: return@after

                    if (result == null) result = firstConstructor {
                        parameters(Context::class)
                    }.create(context)
                }
            }
        }
    }
}