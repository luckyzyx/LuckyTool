package com.luckyzyx.luckytool.hook.scopes.engineermode

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object UnlockSomeHiddenOptions : YukiBaseHooker() {
    override fun onHook() {
        //Source SecrecyServiceHelper
        "com.oplus.engineermode.impl.SecrecyServiceHelper".toClass().resolve().apply {
            firstMethod { name = "isSecrecySupported" }.hook {
                intercept(true)
            }
            firstMethod { name = "getSecrecyState" }.hook {
                intercept(false)
            }
        }
    }
}