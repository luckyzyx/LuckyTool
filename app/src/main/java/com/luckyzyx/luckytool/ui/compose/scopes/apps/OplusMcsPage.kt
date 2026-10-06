package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 系统消息页（旧 ui.fragment.scopes.apps.OplusMcs 的 Compose 等价物）。
 */
object OplusMcsPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_mcs",
        prefsName = ModulePrefs,
        packName = "com.heytap.mcs",
        scopes = arrayOf("com.heytap.mcs"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
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
