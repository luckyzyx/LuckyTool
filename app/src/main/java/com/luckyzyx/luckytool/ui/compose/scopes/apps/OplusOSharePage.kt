package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 旧 ui.fragment.scopes.apps.OplusOShare 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusOSharePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_oshare",
        prefsName = ModulePrefs,
        packName = "com.coloros.oshare",
        scopes = arrayOf("com.coloros.oshare"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_oshare_close_countdown",
            title = c.getString(R.string.remove_oshare_close_countdown),
        )
    }
}
