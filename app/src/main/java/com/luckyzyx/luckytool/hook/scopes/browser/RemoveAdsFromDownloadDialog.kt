package com.luckyzyx.luckytool.hook.scopes.browser

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveAdsFromDownloadDialog(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source DownloadCardAdProvider
        dexKitBridge.findMethod {
            matcher {
                declaredClass {
                addFieldForType(classOf<Context>())
                    addFieldForType(classOf<String>())
                    addMethod {
                    paramTypes(classOf<Context>(), classOf<Int>())
                        returnType(Void.TYPE)
                    }
                    usingStrings("DownloadCardAdProvider")
                }
                usingStrings("DownloadCardAdProvider", "createAdRequest", "appName", "posIds")
            }
        }.apply {
            checkDataList("RemoveAdsFromDownloadDialog")
            single().className.toClass().resolve().apply {
                firstMethod {
                    name = single().methodName
                }.hook {
                    intercept()
                }
            }
        }
    }
}