package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookAlertSlider : YukiBaseHooker() {
    override fun onHook() {
        //Source AlertSliderAudioPolicy
        "com.android.server.audio.AlertSliderAudioPolicy".toClass().resolve().apply {
            firstMethod { name = "setUp" }.hook {
                before {
                    YLog.debug("${method.name} is call")
                    result = null
                }
            }
            firstMethod { name = "setMiddle" }.hook {
                before {
                    YLog.debug("${method.name} is call")
                    result = null
                }
            }
            firstMethod { name = "setDown" }.hook {
                before {
                    YLog.debug("${method.name} is call")
                    result = null
                }
            }
        }
    }
}