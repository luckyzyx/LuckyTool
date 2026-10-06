package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 状态栏网速页（旧 ui.fragment.scopes.statusbar.StatusBarNetWorkSpeed 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调。
 */
object StatusBarNetWorkSpeedPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_network_speed",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "set_network_speed",
            title = c.getString(R.string.set_network_speed),
            notify = true,
        )
        list(
            key = "statusbar_network_layout",
            title = c.getString(R.string.statusbar_network_layout),
            entries = c.resources.getStringArray(R.array.statusbar_network_layout_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            notify = true,
            onChange = { restart?.invoke() },
        )
        switch(
            key = "statusbar_network_user_typeface",
            title = c.getString(R.string.use_user_typeface),
            notify = true,
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("statusbar_network_user_typeface")) {
            switch(
                key = "statusbar_network_use_bold_font_style",
                title = c.getString(R.string.use_bold_font_style),
                notify = true,
            )
        }
        if (state.getString("statusbar_network_layout", "0") != "0") {
            switch(
                key = "statusbar_network_no_second",
                title = c.getString(R.string.statusbar_network_no_second),
                notify = true,
            )
            switch(
                key = "statusbar_network_no_unit",
                title = c.getString(R.string.statusbar_network_no_unit),
                notify = true,
            )
            switch(
                key = "statusbar_network_no_space",
                title = c.getString(R.string.statusbar_network_no_space),
                notify = true,
            )
            slider(
                key = "set_network_speed_font_size",
                title = c.getString(R.string.set_network_speed_font_size),
                valueRange = 0..10,
                default = 7,
                notify = true,
            )
            slider(
                key = "set_network_speed_padding_bottom",
                title = c.getString(R.string.set_network_speed_padding_bottom),
                valueRange = 0..6,
                default = 0,
                notify = true,
            )
            if (state.getString("statusbar_network_layout", "0") == "2") {
                slider(
                    key = "set_network_speed_double_row_spacing",
                    title = c.getString(R.string.set_network_speed_double_row_spacing),
                    valueRange = -1..6,
                    default = -1,
                    notify = true,
                )
            }
        }
    }
}
