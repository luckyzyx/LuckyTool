package com.luckyzyx.luckytool.ui.fragment

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.yukihookapi.hook.xposed.prefs.ui.ModulePreferenceFragment
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.tools.A13
import com.luckyzyx.luckytool.utils.tools.SDK
import com.luckyzyx.luckytool.utils.tools.XposedPrefs

class Android : ModulePreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    setTitle(R.string.corepatch)
                    setSummary(R.string.corepatch_summary)
                    key = "CorePatch"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    setTitle(R.string.downgr)
                    setSummary(R.string.downgr_summary)
                    key = "downgrade"
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    setTitle(R.string.authcreak)
                    setSummary(R.string.authcreak_summary)
                    key = "authcreak"
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    setTitle(R.string.digestCreak)
                    setSummary(R.string.digestCreak_summary)
                    key = "digestCreak"
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    setTitle(R.string.UsePreSig)
                    setSummary(R.string.UsePreSig_summary)
                    key = "UsePreSig"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    setTitle(R.string.enhancedMode)
                    setSummary(R.string.enhancedMode_summary)
                    key = "enhancedMode"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        if (key == "UsePreSig" && sharedPreferences!!.getBoolean(key, false)) {
            MaterialAlertDialogBuilder(requireActivity())
                .setMessage(R.string.usepresig_warn)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
}

class StatusBar : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        findNavController()
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.StatusBarClock)
                    summary = getString(R.string.statusbar_clock_show_second)+","+getString(R.string.statusbar_clock_show_doublerow)+","+getString(
                        R.string.statusbar_clock_doublerow_fontsize)
                    key = "StatusBarClock"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(R.id.action_statusBar_to_statusBarClock,Bundle().apply {
                            putCharSequence("title_label",title)
                        })
                        true
                    }
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.DropDownStatusBarClock)
                    summary = getString(R.string.dropdown_statusbar_clock_show_second)+","+getString(R.string.remove_dropdown_statusbar_clock_style)
                    key = "DropDownStatusBarClock"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(R.id.action_statusBar_to_dropDownStatusBarClock,Bundle().apply {
                            putCharSequence("title_label",title)
                        })
                        true
                    }
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.StatusBarDate)
                    summary = getString(R.string.remove_statusbar_date_comma)
                    key = "StatusBarDate"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(
                            R.id.action_statusBar_to_statusBarDate,Bundle().apply {
                                putCharSequence("title_label",title)
                            })
                        true
                    }
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.StatusBarNotice)
                    summary = getString(R.string.remove_statusbar_top_notification)+","+getString(R.string.remove_charging_completed)
                    key = "StatusBarNotice"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(
                            R.id.action_statusBar_to_statusBarNotice,Bundle().apply {
                                putCharSequence("title_label",title)
                            })
                        true
                    }
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.StatusBarIcon)
                    summary = getString(R.string.remove_statusbar_battery_percent)+","+getString(R.string.remove_statusbar_user_switcher)
                    key = "StatusBarIcon"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(
                            R.id.action_statusBar_to_statusBarIcon,Bundle().apply {
                                putCharSequence("title_label",title)
                            })
                        true
                    }
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.StatusBarContent)
                    summary = getString(R.string.remove_drop_down_statusbar_mydevice)+","+getString(R.string.tile_unexpanded_columns_vertical)
                    key = "StatusBarContent"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(
                            R.id.action_statusBar_to_statusBarTile,Bundle().apply {
                                putCharSequence("title_label",title)
                            })
                        true
                    }
                }
            )
        }
    }
}

