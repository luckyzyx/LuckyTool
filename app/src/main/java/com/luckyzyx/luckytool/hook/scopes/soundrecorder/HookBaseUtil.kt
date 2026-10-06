package com.luckyzyx.luckytool.hook.scopes.soundrecorder

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookBaseUtil : YukiBaseHooker() {
    override fun onHook() {
        //Source BaseUtil
        "com.soundrecorder.base.utils.BaseUtil".toClass().resolve().apply {
            firstMethod { name = "isRealme" }.hook {
                intercept(true)
            }
        }
    }
}