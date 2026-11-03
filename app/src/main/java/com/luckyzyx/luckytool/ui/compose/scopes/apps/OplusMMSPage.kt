package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.checkPackName

/**
 * 短信页：去掉验证码悬浮窗、输入框菜单与卡片营销按钮的开关。
 * 整页要求 SDK >= Android 13 且装有 com.android.mms（见 spec.isVisible），验证码悬浮窗项也在同一 SDK 条件下渲染。
 */
object OplusMMSPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_mms",
        prefsName = ModulePrefs,
        packName = "com.android.mms",
        scopes = arrayOf("com.android.mms"),
        restartEnabled = true,
        isVisible = { SDK >= A13 && checkPackName("com.android.mms") },
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
