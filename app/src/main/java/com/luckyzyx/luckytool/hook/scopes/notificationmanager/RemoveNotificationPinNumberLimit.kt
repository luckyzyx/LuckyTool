package com.luckyzyx.luckytool.hook.scopes.notificationmanager

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveNotificationPinNumberLimit : YukiBaseHooker() {
    override fun onHook() {
        //Source AppNotificationTopController
        "com.oplus.notificationmanager.property.uicontroller.AppNotificationTopController".toClass()
            .let {
                it.resolve().apply {
                    method {
                        parameters(it, "androidx.preference.Preference", Any::class)
                        returnType = Boolean::class
                    }.hookAll {
                        before {
                            val controller = firstArg().get() ?: return@before
                            val bool = lastArg().get<Boolean>() ?: false
                            controller.asResolver().firstMethod { name = "onChange";superclass() }
                                .invoke(bool)
                            result = true
                        }
                    }
                }
            }
    }
}