package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookPowerManager : YukiBaseHooker() {
    override fun onHook() {
        val removeThermal =
            preferences(ModulePrefs).getBoolean("disable_temperature_control_listener", false)
        if (!removeThermal) return

        //Source PowerManager
        "android.os.PowerManager".toClass().resolve().apply {
            firstMethod {
                name = "addThermalStatusListener"
                parameterCount = 1
            }.hook {
                after {
                    val listener = firstArg().get() ?: return@after
                    firstMethod {
                        name = "removeThermalStatusListener"
                        parameterCount = 1
                    }.of(instance).invoke(listener)
                }
            }
            firstMethod { name = "getCurrentThermalStatus" }.hook {
                intercept(0)
            }
        }
    }
}