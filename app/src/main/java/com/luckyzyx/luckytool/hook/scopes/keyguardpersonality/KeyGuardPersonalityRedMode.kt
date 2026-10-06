package com.luckyzyx.luckytool.hook.scopes.keyguardpersonality

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object KeyGuardPersonalityRedMode : YukiBaseHooker() {
    override fun onHook() {
        var redMode = preferences(ModulePrefs).getString("lock_screen_clock_redone_mode", "0")
        dataChannel.wait<String>("lock_screen_clock_redone_mode") { redMode = it }

        // ColorOS 17 将经典/数字时钟移入 personality APK，并提供独立红色 1 开关。
        //Source ClockViewRootModel
        listOf(
            "com.oplus.keyguard.clock.base.domain.model.ClockViewRootModel",
            "com.oplus.keyguard.clock.digital.domain.model.ClockViewRootModel"
        ).forEach { className ->
            className.toClass().resolve().firstMethod {
                name = "setOnePlusRedOneSwitch"
                parameters(Boolean::class)
            }.hook {
                before {
                    when (redMode) {
                        "1" -> arg(0).set(true)
                        "2" -> arg(0).set(false)
                    }
                }
            }
        }
    }
}
