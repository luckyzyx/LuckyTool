package com.luckyzyx.luckytool.hook.scopes.filemanager

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveWordLimitForLabelNameFiles(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source BaseFileNameDialog
        dexKitBridge.findClass {
            matcher {
                methods {
                    add { name("onActivityResume") }
                    add { name("onTextChanged") }
                }
                usingStrings("BaseFileNameDialog")
            }
        }.apply {
            checkDataList("BaseFileNameDialog")

            findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<Int>())
                    usingNumbers(50)
                }
            }.apply {
                checkDataList("MaxCount")

                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        emptyParameters()
                        returnType = Int::class
                    }.hook {
                        intercept(9999)
                    }
                }
            }
        }
    }
}