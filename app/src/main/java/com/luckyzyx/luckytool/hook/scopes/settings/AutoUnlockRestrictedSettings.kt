package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.Context
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.EcmUtils
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class AutoUnlockRestrictedSettings(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        if (osCode >= 34) loadHooker(RestrictedSettings)
        else loadHooker(RestrictedSettingsV14(dexKitBridge))
    }

    @Obfuscate
    object RestrictedSettings : YukiBaseHooker() {
        override fun onHook() {
            //Source RestrictedPreferenceHelper
            "com.oplus.settings.widget.preference.RestrictedPreferenceHelper".toClass().resolve()
                .apply {
                    firstMethod {
                        name = "performClick"
                        emptyParameters()
                        returnType = Boolean::class
                    }.hook {
                        before {
                            val context = firstField { type = Context::class }.of(instance)
                                .get<Context>() ?: return@before
                            val intent = firstField { type = Intent::class }.of(instance)
                                .get<Intent>() ?: return@before
                            val packName =
                                intent.getStringExtra("android.intent.extra.PACKAGE_NAME")
                                    ?: return@before
//                        val permissionName = intent.getStringExtra("android.intent.extra.SUBJECT")
//                            ?: return@before
//                            val uid = intent.getIntExtra("android.intent.extra.UID", -1)
                            EcmUtils(context).autoUnlockRestrictedSettings(packName)
                            firstMethod {
                                name = "setDisabledByEcm"
                                parameters(Intent::class)
                                returnType = Boolean::class
                            }.of(instance).invoke(null)
                            result = false
                        }
                    }
                }
        }
    }

    @Obfuscate
    class RestrictedSettingsV14(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val limit = false
            //Source RestrictedPreferenceHelper
            dexKitBridge.findClass {
                matcher {
                    fields {
                    addForType(classOf<Context>())
                        addForType(classOf<String>())
                        addForType(classOf<Boolean>())
                        addForType(classOf<Int>())
                    }
                    methods {
                        add {
                            paramCount(0)
                            returnType(classOf<Boolean>())
                        }
                        add {
                            paramCount(0)
                            returnType(Void.TYPE)
                        }
                        add {
                        paramTypes(classOf<Boolean>())
                            returnType(classOf<Boolean>())
                        }
                    }
                    usingStrings("RestrictedPreferenceHelper")
                }
            }.apply {
                checkDataList("AutoUnlockRestrictedSettings findClass")
                val findMethod = findMethod {
                    matcher {
                        paramCount(0)
                        returnType(classOf<Boolean>())
                        addCaller {
                            name("performClick")
                            returnType(Void.TYPE)
                        }
                        addUsingField {
                        field { type(classOf<Context>()) }
                            field { type(classOf<Int>()) }
                            field { type(classOf<String>()) }
                            field { type(classOf<Boolean>()) }
                        }
                    }
                }.checkDataList("AutoUnlockRestrictedSettings findMethod").single()

                val fields = findField {
                    matcher {
                        addReadMethod {
                            name(findMethod.methodName)
                            paramCount(0)
                            returnType(classOf<Boolean>())
                        }
                    }
                }.checkDataList("AutoUnlockRestrictedSettings findFields", onlyOne = false)

                val appops = findField {
                    matcher {
                        type(classOf<Boolean>())
                        addReadMethod {
                            name(findMethod.methodName)
                            paramCount(0)
                            returnType(classOf<Boolean>())
                        }
                        addWriteMethod {
                        paramTypes(classOf<Boolean>().name)
                            returnType(classOf<Boolean>())
                        }
                    }
                }.checkDataList("AutoUnlockRestrictedSettings findField AppOps").single()

                val admin =
                    fields.filter { it.typeName == classOf<Boolean>().name }.toMutableList()
                        .apply {
                            removeIf { it.fieldName == appops.fieldName }
                        }.first()

//                val uidname =
//                    fields.find { it.typeName == Int::class.java.name }?.fieldName ?: "uid"
                val packname = fields.find { it.typeName == classOf<String>().name }?.fieldName
                    ?: "packageName"

                findMethod.className.toClass().resolve().apply {
                    firstMethod {
                        name = findMethod.methodName
                        emptyParameters()
                        returnType = Boolean::class
                    }.hook {
                        before {
                            val context =
                                firstField { type = Context::class }.of(instance).get<Context>()
                                    ?: return@before
                            val disabledAdmin =
                                firstField { name = admin.fieldName }.of(instance).get<Boolean>()
                                    ?: false
                            val disabledAppOps = firstField { name = appops.fieldName }.of(instance)
                                .get<Boolean>() ?: false
                            if (disabledAdmin) return@before
                            if (disabledAppOps) {
//                                val uid =
//                                    firstField { name = uidname }.of(instance).get<Int>() ?: -1
                                val packName =
                                    firstField { name = packname }.of(instance).get<String>() ?: ""
                                EcmUtils(context).autoUnlockRestrictedSettings(packName)
                                firstMethod {
                                    parameters(Boolean::class)
                                    returnType = Boolean::class
                                }.of(instance).invoke(limit)
                                result = limit
                            }
                        }
                    }
                }
            }
        }
    }
}