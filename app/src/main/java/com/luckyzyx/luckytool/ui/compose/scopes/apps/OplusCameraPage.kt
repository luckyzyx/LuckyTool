package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.CameraFilter
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.MultiChoiceDialog
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.CameraUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 相机页：相机调试项、水印、滤镜与其它成像开关。
 * 每项都显式声明键、默认值与条件可见性（osCode = getOSVersionCode 判 OS 版本、
 * SDK = Android API 级别）；水印与滤镜含自定义取值与多选对话框。
 * 整页仅在装有 com.oplus.camera 或 com.oneplus.camera 时出现（见 spec.isVisible）。
 * open 菜单（openApp 跳转）不在本页呈现。
 */
object OplusCameraPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_camera",
        prefsName = ModulePrefs,
        packName = "com.oplus.camera",
        scopes = arrayOf("com.oplus.camera", "com.oneplus.camera"),
        restartEnabled = true,
        isVisible = { checkPackName("com.oneplus.camera") || checkPackName("com.oplus.camera") },
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
        // 默认打开相册的应用选择（单选一个应用 → AppPickerDialog）
        if (getOSVersionCode >= 26) {
            custom(
                key = "custom_camera_open_gallery_by_default",
                title = c.getString(R.string.custom_camera_open_gallery_by_default),
            ) { slot ->
                val current by state.stringFlow("custom_camera_open_gallery_by_default")
                    .collectAsStateWithLifecycle()
                var show by remember { mutableStateOf(false) }
                if (LocalUiMode.current == UiMode.Miuix) {
                    MiuixListItem(
                        title = stringResource(R.string.custom_camera_open_gallery_by_default),
                        summary = arraySummaryLine(current.ifBlank { stringResource(R.string.not_set) }),
                        onClick = { show = true },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MaterialListItem(
                            onClick = { show = true },
                            supportingContent = {
                                Text(arraySummaryLine(current.ifBlank { stringResource(R.string.not_set) }))
                            },
                            colors = itemColors(slot),
                            headlineContent = {
                                Text(stringResource(R.string.custom_camera_open_gallery_by_default))
                            },
                        )
                    }
                }
                if (show) {
                    AppPickerDialog(
                        title = stringResource(R.string.custom_camera_open_gallery_by_default),
                        onDismiss = { show = false },
                        onConfirm = { apps ->
                            val packName = apps.firstOrNull()?.packageName.orEmpty()
                            state.set("custom_camera_open_gallery_by_default", packName)
                            sendValue("custom_camera_open_gallery_by_default", packName)
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
            ) { slot ->
                var showDialog by remember { mutableStateOf(false) }
                val saved = state.getStringSet("camera_universal_filter_settings")
                val filters = CameraUtils.getCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                if (LocalUiMode.current == UiMode.Miuix) {
                    MiuixListItem(
                        title = stringResource(R.string.camera_universal_filter_settings),
                        summary = filters.filter { it.isEnable }.map { it.title }.toString(),
                        onClick = { showDialog = true },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MaterialListItem(
                            onClick = { showDialog = true },
                            supportingContent = {
                                Text(filters.filter { it.isEnable }.map { it.title }.toString())
                            },
                            colors = itemColors(slot),
                            headlineContent = {
                                Text(stringResource(R.string.camera_universal_filter_settings))
                            },
                        )
                    }
                }
                if (showDialog) {
                    CameraFilterDialog(
                        c = c,
                        state = state,
                        data = FilterDialogData(
                            key = "camera_universal_filter_settings",
                            title = stringResource(R.string.camera_universal_filter_settings),
                            filters = filters,
                            onSaved = { set ->
                                if (set.contains("master_filter")) {
                                    state.set("enable_hasselblad_watermark_style", true)
                                }
                            },
                        ),
                        onDismiss = { showDialog = false },
                    )
                }
            }
            custom(
                key = "camera_portrait_filter_settings",
                title = c.getString(R.string.camera_portrait_filter_settings),
            ) { slot ->
                var showDialog by remember { mutableStateOf(false) }
                val saved = state.getStringSet("camera_portrait_filter_settings")
                val filters = CameraUtils.getPortraitCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                if (LocalUiMode.current == UiMode.Miuix) {
                    MiuixListItem(
                        title = stringResource(R.string.camera_portrait_filter_settings),
                        summary = filters.filter { it.isEnable }.map { it.title }.toString(),
                        onClick = { showDialog = true },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MaterialListItem(
                            onClick = { showDialog = true },
                            supportingContent = {
                                Text(filters.filter { it.isEnable }.map { it.title }.toString())
                            },
                            colors = itemColors(slot),
                            headlineContent = {
                                Text(stringResource(R.string.camera_portrait_filter_settings))
                            },
                        )
                    }
                }
                if (showDialog) {
                    CameraFilterDialog(
                        c = c,
                        state = state,
                        data = FilterDialogData(
                            key = "camera_portrait_filter_settings",
                            title = stringResource(R.string.camera_portrait_filter_settings),
                            filters = filters,
                        ),
                        onDismiss = { showDialog = false },
                    )
                }
            }
            custom(
                key = "camera_video_filter_settings",
                title = c.getString(R.string.camera_video_filter_settings),
            ) { slot ->
                var showDialog by remember { mutableStateOf(false) }
                val saved = state.getStringSet("camera_video_filter_settings")
                val filters = CameraUtils.getVideoCameraFilters(c).apply {
                    forEach { it.isEnable = it.key in saved }
                }
                if (LocalUiMode.current == UiMode.Miuix) {
                    MiuixListItem(
                        title = stringResource(R.string.camera_video_filter_settings),
                        summary = filters.filter { it.isEnable }.map { it.title }.toString(),
                        onClick = { showDialog = true },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        MaterialListItem(
                            onClick = { showDialog = true },
                            supportingContent = {
                                Text(filters.filter { it.isEnable }.map { it.title }.toString())
                            },
                            colors = itemColors(slot),
                            headlineContent = {
                                Text(stringResource(R.string.camera_video_filter_settings))
                            },
                        )
                    }
                }
                if (showDialog) {
                    CameraFilterDialog(
                        c = c,
                        state = state,
                        data = FilterDialogData(
                            key = "camera_video_filter_settings",
                            title = stringResource(R.string.camera_video_filter_settings),
                            filters = filters,
                        ),
                        onDismiss = { showDialog = false },
                    )
                }
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

    private data class FilterDialogData(
        val key: String,
        val title: String,
        val filters: List<CameraFilter>,
        val onSaved: (Set<String>) -> Unit = {},
    )

    /** 滤镜多选对话框：勾选集写回 state.set 并回调 onSaved */
    @Composable
    private fun CameraFilterDialog(
        c: Context,
        state: PrefState,
        data: FilterDialogData,
        onDismiss: () -> Unit,
    ) {
        // items / selectedKeys 用 remember(data) 固定身份：MultiChoiceDialog 的临时勾选按两者记忆，
        // 每次重组重建集合会把用户刚点的勾选重置回初始值
        val items = remember(data) {
            data.filters.map { filter -> ChoiceItem(key = filter.key, title = filter.title) }
        }
        val selectedKeys = remember(data) {
            data.filters.filter { it.isEnable }.map { it.key }.toSet()
        }
        MultiChoiceDialog(
            title = data.title,
            items = items,
            selectedKeys = selectedKeys,
            onDismiss = onDismiss,
            onConfirm = { keys ->
                state.set(data.key, keys)
                data.onSaved(keys)
                onDismiss()
            },
        )
    }
}
