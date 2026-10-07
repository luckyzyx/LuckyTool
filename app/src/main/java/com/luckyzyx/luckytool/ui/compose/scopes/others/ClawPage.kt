package com.luckyzyx.luckytool.ui.compose.scopes.others

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 小布空间（Claw）页（旧 ui.fragment.scopes.others.Claw 的 Compose 等价物）。
 */
object ClawPage {

    val spec = ScopePageSpec(
        pageKey = "claw",
        prefsName = ModulePrefs,
        packName = "com.oplus.claw",
        scopes = arrayOf("com.oplus.claw"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_root_detection",
            title = c.getString(R.string.remove_root_detection),
            summary = c.getString(R.string.remove_root_detection_summary),
        )
    }
}
