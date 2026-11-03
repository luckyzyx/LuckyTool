package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 无线设置页：WiFi 详情页显示网关的开关。
 */
object OplusWirelessSettingsPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_wireless_settings",
        prefsName = ModulePrefs,
        packName = "com.oplus.wirelesssettings",
        scopes = arrayOf("com.oplus.wirelesssettings"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "enable_wifi_details_display_gateway",
            title = c.getString(R.string.enable_wifi_details_display_gateway),
        )
    }
}