class StatusBarClock : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_enable)
                    key = "statusbar_clock_enable"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_year)
                    key = "statusbar_clock_show_year"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_month)
                    key = "statusbar_clock_show_month"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_day)
                    key = "statusbar_clock_show_day"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_week)
                    key = "statusbar_clock_show_week"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_period)
                    key = "statusbar_clock_show_period"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_second)
                    key = "statusbar_clock_show_second"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_show_doublerow)
                    key = "statusbar_clock_show_doublerow"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_singlerow_fontsize)
                    summary = getString(R.string.statusbar_clock_fontsize_summary)
                    key = "statusbar_clock_singlerow_fontsize"
                    setDefaultValue(0)
                    max = 18
                    min = 0
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_clock_doublerow_fontsize)
                    summary = getString(R.string.statusbar_clock_fontsize_summary)
                    key = "statusbar_clock_doublerow_fontsize"
                    setDefaultValue(0)
                    max = 10
                    min = 0
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                }
            )
        }
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_year")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_month")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_day")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_week")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_period")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_second")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SwitchPreference>("statusbar_clock_show_doublerow")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SeekBarPreference>("statusbar_clock_singlerow_fontsize")?.dependency = "statusbar_clock_enable"
        preferenceScreen.findPreference<SeekBarPreference>("statusbar_clock_doublerow_fontsize")?.dependency = "statusbar_clock_show_doublerow"
    }
}

class DropDownStatusBarClock : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.dropdown_statusbar_clock_show_second)
                    key = "dropdown_statusbar_clock_show_second"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_dropdown_statusbar_clock_style)
                    summary = getString(R.string.remove_dropdown_statusbar_clock_style_summary)
                    key = "remove_dropdown_statusbar_clock_style"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class StatusBarDate : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_date_comma)
                    summary = getString(R.string.remove_statusbar_date_comma_summary)
                    key = "remove_statusbar_date_comma"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class StatusBarNotice : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_top_notification)
                    summary = getString(R.string.remove_statusbar_top_notification_summary)
                    key = "remove_statusbar_top_notification"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_vpn_active_notification)
                    summary = getString(R.string.remove_vpn_active_notification_summary)
                    key = "remove_vpn_active_notification"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_devmode)
                    key = "remove_statusbar_devmode"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_charging_completed)
                    key = "remove_charging_completed"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_bottom_networkwarn)
                    summary = getString(R.string.remove_statusbar_bottom_networkwarn_summary)
                    key = "remove_statusbar_bottom_networkwarn"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_flashlight_open_notification)
                    key = "remove_flashlight_open_notification"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_app_high_battery_consumption_warning)
                    summary = getString(R.string.remove_app_high_battery_consumption_warning_summary)
                    key = "remove_app_high_battery_consumption_warning"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_high_performance_mode_notifications)
                    key = "remove_high_performance_mode_notifications"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class StatusBarIcon : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_battery_percent)
                    key = "remove_statusbar_battery_percent"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.set_network_speed)
                    key = "set_network_speed"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_securepayment_icon)
                    key = "remove_statusbar_securepayment_icon"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_statusbar_user_switcher)
                    key = "remove_statusbar_user_switcher"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = SDK < 33
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_wifi_data_inout)
                    key = "remove_wifi_data_inout"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_mobile_data_inout)
                    key = "remove_mobile_data_inout"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class StatusBarContent : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.StatusBar_UI_Related)
                    key = "StatusBar_UI_Related"
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_drop_down_statusbar_mydevice)
                    key = "remove_drop_down_statusbar_mydevice"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.StatusBar_Tile_Related)
                    key = "StatusBar_Tile_Related"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.statusbar_tile_enable)
                    key = "statusbar_tile_enable"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_unexpanded_columns_vertical)
                    key = "tile_unexpanded_columns_vertical"
                    setDefaultValue(6)
                    max = 6
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK < 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_unexpanded_columns_horizontal)
                    key = "tile_unexpanded_columns_horizontal"
                    setDefaultValue(6)
                    max = 8
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK < 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_expanded_columns_vertical)
                    key = "tile_expanded_columns_vertical"
                    setDefaultValue(4)
                    max = 7
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK < 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_expanded_columns_horizontal)
                    key = "tile_expanded_columns_horizontal"
                    setDefaultValue(6)
                    max = 9
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK < 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_unexpanded_columns_vertical)
                    key = "tile_unexpanded_columns_vertical_c13"
                    setDefaultValue(5)
                    max = 6
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_expanded_rows_vertical)
                    key = "tile_expanded_rows_vertical_c13"
                    setDefaultValue(3)
                    max = 6
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_expanded_columns_vertical)
                    key = "tile_expanded_columns_vertical_c13"
                    setDefaultValue(4)
                    max = 7
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.tile_columns_horizontal_c13)
                    key = "tile_columns_horizontal_c13"
                    setDefaultValue(5)
                    max = 6
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
        }
        preferenceScreen.findPreference<SeekBarPreference>("tile_unexpanded_columns_vertical")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_unexpanded_columns_horizontal")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_expanded_columns_vertical")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_expanded_columns_horizontal")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_unexpanded_columns_vertical_c13")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_expanded_rows_vertical_c13")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_expanded_columns_vertical_c13")?.dependency = "statusbar_tile_enable"
        preferenceScreen.findPreference<SeekBarPreference>("tile_columns_horizontal_c13")?.dependency = "statusbar_tile_enable"
    }
}

