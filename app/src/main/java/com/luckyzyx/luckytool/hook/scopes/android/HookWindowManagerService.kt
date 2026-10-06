package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Context
import android.provider.Settings
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookWindowManagerService : YukiBaseHooker() {
    override fun onHook() {
        //移除DPI重启恢复
        var isDpi = preferences(ModulePrefs).getBoolean("remove_dpi_restart_recovery", false)
        dataChannel.wait<Boolean>("remove_dpi_restart_recovery") { isDpi = it }

        val windowManagerService = "com.android.server.wm.WindowManagerService"

        //Source DisplayWindowSettings
        //分辨率切换时 density==初始密度会传 0 清空持久化值，替换为用户强制密度
        if (SDK >= A14) {
            "com.android.server.wm.DisplayWindowSettings".toClass().resolve().apply {
                method {
                    name = "setForcedDensity"
                    parameterCount { it in 2..3 }
                }.hookAll {
                    before {
                        if (!isDpi) return@before
                        val density = arg(1).get<Int>() ?: 0
//                    val userId = if (method.parameterCount == 3) lastArg().get<Int>() ?: 0 else null
//                    YLog.debug("${method.name} is call -> $density | $userId")

                        val service = firstField { type = windowManagerService }.of(instance).get()
                            ?: return@before
                        val context = service.asResolver().firstField { type = Context::class }
                            .get<Context>() ?: return@before
                        val resolver = context.contentResolver
                        val forcedDensity = Settings.Secure.getString(
                            resolver, "display_density_forced"
                        )?.toIntOrNull() ?: return@before
                        if (density == 0) arg(1).set(forcedDensity)
                    }
                }
            }
        }

        //Source DisplayContentExtImpl
        if (SDK >= A14) {
            "com.android.server.wm.DisplayContentExtImpl".toClass().resolve().apply {
                firstMethod {
                    name = "setForcedDisplayInfoForWmSize"
                    parameterCount = 5
                }.hook {
                    before {
                        if (!isDpi) return@before
//                    val width = firstArg().get<Int>() ?: 0
//                    val height = arg(1).get<Int>() ?: 0
//                    val density = arg(2).get<Int>() ?: 0
//                    val userId = arg(3).get<Int>() ?: 0
                        val service = lastArg().get() ?: return@before
//                    YLog.debug("${method.name} is call -> $width | $height | $density | $userId")

                        val context = service.asResolver().firstField { type = Context::class }
                            .get<Context>() ?: return@before
                        val resolver = context.contentResolver
                        val forcedDensity = Settings.Secure.getString(
                            resolver, "display_density_forced"
                        )?.toIntOrNull() ?: return@before
                        arg(2).set(forcedDensity)
                    }
                }
            }
        }

        //Source OplusResolutionSwitchImpl
        "com.android.server.wm.OplusResolutionSwitchImpl".toClass().resolve().apply {
            firstMethodOrNull { name = "resetDensityIfNeed" }?.hook {
                before {
                    if (isDpi) result = null
                }
            } ?: run {
                firstMethod { name = "onResolutionSettingsChange"; parameterCount = 1 }.hook {
                    before {
                        if (isDpi) firstArg().set(false)
                    }
                }
                firstMethodOrNull {
                    name = "onFakeResolutionSettingsChange"
                    parameterCount = 1
                }?.hook {
                    before {
                        if (isDpi) firstArg().set(false)
                    }
                }
            }
        }
    }
}