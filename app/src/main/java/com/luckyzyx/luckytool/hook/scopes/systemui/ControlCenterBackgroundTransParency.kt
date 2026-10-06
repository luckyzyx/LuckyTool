package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControlCenterBackgroundTransParency : YukiBaseHooker() {
    override fun onHook() {
        var customAlpha =
            preferences(ModulePrefs).getInt("custom_control_center_background_transparency", -1)
        dataChannel.wait<Int>("custom_control_center_background_transparency") {
            customAlpha = it
        }

        //Source ScrimController
        "com.android.systemui.statusbar.phone.ScrimController".toClass().resolve().apply {
            firstMethod { name = "updateScrimColor" }.hook {
                before {
                    if (customAlpha < 0) return@before
                    val value = customAlpha / 10.0F
                    val view = firstArg().get<View>() ?: return@before
                    val alpha = arg(1).get<Float>() ?: return@before
                    val name = safeOfNull { view.resources.getResourceEntryName(view.id) }
                        ?: return@before
                    when (name) {
                        "scrim_in_front" -> {}
                        "scrim_behind" -> if (alpha > value) arg(1).set(value)
                        "scrim_notifications" -> if (alpha > value) arg(1).set(value)
                    }
                }
            }
        }
    }
}