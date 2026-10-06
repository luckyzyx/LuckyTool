package com.luckyzyx.luckytool.hook.scopes.battery

import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookBatteryNotify(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Channel high_performance_channel_id 5
        val highPerformance =
            preferences(ModulePrefs).getBoolean("remove_high_performance_mode_notifications", false)
        //Channel PowerConsumptionOptimizationChannel / PowerConsumptionOptimizationChannelLow 17
        //power_consumption_optimization_title
        val highBatteryConsumption =
            preferences(ModulePrefs).getBoolean("remove_app_high_battery_consumption_warning", false)
        //Channel smart_charge_channel_id 20
//        val smartRapidCharge = preferences(ModulePrefs).getBoolean("remove_smart_rapid_charging_notification", false)

        //Source NotifyUtil
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<Context>())
                    addForType(classOf<Handler>())
                    addForType(classOf<NotificationManager>())
                }
                addMethod {
                    paramTypes(classOf<String>(), classOf<Boolean>())
                    returnType(Void.TYPE)
                }
                usingStrings("NotifyUtil")
            }
        }.apply {
            checkDataList("HookBatteryNotify NotifyUtil")

            if (highPerformance) {
                findMethod {
                    matcher {
                        paramCount(0)
                        returnType(Void.TYPE)
                        usingStrings("high_performance_channel_id", "ACTION_HIGH_PERFORMANCE")
                        usingNumbers(5)
                    }
                }.apply {
                    checkDataList("HookBatteryNotify HighPerformance")
                    single().className.toClass().resolve().apply {
                        firstMethod { name = single().methodName;emptyParameters() }.hook {
                            intercept()
                        }
                    }
                }
            }

            if (highBatteryConsumption) single().name.toClass().resolve().apply {
                method {
                    parameters(String::class, Boolean::class)
                    returnType = Void.TYPE
                }.hookAll {
                    intercept()
                }
            }
        }
    }
}