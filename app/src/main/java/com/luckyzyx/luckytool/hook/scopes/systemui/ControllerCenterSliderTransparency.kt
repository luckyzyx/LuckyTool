package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.CheckBox
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.formatColorAlpha
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControllerCenterSliderTransparency : YukiBaseHooker() {
    override fun onHook() {
        val customAlpha = preferences(ModulePrefs).getInt("custom_control_center_silder_transparency", -1)

        //Source OplusToggleSliderView C14.0
        VariousClass(
            "com.oplusos.systemui.qs.widget.OplusToggleSliderView", //C13
            "com.oplus.systemui.qs.widget.OplusToggleSliderView", //C14.0
        ).toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "setupSliderProgressDrawable" }.hook {
                after {
                    if (customAlpha < 0) return@after
                    val value = customAlpha / 10F
                    val mSlider = firstField { name = "mSlider" }.of(instance).get() ?: return@after
                    val baseProgressColor = mSlider.asResolver().firstField {
                        name = "mProgressColor";superclass()
                    }.get<Int>() ?: return@after
                    mSlider.asResolver().firstMethod {
                        name = "setProgressColor"
                        parameters(ColorStateList::class)
                        superclass()
                    }.invoke(
                        ColorStateList.valueOf(
                            formatColorAlpha(baseProgressColor, value)
                        )
                    )
                    mSlider.asResolver().firstMethod {
                        name = "setThumbColor"
                        parameters(ColorStateList::class)
                        superclass()
                    }.invoke(ColorStateList.valueOf(Color.TRANSPARENT))
                }
            }
            firstMethod { name = "updateToggleBackground" }.hook {
                after {
                    if (customAlpha < 0) return@after
                    val value = 255 / 10 * customAlpha
                    val mToggle = firstField { name = "mToggle" }.of(instance).get<CheckBox>()
                        ?: return@after
                    mToggle.background.alpha = value
                }
            }
        }

        //Source OplusQsToggleSliderLayout C14.0.1 C15.0
        "com.oplus.systemui.qs.widget.OplusQsToggleSliderLayout".toClassOrNull()?.resolve()?.apply {
            firstMethod {
                name = "generateSliderView"
                if (SDK >= A15) superclass()
            }.hook {
                after {
                    if (customAlpha < 0) return@after
                    val value = customAlpha / 10.0F
                    val seekBar = result<View>() ?: return@after
                    val baseColor =
                        seekBar.asResolver().firstField { name = "mProgressColor" }.get<Int>()
                            ?: return@after
                    seekBar.asResolver().firstMethod {
                        name = "setProgressColor"
                        parameters(ColorStateList::class)
                    }.invoke(
                        ColorStateList.valueOf(
                            formatColorAlpha(baseColor, value)
                        )
                    )
                }
            }
        }
    }
}