package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * DirectUI 页（旧 ui.fragment.scopes.apps.OplusDirectUI 的 Compose 等价物）。
 */
object OplusDirectUIPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_direct_ui",
        prefsName = ModulePrefs,
        packName = "com.coloros.directui",
        scopes = arrayOf("com.coloros.directui", "com.coloros.colordirectservice"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_touch_app_recommend_card",
            title = c.getString(R.string.remove_app_recommend_card),
        )
    }
}
