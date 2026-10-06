package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AllowLongPressNotificationModifiable : YukiBaseHooker() {
    override fun onHook() {
        //Source NotificationSettingsModel
        VariousClass(
            "com.oplusos.systemui.notification.settingspanel.NotificationSettingsModel", //C13
            "com.oplusos.systemui.notification.settingspanel.controller.NotificationController", //C13.1
            "com.oplus.systemui.statusbar.notification.settingspanel.controller.NotificationController" //C14
        ).toClassOrNull()?.resolve()?.apply {
            firstMethod {
                name { it.startsWith("resolve") && it.contains("Mode") }
                parameterCount = 1
            }.hook {
                before {
                    firstFieldOrNull { name = "isAppModifiable" }?.of(instance)?.set(true) ?: run {
                        firstArg().get()?.asResolver()?.firstField { name = "isAppModifiable" }
                            ?.set(true)
                    }
                }
            }
        }
    }
}