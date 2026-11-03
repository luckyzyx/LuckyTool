package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 状态栏通知移除页：顶栏通知、VPN、高性能模式等常驻通知的移除开关，写入
 * com.android.systemui、com.oplus.battery、com.coloros.phonemanager。
 * 每项都显式声明键、默认值与条件可见性（osCode = getOSVersionCode 判 OS 版本），摘要用多行提示。
 * remove_smart_rapid_charging_notification 不可见（isVisible = false）且从未展示，故未收录。
 */
object StatusBarNotifyRemovalPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_notify_removal",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui", "com.oplus.battery", "com.coloros.phonemanager"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        switch(
            key = "remove_statusbar_top_notification",
            title = c.getString(R.string.remove_statusbar_top_notification),
            summary = arraySummaryLine(
                c.getString(R.string.remove_statusbar_top_notification_summary),
                c.getString(R.string.need_restart_system)
            ),
        )
        switch(
            key = "remove_vpn_active_notification",
            title = c.getString(R.string.remove_vpn_active_notification),
            summary = arraySummaryLine(
                c.getString(R.string.remove_vpn_active_notification_summary),
                c.getString(R.string.need_restart_system)
            ),
        )
        switch(
            key = "remove_statusbar_devmode",
            title = c.getString(R.string.remove_statusbar_devmode),
            summary = arraySummaryLine(c.getString(R.string.need_restart_system)),
        )
        switch(
            key = "remove_charging_completed",
            title = c.getString(R.string.remove_charging_completed),
        )
        switch(
            key = "remove_flashlight_open_notification",
            title = c.getString(R.string.remove_flashlight_open_notification),
        )
        switch(
            key = "remove_app_high_battery_consumption_warning",
            title = c.getString(R.string.remove_app_high_battery_consumption_warning),
            summary = c.getString(R.string.remove_app_high_battery_consumption_warning_summary),
        )
        switch(
            key = "remove_high_performance_mode_notifications",
            title = c.getString(R.string.remove_high_performance_mode_notifications),
        )
        switch(
            key = "remove_do_not_disturb_mode_notification",
            title = c.getString(R.string.remove_do_not_disturb_mode_notification),
        )
        switch(
            key = "remove_hotspot_power_consumption_notification",
            title = c.getString(R.string.remove_hotspot_power_consumption_notification),
            summary = arraySummaryLine(
                c.getString(R.string.remove_hotspot_power_consumption_notification_summary),
                c.getString(R.string.need_restart_system)
            ),
        )
        // remove_smart_rapid_charging_notification 不可见且从未展示，故未收录
        switch(
            key = "remove_notifications_for_mute_notifications",
            title = c.getString(R.string.remove_notifications_for_mute_notifications),
        )
        if (osCode < 40) {
            switch(
                key = "remove_gt_mode_notification",
                title = c.getString(R.string.remove_gt_mode_notification),
            )
        }
    }
}
