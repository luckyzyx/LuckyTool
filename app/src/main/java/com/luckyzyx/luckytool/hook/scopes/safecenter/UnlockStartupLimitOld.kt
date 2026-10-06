package com.luckyzyx.luckytool.hook.scopes.safecenter

import android.content.Context
import android.content.pm.ApplicationInfo
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class UnlockStartupLimitOld(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        //Source StartupManager.java
        //Search -> auto_start_max_allow_count -> update max allow count
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<Int>())
                    addForType(classOf<Any>())
                    addForType(classOf<Map<*,*>>())
                    addForType(classOf<Boolean>())
                    addForType(classOf<Context>())
                }
                methods {
                add { paramTypes(classOf<List<*>>()) }
                    add { paramTypes(classOf<String>()) }
                    add { returnType(Void.TYPE) }
                    add { returnType(classOf<List<*>>()) }
                    add { returnType(classOf<Boolean>()) }
                    add { returnType(classOf<ApplicationInfo>()) }
                }
                usingStrings("StartupManager")
            }
        }.apply {
            checkDataList("UnlockStartupLimitOld")
            single().name.toClass().resolve().apply {
                method {
                    parameters(Context::class)
                    returnType = Void.TYPE
                }.hookAll {
                    after {
                        firstField { type = Int::class }.set(999)
                    }
                }
            }
        }
    }
}