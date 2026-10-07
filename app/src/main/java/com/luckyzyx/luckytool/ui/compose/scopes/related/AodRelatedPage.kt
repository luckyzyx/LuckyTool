package com.luckyzyx.luckytool.ui.compose.scopes.related

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.putString

/**
 * 息屏显示页（旧 ui.fragment.scopes.related.AodRelated 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（osCode = getOSVersionCode、SDK = Android API）、
 * restart 回调。旧 loadRootPreference 的 isVisible = SDK >= A13 && checkPackName("com.oplus.aod")
 * 属功能树可见性，由功能树 Compose 化终局统一处理（本页无条件渲染）。
 * set_random_text_display_mode 的构建期 when("1"→选文件点击项;"2"→EditText) 迁移为
 * builder 内 when(state.getString(...))（每次 revision 重组重跑，等价）。
 */
object AodRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "aod",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui", "com.oplus.aod", "com.oplus.uiengine"),
        restartEnabled = true,
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
                onChange = { restart?.invoke() },
            )
            when (state.getString("set_random_text_display_mode", "0")) {
                "1" -> {
                    val fileTitle = c.getString(R.string.custom_random_text_file)
                    val path = state.getString("custom_random_text_file", "").orEmpty()
                    custom(
                        key = "custom_random_text_file",
                        title = fileTitle,
                        summary = path.ifBlank { "Null" },
                    ) { slot ->
                        val activity = LocalContext.current as? Activity
                        val pickFile = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null && activity != null) {
                                FileUtils.getDocumentPath(activity, uri)?.let { p ->
                                    c.putString(ModulePrefs, "custom_random_text_file", p)
                                }
                            }
                            restart?.invoke()
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            SegmentedListItem(
                                onClick = { pickFile.launch("text/plain") },
                                supportingContent = { Text(path.ifBlank { "Null" }) },
                                colors = itemColors(slot),
                                headlineContent = { Text(fileTitle) },
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
                onChange = { restart?.invoke() },
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
