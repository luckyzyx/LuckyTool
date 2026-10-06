package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 旧 ui.fragment.scopes.apps.OplusThemeStore 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusThemeStorePage {

    val spec = ScopePageSpec(
        pageKey = "theme_store",
        prefsName = ModulePrefs,
        packName = "com.heytap.themestore",
        scopes = arrayOf("com.heytap.themestore"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "unlock_themestore_vip",
            title = c.getString(R.string.unlock_themestore_vip),
            summary = c.getString(R.string.unlock_themestore_vip_summary),
        )
    }
}
