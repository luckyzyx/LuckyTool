package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.QSFeatureOptionUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getScreenOrientation
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object SpecialTileTopGap : YukiBaseHooker() {
    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        val osCode = getOSVersionCode
        var top = preferences(ModulePrefs).getInt("control_center_special_tile_top_gap", 10)
        dataChannel.wait<Int>("control_center_special_tile_top_gap") { top = it }
        var bottom = preferences(ModulePrefs).getInt("control_center_special_tile_bottom_gap", 0)
        dataChannel.wait<Int>("control_center_special_tile_bottom_gap") { bottom = it }
        var smallBrightness =
            preferences(ModulePrefs).getBoolean("decrease_horizontal_brightness_bar_top_gap", false)
        dataChannel.wait<Boolean>("decrease_horizontal_brightness_bar_top_gap") {
            smallBrightness = it
        }

        val isSupportVolumeSeekBar = QSFeatureOptionUtils(hostClassLoader!!).isSupportVolumeSeekBar()
        if (isSupportVolumeSeekBar) return

        //Source OplusQSTileMediaContainerController
        VariousClass(
            "com.oplusos.systemui.qs.OplusQSTileMediaContainerController", //C13
            "com.oplus.systemui.qs.OplusQSTileMediaContainerController" //C14 C15
        ).toClass().resolve().apply {
            firstMethod { name = "updateResources" }.hook {
                after {
                    val context = firstMethod {
                        name = "getContext";superclass()
                    }.of(instance).invoke<Context>() ?: return@after
                    getScreenOrientation(context) {
                        if (it) return@getScreenOrientation
                        firstField { name = "mTopGap" }.of(instance).set(top.dp)
                        firstField { name = "mBottomGap" }.of(instance).set(bottom.dp)
                    }
                }
            }
        }

        if (osCode < 30) return

        //Source OplusQSBottomImpl C14 C15
        "com.oplus.systemui.qs.OplusQSBottomImpl".toClass().resolve().apply {
            firstMethod { name = "updateResources" }.hook {
                after {
                    if (!smallBrightness) return@after
                    val mPageIndicator = firstField { name = "mPageIndicator" }.of(instance)
                        .get<View>() ?: return@after
                    getScreenOrientation(mPageIndicator) {
                        if (it) return@getScreenOrientation
                        (mPageIndicator.layoutParams as LinearLayout.LayoutParams).apply {
                            bottomMargin = 6.dp
                        }
                    }
                }
            }
        }
    }
}