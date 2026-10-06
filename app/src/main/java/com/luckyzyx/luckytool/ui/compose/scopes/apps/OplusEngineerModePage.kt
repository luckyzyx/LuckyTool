package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 工程模式页（旧 ui.fragment.scopes.apps.OplusEngineerMode 的 Compose 等价物）。
 */
object OplusEngineerModePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_engineer_mode",
        prefsName = ModulePrefs,
        packName = "com.oplus.engineermode",
        scopes = arrayOf("com.oplus.engineermode"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "unlock_some_hidden_options",
            title = c.getString(R.string.unlock_some_hidden_options),
        )
    }
}
