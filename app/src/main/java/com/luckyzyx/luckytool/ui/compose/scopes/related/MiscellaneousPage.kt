package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 杂项页（旧 ui.fragment.scopes.related.Miscellaneous 的 Compose 等价物）。
 * 顶部三条为旧 add(DialogRelated/FingerPrintRelated/SoundRelated().getRootPreference) 的页面入口。
 */
object MiscellaneousPage {

    val spec = ScopePageSpec(
        pageKey = "miscellaneous",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf(
            "com.android.systemui",
            "com.android.externalstorage",
            "com.oplus.exsystemservice",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        page(
            title = c.getString(R.string.FloatingWindowDialogRelated),
            target = "dialogRelated",
            summary = arraySummaryDot(
                c.getString(R.string.remove_low_battery_dialog_warning_summary),
                c.getString(R.string.disable_headphone_high_volume_warning),
            ),
        )
        page(
            title = c.getString(R.string.FingerPrintRelated),
            target = "fingerPrintRelated",
            summary = arraySummaryDot(
                c.getString(R.string.remove_fingerprint_icon),
                c.getString(R.string.replace_fingerprint_icon_switch),
            ),
        )
        page(
            title = c.getString(R.string.SoundRelated),
            target = "soundRelated",
            summary = arraySummaryDot(
                c.getString(R.string.media_volume_level),
                c.getString(R.string.minimum_volume_level_can_be_zero),
            ),
        )
        if (SDK >= A12) {
            switch(
                key = "show_charging_ripple",
                title = c.getString(R.string.show_charging_ripple),
                summary = c.getString(R.string.show_charging_ripple_summary),
            )
        }
        if (getOSVersionCode < 30) {
            switch(
                key = "disable_otg_auto_off",
                title = c.getString(R.string.disable_otg_auto_off),
                summary = c.getString(R.string.disable_otg_auto_off_summary),
            )
        }
        switch(
            key = "remove_storage_limit",
            title = c.getString(R.string.remove_storage_limit),
            summary = c.getString(R.string.remove_storage_limit_summary),
        )
        switch(
            key = "force_enable_systemui_blur_feature",
            title = c.getString(R.string.force_enable_systemui_blur_feature),
        )
        if (SDK >= A14) {
            switch(
                key = "show_manual_lock_button_power_menu",
                title = c.getString(R.string.show_manual_lock_button_power_menu),
            )
        }
        if (SDK >= A13) {
            switch(
                key = "remove_power_menu_sos_button",
                title = c.getString(R.string.remove_power_menu_sos_button),
            )
        }
    }
}
