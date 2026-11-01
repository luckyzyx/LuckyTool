package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName

/**
 * DirectUI 页：去掉触摸操作里的应用推荐卡片。
 * 仅在 com.coloros.directui 与 com.coloros.colordirectservice 都装有时出现（见 spec.isVisible）。
 */
object OplusDirectUIPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_direct_ui",
        prefsName = ModulePrefs,
        packName = "com.coloros.directui",
        scopes = arrayOf("com.coloros.directui", "com.coloros.colordirectservice"),
        restartEnabled = true,
        isVisible = {
            checkPackName("com.coloros.directui") && checkPackName("com.coloros.colordirectservice")
        },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_touch_app_recommend_card",
            title = c.getString(R.string.remove_app_recommend_card),
        )
    }
}
