package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveRotateScreenButton : YukiBaseHooker() {
    override fun onHook() {
        //Source FloatingRotationButton
        VariousClass(
            "com.android.systemui.statusbar.phone.FloatingRotationButton", //A11
            "com.android.systemui.navigationbar.gestural.FloatingRotationButton", //A12
            "com.android.systemui.shared.rotation.FloatingRotationButton" //C13 C14 C16 C17
        ).toClass().resolve().apply {
            //C17 removed the Context constructor, the button is created via the no-arg
            //constructor and mKeyButtonView is assigned afterwards by NavigationBarView,
            //so the constructor hook below no longer matches on C17.
            //updateDimensionResources is invoked right after mKeyButtonView is populated
            //on every version, hide the rotate button there.
            firstMethod {
                name = "updateDimensionResources"
                emptyParameters()
            }.hook {
                after {
                    firstField { name = "mKeyButtonView" }.of(instance).get<View>()
                        ?.isVisible = false
                }
            }
            //Fallback for A11 A12: no updateDimensionResources below C13, hide
            //in the constructor instead. C12 C13 C14 C15 C16 constructors all take
            //Context as the first param and assign mKeyButtonView inside the ctor,
            //so this hook is universal for C16 and below (and doubles as a safety
            //net on C13-C16).
            firstConstructor { parameters { it.isNotEmpty() && it[0] == classOf<Context>() } }.hook {
                after {
                    firstField { name = "mKeyButtonView" }.of(instance).get<View>()
                        ?.isVisible = false
                }
            }
        }
    }
}