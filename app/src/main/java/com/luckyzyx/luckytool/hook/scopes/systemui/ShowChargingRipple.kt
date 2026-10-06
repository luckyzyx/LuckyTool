package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ShowChargingRipple : YukiBaseHooker() {
    override fun onHook() {
        //Source WiredChargingRippleController -> flag_charging_ripple
        VariousClass(
            "com.android.systemui.statusbar.charging.WiredChargingRippleController", //C13
            "com.android.systemui.charging.WiredChargingRippleController" //C14
        ).toClass().resolve().apply {
            firstConstructor().hook {
                after {
                    firstField { name = "rippleEnabled" }.of(instance).set(true)
                }
            }

        }
        if (SDK >= A14) return
        //Sourcee FeatureFlags -> flag_charging_ripple
        "com.android.systemui.statusbar.FeatureFlags".toClass().resolve().apply {
            firstMethod { name = "isChargingRippleEnabled" }.hook {
                intercept(true)
            }
        }
    }
}