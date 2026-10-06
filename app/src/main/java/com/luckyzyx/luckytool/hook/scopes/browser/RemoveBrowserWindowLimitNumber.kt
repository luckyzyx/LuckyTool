package com.luckyzyx.luckytool.hook.scopes.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveBrowserWindowLimitNumber(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source TabManager
        dexKitBridge.findClass {
            matcher {
                className("com.android.browser.TabManager")
            }
        }.findMethod {
            matcher {
                paramCount(0)
                returnType(classOf<Int>())
                usingStrings("TabManager", "multiWindowPerf")
            }
        }.apply {
            checkDataList("RemoveBrowserWindowLimitNumber")
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