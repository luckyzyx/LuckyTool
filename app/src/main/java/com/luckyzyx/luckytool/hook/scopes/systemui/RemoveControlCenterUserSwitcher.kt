package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveControlCenterUserSwitcher : YukiBaseHooker() {
    override fun onHook() {
        //Search Log showUserSwitcher
        "com.oplusos.systemui.qs.OplusQSFooterImpl".toClass().resolve().apply {
            firstMethod {
                name = "showUserSwitcher"
                emptyParameters()
                returnType = Boolean::class
            }.hook {
                intercept(false)
            }
        }
    }
}