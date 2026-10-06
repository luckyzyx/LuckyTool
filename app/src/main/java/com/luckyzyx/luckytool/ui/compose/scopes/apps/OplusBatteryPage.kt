package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK

/**
 * 电池页（旧 ui.fragment.scopes.apps.OplusBattery 的 Compose 等价物）。
 */
object OplusBatteryPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_battery",
        prefsName = ModulePrefs,
        packName = "com.oplus.battery",
        scopes = arrayOf("com.oplus.battery"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (SDK >= A13) {
            switch(
                key = "open_battery_health",
                title = c.getString(R.string.open_battery_health),
                summary = c.getString(R.string.open_battery_health_summary),
                onChange = { restart?.invoke() },
            )
            if (state.getBoolean("open_battery_health", false)) {
                editText(
                    key = "customize_battery_health_data_percentage",
                    title = c.getString(R.string.customize_battery_health_data_percentage),
                    dialogMessage = c.getString(R.string.customize_battery_health_data_percentage),
                    default = "",
                )
                switch(
                    key = "display_module_calculates_battery_health_data",
                    title = c.getString(R.string.display_module_calculates_battery_health_data),
                    summary = c.getString(R.string.display_module_calculates_battery_health_data_summary),
                )
            }
            switch(
                key = "enable_stop_charging_at_80",
                title = c.getString(R.string.enable_stop_charging_at_80),
            )
            switch(
                key = "show_phone_usage_screen_time",
                title = c.getString(R.string.show_phone_usage_screen_time),
            )
            switch(
                key = "open_screen_power_save",
                title = c.getString(R.string.open_screen_power_save),
                summary = c.getString(R.string.open_screen_power_save_summary),
            )
        }
        switch(
            key = "remove_battery_temperature_control",
            title = c.getString(R.string.remove_battery_temperature_control),
            summary = c.getString(R.string.remove_battery_temperature_control_summary),
        )
        category(c.getString(R.string.BatteryOptimization))
        switch(
            key = "remove_battery_restrict_plugin",
            title = c.getString(R.string.remove_battery_restrict_plugin),
        )
        switch(
            key = "restore_default_battery_optimization_whitelist",
            title = c.getString(R.string.restore_default_battery_optimization_whitelist),
        )
    }
}
