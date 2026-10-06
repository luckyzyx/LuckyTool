package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.FingerPrintIconAnim
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSystemUIFingerPrint : YukiBaseHooker() {
    override fun onHook() {
        //指纹图标
        loadHooker(FingerPrintIconAnim)
    }
}