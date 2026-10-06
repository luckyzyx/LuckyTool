package com.luckyzyx.luckytool.hook.globals

import android.content.ContentResolver
import android.database.Cursor
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookAppFeatureProvider(
    val dexKitBridge: DexKitBridge, private val features: Map<String, Any>
) : YukiBaseHooker() {

    private var isFeatureSupport = false
    private var isGetBoolean = false
    private var isGetString = false
    private var isGetInt = false

    override fun onHook() {
        if (features.isEmpty()) return

        //Source AppFeatureProviderUtils
        dexKitBridge.findClass {
            matcher {
//                addFieldForType(Uri::class.java)
//                addMethod { paramTypes(ContentResolverClass, null, String::class.java) }
                addMethod { paramTypes(classOf<ContentResolver>(), null) }
                addMethod {
                    usingStrings("featurename")
                    returnType(classOf<Cursor>())
                }
                usingStrings(
//                    "AppFeatureProviderUtils",
                    "content://com.oplus.customize.coreapp.configmanager.configprovider.AppFeatureProvider"
                )
            }
        }.apply {
            checkDataList("AppFeatureProviderUtils [$packageName]")
            findMethod {
                matcher {
//                    name("isFeatureSupport")
                    paramTypes(classOf<ContentResolver>(), classOf<String>())
                    returnType(classOf<Boolean>())
                }
            }.apply {
                if (!isFeatureSupport) isFeatureSupport = singleOrNull() != null
                singleOrNull()?.let {
                    it.className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            parameters(ContentResolver::class, String::class)
                            returnType = Boolean::class
                        }.hook {
                            before {
                                val key = lastArg().get<String>()
                                if (key.isNullOrBlank()) return@before
                                val value = features[key]
                                if (value != null && value is Boolean) result = value
                            }
                        }
                    }
                }
            }
            findMethod {
                matcher {
//                    name("isFeatureSupport")
                    paramTypes(classOf<ContentResolver>(), null, classOf<String>())
                    returnType(classOf<Boolean>())
                }
            }.apply {
                if (!isFeatureSupport) isFeatureSupport = singleOrNull() != null
                singleOrNull()?.let {
                    it.className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            parameters(ContentResolver::class, VagueType, String::class)
                            returnType = Boolean::class
                        }.hook {
                            before {
                                val key = lastArg().get<String>()
                                if (key.isNullOrBlank()) return@before
                                val value = features[key]
                                if (value != null && value is Boolean) result = value
                            }
                        }
                    }
                }
            }
            if (!isFeatureSupport) {
                YLog.debug("AppFeatureProviderUtils [$packageName] -> isFeatureSupport is null")
            }

            findMethod {
                matcher {
//                    name("getBoolean")
                    paramTypes(classOf<ContentResolver>(), classOf<String>(), classOf<Boolean>())
                    returnType(classOf<Boolean>())
                }
            }.apply {
                isGetBoolean = singleOrNull() != null
                singleOrNull()?.let {
                    it.className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            parameters(
                                ContentResolver::class,
                                String::class,
                                Boolean::class
                            )
                            returnType = Boolean::class
                        }.hook {
                            before {
                                val key = arg(1).get<String>()
                                if (key.isNullOrBlank()) return@before
                                val value = features[key]
                                if (value != null && value is Boolean) result = value
                            }
                        }
                    }
                }
            }

            findMethod {
                matcher {
//                    name("getString")
                    paramTypes(classOf<ContentResolver>(), classOf<String>(), classOf<String>())
                    returnType(classOf<String>())
                }
            }.apply {
                isGetString = singleOrNull() != null
                singleOrNull()?.let {
                    it.className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            parameters(
                                ContentResolver::class,
                                String::class,
                                String::class
                            )
                            returnType = String::class
                        }.hook {
                            before {
                                val key = arg(1).get<String>()
                                if (key.isNullOrBlank()) return@before
                                val value = features[key]
                                if (value != null && value is String) result = value
                            }
                        }
                    }
                }
            }

            findMethod {
                matcher {
//                    name("getInt")
                    paramTypes(classOf<ContentResolver>(), classOf<String>(), classOf<Int>())
                    returnType(classOf<Int>())
                }
            }.apply {
                isGetInt = singleOrNull() != null
                singleOrNull()?.let {
                    it.className.toClass().resolve().apply {
                        firstMethod {
                            name = single().methodName
                            parameters(
                                ContentResolver::class,
                                String::class,
                                Int::class
                            )
                            returnType = Int::class
                        }.hook {
                            before {
                                val key = arg(1).get<String>()
                                if (key.isNullOrBlank()) return@before
                                val value = features[key]
                                if (value != null && value is Int) result = value
                            }
                        }
                    }
                }
            }
        }
    }
}