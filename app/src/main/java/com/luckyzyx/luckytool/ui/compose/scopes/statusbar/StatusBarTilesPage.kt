package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 状态栏磁贴页（旧 ui.fragment.scopes.statusbar.StatusBarTiles 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调、跨键副作用。
 */
object StatusBarTilesPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_tiles",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 自定义 NFC 延时关闭
        switch(
            key = "enable_nfc_delay_shutdown",
            title = c.getString(R.string.enable_nfc_delay_shutdown),
            notify = true,
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_nfc_delay_shutdown")) {
            editText(
                key = "custom_nfc_delay_shutdown_time",
                title = c.getString(R.string.custom_nfc_delay_shutdown_time),
                dialogMessage = """
                 1s / 1S -> 1秒 / 1 Second
                 1m / 1M -> 1分钟 / 1 Minute
                 1h / 1H -> 1小时 / 1 Hour
                """.trimIndent(),
                default = "10M",
                notify = true,
            )
        }
        // 静音或振动
        switch(
            key = "force_display_of_ringing_status_toggle_tiles",
            title = c.getString(R.string.force_display_of_ringing_status_toggle_tiles),
        )
        // 设备控制器
        switch(
            key = "force_display_of_device_controls_tiles",
            title = c.getString(R.string.force_display_of_device_controls_tiles),
        )
        // 特殊磁贴
        if (SDK >= A13) {
            category(c.getString(R.string.SpecialTiles))
            list(
                key = "set_media_player_display_mode",
                title = c.getString(R.string.set_media_player_display_mode),
                entries = c.resources.getStringArray(R.array.set_media_player_display_mode_entries),
                entryValues = arrayOf("0", "1", "2", "3"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
                notify = true,
                onChange = { restart?.invoke() },
            )
            if (state.getString("set_media_player_display_mode", "0") != "0") {
                if (SDK >= A14) {
                    switch(
                        key = "auto_expand_tile_rows_horizontal",
                        title = c.getString(R.string.auto_expand_tile_rows_horizontal),
                        summary = arraySummaryLine(
                            c.getString(R.string.auto_expand_tile_rows_horizontal_summary),
                            c.getString(R.string.auto_expand_tile_rows_horizontal_summary_2),
                        ),
                        notify = true,
                        onChange = { v ->
                            if (v) {
                                state.set("control_center_custom_gaps_for_special_tile", true)
                                state.set("control_center_tile_enable", true)
                            }
                            restart?.invoke()
                        },
                    )
                }
                if (state.getString("set_media_player_display_mode", "0") == "1") {
                    switch(
                        key = "force_enable_media_toggle_button",
                        title = c.getString(R.string.force_enable_media_toggle_button),
                    )
                }
                if (osCode >= 27) {
                    switch(
                        key = "control_center_custom_gaps_for_special_tile",
                        title = c.getString(R.string.control_center_custom_gaps_for_special_tile),
                        summary = if (state.getBoolean("auto_expand_tile_rows_horizontal")) {
                            c.getString(R.string.control_center_custom_gaps_for_special_tile_summary)
                        } else {
                            null
                        },
                        notify = true,
                        onChange = { v ->
                            if (!v) state.set("auto_expand_tile_rows_horizontal", false)
                            restart?.invoke()
                        },
                    )
                    if (state.getBoolean("control_center_custom_gaps_for_special_tile")) {
                        slider(
                            key = "control_center_special_tile_top_gap",
                            title = c.getString(R.string.control_center_special_tile_top_gap),
                            valueRange = 0..20,
                            default = 10,
                            notify = true,
                        )
                        slider(
                            key = "control_center_special_tile_bottom_gap",
                            title = c.getString(R.string.control_center_special_tile_bottom_gap),
                            valueRange = 0..20,
                            default = 0,
                            notify = true,
                        )
                        if (osCode >= 30) {
                            switch(
                                key = "decrease_horizontal_brightness_bar_top_gap",
                                title = c.getString(R.string.decrease_horizontal_brightness_bar_top_gap),
                            )
                        }
                    }
                }
            }
        }
        if (SDK >= A13) {
            // 磁贴长按事件
            category(c.getString(R.string.TileLongClickEvent))
            switch(
                key = "restore_some_tile_long_press_event",
                title = c.getString(R.string.restore_some_tile_long_press_event),
                summary = c.getString(R.string.restore_some_tile_long_press_event_summary),
            )
        }
        // 磁贴样式相关
        if (osCode in 27..33) {
            category(c.getString(R.string.TileStyleRelated))
            slider(
                key = "custom_tile_background_transparency",
                title = c.getString(R.string.custom_tile_background_transparency),
                valueRange = -1..10,
                default = -1,
            )
        }
        // 磁贴布局相关
        category(c.getString(R.string.TileLayoutRelated))
        if (osCode >= 26) {
            switch(
                key = "fix_tile_align_both_sides",
                title = c.getString(R.string.fix_tile_align_both_sides),
                summary = c.getString(R.string.fix_tile_align_both_sides_summary),
            )
        }
        if (SDK >= A13) {
            switch(
                key = "restore_page_layout_row_count_for_edit_tiles",
                title = c.getString(R.string.restore_page_layout_row_count_for_edit_tiles),
            )
        }
        if (osCode < 37) {
            switch(
                key = "remove_control_center_tile_count_limit",
                title = c.getString(R.string.remove_control_center_tile_count_limit),
            )
        }
        if (osCode < 40) {
            switch(
                key = "control_center_tile_enable",
                title = c.getString(R.string.control_center_tile_enable),
                summary = c.getString(R.string.classic_control_center_mode_only),
                onChange = { v ->
                    if (!v) state.set("auto_expand_tile_rows_horizontal", false)
                    restart?.invoke()
                },
            )
        }
        if (state.getBoolean("control_center_tile_enable")) {
            if (SDK < A13) {
                slider(
                    key = "tile_unexpanded_columns_vertical",
                    title = c.getString(R.string.tile_unexpanded_columns_vertical),
                    valueRange = 1..6,
                    default = 6,
                )
                slider(
                    key = "tile_unexpanded_columns_horizontal",
                    title = c.getString(R.string.tile_unexpanded_columns_horizontal),
                    valueRange = 1..8,
                    default = 6,
                )
                slider(
                    key = "tile_expanded_columns_vertical",
                    title = c.getString(R.string.tile_expanded_columns_vertical),
                    valueRange = 1..7,
                    default = 4,
                )
                slider(
                    key = "tile_expanded_columns_horizontal",
                    title = c.getString(R.string.tile_expanded_columns_horizontal),
                    valueRange = 1..9,
                    default = 6,
                )
            }
            if (SDK >= A13) {
                slider(
                    key = "tile_unexpanded_columns_vertical_c13",
                    title = c.getString(R.string.tile_unexpanded_columns_vertical),
                    valueRange = 1..6,
                    default = 5,
                )
                slider(
                    key = "tile_expanded_rows_vertical_c13",
                    title = c.getString(R.string.tile_expanded_rows_vertical),
                    valueRange = 1..6,
                    default = 3,
                )
                slider(
                    key = "tile_expanded_columns_vertical_c13",
                    title = c.getString(R.string.tile_expanded_columns_vertical),
                    valueRange = 1..7,
                    default = 4,
                )
                slider(
                    key = "tile_columns_horizontal_c13",
                    title = c.getString(R.string.tile_columns_horizontal_c13),
                    valueRange = 1..6,
                    default = 5,
                )
            }
        }
    }
}
