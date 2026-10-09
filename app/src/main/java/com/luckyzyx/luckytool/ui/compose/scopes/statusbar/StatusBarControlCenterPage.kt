package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.ColorPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isZh

/**
 * 控制中心页（旧 ui.fragment.scopes.statusbar.StatusBarControlCenter 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（osCode = getOSVersionCode、SDK = Android API）、
 * notify（sendPrefsValue("com.android.systemui")）、restart 回调。
 * ColorPickerPreference 迁移为 custom：点击弹 Compose ui/components/ColorPickerDialog，
 * 存储 #AARRGGBB 十六进制字符串（默认 "#FFFFFFFF"，旧 setDefaultValue(Color.WHITE)），
 * 选色后经 sendValue 通知 SystemUI。
 */
object StatusBarControlCenterPage {

    private const val PROGRESS_COLOR_KEY = "custom_control_center_progress_percent_color"

    val spec = ScopePageSpec(
        pageKey = "statusbar_control_center",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 时钟相关
        category(c.getString(R.string.ControlCenter_Clock_Related))
        if (osCode >= 34) {
            switch(
                key = "remove_control_center_clock_view",
                title = c.getString(R.string.remove_control_center_clock_view),
                onChange = { restart?.invoke() },
            )
        }
        switch(
            key = "control_center_clock_show_second",
            title = c.getString(R.string.control_center_clock_show_second),
        )
        list(
            key = "statusbar_control_center_clock_red_one_mode",
            title = c.getString(R.string.statusbar_control_center_clock_red_one_mode),
            entries = c.resources.getStringArray(R.array.statusbar_control_center_clock_red_one_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            notify = true,
        )
        if (SDK >= A13) {
            list(
                key = "statusbar_control_center_clock_colon_style",
                title = c.getString(R.string.statusbar_control_center_clock_colon_style),
                entries = c.resources.getStringArray(R.array.statusbar_control_center_clock_colon_style_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
                notify = true,
            )
        }
        switch(
            key = "remove_control_center_date_comma",
            title = c.getString(R.string.remove_control_center_date_comma),
            notify = true,
        )
        if (isZh(c)) {
            switch(
                key = "statusbar_control_center_date_show_lunar",
                title = c.getString(R.string.statusbar_control_center_date_show_lunar),
                notify = true,
                onChange = { restart?.invoke() },
            )
        }
        if (SDK >= A13 && isZh(c)) {
            switch(
                key = "statusbar_control_center_date_disable_text_scroll",
                title = c.getString(R.string.statusbar_control_center_date_disable_text_scroll),
                notify = true,
            )
        }
        if (SDK >= A13 && isZh(c) && state.getBoolean("statusbar_control_center_date_show_lunar", false)) {
            list(
                key = "statusbar_control_center_date_set_display_mode_horizontal",
                title = c.getString(R.string.statusbar_control_center_date_set_display_mode_horizontal),
                entries = c.resources.getStringArray(R.array.statusbar_control_center_date_fix_lunar_horizontal_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
                notify = true,
            )
        }
        if (osCode >= 38) {
            switch(
                key = "remove_control_center_carriers",
                title = c.getString(R.string.remove_control_center_carriers),
            )
        }
        // 通知中心
        category(c.getString(R.string.ControlCenterNotificationCenter))
        if (osCode >= 23) {
            switch(
                key = "enable_notification_side_spacing",
                title = c.getString(R.string.enable_notification_side_spacing),
                onChange = { restart?.invoke() },
            )
        }
        if (state.getBoolean("enable_notification_side_spacing", false)) {
            if (osCode >= 23) {
                slider(
                    key = "custom_notification_side_spacing_vertical",
                    title = c.getString(R.string.custom_notification_side_spacing_vertical),
                    valueRange = -30..30,
                    default = 0,
                    notify = true,
                )
                slider(
                    key = "custom__notification_side_spacing_horizontal",
                    title = c.getString(R.string.custom__notification_side_spacing_horizontal),
                    valueRange = -30..30,
                    default = 0,
                    notify = true,
                )
            }
        }
        if (osCode < 30) {
            switch(
                key = "enable_notification_importance_classification",
                title = c.getString(R.string.enable_notification_importance_classification),
            )
        }
        if (osCode in 30..33) {
            slider(
                key = "custom_notification_background_transparency",
                title = c.getString(R.string.custom_notification_background_transparency),
                valueRange = -1..10,
                default = -1,
                summary = c.getString(R.string.force_enable_systemui_blur_feature_tips),
                notify = true,
            )
            switch(
                key = "enable_notification_background_blur_effect",
                title = c.getString(R.string.enable_notification_background_blur_effect),
                summary = arraySummaryLine(
                    c.getString(R.string.force_enable_systemui_blur_feature_tips),
                    c.getString(R.string.force_enable_systemui_blur_feature_tips_2)
                ),
                notify = true,
            )
        }
        switch(
            key = "remove_notification_cleanup_button",
            title = c.getString(R.string.remove_notification_cleanup_button),
        )
        // 滑动条相关
        if (osCode in 26..33) {
            category(c.getString(R.string.ControlCenter_Silder_Related))
            slider(
                key = "custom_control_center_silder_transparency",
                title = c.getString(R.string.custom_control_center_silder_transparency),
                valueRange = -1..10,
                default = -1,
            )
        }
        // UI相关
        category(c.getString(R.string.ControlCenter_UI_Related))
        if (osCode >= 31) {
            list(
                key = "set_control_center_volume_seekbar_mode",
                title = c.getString(R.string.set_control_center_volume_seekbar_mode),
                entries = c.resources.getStringArray(R.array.set_control_center_volume_seekbar_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
            )
        }
        switch(
            key = "enable_control_center_progress_percent_display",
            title = c.getString(R.string.enable_control_center_progress_percent_display),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_control_center_progress_percent_display", false)) {
            custom(
                key = PROGRESS_COLOR_KEY,
                title = c.getString(R.string.custom_control_center_progress_percent_color),
            ) {
                val hex by state.stringFlow(PROGRESS_COLOR_KEY, "#FFFFFFFF")
                    .collectAsStateWithLifecycle()
                val color = remember(hex) {
                    try {
                        Color(hex.toColorInt())
                    } catch (_: Throwable) {
                        Color.Transparent
                    }
                }
                var showPicker by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    PrefRow(
                        title = c.getString(R.string.custom_control_center_progress_percent_color),
                        summary = c.getString(R.string.current_color, hex),
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color, CircleShape)
                            )
                        },
                        onClick = { showPicker = true },
                    )
                }
                if (showPicker) {
                    ColorPickerDialog(
                        initialHex = hex,
                        onDismiss = { showPicker = false },
                        onColorSelected = { _, newHex ->
                            state.set(PROGRESS_COLOR_KEY, newHex)
                            sendValue(PROGRESS_COLOR_KEY, newHex)
                            showPicker = false
                        },
                    )
                }
            }
        }
        if (osCode >= 34) {
            switch(
                key = "remove_control_center_edit_button",
                title = c.getString(R.string.remove_control_center_edit_button),
                summary = c.getString(R.string.separate_control_center_mode_only),
            )
        }
        if (osCode in 34..39) {
            switch(
                key = "remove_control_center_more_button",
                title = c.getString(R.string.remove_control_center_more_button),
                summary = c.getString(R.string.separate_control_center_mode_only),
            )
        }
        if (osCode >= 38) {
            switch(
                key = "remove_control_center_settings_button",
                title = c.getString(R.string.remove_control_center_settings_button),
                summary = c.getString(R.string.separate_control_center_mode_only),
            )
        }
        list(
            key = "set_auto_brightness_button_mode",
            title = c.getString(R.string.set_auto_brightness_button_mode),
            entries = c.resources.getStringArray(R.array.statusbar_control_center_auto_brightness_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
        if (SDK < A13) {
            switch(
                key = "remove_control_center_user_switcher",
                title = c.getString(R.string.remove_control_center_user_switcher),
            )
        }
        if (SDK >= A13) {
            switch(
                key = "remove_control_center_mydevice",
                title = c.getString(R.string.remove_control_center_mydevice),
            )
        }
        list(
            key = "set_control_center_search_button_mode",
            title = c.getString(R.string.set_control_center_search_button_mode),
            entries = c.resources.getStringArray(R.array.set_control_center_search_button_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            notify = true,
        )
        list(
            key = "remove_control_center_networkwarn",
            title = c.getString(R.string.remove_control_center_networkwarn),
            entries = c.resources.getStringArray(R.array.statusbar_control_center_networkwarn_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.remove_control_center_networkwarn_summary)
            ),
            notify = true,
        )
        if (osCode < 34) {
            slider(
                key = "custom_control_center_background_transparency",
                title = c.getString(R.string.custom_control_center_background_transparency),
                valueRange = -1..10,
                default = -1,
                summary = c.getString(R.string.force_enable_systemui_blur_feature_tips),
                notify = true,
            )
        }
    }
}
