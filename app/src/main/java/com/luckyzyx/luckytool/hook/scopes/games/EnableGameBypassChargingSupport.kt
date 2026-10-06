package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableGameBypassChargingSupport : YukiBaseHooker() {
    override fun onHook() {
        //Source COSAExportedImpl
        "com.oplus.cosa.exported.COSAExportedImpl".toClass().resolve().apply {
            firstMethod { name = "getBypassChargingDeviceSupport" }.hook{
                intercept(2)
            }
        }
    }
}