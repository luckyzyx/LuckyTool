package com.luckyzyx.luckytool.ui.fragment.scopes.statusbar

import android.content.Context
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setSummaryProvider
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class StatusBarTiles : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.android.systemui")

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.statusBarTiles

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            title = getString(R.string.StatusBarTiles)
            summary = arraySummaryDot(
                getString(R.string.long_press_wifi_tile_open_the_page),
                getString(R.string.fix_tile_align_both_sides)
            )
            key = "StatusBarTiles"
            isIconSpaceReserved = false
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            //自定义NFC延时关闭
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_nfc_delay_shutdown)
                key = "enable_nfc_delay_shutdown"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("com.android.systemui", key, newValue)
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "enable_nfc_delay_shutdown", false)) {
                add(EditTextPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_nfc_delay_shutdown_time)
                    dialogTitle = title
                    dialogMessage = """
                     1s / 1S -> 1秒 / 1 Second
                     1m / 1M -> 1分钟 / 1 Minute
                     1h / 1H -> 1小时 / 1 Hour
                """.trimIndent()
                    key = "custom_nfc_delay_shutdown_time"
                    setDefaultValue("10M")
                    setSummaryProvider(this)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("com.android.systemui", key, newValue)
                        true
                    }
                })
            }
            //静音或振动
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_of_ringing_status_toggle_tiles)
                key = "force_display_of_ringing_status_toggle_tiles"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //设备控制器
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_display_of_device_controls_tiles)
                key = "force_display_of_device_controls_tiles"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //特殊磁贴
            if (SDK >= A13) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.SpecialTiles)
                    summary = getString(R.string.classic_control_center_mode_only)
                    key = "SpecialTiles"
                    isIconSpaceReserved = false
                })
                add(DropDownPreference(this@loadPreferences).apply {
                    title = getString(R.string.set_media_player_display_mode)
                    summary = getString(R.string.current_mode) + ": %s"
                    key = "set_media_player_display_mode"
                    setEntries(R.array.set_media_player_display_mode_entries)
                    entryValues = arrayOf("0", "1", "2", "3")
                    setDefaultValue("0")
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("com.android.systemui", key, newValue)
                        (activity as MainActivity).restart()
                        true
                    }
                })
                if (getString(ModulePrefs, "set_media_player_display_mode", "0") != "0") {
                    if (SDK >= A14) add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.auto_expand_tile_rows_horizontal)
                        summary = arraySummaryLine(
                            getString(R.string.auto_expand_tile_rows_horizontal_summary),
                            getString(R.string.auto_expand_tile_rows_horizontal_summary_2)
                        )
                        key = "auto_expand_tile_rows_horizontal"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, newValue ->
                            sendPrefsValue("com.android.systemui", key, newValue)
                            if (newValue as Boolean) {
                                findPreference<SwitchPreference>("control_center_custom_gaps_for_special_tile")?.isChecked =
                                    true
                                findPreference<SwitchPreference>("control_center_tile_enable")?.isChecked =
                                    true
                            }
                            (activity as MainActivity).restart()
                            true
                        }
                    })
                }
                if (getString(ModulePrefs, "set_media_player_display_mode") == "1") {
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.force_enable_media_toggle_button)
                        key = "force_enable_media_toggle_button"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                    })
                }
                if (osCode >= 27) {
                    add(SwitchPreference(this@loadPreferences).apply {
                        title = getString(R.string.control_center_custom_gaps_for_special_tile)
                        if (getBoolean(ModulePrefs, "auto_expand_tile_rows_horizontal")) {
                            summary =
                                getString(R.string.control_center_custom_gaps_for_special_tile_summary)
                        }
                        key = "control_center_custom_gaps_for_special_tile"
                        setDefaultValue(false)
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, newValue ->
                            sendPrefsValue("com.android.systemui", key, newValue)
                            if ((newValue as Boolean).not()) findPreference<SwitchPreference>(
                                "auto_expand_tile_rows_horizontal"
                            )?.isChecked = false
                            (activity as MainActivity).restart()
                            true
                        }
                    })
                    if (getBoolean(
                            ModulePrefs, "control_center_custom_gaps_for_special_tile", false
                        )
                    ) {
                        add(SeekBarPreference(this@loadPreferences).apply {
                            title = getString(R.string.control_center_special_tile_top_gap)
                            key = "control_center_special_tile_top_gap"
                            setDefaultValue(10)
                            max = 20
                            min = 0
                            showSeekBarValue = true
                            updatesContinuously = false
                            isIconSpaceReserved = false
                            setOnPreferenceChangeListener { _, newValue ->
                                sendPrefsValue("com.android.systemui", key, newValue)
                                true
                            }
                        })
                        add(SeekBarPreference(this@loadPreferences).apply {
                            title = getString(R.string.control_center_special_tile_bottom_gap)
                            key = "control_center_special_tile_bottom_gap"
                            setDefaultValue(0)
                            max = 20
                            min = 0
                            showSeekBarValue = true
                            updatesContinuously = false
                            isIconSpaceReserved = false
                            setOnPreferenceChangeListener { _, newValue ->
                                sendPrefsValue("com.android.systemui", key, newValue)
                                true
                            }
                        })
                        add(SwitchPreference(this@loadPreferences).apply {
                            title = getString(R.string.decrease_horizontal_brightness_bar_top_gap)
                            key = "decrease_horizontal_brightness_bar_top_gap"
                            setDefaultValue(false)
                            isVisible = osCode >= 30
                            isIconSpaceReserved = false
                        })
                    }
                }
            }
            if (SDK >= A13) {
                //磁贴长按事件
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.TileLongClickEvent)
                    key = "TileLongClickEvent"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.restore_some_tile_long_press_event)
                    summary = getString(R.string.restore_some_tile_long_press_event_summary)
                    key = "restore_some_tile_long_press_event"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            //磁贴样式相关
            if (osCode in 27..33) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.TileStyleRelated)
                    key = "TileStyleRelated"
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_tile_background_transparency)
                    key = "custom_tile_background_transparency"
                    setDefaultValue(-1)
                    max = 10
                    min = -1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isIconSpaceReserved = false
                })
            }
            //磁贴布局相关
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.TileLayoutRelated)
                key = "TileLayoutRelated"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.fix_tile_align_both_sides)
                summary = getString(R.string.fix_tile_align_both_sides_summary)
                key = "fix_tile_align_both_sides"
                setDefaultValue(false)
                isVisible = osCode >= 26
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.restore_page_layout_row_count_for_edit_tiles)
                key = "restore_page_layout_row_count_for_edit_tiles"
                setDefaultValue(false)
                isVisible = SDK >= A13
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_control_center_tile_count_limit)
                key = "remove_control_center_tile_count_limit"
                setDefaultValue(false)
                isVisible = osCode < 37
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.control_center_tile_enable)
                summary = getString(R.string.classic_control_center_mode_only)
                key = "control_center_tile_enable"
                setDefaultValue(false)
                isVisible = osCode < 40
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    if ((newValue as Boolean).not()) findPreference<SwitchPreference>("auto_expand_tile_rows_horizontal")?.isChecked =
                        false
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "control_center_tile_enable", false)) {
                //C12
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_unexpanded_columns_vertical)
                    key = "tile_unexpanded_columns_vertical"
                    setDefaultValue(6)
                    max = 6
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK < A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_unexpanded_columns_horizontal)
                    key = "tile_unexpanded_columns_horizontal"
                    setDefaultValue(6)
                    max = 8
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK < A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_expanded_columns_vertical)
                    key = "tile_expanded_columns_vertical"
                    setDefaultValue(4)
                    max = 7
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK < A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_expanded_columns_horizontal)
                    key = "tile_expanded_columns_horizontal"
                    setDefaultValue(6)
                    max = 9
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK < A13
                    isIconSpaceReserved = false
                })
                //C13+
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_unexpanded_columns_vertical)
                    key = "tile_unexpanded_columns_vertical_c13"
                    setDefaultValue(5)
                    max = 6
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK >= A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_expanded_rows_vertical)
                    key = "tile_expanded_rows_vertical_c13"
                    setDefaultValue(3)
                    max = 6
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK >= A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_expanded_columns_vertical)
                    key = "tile_expanded_columns_vertical_c13"
                    setDefaultValue(4)
                    max = 7
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK >= A13
                    isIconSpaceReserved = false
                })
                add(SeekBarPreference(this@loadPreferences).apply {
                    title = getString(R.string.tile_columns_horizontal_c13)
                    key = "tile_columns_horizontal_c13"
                    setDefaultValue(5)
                    max = 6
                    min = 1
                    showSeekBarValue = true
                    updatesContinuously = false
                    isVisible = SDK >= A13
                    isIconSpaceReserved = false
                })
            }
        }
    }
}