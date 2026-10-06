package com.luckyzyx.luckytool.hook.scopes.android

import android.view.KeyEvent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ReducePowerMenuDisplayDelay : YukiBaseHooker() {
    override fun onHook() {
        var isEnable = preferences(ModulePrefs).getBoolean("reduce_power_menu_display_delay", false)
        dataChannel.wait<Boolean>("reduce_power_menu_display_delay") { isEnable = it }

        //Source PhoneWindowManager -> PowerKeyRule -> super getVeryLongPressTimeoutMs
        "com.android.server.policy.SingleKeyGestureDetectorExtImpl".toClass().resolve().apply {
            firstMethod { name = "modifyPressTimeout" }.hook {
                after {
                    if (!isEnable) return@after
                    val pressType = firstArg().get<Int>() ?: return@after
                    val event = lastArg().get<KeyEvent>() ?: return@after
                    if (pressType == 1 && event.keyCode == 26) result = 800L
                }
            }
        }
    }
}