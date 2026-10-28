package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveAutoEnterSuperPowerSave(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        var isEnable = preferences(ModulePrefs)
            .getBoolean("remove_auto_enter_super_power_save", false)
        dataChannel.wait<Boolean>("remove_auto_enter_super_power_save") {
            isEnable = it
        }

        //Source OplusSuperSaveControllerImpl
        dexKitBridge.findClass {
            matcher {
                addMethod {
                    paramTypes(
                        classOf<Int>(), classOf<Boolean>(), classOf<Boolean>()
                    )
                    returnType(Void.TYPE)
                    usingStrings("batteryValueMin")
                    addInvoke { name("getSuperSaveState") }
                }
                usingStrings("persist.vendor.battery_value_min", "SuperSaveControllerImpl")
            }
        }.apply {
            checkDataList("RemoveAutoEnterSuperPowerSave")
            findMethod {
                matcher {
                    paramTypes(classOf<Int>(), classOf<Boolean>(), classOf<Boolean>())
                    returnType(Void.TYPE)
                    usingStrings("batteryValueMin")
                    addInvoke { name("getSuperSaveState") }
                }
            }.apply {
                checkDataList("autoEnterOrExitSuperSave")
                singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
                    before {
                        //低电量分支的充电状态用于跳过自动进入倒计时。
                        if (isEnable) arg(2).set(true)
                    }
                }
            }
        }
    }
}
