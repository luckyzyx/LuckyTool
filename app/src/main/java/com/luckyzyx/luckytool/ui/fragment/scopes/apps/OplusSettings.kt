package com.luckyzyx.luckytool.ui.fragment.scopes.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.fragment.findNavController
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.contract.CropImageContract
import com.luckyzyx.luckytool.data.CropImageContractOptions
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getColonSummary
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.openApp
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import com.luckyzyx.luckytool.utils.showToast
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusSettings : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf(
        "com.android.settings",
        "com.oplus.safecenter",
        "com.oplus.notificationmanager"
    )

    private val cropImage = registerForActivityResult(CropImageContract()) {
        if (it.second.isSuccessful) {
            val uri = it.second.uriContent
            val path = uri?.path ?: ""
            if (uri == null || uri == Uri.EMPTY) return@registerForActivityResult
            if (path.isNotBlank()) {
                requireActivity().showToast(path)
                requireActivity().putString(ModulePrefs, it.first, path)
                (activity as MainActivity).restart()
            }
        } else {
            LogUtils.e("CropImage", it.first, it.second.error.toString(), true)
        }
    }

    override val isEnableRestartMenu: Boolean = true

    override val isEnableOpenMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.oplusSettings

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.android.settings"
            setPrefsIconRes(key) { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryDot(
                getString(R.string.remove_top_account_display),
                getString(R.string.remove_dpi_restart_recovery)
            )
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            //连接与共享
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_connection_sharing)
                key = "settings_connection_sharing"
                isVisible = false
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_multi_screen_connect)
                key = "force_display_multi_screen_connect"
                setDefaultValue(false)
                isVisible = false
                isIconSpaceReserved = false
            })
            //状态栏
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_status_bar)
                key = "settings_status_bar"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_statusbar_clock_format)
                summary = getString(R.string.enable_statusbar_clock_format_summary)
                key = "enable_statusbar_clock_format"
                setDefaultValue(false)
                isVisible = osCode < 40
                isIconSpaceReserved = false
            })
            //锁屏
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_lock_screen)
                key = "settings_lock_screen"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_show_never_timeout)
                summary = getString(R.string.enable_show_never_timeout_summary)
                key = "enable_show_never_timeout"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //显示
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_display)
                key = "settings_display"
                isIconSpaceReserved = false
            })
            if (osCode >= 30) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_extra_brightness)
                    summary = arraySummaryLine(
                        getString(R.string.need_restart_system)
                    )
                    key = "enable_extra_brightness"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_lowest_allowed_brightness)
                    key = "enable_lowest_allowed_brightness"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            if (osCode >= 26) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_video_memc_frame_insertion)
                    summary = arraySummaryLine(
                        getString(R.string.enable_video_memc_frame_insertion_summary),
                        getString(R.string.need_restart_system)
                    )
                    key = "enable_video_memc_frame_insertion"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                })
                if (getBoolean(ModulePrefs, "enable_video_memc_frame_insertion", false)) {
                    add(Preference(this@loadPreferences).apply {
                        title =
                            getString(R.string.custom_video_dynamic_frame_insertion_configuration)
                        summary = arraySummaryLine(
                            getString(R.string.need_restart_system)
                        )
                        key = "custom_video_dynamic_frame_insertion_configuration"
                        isIconSpaceReserved = false
                        setOnPreferenceClickListener {
                            findNavController().navigatePage(R.id.memcConfigFragment, title)
                            true
                        }
                    })
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.video_frame_insertion_support_2K120)
                        summary = getString(R.string.video_frame_insertion_support_2K120_summary)
                        key = "video_frame_insertion_support_2K120"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                    })
                }
            }
            if (osCode >= 27) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_screen_color_temperature_rgb_ball)
                    summary = arraySummaryLine(
                        getString(R.string.need_restart_system),
                        getString(R.string.enable_screen_color_temperature_rgb_palette_summary)
                    )
                    key = "enable_screen_color_temperature_rgb_ball"
                    setDefaultValue(false)
                    isVisible =
                        Settings.System.getUriFor("oplus_settings_switch_color_mode") != null
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean) findPreference<SwitchPreference>(
                            "enable_screen_color_temperature_rgb_space"
                        )?.isChecked = false
                        true
                    }
                })
            }
            if (osCode >= 30) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_screen_color_temperature_rgb_space)
                    summary = arraySummaryLine(
                        getString(R.string.need_restart_system),
                        getString(R.string.enable_screen_color_temperature_rgb_palette_summary)
                    )
                    key = "enable_screen_color_temperature_rgb_space"
                    setDefaultValue(false)
                    isVisible = Settings.System.getUriFor("color_space_adjustment") != null
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean) findPreference<SwitchPreference>(
                            "enable_screen_color_temperature_rgb_ball"
                        )?.isChecked = false
                        true
                    }
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_smart_switching_screen_resolutions)
                key = "enable_smart_switching_screen_resolutions"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            if (osCode >= 35) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.force_enable_reduce_white_point_value)
                    key = "force_enable_reduce_white_point_value"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            //声音
            if (osCode >= 27) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.settings_sound)
                    key = "settings_sound"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_clear_voice)
                    summary = arraySummaryLine(
                        getString(R.string.enable_clear_voice_tips),
                        getString(R.string.need_restart_system)
                    )
                    key = "enable_clear_voice"
                    setDefaultValue(false)
                    isVisible = SDK >= A14
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_holographic_audio)
                    key = "enable_holographic_audio"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            //应用
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_application)
                key = "settings_application"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_process_management)
                key = "force_display_process_management"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_disabled_apps_manager)
                key = "force_display_disabled_apps_manager"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.auto_unlock_restricted_settings)
                summary = getString(R.string.auto_unlock_restricted_settings_summary)
                key = "auto_unlock_restricted_settings"
                setDefaultValue(false)
                isVisible = SDK >= A13
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.auto_jump_accessibility_settings)
                key = "auto_jump_accessibility_settings"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_dedicated_ram_for_games)
                key = "enable_dedicated_ram_for_games"
                setDefaultValue(false)
                isVisible = osCode >= 27
                isIconSpaceReserved = false
            })
            //密码与安全
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_password_and_security)
                key = "settings_password_and_security"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_google_auto_fill)
                key = "enable_google_auto_fill"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.restore_password_management_settings)
                key = "force_display_password_management_settings"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_device_admin_verification_dialog)
                key = "disable_device_admin_verification_dialog"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //权限与隐私
            if (SDK >= 666) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.settings_authority_and_privacy)
                    key = "settings_authority_and_privacy"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_vip_mode)
                    key = "enable_vip_mode"
                    setDefaultValue(false)
                    isVisible = false
                    isIconSpaceReserved = false
                })
            }
            //其他设置
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_other_advanced_settings)
                key = "settings_other_advanced_settings"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_swipe_up_navigation_gesture)
                key = "enable_swipe_up_navigation_gesture"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_touch_membrane_protector_mode)
                key = "enable_touch_membrane_protector_mode"
                setDefaultValue(false)
                isVisible = osCode >= 30
                isIconSpaceReserved = false
            })
            if (osCode >= 30) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.disable_otg_auto_off)
                    summary = getString(R.string.disable_otg_auto_off_summary)
                    key = "disable_otg_auto_off"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_game_acceleration)
                summary = arraySummaryLine(
                    getString(R.string.need_restart_system)
                )
                key = "enable_game_acceleration"
                setDefaultValue(false)
                isVisible = osCode >= 30
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_content_recommend)
                key = "force_display_content_recommend"
                setDefaultValue(false)
                isVisible = isZh(this@loadPreferences)
                isIconSpaceReserved = false
            })
            //关于本机
            if (SDK >= A13) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.settings_about_device)
                    key = "settings_about_device"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.remove_device_name_change_limit)
                    key = "remove_device_name_change_limit"
                    setDefaultValue(false)
                    isVisible = osCode >= 30
                    isIconSpaceReserved = false
                })
                add(DropDownPreference(this@loadPreferences).apply {
                    title = getString(R.string.set_processor_click_page)
                    summary = "%s"
                    key = "set_processor_click_page"
                    setEntries(R.array.set_processor_click_page_entries)
                    entryValues = arrayOf("0", "1", "2", "3")
                    setDefaultValue("0")
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                })
                if (getString(ModulePrefs, "set_processor_click_page", "0") == "3") {
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.custom_processor_image_path_switch)
                        summary = getColonSummary(
                            getString(R.string.recommended_size), "624x352"
                        )
                        key = "custom_processor_image_path_switch"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, _ ->
                            (activity as MainActivity).restart()
                            true
                        }
                    })
                    if (getBoolean(ModulePrefs, "custom_processor_image_path_switch", false)) {
                        add(Preference(this@loadPreferences).apply {
                            title = getString(R.string.customize_device_ota_card_background_path)
                            key = "customize_processor_image_path"
                            val path = getString(ModulePrefs, key, "")
                            if (path.isBlank()) {
                                summary = "Null"
                                isIconSpaceReserved = false
                            } else {
                                icon = BitmapFactory.decodeFile(path)?.toDrawable(resources)
                                summary = path
                                isCopyingEnabled = true
                            }
                            setOnPreferenceClickListener {
                                val cacheImageFile =
                                    FileUtils.createCacheFile(requireActivity(), "png")
                                val cacheImageUri = FileProvider.getUriForFile(
                                    context, "${BuildConfig.APPLICATION_ID}.FileProvider",
                                    cacheImageFile
                                )
                                cropImage.launch(
                                    key to CropImageContractOptions(
                                        null, CropImageOptions().apply {
                                            activityTitle = title?.toString() ?: ""
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
                                true
                            }
                        })
                    }
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.custom_processor_introduction_text)
                        summary = getString(R.string.custom_processor_introduction_text_summary)
                        key = "custom_processor_introduction_text"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                    })
                }
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_mariana_npu_introduction_page)
                    key = "enable_mariana_npu_introduction_page"
                    setDefaultValue(false)
                    isVisible = osCode >= 27
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_hasselblad_camera_introduction_page)
                    key = "enable_hasselblad_camera_introduction_page"
                    setDefaultValue(false)
                    isVisible = osCode >= 27
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_game_architecture_display)
                    key = "enable_game_architecture_display"
                    setDefaultValue(false)
                    isVisible = osCode >= 34
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.screen_physics_size_shown_cm)
                    key = "screen_physics_size_shown_cm"
                    setDefaultValue(false)
                    isVisible = osCode >= 27
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.customize_device_ota_card_background)
                    summary = getString(R.string.customize_device_ota_card_background_summary)
                    key = "customize_device_ota_card_background"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                })
                if (getBoolean(
                        ModulePrefs, "customize_device_ota_card_background", false
                    )
                ) {
                    add(Preference(this@loadPreferences).apply {
                        title = getString(R.string.customize_device_ota_card_background_path)
                        key = "customize_device_ota_card_background_path"
                        val path = getString(ModulePrefs, key, "")
                        if (path.isBlank()) {
                            summary = "Null"
                            isIconSpaceReserved = false
                        } else {
                            icon = BitmapFactory.decodeFile(path)?.toDrawable(resources)
                            summary = path
                            isCopyingEnabled = true
                        }
                        setOnPreferenceClickListener {
                            val cacheImageFile = FileUtils.createCacheFile(requireActivity(), "png")
                            val cacheImageUri = FileProvider.getUriForFile(
                                context, "${BuildConfig.APPLICATION_ID}.FileProvider",
                                cacheImageFile
                            )
                            cropImage.launch(
                                key to CropImageContractOptions(
                                    null, CropImageOptions().apply {
                                        activityTitle = title?.toString() ?: ""
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
                            true
                        }
                    })
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.hide_ota_card_top_text)
                        key = "hide_ota_card_top_text"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                    })
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.apply_device_parameter_sharing_page)
                        key = "apply_device_parameter_sharing_page"
                        setDefaultValue(false)
                        isVisible = osCode >= 34
                        isIconSpaceReserved = false
                    })
                }
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.customize_device_sharing_page_parameters)
                    key = "customize_device_sharing_page_parameters"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            //其他首选项
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_other_preference)
                key = "settings_other_preference"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_top_account_display)
                key = "remove_top_account_display"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_cn_special_edition_setting)
                key = "disable_cn_special_edition_setting"
                setDefaultValue(false)
                isVisible = isZh(this@loadPreferences)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "disable_cn_special_edition_setting", false)) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.fix_default_app_jump_problem)
                    summary = getString(R.string.fix_default_app_jump_problem_summary)
                    key = "fix_default_app_jump_problem"
                    setDefaultValue(false)
                    isVisible = isZh(this@loadPreferences)
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.force_display_auto_launch_jump_option)
                    key = "force_display_auto_launch_jump_option"
                    setDefaultValue(false)
                    isVisible = isZh(this@loadPreferences)
                    isIconSpaceReserved = false
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_settings_bottom_laboratory)
                key = "remove_settings_bottom_laboratory"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_bottom_google_settings)
                key = "force_display_bottom_google_settings"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //开发者选项
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.settings_developer_preference)
                key = "settings_developer_preference"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_dpi_restart_recovery)
                summary = getString(R.string.remove_dpi_restart_recovery_summary)
                key = "remove_dpi_restart_recovery"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    true
                }
            })
        }
    }

    override fun callOpenMenu() = requireActivity().openApp(scopes)
}