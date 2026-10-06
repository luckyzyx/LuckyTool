package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK

/**
 * 旧 ui.fragment.scopes.apps.OplusTeleService 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusTeleServicePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_tele_service",
        prefsName = ModulePrefs,
        packName = "com.android.phone",
        scopes = arrayOf("com.android.phone", "com.android.incallui"),
        restartEnabled = false,
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
