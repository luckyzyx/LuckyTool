package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.topjohnwu.superuser.ShellUtils

/**
 * 浮窗/分屏页：浮窗显示模式、多浮窗上限、分屏与全景工作台开关，
 * 写入 system 侧 prefs；SystemUI 侧开关经 sendPrefsValue 推给 com.android.systemui。
 */
object FloatingWindowRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "floating_window",
        prefsName = ModulePrefs,
        packName = "system",
        scopes = arrayOf("system"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 浮窗
        category(c.getString(R.string.FloatingWindow))
        list(
            key = "custom_app_floating_window_display_mode",
            title = c.getString(R.string.custom_app_floating_window_display_mode),
            entries = c.resources.getStringArray(R.array.custom_app_floating_window_display_mode_entries),
            entryValues = arrayOf("0", "1", "2", "3"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            onChange = { value ->
                c.sendPrefsValue("system", "custom_app_floating_window_display_mode", value)
                if ((value.toIntOrNull() ?: 0) >= 2) {
                    ShellUtils.fastCmd("settings put global enable_non_resizable_multi_window 1")
                }
            },
        )
        if (state.getString("custom_app_floating_window_display_mode", "0") == "3") {
            page(
                title = c.getString(R.string.zoom_window_support_list),
                target = "zoomWindowFragment",
                summary = c.getString(R.string.zoom_window_support_list_summary),
            )
        }
        //27..34 走 SystemUI 侧 zoom 状态机；35+ 走 system_server 侧后台保活
        if (osCode >= 27) {
            switch(
                key = "run_floating_window_tasks_in_foreground",
                title = c.getString(R.string.run_floating_window_tasks_in_foreground),
                summary = c.getString(R.string.need_restart_system),
                onChange = {
                    c.sendPrefsValue("com.android.systemui", "run_floating_window_tasks_in_foreground", it)
                },
            )
        }
        // 分屏
        if (osCode >= 33) {
            switch(
                key = "force_enable_multi_window_mode",
                title = c.getString(R.string.force_enable_multi_window_mode),
                summary = c.getString(R.string.need_restart_system),
                onChange = { c.sendPrefsValue("system", "force_enable_multi_window_mode", it) },
            )
            slider(
                key = "custom_multi_window_display_upper_limit",
                title = c.getString(R.string.custom_multi_window_display_upper_limit),
                valueRange = 0..20,
                default = 2,
                onChange = { c.sendPrefsValue("system", "custom_multi_window_display_upper_limit", it) },
            )
        }
        if (osCode in 26..33) {
            switch(
                key = "force_all_apps_support_split_screen",
                title = c.getString(R.string.force_all_apps_support_split_screen),
                onChange = { c.sendPrefsValue("system", "force_all_apps_support_split_screen", it) },
            )
        }
        // 全景工作台
        category(c.getString(R.string.PanoramaWorkStation))
        switch(
            key = "panorama_work_station",
            title = c.getString(R.string.panorama_work_station),
            summary = c.getString(R.string.need_restart_system),
            onChange = { c.sendPrefsValue("system", "panorama_work_station", it) },
        )
        if (state.getBoolean("panorama_work_station")) {
            slider(
                key = "panorama_max_win_num",
                title = c.getString(R.string.panorama_max_win_num),
                valueRange = 0..20,
                default = 3,
                onChange = { c.sendPrefsValue("system", "panorama_max_win_num", it) },
            )
        }
    }
}