class Desktop : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_alarmclock_widget_redone)
                    key = "remove_alarmclock_widget_redone"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_appicon_dot)
                    key = "remove_appicon_dot"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.set_folder_layout_4x4)
                    key = "set_folder_layout_4x4"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.launcher_layout_related)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.launcher_layout_enable)
                    summary = getString(R.string.launcher_layout_row_colume)
                    key = "launcher_layout_enable"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.launcher_layout_max_rows)
                    key = "launcher_layout_max_rows"
                    setDefaultValue(6)
                    max = 8
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SeekBarPreference(requireActivity()).apply {
                    title = getString(R.string.launcher_layout_max_columns)
                    key = "launcher_layout_max_columns"
                    setDefaultValue(4)
                    max = 6
                    min = 1
                    seekBarIncrement = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                }
            )
        }
        preferenceScreen.findPreference<SeekBarPreference>("launcher_layout_max_rows")?.dependency = "launcher_layout_enable"
        preferenceScreen.findPreference<SeekBarPreference>("launcher_layout_max_columns")?.dependency = "launcher_layout_enable"
    }
}

class LockScreen : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_lock_screen_redone)
                    key = "remove_lock_screen_redone"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.set_lock_screen_centered)
                    summary = getString(R.string.set_lock_screen_centered_summary)
                    key = "set_lock_screen_centered"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_lock_screen_bottom_left_button)
                    key = "remove_lock_screen_bottom_left_button"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_lock_screen_bottom_right_camera)
                    key = "remove_lock_screen_bottom_right_camera"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_lock_screen_bottom_sos_button)
                    summary = getString(R.string.remove_lock_screen_bottom_sos_button_summary)
                    key = "remove_lock_screen_bottom_sos_button"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = SDK >= 33
                }
            )
        }
    }
}

class Screenshot : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_system_screenshot_delay)
                    summary = getString(R.string.remove_system_screenshot_delay_summary)
                    key = "remove_system_screenshot_delay"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_screenshot_privacy_limit)
                    summary = getString(R.string.remove_screenshot_privacy_limit_summary)
                    key = "remove_screenshot_privacy_limit"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_flag_secure)
                    summary = getString(R.string.disable_flag_secure_summary)
                    key = "disable_flag_secure"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class Application : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.AppStartupRelated)
                    key = "AppStartupRelated"
                    isIconSpaceReserved = false
                    isVisible = SDK >= A13
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_splash_screen)
                    key = "disable_splash_screen"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = SDK >= A13
                }
            )
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.MultiApp)
                    key = "MultiApp"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.multi_app_enable)
                    summary = getString(R.string.multi_app_enable_summary)
                    key = "multi_app_enable"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.multi_app_custom_list)
                    summary = getString(R.string.multi_app_custom_list_summary)
                    key = "multi_app_custom_list"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(R.id.action_application_to_multiFragment,Bundle().apply {
                            putCharSequence("title_label",title)
                        })
                        true
                    }
                }
            )
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.AppInstallationRelated)
                    key = "PackageInstaller"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.skip_apk_scan)
                    summary = getString(R.string.skip_apk_scan_summary)
                    key = "skip_apk_scan"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.allow_downgrade_install)
                    summary = getString(R.string.allow_downgrade_install_summary)
                    key = "allow_downgrade_install"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_install_ads)
                    summary = getString(R.string.remove_install_ads_summary)
                    key = "remove_install_ads"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.replase_aosp_installer)
                    summary = getString(R.string.replase_aosp_installer_summary)
                    key = "replase_aosp_installer"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_adb_install_confirm)
                    summary = getString(R.string.remove_adb_install_confirm_summary)
                    key = "remove_adb_install_confirm"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                PreferenceCategory(requireActivity()).apply {
                    title = getString(R.string.ApplyOtherRestrictions)
                    key = "ApplyOtherRestrictions"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.unlock_startup_limit)
                    summary = getString(R.string.unlock_startup_limit_summary)
                    key = "unlock_startup_limit"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.unlock_task_locks)
                    key = "unlock_task_locks"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
        preferenceScreen.findPreference<Preference>("multi_app_custom_list")?.dependency = "multi_app_enable"
    }
}

