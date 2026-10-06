package com.luckyzyx.luckytool.hook.scopes.battery

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveBatteryTemperatureControl(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source ThermalControlHandler / ThermalControllHandler
        dexKitBridge.findClass {
            matcher {
            addFieldForType(classOf<Context>())
//                addFieldForType(Looper::class.java)
                addMethod { name("handleMessage") }
//                usingStrings("ThermalControllHandler")
                usingStrings("ThermalControl", "Handler")
            }
        }.apply {
            checkDataList("RemoveBatteryTemperatureControl find ThermalControlHandler")
            single().name.toClass().resolve().apply {
                firstMethod { name = "handleMessage" }.hook {
                    intercept()
                }
            }
        }

        //Source ThermalControllerCenter
        dexKitBridge.findClass {
            matcher {
                usingStrings("ThermalControllerCenter")
            }
        }.apply {
            checkDataList("RemoveBatteryTemperatureControl find ThermalControllerCenter")

            single().name.toClass().resolve().apply {
                method { parameters(Looper::class) }.hookAll {
                    intercept()
                }
            }
        }

        //Source ThermalControlMonitor
        dexKitBridge.findClass {
            matcher {
                usingStrings("ThermalControlMonitor")
            }
        }.apply {
            checkDataList(
                "RemoveBatteryTemperatureControl find ThermalControlMonitor", onlyOne = false
            )

            findMethod {
                matcher {
                    paramCount(0)
                    returnType(Void.TYPE)
                    usingFields {
                    add { type(classOf<Boolean>()) }
                        add { type(classOf<Handler>()) }
                        add { type(classOf<ContentResolver>()) }
                        add { type(classOf<ContentObserver>()) }
                    }
                    addInvoke {
                        paramCount(0)
                        returnType(Void.TYPE)
                    }
                }
            }.apply {
                checkDataList("RemoveBatteryTemperatureControl find startMonitor")

                single().className.toClass().resolve().apply {
                    firstMethod { name = single().name; emptyParameters() }.hook {
                        intercept()
                    }
                }
            }
        }

        //Source ThermalControlUtils
        "com.oplus.thermalcontrol.ThermalControlUtils".toClass().resolve().apply {
            firstMethod { parameters(Looper::class) }.hook {
                intercept()
            }
        }
    }
}