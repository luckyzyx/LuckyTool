package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class EnableSupportCompetitionMode(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source CompetitionModeManager
        //Search isSupportCompetitionMode
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<List<*>>())
                }
                methods {
                add { paramCount(0);returnType(classOf<List<*>>()) }
                    add { paramCount(0);returnType(classOf<Boolean>()) }
                    add { paramTypes(classOf<String>(), classOf<ArrayList<*>>()) }
                }
            }
        }.apply {
            checkDataList("EnableSupportCompetitionMode find CompetitionModeManager")

            findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<Boolean>())
                    usingStrings("isSupportCompetitionMode")
                }
            }.apply {
                checkDataList("EnableSupportCompetitionMode find isSupportCompetitionMode")

                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        emptyParameters()
                        returnType = Boolean::class
                    }.hook {
                        intercept(true)
                    }
                }
            }
        }
    }
}