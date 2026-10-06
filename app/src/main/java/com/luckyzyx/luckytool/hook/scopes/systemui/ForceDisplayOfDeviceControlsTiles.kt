package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayOfDeviceControlsTiles : YukiBaseHooker() {
    override fun onHook() {
        //Source OplusDeviceControlsTile
        "com.oplus.systemui.qs.tiles.OplusDeviceControlsTile".toClass().resolve().apply {
            firstMethod { name = "isAvailable" }.hook {
                intercept(true)
            }
        }
    }
}