package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 旧 ui.fragment.scopes.apps.OplusSmartSidebar 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusSmartSidebarPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_smart_sidebar",
        prefsName = ModulePrefs,
        packName = "com.coloros.smartsidebar",
        scopes = arrayOf("com.coloros.smartsidebar"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        if (SDK == A12) {
            switch(
                key = "force_enable_buoy_automatically_hides",
                title = c.getString(R.string.force_enable_buoy_automatically_hides),
            )
        }
        if (SDK == A13) {
            switch(
                key = "unlock_transfer_dock",
                title = c.getString(R.string.unlock_transfer_dock),
            )
            switch(
                key = "unlock_recent_files",
                title = c.getString(R.string.unlock_recent_files),
            )
        }
        if (osCode >= 27) {
            switch(
                key = "enable_run_in_background",
                title = c.getString(R.string.enable_run_in_background),
                summary = if (osCode >= 37) c.getString(R.string.enable_run_in_background_summary) else null,
            )
        }
    }
}
