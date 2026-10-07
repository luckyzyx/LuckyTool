package com.luckyzyx.luckytool.ui.compose.scopes.related

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.topjohnwu.superuser.ShellUtils

/**
 * 桌面相关页（旧 ui.fragment.scopes.related.LauncherRelated 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调。
 */
object LauncherRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "launcher",
        prefsName = ModulePrefs,
        packName = "com.android.launcher",
        scopes = arrayOf("com.android.launcher", "com.oppo.launcher"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 小组件
        if (osCode >= 26) {
            category(c.getString(R.string.WidgetRelated))
            switch(
                key = "remove_launcher_card_name",
                title = c.getString(R.string.remove_launcher_card_name),
            )
            if (osCode >= 30) {
                switch(
                    key = "remove_widgets_add_request_whitelist",
                    title = c.getString(R.string.remove_widgets_add_request_whitelist),
                )
            }
        }
        // 应用图标
        category(c.getString(R.string.AppIcon))
        switch(
            key = "allow_app_names_display_multiple_lines",
            title = c.getString(R.string.allow_app_names_display_multiple_lines),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("allow_app_names_display_multiple_lines")) {
            slider(
                key = "custom_app_icon_name_line_height",
                title = c.getString(R.string.custom_app_icon_name_line_height),
                valueRange = -1..15,
                default = -1,
            )
        }
        slider(
            key = "custom_launcher_app_icon_size",
            title = c.getString(R.string.custom_launcher_app_icon_size),
            valueRange = 0..100,
            default = 0,
            summary = c.getString(R.string.statusbar_clock_if_zero_summary),
        )
        if (osCode >= 37) {
            switch(
                key = "disable_long_press_app_icon_secondary_menu",
                title = c.getString(R.string.disable_long_press_app_icon_secondary_menu),
            )
        }
        // 应用徽标
        category(c.getString(R.string.AppBadgeRelated))
        if (osCode < 33) {
            switch(
                key = "enable_display_app_update_dot",
                title = c.getString(R.string.enable_display_app_update_dot),
                summary = c.getString(R.string.enable_display_app_update_dot_summary),
            )
        }
        if (osCode >= 33) {
            list(
                key = "set_app_update_dot_display_mode",
                title = c.getString(R.string.set_app_update_dot_display_mode),
                entries = c.resources.getStringArray(R.array.set_app_update_dot_display_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = arraySummaryLine(
                    c.getString(R.string.current_mode) + ": %s",
                    *when (state.getString("set_app_update_dot_display_mode", "0")) {
                        "1" -> arrayOf(c.getString(R.string.need_restart_system))
                        "2" -> arrayOf(c.getString(R.string.need_restart_scope))
                        else -> emptyArray()
                    },
                ),
                onChange = { restart?.invoke() },
            )
        }
        if (SDK >= A13) {
            switch(
                key = "remove_app_shortcut_badge",
                title = c.getString(R.string.remove_app_shortcut_badge),
            )
            switch(
                key = "remove_app_work_badge",
                title = c.getString(R.string.remove_app_work_badge),
            )
            switch(
                key = "remove_app_clone_badge",
                title = c.getString(R.string.remove_app_clone_badge),
            )
        }
        // 文件夹布局
        category(c.getString(R.string.FolderLayoutRelated))
        switch(
            key = "remove_folder_name_input_limit",
            title = c.getString(R.string.remove_folder_name_input_limit),
        )
        if (osCode >= 34) {
            switch(
                key = "enable_auto_close_folder",
                title = c.getString(R.string.enable_auto_close_folder),
            )
        }
        switch(
            key = "remove_folder_preview_background",
            title = c.getString(R.string.remove_folder_preview_background),
        )
        switch(
            key = "enable_folder_layout_adjustment",
            title = c.getString(R.string.enable_folder_layout_adjustment),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_folder_layout_adjustment")) {
            slider(
                key = "set_icon_rows_in_folder",
                title = c.getString(R.string.set_icon_rows_in_folder),
                valueRange = 4..10,
                default = 4,
            )
            slider(
                key = "set_icon_columns_in_folder",
                title = c.getString(R.string.set_icon_columns_in_folder),
                valueRange = 3..10,
                default = 3,
            )
            switch(
                key = "sync_folder_icon_column_number_preview",
                title = c.getString(R.string.sync_folder_icon_column_number_preview),
            )
        }
        // 分页组件
        category(c.getString(R.string.PaginationComponentRelated))
        switch(
            key = "remove_pagination_component",
            title = c.getString(R.string.remove_pagination_component),
        )
        switch(
            key = "remove_folder_pagination_component",
            title = c.getString(R.string.remove_folder_pagination_component),
        )
        if (SDK >= A13) {
            switch(
                key = "disable_pagination_component_sliding",
                title = c.getString(R.string.disable_pagination_component_sliding),
            )
        }
        // 最近任务列表
        category(c.getString(R.string.RecentTaskListRelated))
        if (osCode >= 37) {
            switch(
                key = "enable_recent_task_pin_capsule",
                title = c.getString(R.string.enable_recent_task_pin_capsule),
            )
        }
        if (osCode >= 30) {
            switch(
                key = "force_enable_recent_task_memory_display",
                title = c.getString(R.string.force_enable_recent_task_memory_display),
            )
        }
        if (osCode >= 34) {
            switch(
                key = "disable_auto_switch_last_task",
                title = c.getString(R.string.disable_auto_switch_last_task),
            )
        }
        switch(
            key = "long_press_app_icon_open_app_details",
            title = c.getString(R.string.long_press_app_icon_open_app_details),
        )
        switch(
            key = "remove_bottom_app_icon_of_recent_task_list",
            title = c.getString(R.string.remove_bottom_app_icon_of_recent_task_list),
        )
        switch(
            key = "remove_recent_task_list_clear_button",
            title = c.getString(R.string.remove_recent_task_list_clear_button),
        )
        switch(
            key = "unlock_task_locks",
            title = c.getString(R.string.unlock_task_locks),
        )
        switch(
            key = "allow_locking_unlocking_of_excluded_activity",
            title = c.getString(R.string.allow_locking_unlocking_of_excluded_activity),
        )
        list(
            key = "custom_app_floating_window_display_mode",
            title = c.getString(R.string.custom_app_floating_window_display_mode),
            entries = c.resources.getStringArray(R.array.custom_app_floating_window_display_mode_entries),
            entryValues = arrayOf("0", "1", "2", "3"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            onChange = { value ->
                c.sendPrefsValue("android", "custom_app_floating_window_display_mode", value)
                if ((value.toIntOrNull() ?: 0) >= 2) {
                    ShellUtils.fastCmd("settings put global enable_non_resizable_multi_window 1")
                }
                restart?.invoke()
            },
        )
        if (state.getString("custom_app_floating_window_display_mode", "0") == "3") {
            page(
                title = c.getString(R.string.zoom_window_support_list),
                target = "zoomWindowFragment",
                summary = c.getString(R.string.zoom_window_support_list_summary),
            )
        }
        if (osCode >= 33) {
            switch(
                key = "force_enable_multi_window_mode",
                title = c.getString(R.string.force_enable_multi_window_mode),
                summary = c.getString(R.string.need_restart_system),
                onChange = { c.sendPrefsValue("android", "force_enable_multi_window_mode", it) },
            )
            slider(
                key = "custom_multi_window_display_upper_limit",
                title = c.getString(R.string.custom_multi_window_display_upper_limit),
                valueRange = 0..20,
                default = 2,
                onChange = { c.sendPrefsValue("android", "custom_multi_window_display_upper_limit", it) },
            )
        }
        if (osCode in 26..33) {
            switch(
                key = "force_all_apps_support_split_screen",
                title = c.getString(R.string.force_all_apps_support_split_screen),
                onChange = { c.sendPrefsValue("android", "force_all_apps_support_split_screen", it) },
            )
        }
        // 抽屉布局
        category(c.getString(R.string.launcher_drawer_layout_related))
        switch(
            key = "enable_drawer_layout_adjustment",
            title = c.getString(R.string.enable_drawer_layout_adjustment),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_drawer_layout_adjustment")) {
            slider(
                key = "set_icon_columns_in_drawer",
                title = c.getString(R.string.set_icon_columns_in_drawer),
                valueRange = 4..10,
                default = 4,
            )
        }
        // 桌面布局
        category(c.getString(R.string.launcher_layout_related))
        if (osCode >= 37) {
            switch(
                key = "enable_launcher_indicator_entry",
                title = c.getString(R.string.enable_launcher_indicator_entry),
            )
        }
        editText(
            key = "custom_desktop_default_home_page",
            title = c.getString(R.string.custom_desktop_default_home_page),
            dialogMessage = c.getString(R.string.custom_desktop_default_home_page),
            default = "0",
        )
        if (osCode >= 26) {
            switch(
                key = "enable_docker_background",
                title = c.getString(R.string.enable_docker_background),
            )
        }
        if (osCode >= 37) {
            switch(
                key = "force_enable_docker_background_blur",
                title = c.getString(R.string.force_enable_docker_background_blur),
            )
        }
        switch(
            key = "remove_docker_max_number_limit",
            title = c.getString(R.string.remove_docker_max_number_limit),
            summary = c.getString(R.string.remove_docker_max_number_limit_summary),
        )
        switch(
            key = "launcher_layout_enable",
            title = c.getString(R.string.launcher_layout_enable),
            summary = c.getString(R.string.launcher_layout_row_colume),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("launcher_layout_enable")) {
            slider(
                key = "launcher_layout_max_rows",
                title = c.getString(R.string.launcher_layout_max_rows),
                valueRange = 6..11,
                default = 6,
            )
            slider(
                key = "launcher_layout_max_columns",
                title = c.getString(R.string.launcher_layout_max_columns),
                valueRange = 4..9,
                default = 4,
                summary = if (osCode >= 30) c.getString(R.string.launcher_layout_max_columns_summary) else null,
            )
        }
    }
}
