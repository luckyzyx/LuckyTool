package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 悬浮窗/对话框页：重复悬浮窗、低电量、USB 连接、无障碍等系统对话框的屏蔽开关
 * （写入 com.android.systemui 与 com.oplus.exsystemservice，部分条目按 getOSVersionCode 门控）。
 */
object DialogRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "dialog_related",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui", "com.oplus.exsystemservice"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 26) {
            switch(
                key = "disable_duplicate_floating_window",
                title = c.getString(R.string.disable_duplicate_floating_window),
                summary = c.getString(R.string.disable_duplicate_floating_window_summary),
            )
        }
        switch(
            key = "remove_low_battery_dialog_warning",
            title = c.getString(R.string.remove_low_battery_dialog_warning),
            summary = c.getString(R.string.remove_low_battery_dialog_warning_summary),
        )
        switch(
            key = "remove_usb_connect_dialog",
            title = c.getString(R.string.remove_usb_connect_dialog),
            summary = c.getString(R.string.remove_usb_connect_dialog_summary),
        )
        if (SDK >= A13) {
            switch(
                key = "remove_access_device_log_dialog",
                title = c.getString(R.string.remove_access_device_log_dialog),
            )
        }
        //27..34 走 SystemUI 侧的 zoom 状态机；35+ 走 system_server 侧的后台保活（挂起即上虚拟屏）
        if (getOSVersionCode >= 27) {
            switch(
                key = "run_floating_window_tasks_in_foreground",
                title = c.getString(R.string.run_floating_window_tasks_in_foreground),
                summary = c.getString(R.string.need_restart_system),
                notify = true,
            )
        }
        switch(
            key = "remove_start_recording_or_casting_dialog",
            title = c.getString(R.string.remove_start_recording_or_casting_dialog),
        )
        switch(
            key = "force_show_toast_icon",
            title = c.getString(R.string.force_show_toast_icon),
        )
        if (getOSVersionCode >= 38) {
            switch(
                key = "disable_accessibility_warning_dialog",
                title = c.getString(R.string.disable_accessibility_warning_dialog),
                summary = "BottomSheet + Dialog",
            )
        }
    }
}
