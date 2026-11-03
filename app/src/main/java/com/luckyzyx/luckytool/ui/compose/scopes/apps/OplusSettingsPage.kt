package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.contract.CropImageContract
import com.luckyzyx.luckytool.data.CropImageContractOptions
import com.luckyzyx.luckytool.ui.compose.components.PrefImageRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.components.PrefVideoRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getColonSummary
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.showToast

/**
 * 设置页：系统设置各分类的开关与取值（状态栏、锁屏、显示、声音、应用、安全、关于本机、其它）。
 * 「连接与共享」category 与 force_display_multi_screen_connect、enable_vip_mode 均 isVisible=false 从不展示，本页不含这些条目；
 * 「权限与隐私」category 依赖 SDK >= 666 永假，等价空组。
 */
object OplusSettingsPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_settings",
        prefsName = ModulePrefs,
        packName = "com.android.settings",
        scopes = arrayOf(
            "com.android.settings",
            "com.oplus.safecenter",
            "com.oplus.notificationmanager"
        ),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        // 状态栏
        category(c.getString(R.string.settings_status_bar))
        if (osCode < 40) {
            switch(
                key = "enable_statusbar_clock_format",
                title = c.getString(R.string.enable_statusbar_clock_format),
                summary = c.getString(R.string.enable_statusbar_clock_format_summary),
            )
        }
        // 锁屏
        category(c.getString(R.string.settings_lock_screen))
        switch(
            key = "enable_show_never_timeout",
            title = c.getString(R.string.enable_show_never_timeout),
            summary = c.getString(R.string.enable_show_never_timeout_summary),
        )
        // 显示
        category(c.getString(R.string.settings_display))
        if (osCode >= 30) {
            switch(
                key = "enable_extra_brightness",
                title = c.getString(R.string.enable_extra_brightness),
                summary = arraySummaryLine(c.getString(R.string.need_restart_system)),
            )
            switch(
                key = "enable_lowest_allowed_brightness",
                title = c.getString(R.string.enable_lowest_allowed_brightness),
            )
        }
        if (osCode >= 26) {
            switch(
                key = "enable_video_memc_frame_insertion",
                title = c.getString(R.string.enable_video_memc_frame_insertion),
                summary = arraySummaryLine(
                    c.getString(R.string.enable_video_memc_frame_insertion_summary),
                    c.getString(R.string.need_restart_system)
                ),
            )
            if (state.getBoolean("enable_video_memc_frame_insertion")) {
                page(
                    title = c.getString(R.string.custom_video_dynamic_frame_insertion_configuration),
                    target = "memcConfigFragment",
                    summary = arraySummaryLine(c.getString(R.string.need_restart_system)),
                )
                switch(
                    key = "video_frame_insertion_support_2K120",
                    title = c.getString(R.string.video_frame_insertion_support_2K120),
                    summary = c.getString(R.string.video_frame_insertion_support_2K120_summary),
                )
            }
        }
        if (osCode >= 27 && Settings.System.getUriFor("oplus_settings_switch_color_mode") != null) {
            switch(
                key = "enable_screen_color_temperature_rgb_ball",
                title = c.getString(R.string.enable_screen_color_temperature_rgb_ball),
                summary = arraySummaryLine(
                    c.getString(R.string.need_restart_system),
                    c.getString(R.string.enable_screen_color_temperature_rgb_palette_summary)
                ),
                onChange = { v ->
                    if (v) state.set("enable_screen_color_temperature_rgb_space", false)
                },
            )
        }
        if (osCode >= 30 && Settings.System.getUriFor("color_space_adjustment") != null) {
            switch(
                key = "enable_screen_color_temperature_rgb_space",
                title = c.getString(R.string.enable_screen_color_temperature_rgb_space),
                summary = arraySummaryLine(
                    c.getString(R.string.need_restart_system),
                    c.getString(R.string.enable_screen_color_temperature_rgb_palette_summary)
                ),
                onChange = { v ->
                    if (v) state.set("enable_screen_color_temperature_rgb_ball", false)
                },
            )
        }
        switch(
            key = "enable_smart_switching_screen_resolutions",
            title = c.getString(R.string.enable_smart_switching_screen_resolutions),
        )
        if (osCode >= 35) {
            switch(
                key = "force_enable_reduce_white_point_value",
                title = c.getString(R.string.force_enable_reduce_white_point_value),
            )
        }
        // 声音
        if (osCode >= 27) {
            category(c.getString(R.string.settings_sound))
            if (SDK >= A14) {
                switch(
                    key = "enable_clear_voice",
                    title = c.getString(R.string.enable_clear_voice),
                    summary = arraySummaryLine(
                        c.getString(R.string.enable_clear_voice_tips),
                        c.getString(R.string.need_restart_system)
                    ),
                )
            }
            switch(
                key = "enable_holographic_audio",
                title = c.getString(R.string.enable_holographic_audio),
            )
        }
        // 应用
        category(c.getString(R.string.settings_application))
        switch(
            key = "force_display_process_management",
            title = c.getString(R.string.force_display_process_management),
        )
        switch(
            key = "force_display_disabled_apps_manager",
            title = c.getString(R.string.force_display_disabled_apps_manager),
        )
        if (SDK >= A13) {
            switch(
                key = "auto_unlock_restricted_settings",
                title = c.getString(R.string.auto_unlock_restricted_settings),
                summary = c.getString(R.string.auto_unlock_restricted_settings_summary),
            )
        }
        switch(
            key = "auto_jump_accessibility_settings",
            title = c.getString(R.string.auto_jump_accessibility_settings),
        )
        if (osCode >= 27) {
            switch(
                key = "enable_dedicated_ram_for_games",
                title = c.getString(R.string.enable_dedicated_ram_for_games),
            )
        }
        // 密码与安全
        category(c.getString(R.string.settings_password_and_security))
        switch(
            key = "enable_google_auto_fill",
            title = c.getString(R.string.enable_google_auto_fill),
        )
        switch(
            key = "force_display_password_management_settings",
            title = c.getString(R.string.restore_password_management_settings),
        )
        switch(
            key = "disable_device_admin_verification_dialog",
            title = c.getString(R.string.disable_device_admin_verification_dialog),
        )
        // 其他设置
        category(c.getString(R.string.settings_other_advanced_settings))
        switch(
            key = "enable_swipe_up_navigation_gesture",
            title = c.getString(R.string.enable_swipe_up_navigation_gesture),
        )
        if (osCode >= 30) {
            switch(
                key = "enable_touch_membrane_protector_mode",
                title = c.getString(R.string.enable_touch_membrane_protector_mode),
            )
            switch(
                key = "disable_otg_auto_off",
                title = c.getString(R.string.disable_otg_auto_off),
                summary = c.getString(R.string.disable_otg_auto_off_summary),
            )
            switch(
                key = "enable_game_acceleration",
                title = c.getString(R.string.enable_game_acceleration),
                summary = arraySummaryLine(c.getString(R.string.need_restart_system)),
            )
            if (isZh(c)) {
                switch(
                    key = "force_display_content_recommend",
                    title = c.getString(R.string.force_display_content_recommend),
                )
            }
        }
        // 关于本机
        if (SDK >= A13) {
            category(c.getString(R.string.settings_about_device))
            if (osCode >= 30) {
                switch(
                    key = "remove_device_name_change_limit",
                    title = c.getString(R.string.remove_device_name_change_limit),
                )
            }
            list(
                key = "set_processor_click_page",
                title = c.getString(R.string.set_processor_click_page),
                entries = c.resources.getStringArray(R.array.set_processor_click_page_entries),
                entryValues = arrayOf("0", "1", "2", "3"),
                default = "0",
                summary = "%s",
            )
            if (state.getString("set_processor_click_page", "0") == "3") {
                switch(
                    key = "custom_processor_image_path_switch",
                    title = c.getString(R.string.custom_processor_image_path_switch),
                    summary = getColonSummary(c.getString(R.string.recommended_size), "624x352"),
                )
                if (state.getBoolean("custom_processor_image_path_switch")) {
                    val processorTitle = c.getString(R.string.customize_device_ota_card_background_path)
                    val processorPath = state.getString("customize_processor_image_path", "") ?: ""
                    val processorSummary = processorPath.ifBlank { "Null" }
                    custom(
                        key = "customize_processor_image_path",
                        title = processorTitle,
                        summary = processorSummary,
                    ) {
                        val launcher = rememberLauncherForActivityResult(CropImageContract()) {
                            if (it.second.isSuccessful) {
                                val uri = it.second.uriContent
                                val path = uri?.path ?: ""
                                if (uri == null || uri == Uri.EMPTY) {
                                    return@rememberLauncherForActivityResult
                                }
                                if (path.isNotBlank()) {
                                    c.showToast(path)
                                    c.putString(ModulePrefs, it.first, path)
                                }
                            } else {
                                LogUtils.e("CropImage", it.first, it.second.error.toString(), true)
                            }
                        }
                        PrefRowInset {
                            PrefRow(
                                title = processorTitle,
                                summary = processorSummary,
                                onClick = {
                                    val cacheImageFile = FileUtils.createCacheFile(c, "png")
                                    val cacheImageUri = FileProvider.getUriForFile(
                                        c, "${BuildConfig.APPLICATION_ID}.FileProvider", cacheImageFile
                                    )
                                    launcher.launch(
                                        "customize_processor_image_path" to CropImageContractOptions(
                                            null, CropImageOptions().apply {
                                                activityTitle = processorTitle
                                                cropShape = CropImageView.CropShape.RECTANGLE
                                                guidelines = CropImageView.Guidelines.ON_TOUCH
                                                aspectRatioX = 624
                                                aspectRatioY = 352
                                                fixAspectRatio = true
                                                customOutputUri = cacheImageUri
                                                outputCompressFormat = Bitmap.CompressFormat.PNG
                                                outputCompressQuality = 100
                                            }
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
                switch(
                    key = "custom_processor_introduction_text",
                    title = c.getString(R.string.custom_processor_introduction_text),
                    summary = c.getString(R.string.custom_processor_introduction_text_summary),
                )
            }
            if (osCode >= 27) {
                switch(
                    key = "enable_mariana_npu_introduction_page",
                    title = c.getString(R.string.enable_mariana_npu_introduction_page),
                )
                switch(
                    key = "enable_hasselblad_camera_introduction_page",
                    title = c.getString(R.string.enable_hasselblad_camera_introduction_page),
                )
            }
            if (osCode >= 34) {
                switch(
                    key = "enable_game_architecture_display",
                    title = c.getString(R.string.enable_game_architecture_display),
                )
            }
            if (osCode >= 27) {
                switch(
                    key = "screen_physics_size_shown_cm",
                    title = c.getString(R.string.screen_physics_size_shown_cm),
                )
            }
            switch(
                key = "customize_device_ota_card_background",
                title = c.getString(R.string.customize_device_ota_card_background),
                summary = c.getString(R.string.customize_device_ota_card_background_summary),
            )
            if (state.getBoolean("customize_device_ota_card_background")) {
                val otaTitle = c.getString(R.string.customize_device_ota_card_background_path)
                val otaPath = state.getString("customize_device_ota_card_background_path", "") ?: ""
                val otaSummary = otaPath.ifBlank { "Null" }
                custom(
                    key = "customize_device_ota_card_background_path",
                    title = otaTitle,
                    summary = otaSummary,
                ) {
                    val launcher = rememberLauncherForActivityResult(CropImageContract()) {
                        if (it.second.isSuccessful) {
                            val uri = it.second.uriContent
                            val path = uri?.path ?: ""
                            if (uri == null || uri == Uri.EMPTY) {
                                return@rememberLauncherForActivityResult
                            }
                            if (path.isNotBlank()) {
                                c.showToast(path)
                                c.putString(ModulePrefs, it.first, path)
                            }
                        } else {
                            LogUtils.e("CropImage", it.first, it.second.error.toString(), true)
                        }
                    }
                    PrefImageRow(
                        title = otaTitle,
                        path = otaPath,
                        onClick = {
                            val cacheImageFile = FileUtils.createCacheFile(c, "png")
                            val cacheImageUri = FileProvider.getUriForFile(
                                c, "${BuildConfig.APPLICATION_ID}.FileProvider", cacheImageFile
                            )
                            launcher.launch(
                                "customize_device_ota_card_background_path" to CropImageContractOptions(
                                    null, CropImageOptions().apply {
                                        activityTitle = otaTitle
                                        cropShape = CropImageView.CropShape.RECTANGLE
                                        guidelines = CropImageView.Guidelines.ON_TOUCH
                                        if (osCode >= 34) {
                                            aspectRatioX = 984
                                            aspectRatioY = 702
                                        } else {
                                            aspectRatioX = 328
                                            aspectRatioY = 124
                                        }
                                        fixAspectRatio = true
                                        customOutputUri = cacheImageUri
                                        outputCompressFormat = Bitmap.CompressFormat.PNG
                                        outputCompressQuality = 100
                                    }
                                )
                            )
                        },
                        // 长按清除已选背景：hook 侧读到空路径就回落系统默认封面
                        onLongClick = {
                            state.set("customize_device_ota_card_background_path", null)
                        },
                    )
                }
                switch(
                    key = "hide_ota_card_top_text",
                    title = c.getString(R.string.hide_ota_card_top_text),
                )
                if (osCode >= 34) {
                    switch(
                        key = "apply_device_parameter_sharing_page",
                        title = c.getString(R.string.apply_device_parameter_sharing_page),
                    )
                }
            }
            // 自定义 OTA 卡片视频（仅 ColorOS 17 及以上）
            if (osCode >= 40) {
                switch(
                    key = "customize_device_ota_card_video",
                    title = c.getString(R.string.customize_device_ota_card_video),
                    summary = c.getString(R.string.customize_device_ota_card_video_summary),
                )
                if (state.getBoolean("customize_device_ota_card_video")) {
                    val videoTitle = c.getString(R.string.customize_device_ota_card_video_path)
                    val videoPath =
                        state.getString("customize_device_ota_card_video_path", "").orEmpty()
                    val videoSummary = videoPath.ifBlank { "Null" }
                    custom(
                        key = "customize_device_ota_card_video_path",
                        title = videoTitle,
                        summary = videoSummary,
                    ) {
                        val videoLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) { uri ->
                            // 取消选择时 uri 为 null
                            if (uri == null || uri == Uri.EMPTY) {
                                return@rememberLauncherForActivityResult
                            }
                            // hook 侧只认缓存目录内的绝对路径：先落盘再写 pref
                            val copied = FileUtils.copyUriToFile(
                                c, uri, FileUtils.createCacheFile(c, "mp4")
                            )
                            if (copied == null) {
                                LogUtils.e("OtaCardVideo", "copyUriToFile", uri.toString(), true)
                                return@rememberLauncherForActivityResult
                            }
                            c.showToast(copied.absolutePath)
                            c.putString(
                                ModulePrefs,
                                "customize_device_ota_card_video_path",
                                copied.absolutePath
                            )
                        }
                        PrefVideoRow(
                            title = videoTitle,
                            path = videoPath,
                            summary = videoSummary,
                            onClick = { videoLauncher.launch("video/*") },
                            // 长按清除已选视频：hook 侧读到空路径就不再接管视频
                            onLongClick = {
                                state.set("customize_device_ota_card_video_path", null)
                            },
                        )
                    }
                }
            }
            switch(
                key = "customize_device_sharing_page_parameters",
                title = c.getString(R.string.customize_device_sharing_page_parameters),
            )
        }
        // 其他首选项
        category(c.getString(R.string.settings_other_preference))
        switch(
            key = "remove_top_account_display",
            title = c.getString(R.string.remove_top_account_display),
        )
        if (isZh(c)) {
            switch(
                key = "disable_cn_special_edition_setting",
                title = c.getString(R.string.disable_cn_special_edition_setting),
            )
        }
        if (state.getBoolean("disable_cn_special_edition_setting")) {
            if (isZh(c)) {
                switch(
                    key = "fix_default_app_jump_problem",
                    title = c.getString(R.string.fix_default_app_jump_problem),
                    summary = c.getString(R.string.fix_default_app_jump_problem_summary),
                )
                switch(
                    key = "force_display_auto_launch_jump_option",
                    title = c.getString(R.string.force_display_auto_launch_jump_option),
                )
            }
        }
        switch(
            key = "remove_settings_bottom_laboratory",
            title = c.getString(R.string.remove_settings_bottom_laboratory),
        )
        switch(
            key = "force_display_bottom_google_settings",
            title = c.getString(R.string.force_display_bottom_google_settings),
        )
        // 开发者选项
        category(c.getString(R.string.settings_developer_preference))
        switch(
            key = "remove_dpi_restart_recovery",
            title = c.getString(R.string.remove_dpi_restart_recovery),
            summary = c.getString(R.string.remove_dpi_restart_recovery_summary),
            onChange = { v -> c.sendPrefsValue("system", "remove_dpi_restart_recovery", v) },
        )
    }
}
