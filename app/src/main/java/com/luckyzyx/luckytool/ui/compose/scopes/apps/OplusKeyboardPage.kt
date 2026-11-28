package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 小布输入法（com.oplus.keyboard）功能页。
 */
object OplusKeyboardPage {

    val spec = PageSpec(
        pageKey = "oplus_keyboard",
        prefsName = ModulePrefs,
        packName = "com.oplus.keyboard",
        scopes = arrayOf("com.oplus.keyboard"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "PageScreen 未注入 Context" }
        switch(
            key = "force_enable_oplus_keyboard_blur",
            title = c.getString(R.string.force_enable_oplus_keyboard_blur),
            summary = c.getString(R.string.force_enable_oplus_keyboard_blur_summary),
        )
        if (state.getBoolean("force_enable_oplus_keyboard_blur")) {
            slider(
                key = "oplus_keyboard_blur_radius",
                title = c.getString(R.string.oplus_keyboard_blur_radius),
                valueRange = 50..500,
                step = 10,
                default = 150,
                summary = c.getString(R.string.oplus_keyboard_blur_radius_summary),
            )
        }
    }
}
