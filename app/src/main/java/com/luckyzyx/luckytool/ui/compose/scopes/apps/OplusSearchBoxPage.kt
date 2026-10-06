package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 旧 ui.fragment.scopes.apps.OplusSearchBox 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusSearchBoxPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_search_box",
        prefsName = ModulePrefs,
        packName = "com.heytap.quicksearchbox",
        scopes = arrayOf("com.heytap.quicksearchbox"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_searchbox_app_recommend_card",
            title = c.getString(R.string.remove_app_recommend_card),
        )
        switch(
            key = "remove_searchbox_uninstalled_app_suggestions",
            title = c.getString(R.string.remove_searchbox_uninstalled_app_suggestions),
        )
        switch(
            key = "searchbox_default_search_local_tab",
            title = c.getString(R.string.searchbox_default_search_local_tab),
        )
    }
}
