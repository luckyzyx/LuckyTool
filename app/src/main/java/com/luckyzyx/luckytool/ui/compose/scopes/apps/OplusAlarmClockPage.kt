package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 闹钟页（旧 ui.fragment.scopes.apps.OplusAlarmClock 的 Compose 等价物）。
 */
object OplusAlarmClockPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_alarm_clock",
        prefsName = ModulePrefs,
        packName = "com.coloros.alarmclock",
        scopes = arrayOf("com.coloros.alarmclock"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        list(
            key = "alarmclock_widget_redone_mode",
            title = c.getString(R.string.alarmclock_widget_redone_mode),
            entries = c.resources.getStringArray(
                R.array.statusbar_control_center_clock_red_one_mode_entries
            ),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            notify = true,
        )
    }
}
