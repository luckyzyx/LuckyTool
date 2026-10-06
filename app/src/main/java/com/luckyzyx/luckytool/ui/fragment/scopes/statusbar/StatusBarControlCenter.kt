package com.luckyzyx.luckytool.ui.fragment.scopes.statusbar

import android.content.Context
import android.graphics.Color
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.luckyzyx.colorpicker.ColorPickerPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.sendPrefsValue
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class StatusBarControlCenter : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.android.systemui")

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.statusBarControlCenter

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            title = getString(R.string.StatusBarControlCenter)
            summary = arraySummaryDot(
                getString(R.string.control_center_clock_show_second),
                getString(R.string.remove_control_center_clock_red_one)
            )
            key = "StatusBarControlCenter"
            isIconSpaceReserved = false
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            //时钟相关
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.ControlCenter_Clock_Related)
                key = "ControlCenter_Clock_Related"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_clock_view)
                key = "remove_control_center_clock_view"
                setDefaultValue(false)
                isVisible = osCode >= 34
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.control_center_clock_show_second)
                key = "control_center_clock_show_second"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.statusbar_control_center_clock_red_one_mode)
                summary = getString(R.string.current_mode) + ": %s"
                key = "statusbar_control_center_clock_red_one_mode"
                setEntries(R.array.statusbar_control_center_clock_red_one_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.statusbar_control_center_clock_colon_style)
                summary = getString(R.string.current_mode) + ": %s"
                key = "statusbar_control_center_clock_colon_style"
                setEntries(R.array.statusbar_control_center_clock_colon_style_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                isVisible = SDK >= A13
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_date_comma)
                key = "remove_control_center_date_comma"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.statusbar_control_center_date_show_lunar)
                key = "statusbar_control_center_date_show_lunar"
                setDefaultValue(false)
                isVisible = isZh(this@loadPreferences)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    (activity as MainActivity).restart()
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.statusbar_control_center_date_disable_text_scroll)
                key = "statusbar_control_center_date_disable_text_scroll"
                setDefaultValue(false)
                isVisible = SDK >= A13 && isZh(this@loadPreferences)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title =
                    getString(R.string.statusbar_control_center_date_set_display_mode_horizontal)
                summary = getString(R.string.current_mode) + ": %s"
                key = "statusbar_control_center_date_set_display_mode_horizontal"
                setEntries(R.array.statusbar_control_center_date_fix_lunar_horizontal_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isVisible = SDK >= A13 && isZh(this@loadPreferences) && getBoolean(
                    ModulePrefs, "statusbar_control_center_date_show_lunar", false
                )
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_carriers)
                key = "remove_control_center_carriers"
                setDefaultValue(false)
                isVisible = osCode >= 38
                isIconSpaceReserved = false
            })
            //通知中心
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.ControlCenterNotificationCenter)
                key = "ControlCenterNotificationCenter"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_notification_side_spacing)
                key = "enable_notification_side_spacing"
                setDefaultValue(false)
                isVisible = osCode >= 23
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "enable_notification_side_spacing", false)) {
                if (osCode >= 23) {
                    add(SeekBarPreference(this@loadPreferences).apply {
                        title = getString(R.string.custom_notification_side_spacing_vertical)
                        key = "custom_notification_side_spacing_vertical"
                        setDefaultValue(0)
                        max = 30
                        min = -30
                        showSeekBarValue = true
                        updatesContinuously = false
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, newValue ->
                            sendPrefsValue("com.android.systemui", key, newValue)
                            true
                        }
                    })
                    add(SeekBarPreference(this@loadPreferences).apply {
                        title = getString(R.string.custom__notification_side_spacing_horizontal)
                        key = "custom__notification_side_spacing_horizontal"
                        setDefaultValue(0)
                        max = 30
                        min = -30
                        showSeekBarValue = true
                        updatesContinuously = false
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, newValue ->
                            sendPrefsValue("com.android.systemui", key, newValue)
                            true
                        }
                    })
                }
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_notification_importance_classification)
                key = "enable_notification_importance_classification"
                setDefaultValue(false)
                isVisible = osCode < 30
                isIconSpaceReserved = false
            })
            if (osCode in 30..33) {
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_notification_background_transparency)
                    summary = getString(R.string.force_enable_systemui_blur_feature_tips)
                    key = "custom_notification_background_transparency"
                    setDefaultValue(-1)
                    max = 10
                    min = -1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("com.android.systemui", key, newValue)
                        true
                    }
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_notification_background_blur_effect)
                    summary = arraySummaryLine(
                        getString(R.string.force_enable_systemui_blur_feature_tips),
                        getString(R.string.force_enable_systemui_blur_feature_tips_2)
                    )
                    key = "enable_notification_background_blur_effect"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("com.android.systemui", key, newValue)
                        true
                    }
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_notification_cleanup_button)
                key = "remove_notification_cleanup_button"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //滑动条相关
            if (osCode in 26..33) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.ControlCenter_Silder_Related)
                    key = "ControlCenter_Silder_Related"
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_control_center_silder_transparency)
                    key = "custom_control_center_silder_transparency"
                    setDefaultValue(-1)
                    max = 10
                    min = -1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                })
            }
            //UI相关
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.ControlCenter_UI_Related)
                key = "ControlCenter_UI_Related"
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_control_center_volume_seekbar_mode)
                summary = getString(R.string.current_mode) + ": %s"
                key = "set_control_center_volume_seekbar_mode"
                setEntries(R.array.set_control_center_volume_seekbar_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isVisible = osCode >= 31
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_control_center_progress_percent_display)
                key = "enable_control_center_progress_percent_display"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "enable_control_center_progress_percent_display", false)) {
                add(ColorPickerPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_control_center_progress_percent_color)
                    key = "custom_control_center_progress_percent_color"
                    setDefaultValue(Color.WHITE)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("com.android.systemui", key, newValue)
                        true
                    }
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_edit_button)
                summary = getString(R.string.separate_control_center_mode_only)
                key = "remove_control_center_edit_button"
                setDefaultValue(false)
                isVisible = osCode >= 34
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_more_button)
                summary = getString(R.string.separate_control_center_mode_only)
                key = "remove_control_center_more_button"
                setDefaultValue(false)
                isVisible = osCode in 34..39
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_settings_button)
                summary = getString(R.string.separate_control_center_mode_only)
                key = "remove_control_center_settings_button"
                setDefaultValue(false)
                isVisible = osCode >= 38
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_auto_brightness_button_mode)
                summary = getString(R.string.current_mode) + ": %s"
                key = "set_auto_brightness_button_mode"
                setEntries(R.array.statusbar_control_center_auto_brightness_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_user_switcher)
                key = "remove_control_center_user_switcher"
                setDefaultValue(false)
                isVisible = SDK < A13
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_mydevice)
                key = "remove_control_center_mydevice"
                setDefaultValue(false)
                isVisible = SDK >= A13
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_control_center_search_button_mode)
                summary = getString(R.string.current_mode) + ": %s"
                key = "set_control_center_search_button_mode"
                setEntries(R.array.set_control_center_search_button_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_networkwarn)
                summary = arraySummaryLine(
                    getString(R.string.current_mode) + ": %s",
                    getString(R.string.remove_control_center_networkwarn_summary)
                )
                key = "remove_control_center_networkwarn"
                setEntries(R.array.statusbar_control_center_networkwarn_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
            add(SeekBarPreference(this@loadPreferences).apply {
                title = getString(R.string.custom_control_center_background_transparency)
                summary = getString(R.string.force_enable_systemui_blur_feature_tips)
                key = "custom_control_center_background_transparency"
                setDefaultValue(-1)
                max = 10
                min = -1
                showSeekBarValue = true
                updatesContinuously = false
                isVisible = osCode < 34
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    true
                }
            })
        }
    }
}