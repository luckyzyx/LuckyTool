package com.luckyzyx.luckytool.ui.fragment.scopes.statusbar

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class StatusBarNotifyRemoval : BaseScopePreferenceFeagment() {
    override val scopes =
        arrayOf("com.android.systemui", "com.oplus.battery", "com.coloros.phonemanager")

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.statusBarNotifyRemoval

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            title = getString(R.string.RemoveStatusBarNotifications)
            summary = arraySummaryDot(
                getString(R.string.remove_statusbar_top_notification),
                getString(R.string.remove_statusbar_devmode)
            )
            key = "RemoveStatusBarNotifications"
            isIconSpaceReserved = false
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_statusbar_top_notification)
                summary = arraySummaryLine(
                    getString(R.string.remove_statusbar_top_notification_summary),
                    getString(R.string.need_restart_system)
                )
                key = "remove_statusbar_top_notification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_vpn_active_notification)
                summary = arraySummaryLine(
                    getString(R.string.remove_vpn_active_notification_summary),
                    getString(R.string.need_restart_system)
                )
                key = "remove_vpn_active_notification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_statusbar_devmode)
                summary = arraySummaryLine(
                    getString(R.string.need_restart_system)
                )
                key = "remove_statusbar_devmode"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_charging_completed)
                key = "remove_charging_completed"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_flashlight_open_notification)
                key = "remove_flashlight_open_notification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_app_high_battery_consumption_warning)
                summary = getString(R.string.remove_app_high_battery_consumption_warning_summary)
                key = "remove_app_high_battery_consumption_warning"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_high_performance_mode_notifications)
                key = "remove_high_performance_mode_notifications"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_do_not_disturb_mode_notification)
                key = "remove_do_not_disturb_mode_notification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_hotspot_power_consumption_notification)
                summary = arraySummaryLine(
                    getString(R.string.remove_hotspot_power_consumption_notification_summary),
                    getString(R.string.need_restart_system)
                )
                key = "remove_hotspot_power_consumption_notification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_smart_rapid_charging_notification)
                key = "remove_smart_rapid_charging_notification"
                setDefaultValue(false)
                isVisible = false
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_notifications_for_mute_notifications)
                key = "remove_notifications_for_mute_notifications"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_gt_mode_notification)
                key = "remove_gt_mode_notification"
                setDefaultValue(false)
                isVisible = osCode < 40
                isIconSpaceReserved = false
            })
        }
    }
}