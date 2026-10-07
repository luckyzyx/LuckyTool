package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * 锁屏相关页（旧 ui.fragment.scopes.related.LockScreenRelated 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调。
 */
object LockScreenRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "lock_screen",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf(
            "com.android.systemui",
            "com.oplus.notificationmanager",
            "com.oplus.keyguard.clock.base",
            "com.oplus.keyguard.personality.clocks",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 状态栏组件
        category(c.getString(R.string.LockScreenStatusBar))
        switch(
            key = "hide_lock_screen_status_bar_display",
            title = c.getString(R.string.hide_lock_screen_status_bar_display),
        )
        switch(
            key = "remove_statusbar_carriers",
            title = c.getString(R.string.remove_statusbar_carriers),
        )
        editText(
            key = "statusbar_custom_carrier_display_text",
            title = c.getString(R.string.statusbar_custom_carrier_display_text),
            dialogMessage = c.getString(R.string.statusbar_custom_carrier_display_text),
            default = "",
        )
        switch(
            key = "statusbar_carriers_use_user_typeface",
            title = c.getString(R.string.statusbar_carriers_use_user_typeface),
        )
        // 时钟组件
        category(c.getString(R.string.LockScreenClockComponent))
        switch(
            key = "remove_lock_screen_clock_component",
            title = c.getString(R.string.remove_lock_screen_clock_component),
            notify = true,
            onChange = { restart?.invoke() },
        )
        if (!state.getBoolean("remove_lock_screen_clock_component")) {
            list(
                key = "lock_screen_clock_redone_mode",
                title = c.getString(R.string.lock_screen_clock_redone_mode),
                entries = c.resources.getStringArray(R.array.statusbar_control_center_clock_red_one_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
                notify = true,
                onChange = { c.sendPrefsValue("com.oplus.keyguard.clock.base", "lock_screen_clock_redone_mode", it) },
            )
            if (osCode < 34) {
                switch(
                    key = "apply_lock_screen_dual_clock_redone",
                    title = c.getString(R.string.apply_lock_screen_dual_clock_redone),
                    notify = true,
                )
                list(
                    key = "lock_screen_custom_clock_component_style",
                    title = c.getString(R.string.lock_screen_custom_clock_component_style),
                    entries = c.resources.getStringArray(R.array.lock_screen_custom_clock_component_style_entries),
                    entryValues = arrayOf("0", "1", "2"),
                    default = "0",
                    summary = c.getString(R.string.current_mode) + ": %s",
                    onChange = { restart?.invoke() },
                )
                if (state.getString("lock_screen_custom_clock_component_style", "0") == "1") {
                    switch(
                        key = "force_display_clock_style_options",
                        title = c.getString(R.string.force_display_clock_style_options),
                        summary = c.getString(R.string.force_display_clock_style_options_summary),
                    )
                }
                switch(
                    key = "set_lock_screen_centered",
                    title = c.getString(R.string.set_lock_screen_centered),
                    summary = c.getString(R.string.set_lock_screen_centered_summary),
                )
                switch(
                    key = "lock_screen_clock_use_user_typeface",
                    title = c.getString(R.string.lock_screen_clock_use_user_typeface),
                )
            }
        }
        // 充电组件
        category(c.getString(R.string.LockScreenChargingComponent))
        if (SDK == A13) {
            list(
                key = "set_lock_screen_warp_charging_style",
                title = c.getString(R.string.set_lock_screen_warp_charging_style),
                entries = c.resources.getStringArray(R.array.set_lock_screen_warp_charging_style_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
                notify = true,
                onChange = { restart?.invoke() },
            )
        }
        list(
            key = "set_lock_screen_charging_text_logo_style",
            title = c.getString(R.string.set_lock_screen_charging_text_logo_style),
            entries = c.resources.getStringArray(R.array.set_lock_screen_charging_text_logo_style_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            notify = true,
            onChange = { restart?.invoke() },
        )
        if (SDK >= A13 && state.getString("set_lock_screen_charging_text_logo_style") != "2") {
            switch(
                key = "lock_screen_show_real_charging_technology",
                title = c.getString(R.string.lock_screen_show_real_charging_technology),
                notify = true,
            )
        }
        if (osCode >= 34) {
            switch(
                key = "replace_charging_technology_drawing_style",
                title = c.getString(R.string.replace_charging_technology_drawing_style),
                notify = true,
            )
        }
        if (SDK >= A13) {
            switch(
                key = "force_lock_screen_charging_show_wattage",
                title = c.getString(R.string.force_lock_screen_charging_show_wattage),
                notify = true,
            )
        }
        switch(
            key = "lock_screen_charging_use_user_typeface",
            title = c.getString(R.string.lock_screen_charging_use_user_typeface),
            notify = true,
        )
        if (osCode in 27..29) {
            list(
                key = "set_full_screen_charging_animation_mode",
                title = c.getString(R.string.set_full_screen_charging_animation_mode),
                entries = c.resources.getStringArray(R.array.set_full_screen_charging_animation_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = arraySummaryLine(
                    c.getString(R.string.current_mode) + ": %s",
                    c.getString(R.string.need_restart_scope),
                ),
                notify = true,
            )
        }
        // 锁屏按钮
        category(c.getString(R.string.LockScreenButton))
        switch(
            key = "remove_top_lock_screen_icon",
            title = c.getString(R.string.remove_top_lock_screen_icon),
        )
        if (osCode < 37) {
            switch(
                key = "remove_lock_screen_bottom_left_button",
                title = c.getString(R.string.remove_lock_screen_bottom_left_button),
                notify = true,
                onChange = { restart?.invoke() },
            )
        }
        if (SDK < A14) {
            switch(
                key = "lock_screen_bottom_left_button_replace_with_flashlight",
                title = c.getString(R.string.lock_screen_bottom_left_button_replace_with_flashlight),
                notify = true,
            )
        }
        switch(
            key = "lock_screen_switch_flashlight_auto_close_screen",
            title = c.getString(R.string.lock_screen_switch_flashlight_auto_close_screen),
            notify = true,
        )
        if (osCode < 37) {
            switch(
                key = "remove_lock_screen_bottom_right_camera",
                title = c.getString(R.string.remove_lock_screen_bottom_right_camera),
                notify = true,
            )
        }
        if (osCode < 33) {
            switch(
                key = "remove_lock_screen_close_notification_button",
                title = c.getString(R.string.remove_lock_screen_close_notification_button),
            )
        }
        if (SDK >= A13) {
            switch(
                key = "remove_lock_screen_bottom_sos_button",
                title = c.getString(R.string.remove_lock_screen_bottom_sos_button),
                summary = c.getString(R.string.remove_lock_screen_bottom_sos_button_summary),
            )
        }
        // 锁屏事件
        category(c.getString(R.string.LockScreenEvent))
        switch(
            key = "auto_wake_up_face_unlock_notification",
            title = c.getString(R.string.auto_wake_up_face_unlock_notification),
        )
        switch(
            key = "remove_72hour_password_verification",
            title = c.getString(R.string.remove_72hour_password_verification),
            summary = c.getString(R.string.remove_72hour_password_verification_summary),
        )
    }
}
