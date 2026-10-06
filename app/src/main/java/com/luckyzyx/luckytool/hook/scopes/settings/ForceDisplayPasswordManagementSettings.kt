package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayPasswordManagementSettings : YukiBaseHooker() {

    override fun onHook() {
        if (getOSVersionCode >= 30) loadHooker(PasswordManagementSettings)
        else loadHooker(PasswordManagementSettingsV13)
    }

    @Obfuscate
    object PasswordManagementSettings : YukiBaseHooker() {
        override fun onHook() {
            //Source PasswordManagerPreferenceController
            "com.oplus.settings.feature.password.controller.PasswordManagerPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "isPreferenceNotAvailable" }.hook {
                        intercept(false)
                    }
                }
        }
    }

    @Obfuscate
    object PasswordManagementSettingsV13 : YukiBaseHooker() {
        override fun onHook() {
            //Source PasswordManagerPreferenceController
            "com.oplus.settings.feature.password.controller.PasswordManagerPreferenceController".toClass()
                .resolve().apply {
                    firstMethod { name = "displayPreference" }.hook {
                        after {
                            val preferenceScreen = firstArg().get() ?: return@after
                            val preference = preferenceScreen.asResolver().firstMethod {
                                name = "findPreference"
                                parameters(CharSequence::class)
                                superclass()
                            }.invoke("key_password_manager") ?: return@after
                            preference.asResolver().firstMethod {
                                name = "setVisible"
                                parameters(Boolean::class)
                                superclass()
                            }.invoke(true)
                        }
                    }
                    firstMethod { name = "updateState" }.hook {
                        after {
                            val preference = firstArg().get() ?: return@after
                            preference.asResolver().firstMethod {
                                name = "setVisible"
                                parameters(Boolean::class)
                                superclass()
                            }.invoke(true)
                        }
                    }
                }
        }
    }
}