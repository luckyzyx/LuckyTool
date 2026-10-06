package com.luckyzyx.luckytool.hook.scopes.notificationmanager

import android.annotation.SuppressLint
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused", "ConstPropertyName")
object ForceDisplayClockStyleOptionsV14 : YukiBaseHooker() {
    private const val searchItemBuilder =
        "com.oplus.keyguard.settingsearch.KeyguardSettingsSearchProvider\$SearchItem\$Builder"
    private const val providerClient = "com.oplus.keyguard.common.KeyguardSettingProviderClient"
    private const val type = "TYPE_PREFRENCE_JUMP"
    private const val key = "key_keyguard_land_clock_screen"
    private const val category = "key_keyguard_category"
    private var launcherActivity = "com.android.launcher.settings.LauncherSettingsActivity"
    private var launcherAction = "com.android.launcher.action.settings.LAUNCHER_SETTINGS"
    private var launcherPackName = "com.android.launcher"

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        //Source KeyguardLauncherPageProvider
        "com.oplus.keyguard.keyguardsettings.KeyguardLauncherPageProvider".toClass().resolve()
            .apply {
                firstMethod { name = "initKeyguardLandClockPf" }.hook {
                    before {
                        val isFlavorTwoDevice = providerClient.toClass().resolve().firstField {
                            name = "isFlavorTwoDevice"
                        }.get<Boolean>() ?: false
                        if (!isFlavorTwoDevice) return@before

                        val list = firstArg().get<ArrayList<Any>>()
                        val context = firstMethod { name = "getContext";superclass() }.of(instance)
                            .invoke<Context>()
                        val clockTitle = context?.getString(
                            context.resources.getIdentifier(
                                "oplus_keyguard_land_clock_type_title", "string",
                                this@ForceDisplayClockStyleOptionsV14.packageName
                            )
                        )
                        val keyguardLandClockPf = firstMethod {
                            name = "createPerfrenceBean";superclass()
                        }.of(instance).invoke(type, key, 70, clockTitle, category)
                        keyguardLandClockPf?.asResolver()?.firstMethod { name = "setIntentPackage" }
                            ?.invoke("com.oplus.notificationmanager")
                        keyguardLandClockPf?.asResolver()?.firstMethod { name = "setIntentClass" }
                            ?.invoke("com.oplus.keyguard.keyguardsettings.KeyguardLandClockActivity")

                        val hashMap = firstField { name = "preferenceHashMap" }.of(instance)
                            .get<HashMap<String, Any>>()
                        firstMethod { name = "addPreferenceMap" }.of(instance).invoke(
                            hashMap, key, keyguardLandClockPf
                        )
                        keyguardLandClockPf?.let { list?.add(it) }
                        result = null
                    }
                }
            }

        //Source KeyguardSettingsSearchProvider
//        "com.oplus.keyguard.settingsearch.KeyguardSettingsSearchProvider".toClass().apply {
//            method { name = "initSearchData";paramCount = 1 }.hook {
//                after {
//                    val isFlavorTwoDevice = providerClient.toClass().field {
//                        name = "isFlavorTwoDevice"
//                    }.get().boolean()
//                    if (!isFlavorTwoDevice) return@after
//
//                    val context = firstArg().get<Context>() ?: return@after
//                    val subTitle = context.resources.getIdentifier(
//                        "settings_search_sub_title",
//                        "string",
//                        this@ForceDisplayClockStyleOptionsV14.packageName
//                    )
//                    val clockTitle = context.resources.getIdentifier(
//                        "oplus_keyguard_land_clock_type_title",
//                        "string",
//                        this@ForceDisplayClockStyleOptionsV14.packageName
//                    )
//                    val settingsWallpaper = context.resources.getIdentifier(
//                        "settings_wallpaper_ic",
//                        "drawable",
//                        this@ForceDisplayClockStyleOptionsV14.packageName
//                    )
//
//                    val builder = searchItemBuilder.toClass().field { name = "INSTANCE" }.get()
//                        .any() ?: return@after
//                    val build = builder.current().method {
//                        name = "build";param { it[4] == IntType && it[7] == IntArrayType }
//                    }.call(
//                        context, 2, settingsWallpaper, key, clockTitle, -1, -1,
//                        intArrayOf(subTitle, clockTitle), launcherActivity, launcherAction,
//                        launcherPackName, launcherActivity, -1
//                    )
//                    method { name = "autoAddItems" }.get(instance).call(build)
//                }
//            }
//        }
    }
}