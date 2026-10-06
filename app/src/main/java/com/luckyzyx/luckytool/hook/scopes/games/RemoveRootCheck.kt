package com.luckyzyx.luckytool.hook.scopes.games

import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveRootCheck(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source COSASDKManager
        //Search getSupportCoolEx new Bundle -> Class
        //Search getFeature -> dynamic_feature_cool_ex
        //isSafe:null; -> isSafe:0
        dexKitBridge.findClass {
            matcher {
                fields {
//                    addForType(String::class.java)
                    addForType(classOf<Boolean>())
                    addForType(classOf<Int>())
                }
                methods {
//                    add { name = "clear"; paramCount(0) }
                    add { paramCount(0); returnType(classOf<Bundle>()) }
                }
                usingStrings("COSASDKManager")
            }
        }.apply {
            checkDataList("RemoveRootCheck")
            single().name.toClass().resolve().apply {
                firstMethod {
                    emptyParameters()
                    returnType = Bundle::class
                }.hook {
                    after {
                        result<Bundle>()?.putInt("isSafe", 0)
                    }
                }
            }
        }
    }
}
