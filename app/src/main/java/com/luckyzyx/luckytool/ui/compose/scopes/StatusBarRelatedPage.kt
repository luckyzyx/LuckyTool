package com.luckyzyx.luckytool.ui.compose.scopes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * 状态栏相关页（旧 ui.fragment.scopes.related.StatusBarRelated 的 Compose 等价物）。
 * 含 8 个页面入口 + 状态栏事件 + 音乐流体云（osCode 33/35 门控）。
 */
object StatusBarRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf(
            "com.android.systemui",
            "com.oplus.battery",
            "com.coloros.phonemanager",
            "com.oplus.notificationmanager",
            "com.oplus.mediacontroller",
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        page(c.getString(R.string.StatusBarClock), "statusBarClock")
        page(c.getString(R.string.StatusBarNetWorkSpeed), "statusBarNetWorkSpeed")
        page(c.getString(R.string.StatusBarNotice), "statusBarNotify")
        page(c.getString(R.string.StatusBarIcon), "statusBarIcon")
        page(c.getString(R.string.StatusBarControlCenter), "statusBarControlCenter")
        page(c.getString(R.string.StatusBarTiles), "statusBarTiles")
        page(c.getString(R.string.StatusBarLayout), "statusBarLayout")
        page(c.getString(R.string.StatusBarBattery), "statusBarBattery")

        // 状态栏事件
        category(c.getString(R.string.StatusbarEvents))
        switch(
            key = "statusbar_double_click_lock_screen",
            title = c.getString(R.string.statusbar_double_click_lock_screen),
        )
        if (getOSVersionCode >= 26) {
            switch(
                key = "vibrate_when_opening_the_statusbar",
                title = c.getString(R.string.vibrate_when_opening_the_statusbar),
            )
        }
        if (SDK >= A13) {
            list(
                key = "set_click_statusbar_scroll_to_top_mode",
                title = c.getString(R.string.set_click_statusbar_scroll_to_top_mode),
                entries = c.resources.getStringArray(R.array.set_click_statusbar_scroll_to_top_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s\n" +
                    c.getString(R.string.need_restart_system),
            )
        }
        // 音乐流体云
        if (getOSVersionCode >= 33) {
            switch(
                key = "custom_music_fluid_cloud_whitelist",
                title = c.getString(R.string.custom_music_fluid_cloud_whitelist),
                onChange = { restart?.invoke() },
            )
            if (state.getBoolean("custom_music_fluid_cloud_whitelist")) {
                switch(
                    key = "disable_music_fluid_cloud_display",
                    title = c.getString(R.string.disable_music_fluid_cloud_display),
                )
                custom(
                    key = "set_custom_music_fluid_cloud_whitelist",
                    title = c.getString(R.string.set_custom_music_fluid_cloud_whitelist),
                ) { slot ->
                    // 应用选择行：summary=当前白名单集合，enabled=未禁用显示
                    val whitelist by state.stringSetFlow("set_custom_music_fluid_cloud_whitelist")
                        .collectAsStateWithLifecycle()
                    val enabled = !state.getBoolean("disable_music_fluid_cloud_display")
                    var showPicker by remember { mutableStateOf(false) }
                    if (LocalUiMode.current == UiMode.Miuix) {
                        MiuixListItem(
                            title = c.getString(R.string.set_custom_music_fluid_cloud_whitelist),
                            summary = whitelist.toString(),
                            onClick = { if (enabled) showPicker = true },
                            enabled = enabled,
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            SegmentedListItem(
                                onClick = { if (enabled) showPicker = true },
                                supportingContent = { Text(whitelist.toString()) },
                                enabled = enabled,
                                colors = itemColors(slot),
                                headlineContent = {
                                    Text(c.getString(R.string.set_custom_music_fluid_cloud_whitelist))
                                },
                            )
                        }
                    }
                    if (showPicker) {
                        AppPickerDialog(
                            title = c.getString(R.string.set_custom_music_fluid_cloud_whitelist),
                            multiMode = true,
                            enabledList = whitelist,
                            onDismiss = { showPicker = false },
                            onConfirm = { apps ->
                                state.set(
                                    "set_custom_music_fluid_cloud_whitelist",
                                    apps.map { it.packageName }.toSet(),
                                )
                                restart?.invoke()
                            },
                        )
                    }
                }
                if (getOSVersionCode >= 35) {
                    switch(
                        key = "disable_media_music_fluid_cloud_blacklist",
                        title = c.getString(R.string.disable_media_music_fluid_cloud_blacklist),
                    )
                }
            }
            // 目标包与页面 packName 不同：走 onChange 直接发送
            switch(
                key = "force_enable_media_music_fluid_cloud_ripple",
                title = c.getString(R.string.force_enable_media_music_fluid_cloud_ripple),
                onChange = { newValue ->
                    c.sendPrefsValue(
                        "com.oplus.mediacontroller",
                        "force_enable_media_music_fluid_cloud_ripple",
                        newValue,
                    )
                },
            )
        }
    }
}
