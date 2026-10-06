package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAodMusicWhitelist : YukiBaseHooker() {
    override fun onHook() {
        //Source AodMediaDataListener
        "com.oplusos.systemui.aod.mediapanel.AodMediaDataListener\$Companion".toClass().resolve().apply {
            firstMethod { name = "isAodMediaSupport" }.hook {
                intercept(true)
            }
            firstMethod { name = "isAodMediaSupportWithoutFeature" }.hook {
                intercept(true)
            }
        }
    }
}