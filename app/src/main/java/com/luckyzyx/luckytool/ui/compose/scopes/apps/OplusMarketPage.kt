package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 软件商店页（旧 ui.fragment.scopes.apps.OplusMarket 的 Compose 等价物）。
 */
object OplusMarketPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_market",
        prefsName = ModulePrefs,
        packName = "com.heytap.market",
        scopes = arrayOf("com.heytap.market"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_market_splash_page_app_recommend",
            title = c.getString(R.string.remove_market_splash_page_app_recommend),
        )
        switch(
            key = "remove_market_update_download_page_app_recommend",
            title = c.getString(R.string.remove_market_update_download_page_app_recommend),
        )
        switch(
            key = "remove_market_mine_page_app_recommend",
            title = c.getString(R.string.remove_market_mine_page_app_recommend),
        )
        switch(
            key = "default_expand_update_list",
            title = c.getString(R.string.default_expand_update_list),
        )
    }
}
