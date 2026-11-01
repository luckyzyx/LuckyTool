package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 状态栏网速页：网速刷新间隔、布局与字号/上下间距取值（写入 com.android.systemui）。
 * 每项都显式声明键与默认值；标了 notify 的项写值后经
 * sendPrefsValue("com.android.systemui") 通知宿主。
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
        // 值为 0.1 秒为单位的整数：-1 不开启，0 由 hook 按下限 0.1 秒执行
        // 旧版 set_network_speed 开关开启时回落为 1.0 秒，避免升级后悄悄关闭
        val legacyEnable = state.getBoolean("set_network_speed", false)
        slider(
            key = "set_network_speed_interval",
            title = c.getString(R.string.set_network_speed_interval),
            valueRange = -1..50,
            default = if (legacyEnable) 10 else -1,
            summary = c.getString(R.string.set_network_speed_interval_summary),
            valueLabel = { value ->
                if (value < 0) c.getString(R.string.close)
                else c.getString(
                    R.string.set_network_speed_interval_value, "%.1f".format(value / 10f)
                )
            },
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
        )
        switch(
            key = "statusbar_network_user_typeface",
            title = c.getString(R.string.use_user_typeface),
            notify = true,
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
