package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * 状态栏通知页（旧 ui.fragment.scopes.statusbar.StatusBarNotify 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue，目标包含 android）、restart 回调。
 */
object StatusBarNotifyPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_notify",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf(
            "com.android.systemui",
            "com.oplus.battery",
            "com.coloros.phonemanager",
            "com.oplus.notificationmanager",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 状态栏通知移除子页入口
        page(c.getString(R.string.RemoveStatusBarNotifications), "statusBarNotifyRemoval")
        if (osCode <= 30) {
            switch(
                key = "allow_long_press_notification_modifiable",
                title = c.getString(R.string.allow_long_press_notification_modifiable),
            )
        }
        switch(
            key = "remove_notification_manager_limit",
            title = c.getString(R.string.remove_notification_manager_limit),
        )
        switch(
            key = "disable_high_volume_warning_notifications",
            title = c.getString(R.string.disable_high_volume_warning_notifications),
        )
        if (osCode < 34) {
            switch(
                key = "remove_small_window_reply_whitelist",
                title = c.getString(R.string.remove_small_window_reply_whitelist),
                onChange = { restart?.invoke() },
            )
            if (state.getBoolean("remove_small_window_reply_whitelist")) {
                custom(
                    key = "set_small_window_reply_blacklist_list",
                    title = c.getString(R.string.set_small_window_reply_blacklist),
                ) {
                    val blacklist by state.stringSetFlow("set_small_window_reply_blacklist_list")
                        .collectAsStateWithLifecycle()
                    var showPicker by remember { mutableStateOf(false) }
                    PrefRowInset {
                        PrefRow(
                            title = c.getString(R.string.set_small_window_reply_blacklist),
                            summary = arraySummaryLine(
                                c.getString(R.string.set_small_window_reply_blacklist_message),
                                blacklist.toString(),
                            ),
                            onClick = { showPicker = true },
                        )
                    }
                    if (showPicker) {
                        AppPickerDialog(
                            title = c.getString(R.string.set_small_window_reply_blacklist),
                            multiMode = true,
                            showSystemApps = true,
                            enabledList = blacklist,
                            onDismiss = { showPicker = false },
                            onConfirm = { apps ->
                                val set = apps.map { it.packageName }.toSet()
                                state.set("set_small_window_reply_blacklist_list", set)
                                sendValue("set_small_window_reply_blacklist_list", set)
                                restart?.invoke()
                            },
                        )
                    }
                }
            }
        }
        if (osCode >= 33) {
            switch(
                key = "remove_notification_pin_number_limit",
                title = c.getString(R.string.remove_notification_pin_number_limit),
            )
        }
        switch(
            key = "enable_keep_notification_when_app_stop",
            title = c.getString(R.string.enable_keep_notification_when_app_stop),
            summary = c.getString(R.string.need_restart_system),
            onChange = { v ->
                c.sendPrefsValue("android", "enable_keep_notification_when_app_stop", v)
            },
        )
        switch(
            key = "enable_global_notification_simple_banner_mode",
            title = c.getString(R.string.enable_global_notification_simple_banner_mode),
            onChange = { v ->
                c.sendPrefsValue("android", "enable_global_notification_simple_banner_mode", v)
            },
        )
    }
}
