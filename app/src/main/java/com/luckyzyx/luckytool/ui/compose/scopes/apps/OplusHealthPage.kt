package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 健康页：去掉 Root 检测提示对话框的开关。
 */
object OplusHealthPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_health",
        prefsName = ModulePrefs,
        packName = "com.heytap.health",
        scopes = arrayOf("com.heytap.health"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_health_root_check_dialog",
            title = c.getString(R.string.remove_health_root_check_dialog),
        )
    }
}
