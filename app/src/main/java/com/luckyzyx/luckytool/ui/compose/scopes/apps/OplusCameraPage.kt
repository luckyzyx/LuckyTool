package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Context
import android.os.Build
import android.util.ArraySet
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.CameraFilter
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.CameraUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 相机页（旧 ui.fragment.scopes.apps.OplusCamera 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（osCode 沿用 getOSVersionCode、SDK 用 Android 版本）、
 * 滤镜多选对话框与自定义水印。旧 open 菜单（openApp）不在 Compose 页呈现。
 */
object OplusCameraPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_camera",
        prefsName = ModulePrefs,
        packName = "com.oplus.camera",
        scopes = arrayOf("com.oplus.camera", "com.oneplus.camera"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        // 调试 UI 选项
        if (getOSVersionCode >= 30) {
            switch(
                key = "enable_camera_debug_ui_option",
                title = c.getString(R.string.enable_camera_debug_ui_option),
                summary = c.getString(R.string.enable_camera_debug_ui_option_summary).trimIndent(),
            )
        }
        // 默认打开相册的应用选择（旧 AppInfoSelectDialog 单选 → Compose AppPickerDialog）
        if (getOSVersionCode >= 26) {
            custom(
                key = "custom_camera_open_gallery_by_default",
                title = c.getString(R.string.custom_camera_open_gallery_by_default),
            ) {
                val current by state.stringFlow("custom_camera_open_gallery_by_default")
                    .collectAsStateWithLifecycle()
                var show by remember { mutableStateOf(false) }
                ListItem(
                    onClick = { show = true },
                    supportingContent = {
                        Text(arraySummaryLine(current.ifBlank { c.getString(R.string.not_set) }))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.custom_camera_open_gallery_by_default)) }
                if (show) {
                    AppPickerDialog(
                        title = c.getString(R.string.custom_camera_open_gallery_by_default),
                        onDismiss = { show = false },
                        onConfirm = { apps ->
                            val packName = apps.firstOrNull()?.packageName.orEmpty()
                            state.set("custom_camera_open_gallery_by_default", packName)
                            sendValue("custom_camera_open_gallery_by_default", packName)
                            restart?.invoke()
                        },
                    )
                }
            }
        }
        if (getOSVersionCode >= 28) {
            switch(
                key = "enable_camera_night_zoom_30x",
                title = c.getString(R.string.enable_camera_night_zoom_30x),
            )
            switch(
                key = "enable_video_capture_roulette_zoom",
                title = c.getString(R.string.enable_video_capture_roulette_zoom),
            )
        }
        if (getOSVersionCode >= 26) {
            switch(
                key = "remove_camera_flash_limit",
                title = c.getString(R.string.remove_camera_flash_limit),
            )
        }
        // 水印
        category(c.getString(R.string.CameraWaterMark))
        switch(
            key = "remove_watermark_word_limit",
            title = c.getString(R.string.remove_watermark_word_limit),
        )
        if (SDK >= A13) {
            switch(
                key = "enable_frame_watermark_style",
                title = c.getString(R.string.enable_frame_watermark_style),
                onChange = { newValue ->
                    if (newValue) state.set("enable_hasselblad_watermark_style", false)
                },
            )
            switch(
                key = "enable_hasselblad_watermark_style",
                title = c.getString(R.string.enable_hasselblad_watermark_style),
                onChange = { newValue ->
                    if (newValue) state.set("enable_frame_watermark_style", false)
                },
            )
            if (!Build.FINGERPRINT.contains("RMX", true)) {
                editText(
                    key = "custom_model_watermark",
                    title = c.getString(R.string.custom_model_watermark),
                    default = "None",
                )
            }
        }
        // 滤镜
        if (SDK >= A13) {
            category(c.getString(R.string.CameraFilter))
            if (getOSVersionCode >= 34) {
                switch(
                    key = "remove_filter_model_limit",
                    title = c.getString(R.string.remove_filter_model_limit),
                )
            }
            custom(
                key = "camera_universal_filter_settings",
                title = c.getString(R.string.camera_universal_filter_settings),
            ) {
                val saved = state.getStringSet("camera_universal_filter_settings")
                val filters = CameraUtils.getCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                ListItem(
                    onClick = {
                        showCameraFilterDialog(
                            c = c,
                            state = state,
                            key = "camera_universal_filter_settings",
                            title = c.getString(R.string.camera_universal_filter_settings),
                            filters = filters,
                            onSaved = { set ->
                                if (set.contains("master_filter")) {
                                    state.set("enable_hasselblad_watermark_style", true)
                                }
                            },
                            onRestart = { restart?.invoke() },
                        )
                    },
                    supportingContent = {
                        Text(filters.filter { it.isEnable }.map { it.title }.toString())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.camera_universal_filter_settings)) }
            }
            custom(
                key = "camera_portrait_filter_settings",
                title = c.getString(R.string.camera_portrait_filter_settings),
            ) {
                val saved = state.getStringSet("camera_portrait_filter_settings")
                val filters = CameraUtils.getPortraitCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                ListItem(
                    onClick = {
                        showCameraFilterDialog(
                            c = c,
                            state = state,
                            key = "camera_portrait_filter_settings",
                            title = c.getString(R.string.camera_portrait_filter_settings),
                            filters = filters,
                            onRestart = { restart?.invoke() },
                        )
                    },
                    supportingContent = {
                        Text(filters.filter { it.isEnable }.map { it.title }.toString())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.camera_portrait_filter_settings)) }
            }
            custom(
                key = "camera_video_filter_settings",
                title = c.getString(R.string.camera_video_filter_settings),
            ) {
                val saved = state.getStringSet("camera_video_filter_settings")
                val filters = CameraUtils.getVideoCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                ListItem(
                    onClick = {
                        showCameraFilterDialog(
                            c = c,
                            state = state,
                            key = "camera_video_filter_settings",
                            title = c.getString(R.string.camera_video_filter_settings),
                            filters = filters,
                            onRestart = { restart?.invoke() },
                        )
                    },
                    supportingContent = {
                        Text(filters.filter { it.isEnable }.map { it.title }.toString())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.camera_video_filter_settings)) }
            }
        }
        // 其他
        if (SDK >= A13) {
            category(c.getString(R.string.settings_other_preference))
            switch(
                key = "enable_10_bit_image_support",
                title = c.getString(R.string.enable_10_bit_image_support),
                summary = c.getString(R.string.enable_10_bit_image_support_summary),
            )
        }
    }

    /** 旧多选滤镜对话框（保留 Material 对话框，语义一致：默认启用 || 已保存集合） */
    private fun showCameraFilterDialog(
        c: Context,
        state: PrefState,
        key: String,
        title: String,
        filters: List<CameraFilter>,
        onSaved: (Set<String>) -> Unit = {},
        onRestart: () -> Unit = {},
    ) {
        val keys = filters.map { it.key }
        val titles = filters.map { it.title }
        val values = filters.map { it.isEnable }.toBooleanArray()
        MaterialAlertDialogBuilder(c, dialogCentered).apply {
            setTitle(title)
            setMultiChoiceItems(titles.toTypedArray(), values, null)
            setPositiveButton(android.R.string.ok) { dialog, _ ->
                val positions =
                    (dialog as androidx.appcompat.app.AlertDialog).listView.checkedItemPositions
                val set = ArraySet<String>()
                for (i in 0 until positions.size()) {
                    if (positions.valueAt(i)) set.add(keys[positions.keyAt(i)])
                }
                state.set(key, set.toSet())
                onSaved(set.toSet())
                onRestart()
            }
            setNeutralButton(android.R.string.cancel, null)
        }.show()
    }
}
