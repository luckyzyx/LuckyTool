package com.luckyzyx.luckytool.hook.scopes.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ComponentInfo
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class EnableGoogleAutoFill(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        if (getOSVersionCode >= 30) loadHooker(GoogleAutoFill)
        else loadHooker(GoogleAutoFillV13(dexKitBridge))
    }

    @Obfuscate
    object GoogleAutoFill : YukiBaseHooker() {
        override fun onHook() {
            //Source DefaultAppInfo
            val defaultAppInfoClazz = "com.android.settingslib.applications.DefaultAppInfo"

            //Source OplusDefaultAutofillPicker
            "com.oplus.settings.feature.othersettings.input.OplusDefaultAutofillPicker".toClass()
                .resolve().apply {
                    firstMethod { name = "getCandidates" }.hook {
                        before {
                            val list = ArrayList<Any>()
                            val context =
                                firstMethod { name = "getContext";superclass() }.of(instance)
                                    .invoke<Context>() ?: return@before
                            val pm = firstField {
                                type = PackageManager::class;superclass()
                            }.of(instance).get<PackageManager>() ?: return@before
                            val allProviders = firstMethod {
                                name = "getAllProviders";superclass()
                            }.of(instance).invoke<List<Any>>() ?: listOf()
                            val users = firstMethod {
                                name = "getUser";superclass()
                            }.of(instance).invoke<Int>() ?: 0
                            allProviders.forEachIndexed { _, info ->
                                val settingsSubtitle =
                                    info.asResolver().firstMethod { name = "getSettingsSubtitle" }
                                        .invoke()
                                val brandingService =
                                    info.asResolver().firstMethod { name = "getBrandingService" }
                                        .invoke()
                                val defaultAppInfo = defaultAppInfoClazz.toClass().resolve()
                                    .firstConstructor {
                                        parameters(
                                            Context::class, PackageManager::class, Int::class,
                                            PackageItemInfo::class, String::class, Boolean::class
                                        )
                                    }.create(
                                        context, pm, users, brandingService, settingsSubtitle, true
                                    )
                                list.add(defaultAppInfo)
                            }
                            if (list.isNotEmpty()) result = list
                        }
                    }
//                method { name = "newAddServicePreferenceOrNull" }.hook {
//                    before {
//                        val context = method { name = "getPrefContext";superClass() }.get(instance)
//                            .invoke<Context>() ?: return@before
//                        val preference = method {
//                            name = "newAddServicePreferenceOrNull";superClass()
//                        }.get(instance).original().call()
//                        YLog.debug("newAddServicePreferenceOrNull -> ${preference.toString()}")
//
//                        if (preference != null) {
//                            val title = context.resources.getIdentifier(
//                                "add_autofill_services", "string", this@HookSettings.packageName
//                            )
//                            if (title == 0) return@before
//                            preference.current().method { name = "setTitle";param(IntType) }.call(title)
//                            preference.current().method { name = "setOrder";param(IntType) }
//                                .call(2147483645)
//                            preference.current().method { name = "setPersistent";param(BooleanType) }
//                                .call(false)
//                            preference.current().method { name = "setKey";param(StringClass) }
//                                .call("add_autofill_services")
//
//                            YLog.debug("preference -> ${preference.toString()}")
//
//                            result = preference
//                        }
//                    }
//                }
                }
        }
    }

    @Obfuscate
    class GoogleAutoFillV13(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //Source DefaultAppInfo
            val defaultAppInfoClazz = dexKitBridge.findClass {
                matcher {
                    fields {
                    addForType(classOf<Int>())
                        addForType(classOf<String>())
                        addForType(classOf<Context>())
                        addForType(classOf<ComponentName>())
                        addForType(classOf<PackageManager>())
                        addForType(classOf<PackageItemInfo>())
                    }
                    methods {
                    add { paramCount(0);returnType(classOf<String>()) }
                        add { paramCount(0);returnType(classOf<Drawable>()) }
                        add { paramCount(0);returnType(classOf<CharSequence>()) }
                        add { paramCount(0);returnType(classOf<ComponentInfo>()) }
                    }
                }
            }.let {
                it.checkDataList("GoogleAutoFillV13")
                it.single().name
            }

            //Source OplusDefaultAutofillPicker
            "com.oplus.settings.feature.othersettings.input.OplusDefaultAutofillPicker".toClass()
                .resolve().apply {
                    firstMethod {
                        emptyParameters()
                        returnType = List::class
                    }.hook {
                        before {
                            val list = ArrayList<Any>()
                            val context =
                                firstMethod { name = "getContext";superclass() }.of(instance)
                                    .invoke<Context>() ?: return@before
                            val packageManager = firstField {
                                type = classOf<PackageManager>();superclass()
                            }.of(instance).get<PackageManager>() ?: return@before
                            val intent = firstField {
                                type = Intent::class;superclass()
                            }.of(instance).get<Intent>() ?: return@before
                            val users =
                                UserHandle::class.resolve().firstMethod { name = "myUserId" }
                                    .invoke<Int>() ?: 0
                            val queryIntentServicesAsUser =
                                packageManager.asResolver().firstMethod {
                                    name = "queryIntentServicesAsUser"
                                    parameters(Intent::class, Int::class, Int::class)
                                }.invoke<List<ResolveInfo>>(intent, 128, users) ?: return@before
                            queryIntentServicesAsUser.forEachIndexed { _, resolveInfo ->
                                val serviceInfo = resolveInfo.serviceInfo
                                val permission = serviceInfo.permission
                                if (permission == "android.permission.BIND_AUTOFILL_SERVICE") {
                                    val defaultAppInfo =
                                        defaultAppInfoClazz.toClass().createInstance(
                                            context, packageManager, users, ComponentName(
                                                serviceInfo.packageName, serviceInfo.name
                                            ), isPublic = false
                                        )
                                    list.add(defaultAppInfo)
                                }
                                if (permission == "android.permission.BIND_AUTOFILL") {
                                    val defaultAppInfo =
                                        defaultAppInfoClazz.toClass().createInstance(
                                            context, packageManager, users, ComponentName(
                                                serviceInfo.packageName, serviceInfo.name
                                            ), isPublic = false
                                        )
                                    list.add(defaultAppInfo)
                                }
                            }
                            if (list.isNotEmpty()) result = list
                        }
                    }
                }
        }
    }
}