package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 状态栏通知移除页（旧 ui.fragment.scopes.statusbar.StatusBarNotifyRemoval 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（osCode = getOSVersionCode）、summary 多行提示。
 * 旧 remove_smart_rapid_charging_notification 条目 isVisible = false 从未展示，按规则丢弃。
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
        // 旧 remove_smart_rapid_charging_notification：isVisible = false，从未展示，按规则丢弃
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
