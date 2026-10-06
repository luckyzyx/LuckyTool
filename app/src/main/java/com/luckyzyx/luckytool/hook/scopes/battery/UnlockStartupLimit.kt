package com.luckyzyx.luckytool.hook.scopes.battery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class UnlockStartupLimit(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        val recordDatabase = "com.oplus.startupapp.data.database.RecordDatabase"

        //Source StartupManager.java
        //Search -> ? 5 : 20; -> Method
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                addFieldForType(classOf<Context>())
                    addFieldForType(recordDatabase)
                    usingStrings("StartupManager")
                    addMethod { paramCount(0);returnType(classOf<Int>()) }
                    addMethod { paramTypes(classOf<Intent>());returnType(Void.TYPE) }
                    addMethod { paramTypes(classOf<Bundle>());returnType(Void.TYPE) }
                }
                paramCount(0)
                returnType(classOf<Int>())
                usingNumbers(5, 20)
            }
        }.apply {
            checkDataList("UnlockStartupLimit")
            single().className.toClass().resolve().apply {
                firstMethod {
                    name = single().methodName
                    emptyParameters()
                    returnType = Int::class
                }.hook {
                    intercept(999)
                }
            }
        }
    }
}