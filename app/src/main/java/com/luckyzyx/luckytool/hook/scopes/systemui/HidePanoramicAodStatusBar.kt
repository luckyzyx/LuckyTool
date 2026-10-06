package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HidePanoramicAodStatusBar : YukiBaseHooker() {
    override fun onHook() {
        //Source AodData
        val aodData = "com.oplus.systemui.aod.aodclock.constant.AodData".toClass()

        //Source KeyguardStatusBarViewExImpl
        "com.oplus.systemui.statusbar.phone.KeyguardStatusBarViewExImpl".toClass().resolve().apply {
            firstMethod {
                name = "hookDozingState"
                parameters(Boolean::class)
                returnType = Boolean::class
            }.hook {
                before {
                    val context = firstField { type = Context::class }.of(instance).get<Context>()
                        ?: return@before
                    val aodDataInstance = aodData.asResolver().firstMethod {
                        name = "getInstance"
                        parameters(Context::class)
                    }.invoke(context) ?: return@before
                    val isPanoramicAod = aodDataInstance.asResolver().firstMethod {
                        name = "isPanoramicAod"
                    }.invoke<Boolean>() ?: return@before
                    if (arg(0).get<Boolean>() ?: false && isPanoramicAod) result = false
                }
            }
        }
    }
}
