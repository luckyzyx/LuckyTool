package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAccessibilityWarningDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source FraudBehaviorDetectManager
        "com.android.server.am.FraudBehaviorDetectManager".toClass().resolve().apply {
            firstMethodOrNull {
                name = "updateGlobalCloseConfigToXmlFile"
                parameters(Boolean::class, Int::class)
            }?.hook {
                after {
                    val mConfig = firstField { name = "mConfig" }.of(instance).get() ?: return@after
                    mConfig.asResolver().firstField { name = "enabled" }.set(false)
                }
            }
            firstMethodOrNull {
                name = "handleChangedApps"
                parameterCount = 3
            }?.hook {
                before {
                    val mConfig =
                        firstField { name = "mConfig" }.of(instance).get() ?: return@before
                    mConfig.asResolver().firstField { name = "enabled" }.set(false)
                }
            }
            firstMethod {
                name = "jsonToConfig"
                parameters("java.io.InputStream")
            }.hook {
                after {
                    val mConfig = firstField { name = "mConfig" }.of(instance).get() ?: return@after
                    mConfig.asResolver().firstField { name = "enabled" }.set(false)
                }
            }
        }
    }
}