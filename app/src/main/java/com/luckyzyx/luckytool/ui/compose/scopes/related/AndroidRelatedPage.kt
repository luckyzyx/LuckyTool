package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * Android 系统页（旧 ui.fragment.scopes.related.AndroidRelated 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、restart 回调。
 */
object AndroidRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "android_related",
        prefsName = ModulePrefs,
        packName = "system",
        scopes = arrayOf("system"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_gms_usage_restrictions",
            title = c.getString(R.string.remove_gms_usage_restrictions),
            summary = arraySummaryLine(c.getString(R.string.need_restart_system)),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("remove_gms_usage_restrictions", false)) {
            editText(
                key = "custom_remote_provisioning_hostname",
                title = c.getString(R.string.custom_remote_provisioning_hostname),
                dialogMessage = arraySummaryLine(
                    c.getString(R.string.custom_remote_provisioning_hostname_summary),
                    c.getString(R.string.need_restart_system),
                ),
                default = "remoteprovisioning.grapheneos.org",
            )
        }
        if (SDK >= A12) {
            switch(
                key = "replace_system_root_state_detection",
                title = c.getString(R.string.replace_system_root_state_detection),
                summary = c.getString(R.string.need_restart_system),
            )
            switch(
                key = "allow_untrusted_touch",
                title = c.getString(R.string.allow_untrusted_touch),
                summary = c.getString(R.string.allow_untrusted_touch_summary),
            )
        }
        switch(
            key = "disable_long_press_home_key_start_speech_asssist",
            title = c.getString(R.string.disable_long_press_home_key_start_speech_asssist),
            summary = c.getString(R.string.need_restart_system),
        )
        list(
            key = "customized_gaussian_blur_effect_level",
            title = c.getString(R.string.customized_gaussian_blur_effect_level),
            entries = c.resources.getStringArray(R.array.customized_gaussian_blur_effect_level_entries),
            entryValues = arrayOf("-1", "0", "1", "2", "3"),
            default = "-1",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.need_restart_system),
            ),
            onChange = { restart?.invoke() },
        )
        category("LTPO")
        list(
            key = "set_ltpo_refresh_rate_mode",
            title = c.getString(R.string.set_ltpo_refresh_rate_mode),
            entries = c.resources.getStringArray(R.array.set_ltpo_refresh_rate_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.need_restart_system),
            ),
            onChange = { restart?.invoke() },
        )
        if (state.getString("set_ltpo_refresh_rate_mode", "0") == "1") {
            switch(
                key = "enable_full_brightness_refresh_rate_minimum_one",
                title = c.getString(R.string.enable_full_brightness_refresh_rate_minimum_one),
                summary = arraySummaryLine(
                    c.getString(R.string.enable_full_brightness_refresh_rate_minimum_one_summary),
                    c.getString(R.string.need_restart_system),
                ),
            )
        }
    }
}
