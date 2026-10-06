package com.luckyzyx.luckytool.hook.scopes.gallery

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookFunctionManager(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //姜文电影滤镜
        val jangWen = preferences(ModulePrefs).getBoolean("enable_gallery_jiangwen_filter", false)

        //Source FunctionSwitchManager
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<Map<*,*>>())
                }
                methods {
                    add {
                    paramTypes(classOf<String>())
                        returnType(classOf<Boolean>())
                        usingStrings("FunctionSwitchManager", "getGroupName", "spKey")
                    }
                    add {
                        paramCount(1..5)
                        returnType(Void.TYPE)
                    }
                }
                usingStrings("FunctionSwitchManager")
            }
        }.apply {
            checkDataList("HookFunctionManager")
            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(String::class)
                    returnType(Boolean::class)
                }.hook {
                    after {
                        when (firstArg().get<String>() ?: "") {
                            //姜文电影滤镜
                            "pref_jiangwen_filter_enable" -> if (jangWen) result = true
                        }
                    }
                }
            }
        }
    }
}