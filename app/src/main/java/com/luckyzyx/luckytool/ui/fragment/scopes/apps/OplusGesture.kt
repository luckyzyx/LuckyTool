package com.luckyzyx.luckytool.ui.fragment.scopes.apps

import android.content.Context
import android.graphics.BitmapFactory
import android.util.ArraySet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.listener.OnSelectAppInfoListener
import com.luckyzyx.luckytool.selector.AppInfoSelectDialog
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.fixIconSize
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusGesture : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.android.systemui", "com.oplus.gesture")

    private val pickLeftMedia = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val cacheFile = FileUtils.getMSMCacheFile(requireActivity(), it)
            requireActivity().putString(
                ModulePrefs, "replace_side_slider_icon_on_left", cacheFile?.path ?: ""
            )
        }
        (activity as MainActivity).restart()
    }

    private val pickRightMedia = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val cacheFile = FileUtils.getMSMCacheFile(requireActivity(), it)
            requireActivity().putString(
                ModulePrefs, "replace_side_slider_icon_on_right", cacheFile?.path ?: ""
            )
        }
        (activity as MainActivity).restart()
    }

    override val isEnableRestartMenu: Boolean = true

    override val isEnableOpenMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.oplusGesture

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.oplus.gesture"
            setPrefsIconRes(key) { resource, show ->
                icon = fixIconSize(resource)
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryDot(
                getString(R.string.enable_volume_key_control_flashlight),
                getString(R.string.force_enable_aon_gestures)
            )
            isVisible = checkPackName(key)
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            //音量键手电筒
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_volume_key_control_flashlight)
                summary = arraySummaryLine(
                    getString(R.string.enable_volume_key_control_flashlight_summary),
                    getString(R.string.need_restart_system)
                )
                key = "enable_volume_key_control_flashlight"
                setDefaultValue(false)
                isVisible = osCode >= 27
                isIconSpaceReserved = false
            })
            //隔空手势
            if (SDK >= A13) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.AonGesture)
                    key = "AonGesture"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.force_enable_aon_gestures)
                    summary = getString(R.string.force_enable_aon_gestures_summary)
                    key = "force_enable_aon_gestures"
                    setDefaultValue(false)
                    isEnabled =
                        checkPackName("com.oplus.gesture") && checkPackName("com.aiunit.aon")
                    isIconSpaceReserved = false
                })
                add(Preference(this@loadPreferences).apply {
                    key = "custom_aon_gesture_scroll_page_whitelist_list"
                    title = getString(R.string.custom_aon_gesture_scroll_page_whitelist)
                    val value = getStringSet(ModulePrefs, key, ArraySet())
                    summary = arraySummaryLine(
                        getString(R.string.custom_aon_gesture_whitelist_tips), value.toString()
                    )
                    isEnabled = checkPackName("com.aiunit.aon")
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        AppInfoSelectDialog(this@loadPreferences, true).apply {
                            setEnabledList(ArrayList(value))
                            setOnSelectAppListener(object : OnSelectAppInfoListener {
                                override fun resultSelectAppInfos(list: ArrayList<AppInfo>) {
                                    val set = ArraySet<String>().apply {
                                        list.forEachIndexed { _, appInfo ->
                                            add(appInfo.packageName)
                                        }
                                    }
                                    putStringSet(ModulePrefs, key, set.toSet())
                                    (activity as MainActivity).restart()
                                }
                            })
                            show()
                        }
                        true
                    }
                })
                add(Preference(this@loadPreferences).apply {
                    key = "custom_aon_gesture_video_whitelist_list"
                    title = getString(R.string.custom_aon_gesture_video_whitelist)
                    val value = getStringSet(ModulePrefs, key, ArraySet())
                    summary = arraySummaryLine(
                        getString(R.string.custom_aon_gesture_whitelist_tips), value.toString()
                    )
                    isEnabled = checkPackName("com.aiunit.aon")
                    isVisible = false //SDK >= A13
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        AppInfoSelectDialog(this@loadPreferences, true).apply {
                            setEnabledList(ArrayList(value))
                            setOnSelectAppListener(object : OnSelectAppInfoListener {
                                override fun resultSelectAppInfos(list: ArrayList<AppInfo>) {
                                    val set = ArraySet<String>().apply {
                                        list.forEachIndexed { _, appInfo ->
                                            add(appInfo.packageName)
                                        }
                                    }
                                    putStringSet(ModulePrefs, key, set.toSet())
                                    (activity as MainActivity).restart()
                                }
                            })
                            show()
                        }
                        true
                    }
                })
            }
            //全面屏手势
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.FullScreenGestureRelated)
                key = "FullScreenGestureRelated"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_side_slider)
                key = "remove_side_slider"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_side_slider_black_background)
                key = "remove_side_slider_black_background"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_rotate_screen_button)
                key = "remove_rotate_screen_button"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_back_gesture_confirmation_limit)
                key = "remove_back_gesture_confirmation_limit"
                setDefaultValue(false)
                isVisible = osCode in 35..36
                isIconSpaceReserved = false
            })
            //自定义侧滑条图标
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.CustomSideSliderIcon)
                key = "CustomSideSliderIcon"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.replace_side_slider_icon_switch)
                summary = getString(R.string.replace_side_slider_icon_switch_summary)
                key = "replace_side_slider_icon_switch"
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "replace_side_slider_icon_switch", false)) {
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.replace_side_slider_icon_on_left)
                    key = "replace_side_slider_icon_on_left"
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
                        pickLeftMedia.launch("image/*")
                        true
                    }
                })
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.replace_side_slider_icon_on_right)
                    key = "replace_side_slider_icon_on_right"
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
                        pickRightMedia.launch("image/*")
                        true
                    }
                })
            }
            //自定义快捷键
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.CustomActionButton)
                key = "CustomActionButton"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.action_button_nothing_enable)
                key = "action_button_nothing_enable"
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.action_button_ring_cycle_enable)
                key = "action_button_ring_cycle_enable"
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "action_button_ring_cycle_enable")) {
                add(DropDownPreference(this@loadPreferences).apply {
                    title = getString(R.string.action_button_ring_cycle_mode)
                    key = "action_button_ring_cycle_mode"
                    summary = arraySummaryLine(getString(R.string.current_mode) + ": %s")
                    setEntries(R.array.action_button_ring_cycle_mode_entries)
                    entryValues = arrayOf("ring_vibrate_silent", "ring_vibrate", "ring_silent")
                    setDefaultValue("ring_vibrate_silent")
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("android", key, newValue)
                        true
                    }
                })
            }
        }
    }

    override fun callOpenMenu() = IntentUtils(requireActivity()).jumpGesture()
}