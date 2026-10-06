package com.luckyzyx.luckytool.hook.scopes.mms

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveMmsBottomInputBoxMenu : YukiBaseHooker() {
    override fun onHook() {
        //Source MenuInfoBaseBean
        "com.opos.smart.mms.interfaces.netmsg.menu.MenuInfoBaseBean".toClass().resolve().apply {
            firstMethod {
                name = "getMenus"
                returnType = List::class
            }.hook {
                intercept(ArrayList<Any>())
            }
        }
    }
}