package com.luckyzyx.luckytool.ui.compose.scopes.apps

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * 手势页：熄屏手势、全屏手势、侧滑图标与自定义按键的开关与取值。
 * open 菜单（IntentUtils.jumpGesture）不在本页呈现；视频白名单项的 isVisible=false，不渲染。
 * 注意：action_button_* 系列通知目标是 "android" 而非 scopes[0]，故用 onChange + sendPrefsValue 而非 notify。
 */
object OplusGesturePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_gesture",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        displayPackName = "com.oplus.gesture",
        scopes = arrayOf("com.android.systemui", "com.oplus.gesture"),
        restartEnabled = true,
        isVisible = { checkPackName("com.oplus.gesture") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        // 音量键手电筒
        if (getOSVersionCode >= 27) {
            switch(
                key = "enable_volume_key_control_flashlight",
                title = c.getString(R.string.enable_volume_key_control_flashlight),
                summary = arraySummaryLine(
                    c.getString(R.string.enable_volume_key_control_flashlight_summary),
                    c.getString(R.string.need_restart_system),
                ),
            )
        }
        // 隔空手势
        if (SDK >= A13) {
            category(c.getString(R.string.AonGesture))
            switch(
                key = "force_enable_aon_gestures",
                title = c.getString(R.string.force_enable_aon_gestures),
                summary = c.getString(R.string.force_enable_aon_gestures_summary),
                enabled = c.checkPackName("com.oplus.gesture") && c.checkPackName("com.aiunit.aon"),
            )
            custom(
                key = "custom_aon_gesture_scroll_page_whitelist_list",
                title = c.getString(R.string.custom_aon_gesture_scroll_page_whitelist),
            ) {
                val saved by state
                    .stringSetFlow("custom_aon_gesture_scroll_page_whitelist_list")
                    .collectAsStateWithLifecycle()
                var show by remember { mutableStateOf(false) }
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.custom_aon_gesture_scroll_page_whitelist),
                        summary = arraySummaryLine(
                            stringResource(R.string.custom_aon_gesture_whitelist_tips),
                            saved.toString(),
                        ),
                        onClick = { show = true },
                        enabled = c.checkPackName("com.aiunit.aon"),
                    )
                }
                if (show) {
                    AppPickerDialog(
                        title = stringResource(R.string.custom_aon_gesture_scroll_page_whitelist),
                        multiMode = true,
                        enabledList = saved,
                        onDismiss = { show = false },
                        onConfirm = { apps ->
                            state.set(
                                "custom_aon_gesture_scroll_page_whitelist_list",
                                apps.map { it.packageName }.toSet(),
                            )
                        },
                    )
                }
            }
        }
        // 全面屏手势
        category(c.getString(R.string.FullScreenGestureRelated))
        switch(
            key = "remove_side_slider",
            title = c.getString(R.string.remove_side_slider),
        )
        switch(
            key = "remove_side_slider_black_background",
            title = c.getString(R.string.remove_side_slider_black_background),
        )
        switch(
            key = "remove_rotate_screen_button",
            title = c.getString(R.string.remove_rotate_screen_button),
        )
        slider(
            key = "custom_rotate_screen_button_size",
            title = c.getString(R.string.custom_rotate_screen_button_size),
            valueRange = 50..100,
            default = 100,
            summary = c.getString(R.string.need_restart_scope),
            valueLabel = { "$it%" },
        )
        if (getOSVersionCode in 35..36) {
            switch(
                key = "remove_back_gesture_confirmation_limit",
                title = c.getString(R.string.remove_back_gesture_confirmation_limit),
            )
        }
        // 自定义侧滑条图标
        category(c.getString(R.string.CustomSideSliderIcon))
        switch(
            key = "replace_side_slider_icon_switch",
            title = c.getString(R.string.replace_side_slider_icon_switch),
            summary = c.getString(R.string.replace_side_slider_icon_switch_summary),
        )
        if (state.getBoolean("replace_side_slider_icon_switch")) {
            custom(
                key = "replace_side_slider_icon_on_left",
                title = c.getString(R.string.replace_side_slider_icon_on_left),
            ) {
                val path by state.stringFlow("replace_side_slider_icon_on_left")
                    .collectAsStateWithLifecycle()
                val activity = LocalActivity.current
                val pickMedia = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null && activity != null) {
                        val cacheFile = FileUtils.getMSMCacheFile(activity, uri)
                        state.set(
                            "replace_side_slider_icon_on_left", cacheFile?.path.orEmpty()
                        )
                    }
                }
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.replace_side_slider_icon_on_left),
                        summary = path.ifBlank { "Null" },
                        onClick = { pickMedia.launch("image/*") },
                    )
                }
            }
            custom(
                key = "replace_side_slider_icon_on_right",
                title = c.getString(R.string.replace_side_slider_icon_on_right),
            ) {
                val path by state.stringFlow("replace_side_slider_icon_on_right")
                    .collectAsStateWithLifecycle()
                val activity = LocalActivity.current
                val pickMedia = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null && activity != null) {
                        val cacheFile = FileUtils.getMSMCacheFile(activity, uri)
                        state.set(
                            "replace_side_slider_icon_on_right", cacheFile?.path.orEmpty()
                        )
                    }
                }
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.replace_side_slider_icon_on_right),
                        summary = path.ifBlank { "Null" },
                        onClick = { pickMedia.launch("image/*") },
                    )
                }
            }
        }
        // 自定义快捷键
        category(c.getString(R.string.CustomActionButton))
        switch(
            key = "action_button_nothing_enable",
            title = c.getString(R.string.action_button_nothing_enable),
            onChange = { newValue ->
                c.sendPrefsValue("system", "action_button_nothing_enable", newValue)
            },
        )
        switch(
            key = "action_button_ring_cycle_enable",
            title = c.getString(R.string.action_button_ring_cycle_enable),
            onChange = { newValue ->
                c.sendPrefsValue("system", "action_button_ring_cycle_enable", newValue)
            },
        )
        if (state.getBoolean("action_button_ring_cycle_enable")) {
            list(
                key = "action_button_ring_cycle_mode",
                title = c.getString(R.string.action_button_ring_cycle_mode),
                entries = c.resources.getStringArray(R.array.action_button_ring_cycle_mode_entries),
                entryValues = arrayOf("ring_vibrate_silent", "ring_vibrate", "ring_silent"),
                default = "ring_vibrate_silent",
                summary = c.getString(R.string.current_mode) + ": %s",
                onChange = { newValue ->
                    c.sendPrefsValue("system", "action_button_ring_cycle_mode", newValue)
                },
            )
        }
    }
}
