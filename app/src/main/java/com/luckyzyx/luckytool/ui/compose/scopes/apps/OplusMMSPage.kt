package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK

/**
 * 短信页（旧 ui.fragment.scopes.apps.OplusMMS 的 Compose 等价物）。
 */
object OplusMMSPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_mms",
        prefsName = ModulePrefs,
        packName = "com.android.mms",
        scopes = arrayOf("com.android.mms"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (SDK >= A13) {
            switch(
                key = "remove_verification_code_floating_window",
                title = c.getString(R.string.remove_verification_code_floating_window),
            )
        }
        switch(
            key = "remove_mms_bottom_input_box_menu",
            title = c.getString(R.string.remove_mms_bottom_input_box_menu),
        )
        switch(
            key = "remove_mms_card_marketing_button",
            title = c.getString(R.string.remove_mms_card_marketing_button),
        )
    }
}
