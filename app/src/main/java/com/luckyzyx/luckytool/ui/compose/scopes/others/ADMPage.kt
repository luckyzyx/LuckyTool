package com.luckyzyx.luckytool.ui.compose.scopes.others

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * ADM 页（旧 ui.fragment.scopes.others.ADM 的 Compose 等价物）。
 */
object ADMPage {

    val spec = ScopePageSpec(
        pageKey = "adm",
        prefsName = ModulePrefs,
        packName = "com.dv.adm",
        scopes = arrayOf("com.dv.adm"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "adm_unlock_pro",
            title = c.getString(R.string.adm_unlock_pro),
        )
        list(
            key = "adm_unlock_more_threads",
            title = c.getString(R.string.adm_unlock_more_threads),
            entries = c.resources.getStringArray(R.array.adm_unlock_more_threads_entries),
            entryValues = arrayOf("0", "32", "64", "128"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
    }
}
