package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.showToast

/**
 * 浏览器页（旧 ui.fragment.scopes.apps.OplusBrowser 的 Compose 等价物）。
 */
object OplusBrowserPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_browser",
        prefsName = ModulePrefs,
        packName = "com.heytap.browser",
        scopes = arrayOf("com.heytap.browser"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (c.checkPackName("com.heytap.browser")) {
            custom(title = c.getString(R.string.browser_concise_mode)) { slot ->
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SegmentedListItem(
                        onClick = {
                            try {
                                Intent().apply {
                                    setClassName(
                                        "com.heytap.browser",
                                        "com.heytap.browser.settings.component.BrowserPreferenceActivity"
                                    )
                                    putExtra(
                                        "key.fragment.name",
                                        "com.heytap.browser.settings.homepage.HomepagePreferenceFragment"
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    c.startActivity(this)
                                }
                            } catch (_: Exception) {
                                c.showToast("Error: Please check your browser version!")
                            }
                        },
                        colors = itemColors(slot),
                        headlineContent = { Text(c.getString(R.string.browser_concise_mode)) },
                    )
                }
            }
        }
        category(c.getString(R.string.ads))
        switch(
            key = "remove_ads_from_download_dialog",
            title = c.getString(R.string.remove_ads_from_download_dialog),
        )
        switch(
            key = "remove_ads_at_download_page_bottom",
            title = c.getString(R.string.remove_ads_at_download_page_bottom),
        )
        switch(
            key = "remove_browser_window_limit_number",
            title = c.getString(R.string.remove_browser_window_limit_number),
        )
        switch(
            key = "remove_browser_search_bar_app_promotion",
            title = c.getString(R.string.remove_browser_search_bar_app_promotion),
        )
    }
}
