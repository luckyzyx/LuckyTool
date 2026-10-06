package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayClockStyleOptionsV13 : YukiBaseHooker() {
    private const val flavorTwoFeatureOption =
        "com.oplusos.systemui.common.feature.FlavorTwoFeatureOption"
    private const val type = "TYPE_PREFRENCE_JUMP"
    private const val key = "key_keyguard_land_clock_screen"
    private const val category = "key_keyguard_category"

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        //Source KeyguardLauncherPageProvider
        VariousClass(
            "com.oplusos.systemui.keyguard.keyguardsetting.KeyguardLauncherPageProvider", //C13.0
            "com.oplus.systemui.keyguard.keyguardsetting.KeyguardLauncherPageProvider" //C13.1
        ).toClass().resolve().apply {
            firstMethod { name = "initKeyguardLandClockPf" }.hook {
                before {
                    val isFlavorTwoDevice = flavorTwoFeatureOption.toClass().resolve().firstMethod {
                        name = "isFlavorTwoDevice"
                    }.invoke<Boolean>() ?: false
                    if (!isFlavorTwoDevice) return@before

                    val list = firstArg().get<ArrayList<Any>>()
                    val context = firstMethod { name = "getContext";superclass() }.of(instance)
                        .invoke<Context>()
                    val clockTitle = context?.getString(
                        context.resources.getIdentifier(
                            "oplus_keyguard_land_clock_type_title", "string",
                            this@ForceDisplayClockStyleOptionsV13.packageName
                        )
                    )
                    val keyguardLandClockPf =
                        firstMethod { name = "createPerfrenceBean";superclass() }.of(instance)
                            .invoke(type, key, 70, clockTitle, category)
                    keyguardLandClockPf?.asResolver()?.firstMethod { name = "setIntentPackage" }
                        ?.invoke("com.android.systemui")
                    keyguardLandClockPf?.asResolver()?.firstMethod { name = "setIntentClass" }
                        ?.invoke("com.oplus.systemui.keyguard.keyguardsetting.KeyguardLandClockActivity")

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
    }
}