package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.luckyzyx.luckytool.R
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

/**
 * 息屏显示页：AOD 相关开关与列表取值。
 * 每项都显式声明键、默认值与条件可见性（osCode = getOSVersionCode 判 OS 版本、
 * SDK = Android API 级别）。
 * 整页仅在 SDK >= A13 且装有 com.oplus.aod 时出现（见 spec.isVisible），本页 DSL 本身无条件渲染。
 * set_random_text_display_mode 选到 "1" 时追加一条选文件项（custom + GetContent，结果写入
 * custom_random_text_file），选到 "2" 时追加一条 EditText 项（custom_random_text_api）。
 * 这层 when 在 builder lambda 里按 state.getString(...) 读当前值、每次重组都重跑，
 * 所以条件条目与摘要会跟着写入即时刷新；写入一律走 state.set（revision 前进 → 重组），
 * 不需要在写值后重建整页。
 */
object AodRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "aod",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        displayPackName = "com.oplus.aod",
        scopes = arrayOf("com.android.systemui", "com.oplus.aod", "com.oplus.uiengine"),
        restartEnabled = true,
        isVisible = { SDK >= A13 && checkPackName("com.oplus.aod") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 息屏相关
        category(c.getString(R.string.AodRelated))
        switch(
            key = "remove_aod_music_whitelist",
            title = c.getString(R.string.remove_aod_music_whitelist),
        )
        if (SDK == A13) {
            switch(
                key = "remove_aod_notification_icon_whitelist",
                title = c.getString(R.string.remove_aod_notification_icon_whitelist),
            )
        }
        if (SDK >= A13) {
            list(
                key = "set_aod_notification_icon_style",
                title = c.getString(R.string.set_aod_notification_icon_style),
                entries = c.resources.getStringArray(R.array.set_aod_notification_icon_style_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
            )
        }
        if (osCode in 26..33) {
            switch(
                key = "force_enable_screen_off_music_support",
                title = c.getString(R.string.force_enable_screen_off_music_support),
                summary = c.getString(R.string.force_enable_screen_off_music_support_summary),
            )
        }
        if (osCode >= 37) {
            switch(
                key = "hide_panoramic_aod_status_bar",
                title = c.getString(R.string.hide_panoramic_aod_status_bar),
                summary = c.getString(R.string.hide_panoramic_aod_status_bar_summary),
            )
        }
        // 随机一言
        if (osCode >= 26) {
            category(c.getString(R.string.AodRandomText))
            list(
                key = "set_random_text_display_mode",
                title = c.getString(R.string.set_random_text_display_mode),
                entries = c.resources.getStringArray(R.array.set_random_text_display_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = arraySummaryLine(
                    c.getString(R.string.current_mode) + ": %s",
                    c.getString(R.string.set_random_text_display_mode_tips1),
                    c.getString(R.string.set_random_text_display_mode_tips2)
                ),
            )
            when (state.getString("set_random_text_display_mode", "0")) {
                "1" -> {
                    val fileTitle = c.getString(R.string.custom_random_text_file)
                    val path = state.getString("custom_random_text_file", "").orEmpty()
                    custom(
                        key = "custom_random_text_file",
                        title = fileTitle,
                        summary = path.ifBlank { "Null" },
                    ) {
                        val activity = LocalActivity.current
                        val pickFile = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null && activity != null) {
                                FileUtils.getDocumentPath(activity, uri)?.let { p ->
                                    // 走 state.set 而非裸 SP 写入：revision 前进让构建 lambda 重跑，
                                    // 摘要中的 path 自行刷新（无需 recreate）
                                    state.set("custom_random_text_file", p)
                                }
                            }
                        }
                        PrefRowInset {
                            PrefRow(
                                title = fileTitle,
                                summary = path.ifBlank { "Null" },
                                onClick = { pickFile.launch("text/plain") },
                            )
                        }
                    }
                }

                "2" -> editText(
                    key = "custom_random_text_api",
                    title = c.getString(R.string.custom_random_text_api),
                    dialogMessage = c.getString(R.string.custom_random_text_api),
                    default = "",
                )
            }
        }
        // 字体样式
        if (osCode >= 26) {
            category(c.getString(R.string.AodTypface))
            list(
                key = "set_aod_typeface_mode",
                title = c.getString(R.string.set_aod_typeface_mode),
                entries = c.resources.getStringArray(R.array.set_aod_typeface_mode_entries),
                entryValues = arrayOf("0", "1", "2"),
                default = "0",
                summary = c.getString(R.string.current_mode) + ": %s",
            )
            if (state.getString("set_aod_typeface_mode", "0") != "0") {
                switch(
                    key = "apply_aod_clock_typeface",
                    title = c.getString(R.string.apply_aod_clock_typeface),
                )
            }
        }
    }
}
