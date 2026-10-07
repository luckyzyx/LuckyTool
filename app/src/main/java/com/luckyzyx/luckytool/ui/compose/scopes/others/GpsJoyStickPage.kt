package com.luckyzyx.luckytool.ui.compose.scopes.others

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * GPS 摇杆页（旧 ui.fragment.scopes.others.GpsJoyStick 的 Compose 等价物）。
 */
object GpsJoyStickPage {

    val spec = ScopePageSpec(
        pageKey = "gps_joy_stick",
        prefsName = ModulePrefs,
        packName = "com.theappninjas.fakegpsjoystick",
        scopes = arrayOf("com.theappninjas.fakegpsjoystick"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "gps_joystick_unlock_pro",
            title = c.getString(R.string.adm_unlock_pro),
        )
    }
}
