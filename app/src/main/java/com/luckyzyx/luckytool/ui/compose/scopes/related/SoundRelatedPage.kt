package com.luckyzyx.luckytool.ui.compose.scopes.related

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
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 声音相关页（旧 ui.fragment.scopes.related.SoundRelated 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（osCode = getOSVersionCode、SDK = Android API）、
 * notify（sendPrefsValue("com.android.systemui")）、restart 回调。
 * ColorPickerPreference 迁移为 custom：Compose ui/components/ColorPickerDialog，存储 #AARRGGBB
 * 字符串（默认 "#FFFFFFFF"，旧 setDefaultValue(Color.WHITE)），选色后 sendValue 通知 SystemUI。
 */
object SoundRelatedPage {

    private const val PERCENT_COLOR_KEY = "custom_volume_bar_percent_color"

    val spec = ScopePageSpec(
        pageKey = "sound_related",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui", "com.android.settings"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        switch(
            key = "disable_headphone_high_volume_warning",
            title = c.getString(R.string.disable_headphone_high_volume_warning),
            summary = c.getString(R.string.disable_headphone_high_volume_warning_summary),
        )
        slider(
            key = "media_volume_level",
            title = c.getString(R.string.media_volume_level),
            valueRange = 0..50,
            default = 0,
            summary = c.getString(R.string.media_volume_level_summary),
        )
        if (SDK >= A13) {
            switch(
                key = "enable_super_volume_mode",
                title = c.getString(R.string.enable_super_volume_mode),
            )
        }
        if (osCode >= 27) {
            switch(
                key = "enable_super_volume_mode_for_calls",
                title = c.getString(R.string.enable_super_volume_mode_for_calls),
            )
        }
        if (SDK >= A12) {
            switch(
                key = "minimum_volume_level_can_be_zero",
                title = c.getString(R.string.minimum_volume_level_can_be_zero),
            )
        }
        if (osCode >= 27) {
            switch(
                key = "enable_app_specific_media_volume",
                title = c.getString(R.string.enable_app_specific_media_volume),
                summary = arraySummaryLine(
                    c.getString(R.string.need_restart_system),
                    c.getString(R.string.enable_app_specific_media_volume_summary),
                    c.getString(R.string.enable_app_specific_media_volume_tips)
                ),
            )
        }
        if (osCode >= 30) {
            switch(
                key = "disable_volume_bar_thickness_effect",
                title = c.getString(R.string.disable_volume_bar_thickness_effect),
            )
        }
        if (SDK >= A13) {
            list(
                key = "set_volume_bar_display_position",
                title = c.getString(R.string.set_volume_bar_display_position),
                entries = c.resources.getStringArray(R.array.set_volume_bar_display_position_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
            )
        }
        slider(
            key = "custom_volume_dialog_background_transparency",
            title = c.getString(R.string.custom_volume_dialog_background_transparency),
            valueRange = -1..10,
            default = -1,
            summary = c.getString(R.string.force_enable_systemui_blur_feature_tips),
            notify = true,
        )
        switch(
            key = "enable_volume_bar_percent_display",
            title = c.getString(R.string.enable_volume_bar_percent_display),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_volume_bar_percent_display", false)) {
            custom(
                key = PERCENT_COLOR_KEY,
                title = c.getString(R.string.custom_volume_bar_percent_color),
            ) {
                val hex by state.stringFlow(PERCENT_COLOR_KEY, "#FFFFFFFF")
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
                        title = c.getString(R.string.custom_volume_bar_percent_color),
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
                            state.set(PERCENT_COLOR_KEY, newHex)
                            sendValue(PERCENT_COLOR_KEY, newHex)
                            showPicker = false
                        },
                    )
                }
            }
        }
        switch(
            key = "disable_audio_focus",
            title = c.getString(R.string.disable_audio_focus),
            summary = c.getString(R.string.need_restart_system),
        )
    }
}
