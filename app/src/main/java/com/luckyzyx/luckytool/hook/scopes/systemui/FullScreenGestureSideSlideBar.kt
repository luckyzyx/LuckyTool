package com.luckyzyx.luckytool.hook.scopes.systemui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object FullScreenGestureSideSlideBar : Hooker {

    override fun onHook() {
        //Source SideGestureViewManager
        //Source SideGestureNavView navbar_gesture_background
        val removeView = prefs(ModulePrefs).getBoolean("remove_side_slider", false)
        val removeBackground =
            prefs(ModulePrefs).getBoolean("remove_side_slider_black_background", false)
        val isReplace = prefs(ModulePrefs).getBoolean("replace_side_slider_icon_switch", false)
        val leftPath = prefs(ModulePrefs).getString("replace_side_slider_icon_on_left", "")
        val rightPath = prefs(ModulePrefs).getString("replace_side_slider_icon_on_right", "")
        VariousClass(
            "com.oplusos.systemui.navbar.gesture.sidegesture.SideGestureNavView", //A11
            "com.oplusos.systemui.navigationbar.gesture.sidegesture.SideGestureNavView",
            "com.oplus.systemui.navigationbar.gesture.sidegesture.SideGestureNavView", //C14 C15
            "com.oplus.systemui.navigationbar.gesture.sidegesture.view.SideGestureNavView" //C16
        ).toClass().resolve().optional(true).apply {
            firstMethod { name = "onDraw";parameterCount = 1 }.hook {
                if (removeView) intercept()
            }
            (firstMethodOrNull { name = "initPaint" } ?: firstConstructor()).hook {
                after {
                    if (!removeBackground) return@after
                    firstField {
                        name { it.contains("bezierPaint", true) }
                        type = Paint::class
                    }.of(instance).get<Paint>()?.color = Color.TRANSPARENT
                }
            }
            firstMethod { name = "setBackIcon";parameters(Bitmap::class) }.hook {
                before {
                    if (!isReplace) return@before
                    val type = firstField {
                        name { it.contains("position", true) }
                    }.of(instance).get<Int>()
                    val bitmap = when (type) {
                        0 -> BitmapFactory.decodeFile(leftPath)
                        1 -> BitmapFactory.decodeFile(rightPath)
                        else -> return@before
                    }
                    bitmap?.let { args().first().set(it) }
                }
            }
        }
    }
}