package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedSwitchItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsKey
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * 应用相关页（旧 ui.fragment.scopes.related.ApplicationRelated 的 Compose 等价物）。
 */
object ApplicationRelatedPage {

    private const val MULTI_APP = "com.oplus.multiapp"

    val spec = ScopePageSpec(
        pageKey = "application",
        prefsName = ModulePrefs,
        packName = "com.oplus.battery",
        scopes = arrayOf(
            "com.oplus.battery",
            "com.oplus.safecenter",
            "com.coloros.safecenter",
            "com.android.settings",
            "com.android.packageinstaller",
            "com.oplus.multiapp",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        // 应用启动
        if (SDK >= A13) {
            category(c.getString(R.string.AppStartupRelated))
            switch(
                key = "disable_splash_screen",
                title = c.getString(R.string.disable_splash_screen),
                summary = arraySummaryLine(
                    c.getString(R.string.need_restart_system),
                    c.getString(R.string.disable_splash_screen_summary),
                ),
            )
            switch(
                key = "disable_preload_splash",
                title = c.getString(R.string.disable_preload_splash),
                summary = c.getString(R.string.need_restart_system),
            )
        }
        // 应用列表
        category(c.getString(R.string.APPRelatedList))
        page(
            title = c.getString(R.string.custom_config_app_intent_list),
            target = "hideAppIntentFragment",
            summary = c.getString(R.string.need_restart_system),
        )
        page(
            title = c.getString(R.string.dark_mode_support_list),
            target = "darkModeFragment",
            summary = c.getString(R.string.zoom_window_support_list_summary),
        )
        list(
            key = "set_multi_app_support_mode",
            title = c.getString(R.string.set_multi_app_support_mode),
            entries = c.resources.getStringArray(
                if (getOSVersionCode < 27) R.array.set_multi_app_support_mode_low_entries
                else R.array.set_multi_app_support_mode_entries
            ),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.need_restart_system),
                c.getString(R.string.set_multi_app_support_mode_tips),
            ),
            onChange = { value ->
                c.sendPrefsValue("android", "set_multi_app_support_mode", value)
                c.sendPrefsValue(MULTI_APP, "set_multi_app_support_mode", value)
                restart?.invoke()
            },
        )
        if (state.getString("set_multi_app_support_mode", "0") == "1") {
            page(
                title = c.getString(R.string.multi_app_custom_list),
                target = "multiAppFragment",
                summary = c.getString(R.string.multi_app_custom_list_summary),
            )
        }
        if (getOSVersionCode >= 31) {
            switch(
                key = "remove_multi_app_blacklist",
                title = c.getString(R.string.remove_multi_app_blacklist),
                summary = c.getString(R.string.need_restart_system),
            )
        }
        if (getOSVersionCode >= 38) {
            switch(
                key = "remove_multi_app_created_num_limit_for_users",
                title = c.getString(R.string.remove_multi_app_created_num_limit),
                summary = arraySummaryLine(
                    c.getString(R.string.remove_multi_app_created_num_limit_for_users),
                    c.getString(R.string.need_restart_system),
                ),
            )
        }
        if (getOSVersionCode >= 31) {
            switch(
                key = "remove_multi_app_created_num_limit_for_apps",
                title = c.getString(R.string.remove_multi_app_created_num_limit),
                summary = arraySummaryLine(
                    c.getString(R.string.remove_multi_app_created_num_limit_for_apps),
                    c.getString(R.string.need_restart_system),
                ),
            )
        }
        list(
            key = "set_wlan_sla_whitelist_mode",
            title = c.getString(R.string.set_wlan_sla_whitelist_mode),
            entries = c.resources.getStringArray(R.array.set_wlan_sla_whitelist_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = arraySummaryLine(
                c.getString(R.string.current_mode) + ": %s",
                c.getString(R.string.need_restart_system),
            ),
            onChange = { value ->
                c.sendPrefsValue("android", "set_wlan_sla_whitelist_mode", value)
                restart?.invoke()
            },
        )
        if (state.getString("set_wlan_sla_whitelist_mode", "0") != "0") {
            switch(
                key = "remove_wlan_sla_blacklist",
                title = c.getString(R.string.remove_wlan_sla_blacklist),
                onChange = { value ->
                    c.sendPrefsValue("android", "remove_wlan_sla_blacklist", value)
                },
            )
            wlanWhitelist(c, key = "custom_wlan_sla_whitelist")
            wlanWhitelist(c, key = "custom_wlan_sla_game_whitelist")
        }
        // 应用安装
        category(c.getString(R.string.AppInstallationRelated))
        custom {
            Text(
                c.getString(R.string.PackageInstaller_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        page(
            title = c.getString(R.string.corepatch),
            target = "corePatch",
            summary = c.getString(R.string.corepatch_summary, "11-17"),
        )
        switch(
            key = "force_enable_32_bit_support",
            title = c.getString(R.string.force_enable_32_bit_support),
            summary = c.getString(R.string.need_restart_system),
        )
        switch(
            key = "fix_install_button_display_exception",
            title = c.getString(R.string.fix_install_button_display_exception),
            summary = c.getString(R.string.fix_install_button_display_exception_summary),
        )
        switch(
            key = "disable_start_app_detail",
            title = c.getString(R.string.disable_start_app_detail),
            summary = c.getString(R.string.disable_start_app_detail_summary),
        )
        switch(
            key = "skip_apk_scan",
            title = c.getString(R.string.skip_apk_scan),
            summary = c.getString(R.string.skip_apk_scan_summary),
        )
        switch(
            key = "allow_downgrade_install",
            title = c.getString(R.string.allow_downgrade_install),
            summary = c.getString(R.string.allow_downgrade_install_summary),
        )
        switch(
            key = "remove_install_ads",
            title = c.getString(R.string.remove_install_ads),
            summary = c.getString(R.string.remove_install_ads_summary),
        )
        switch(
            key = "auto_click_install_button",
            title = c.getString(R.string.auto_click_install_button),
        )
        switch(
            key = "auto_click_uninstall_button",
            title = c.getString(R.string.auto_click_uninstall_button),
        )
        switch(
            key = "show_more_apk_package_information",
            title = c.getString(R.string.show_more_apk_package_information),
            summary = c.getString(R.string.show_more_apk_package_information_summary),
        )
        switch(
            key = "remove_adb_install_confirm",
            title = c.getString(R.string.remove_adb_install_confirm),
            summary = c.getString(R.string.remove_adb_install_confirm_summary),
        )
        // 其他限制
        category(c.getString(R.string.ApplyOtherRestrictions))
        switch(
            key = "unlock_startup_limit",
            title = c.getString(R.string.unlock_startup_limit),
            summary = c.getString(R.string.unlock_startup_limit_summary),
        )
        // 应用详情相关
        category(c.getString(R.string.AppDetailsRelated))
        switch(
            key = "show_package_name_in_app_details",
            title = c.getString(R.string.show_package_name_in_app_details),
        )
        switch(
            key = "show_sdk_in_app_details",
            title = c.getString(R.string.show_sdk_in_app_details),
        )
        switch(
            key = "show_first_install_time_in_app_details",
            title = c.getString(R.string.show_first_install_time_in_app_details),
        )
        switch(
            key = "show_last_update_time_in_app_details",
            title = c.getString(R.string.show_last_update_time_in_app_details),
        )
        switch(
            key = "show_install_source_in_app_details",
            title = c.getString(R.string.show_install_source_in_app_details),
        )
        switch(
            key = "enable_long_press_to_copy_in_app_details",
            title = c.getString(R.string.enable_long_press_to_copy_in_app_details),
        )
        switch(
            key = "enable_quick_open_market_page",
            title = c.getString(R.string.enable_quick_open_market_page),
        )
        if (getOSVersionCode >= 27) {
            switch(
                key = "enable_app_clone_quick_jump",
                title = c.getString(R.string.enable_app_clone_quick_jump),
            )
        }
        switch(
            key = "allow_disabling_system_apps",
            title = c.getString(R.string.allow_disabling_system_apps),
            summary = c.getString(R.string.allow_disabling_system_apps_summary),
        )
        if (SDK >= A13) {
            switch(
                key = "remove_app_uninstall_button_blacklist",
                title = c.getString(R.string.remove_app_uninstall_button_blacklist),
            )
        }
        if (SDK >= A14) {
            switch(
                key = "enable_custom_app_language",
                title = c.getString(R.string.enable_custom_app_language),
            )
        }
        //《自动释放应用空间》默认关闭（旧代码 setDefaultValue(true) → 用 booleanFlow default=true 自定义项）
        if (getOSVersionCode >= 36) {
            val archivingKey = "disable_app_archiving"
            custom(
                key = archivingKey,
                title = c.getString(R.string.disable_app_archiving),
                summary = c.getString(R.string.disable_app_archiving_summary),
            ) { slot ->
                val checked by state.booleanFlow(archivingKey, true).collectAsStateWithLifecycle()
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SegmentedSwitchItem(
                        title = c.getString(R.string.disable_app_archiving),
                        summary = c.getString(R.string.disable_app_archiving_summary),
                        checked = checked,
                        colors = itemColors(slot),
                        onCheckedChange = { v ->
                            state.set(archivingKey, v)
                            c.sendPrefsValue("android", archivingKey, v)
                            c.sendPrefsValue("com.android.settings", archivingKey, v)
                        },
                    )
                }
            }
        }
    }

    private fun com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder.wlanWhitelist(
        c: android.content.Context,
        key: String,
    ) {
        val title = when (key) {
            "custom_wlan_sla_whitelist" -> c.getString(R.string.custom_wlan_sla_whitelist)
            else -> c.getString(R.string.custom_wlan_sla_game_whitelist)
        }
        custom(key = key, title = title) { slot ->
            var showPicker by remember { mutableStateOf(false) }
            val current = state.getStringSet(key).toString()
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SegmentedListItem(
                    onClick = { showPicker = true },
                    supportingContent = { Text(current) },
                    colors = itemColors(slot),
                    headlineContent = { Text(title) },
                )
            }
            if (showPicker) {
                AppPickerDialog(
                    title = title,
                    multiMode = true,
                    enabledList = state.getStringSet(key),
                    onDismiss = { showPicker = false },
                    onConfirm = { apps ->
                        state.set(key, apps.map { it.packageName }.toSet())
                        c.sendPrefsKey("android", key)
                        restart?.invoke()
                        showPicker = false
                    },
                )
            }
        }
    }
}
