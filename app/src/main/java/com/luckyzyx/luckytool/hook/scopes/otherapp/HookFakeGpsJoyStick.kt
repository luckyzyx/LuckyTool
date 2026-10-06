package com.luckyzyx.luckytool.hook.scopes.otherapp

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookFakeGpsJoyStick : YukiBaseHooker() {
    override fun onHook() {
        if (!preferences(ModulePrefs).getBoolean("gps_joystick_unlock_pro", false)) return

        //Source
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            dexKitBridge.findClass {
                matcher {
                    addMethod { paramCount(0); returnType(classOf<Int>()) }
                    usingStrings("AD_FREE", "PRO")
                }
            }.apply {
                checkDataList("Clazz")

                findMethod {
                    matcher {
                        paramCount(0)
                        returnType(classOf<Int>())
                        usingStrings("AD_FREE", "PRO")
                        usingNumbers(3600000, 25200000)
                    }
                }.apply {
                    checkDataList("Method Pro")

                    single().className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            emptyParameters()
                            returnType = Int::class
                        }.hook {
                            intercept(3)
                        }
                    }
                }
            }
        }
    }
}