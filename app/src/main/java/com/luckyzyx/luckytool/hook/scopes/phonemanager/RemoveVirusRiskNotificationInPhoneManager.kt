package com.luckyzyx.luckytool.hook.scopes.phonemanager

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveVirusRiskNotificationInPhoneManager(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source VirusScanNotifyListener
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<Context>())
                    addForType(classOf<String>())
                }
                methods {
                add { paramTypes(classOf<ArrayList<*>>()) }
                    add { returnType(classOf<Int>()) }
                    add { returnType(classOf<String>()) }
                }
                usingStrings("VirusScanNotifyListener")
            }
        }.apply {
            checkDataList("RemoveVirusRiskNotificationInPhoneManager")
            single().name.toClass().resolve().apply {
                method { parameters(ArrayList::class) }.hookAll {
                    intercept()
                }
            }
        }
    }
}