class Miscellaneous : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                Preference(requireActivity()).apply {
                    title = getString(R.string.PowerMenu)
                    summary = getString(R.string.PowerMenu_summary)
                    key = "PowerMenu"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(R.id.action_miscellaneous_to_powerMenu,Bundle().apply {
                            putCharSequence("title_label",title)
                        })
                        true
                    }
                    isVisible = Build.VERSION.SDK_INT in 31..32
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.show_charging_ripple)
                    summary = getString(R.string.show_charging_ripple_summary)
                    key = "show_charging_ripple"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = Build.VERSION.SDK_INT >= 31
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_duplicate_floating_window)
                    summary = getString(R.string.disable_duplicate_floating_window_summary)
                    key = "disable_duplicate_floating_window"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    isVisible = Build.VERSION.SDK_INT >= 33
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_headphone_high_volume_warning)
                    summary = getString(R.string.disable_headphone_high_volume_warning_summary)
                    key = "disable_headphone_high_volume_warning"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_otg_auto_off)
                    summary = getString(R.string.disable_otg_auto_off_summary)
                    key = "disable_otg_auto_off"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.disable_dpi_reboot_recovery)
                    summary = getString(R.string.disable_dpi_reboot_recovery_summary)
                    key = "disable_dpi_reboot_recovery"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_low_battery_dialog_warning)
                    summary = getString(R.string.remove_low_battery_dialog_warning_summary)
                    key = "remove_low_battery_dialog_warning"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_warning_dialog_that_app_runs_on_desktop)
                    summary = getString(R.string.remove_warning_dialog_that_app_runs_on_desktop_summary)
                    key = "remove_warning_dialog_that_app_runs_on_desktop"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class PowerMenu : ModulePreferenceFragment(){
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.power_menu_enable)
                    summary = getString(R.string.power_menu_enable_summary)
                    key = "power_menu_enable"
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.power_menu_sos_button)
                    summary = getString(R.string.power_menu_button_summary)
                    key = "power_menu_sos_button"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.power_menu_lock_button)
                    summary = getString(R.string.power_menu_button_summary)
                    key = "power_menu_lock_button"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.power_menu_simple_layout)
                    summary = getString(R.string.power_menu_simple_layout_summary)
                    key = "power_menu_simple_layout"
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.power_menu_remove_add_controls)
                    summary = getString(R.string.power_menu_remove_add_controls_summary)
                    key = "power_menu_remove_add_controls"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
        preferenceScreen.findPreference<SwitchPreference>("power_menu_sos_button")?.dependency = "power_menu_enable"
        preferenceScreen.findPreference<SwitchPreference>("power_menu_lock_button")?.dependency = "power_menu_enable"
        preferenceScreen.findPreference<SwitchPreference>("power_menu_simple_layout")?.dependency = "power_menu_enable"
        preferenceScreen.findPreference<SwitchPreference>("power_menu_remove_add_controls")?.dependency = "power_menu_enable"
    }
}

class Camera : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_watermark_word_limit)
                    key = "remove_watermark_word_limit"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class OplusGames : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_root_check)
                    summary = getString(R.string.remove_root_check_summary)
                    key = "remove_root_check"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_startup_animation)
                    key = "remove_startup_animation"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class CloudService : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.remove_network_limit)
                    summary = getString(R.string.remove_network_limit_summary)
                    key = "remove_network_limit"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}

class ThemeStore : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = XposedPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                SwitchPreference(requireActivity()).apply {
                    title = getString(R.string.unlock_themestore_vip)
                    summary = getString(R.string.unlock_themestore_vip_summary)
                    key = "unlock_themestore_vip"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                }
            )
        }
    }
}