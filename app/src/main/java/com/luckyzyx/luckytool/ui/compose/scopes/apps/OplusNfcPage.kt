package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 旧 ui.fragment.scopes.apps.OplusNfc 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusNfcPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_nfc",
        prefsName = ModulePrefs,
        packName = "com.android.nfc",
        scopes = arrayOf("com.android.nfc"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "scan_nfc_tag_auto_click",
            title = c.getString(R.string.scan_nfc_tag_auto_click),
            summary = c.getString(R.string.need_restart_system),
        )
    }
}
