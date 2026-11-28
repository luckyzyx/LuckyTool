package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 系统消息页：系统消息的区域默认值取值。
 */
object OplusMcsPage {

    val spec = PageSpec(
        pageKey = "oplus_mcs",
        prefsName = ModulePrefs,
        packName = "com.heytap.mcs",
        scopes = arrayOf("com.heytap.mcs"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "PageScreen 未注入 Context" }
        list(
            key = "custom_system_message_region_defaults",
            title = c.getString(R.string.custom_system_message_region_defaults),
            entries = c.resources.getStringArray(
                R.array.custom_system_message_region_defaults_entries
            ),
            entryValues = arrayOf("", "CN", "IN", "US"),
            default = "",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
    }
}
