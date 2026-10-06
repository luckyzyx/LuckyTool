package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 状态栏电池页（旧 ui.fragment.scopes.statusbar.StatusBarBattery 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调。
 */
object StatusBarBatteryPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_battery",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        switch(
            key = "remove_statusbar_battery_percent",
            title = c.getString(R.string.remove_statusbar_battery_percent),
        )
        switch(
            key = "statusbar_power_user_typeface",
            title = c.getString(R.string.use_user_typeface),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("statusbar_power_user_typeface")) {
            switch(
                key = "statusbar_power_use_bold_font_style",
                title = c.getString(R.string.use_bold_font_style),
            )
            slider(
                key = "statusbar_power_font_size",
                title = c.getString(R.string.statusbar_power_font_size),
                valueRange = 0..15,
                default = 0,
                summary = c.getString(R.string.statusbar_clock_if_zero_summary),
            )
        }
        if (osCode < 33) {
            switch(
                key = "statusbar_power_apply_to_battery_icon",
                title = c.getString(R.string.statusbar_power_apply_to_battery_icon),
            )
        }
        // 状态栏电池通知
        if (SDK >= A12) {
            category(c.getString(R.string.StatusBarBatteryNotify))
            list(
                key = "battery_information_display_mode",
                title = c.getString(R.string.battery_information_display_mode),
                entries = c.resources.getStringArray(R.array.statusbar_battery_information_notify_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = arraySummaryLine(
                    c.getString(R.string.current_mode) + ": %s",
                    c.getString(R.string.battery_information_display_mode_summary),
                ),
                notify = true,
                onChange = { restart?.invoke() },
            )
            if (state.getString("battery_information_display_mode", "0") != "0") {
                switch(
                    key = "battery_information_show_charge_info",
                    title = c.getString(R.string.battery_information_show_charge),
                    summary = c.getString(R.string.battery_information_show_charge_summary),
                    notify = true,
                )
                list(
                    key = "battery_information_voltage_display_mode",
                    title = c.getString(R.string.battery_information_voltage_display_mode),
                    entries = c.resources.getStringArray(R.array.battery_information_voltage_display_mode_entries),
                    entryValues = arrayOf("0", "1", "2"),
                    default = "0",
                    summary = arraySummaryLine(c.getString(R.string.current_mode) + ": %s"),
                    notify = true,
                )
                switch(
                    key = "battery_information_show_battery_health",
                    title = c.getString(R.string.battery_information_show_battery_health),
                    notify = true,
                )
                switch(
                    key = "battery_information_always_show_positive_current",
                    title = c.getString(R.string.battery_information_always_show_positive_current),
                    notify = true,
                )
                switch(
                    key = "battery_information_show_simple_mode",
                    title = c.getString(R.string.battery_information_show_simple_mode),
                    notify = true,
                )
                switch(
                    key = "battery_information_show_update_time",
                    title = c.getString(R.string.battery_information_show_update_time),
                    summary = c.getString(R.string.battery_information_show_update_time_summary),
                    notify = true,
                )
                slider(
                    key = "battery_information_custom_font_size",
                    title = c.getString(R.string.battery_information_custom_font_size),
                    valueRange = 11..20,
                    default = 11,
                    notify = true,
                )
            }
        }
    }
}
