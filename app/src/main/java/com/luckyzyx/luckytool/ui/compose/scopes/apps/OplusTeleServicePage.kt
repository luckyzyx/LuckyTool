package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.checkPackName

/**
 * 电话服务页：强制显示 5G 开关、VoLTE 与首选网络类型，以及声音封印通话的开关。
 * 整页要求 SDK >= Android 13 且装有 com.android.phone（见 spec.isVisible）；
 * 前三个开关要求 SDK >= A13，声音封印通话要求 SDK >= A14。
 */
object OplusTeleServicePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_tele_service",
        prefsName = ModulePrefs,
        packName = "com.android.phone",
        scopes = arrayOf("com.android.phone", "com.android.incallui"),
        restartEnabled = false,
        isVisible = { SDK >= A13 && checkPackName("com.android.phone") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (SDK >= A13) {
            switch(
                key = "force_display_five_g_switch",
                title = c.getString(R.string.force_display_five_g_switch),
            )
            switch(
                key = "force_display_volte_calls",
                title = c.getString(R.string.force_display_volte_calls),
            )
            switch(
                key = "force_display_preferred_network_type",
                title = c.getString(R.string.force_display_preferred_network_type),
            )
        }
        if (SDK >= A14) {
            switch(
                key = "enable_sound_sealed_call",
                title = c.getString(R.string.enable_sound_sealed_call),
            )
        }
    }
}
