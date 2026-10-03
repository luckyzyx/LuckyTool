package com.luckyzyx.luckytool.ui.fragment.scopes.related

import android.content.Context
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import com.luckyzyx.luckytool.utils.setSummaryProvider
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class AndroidRelated : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("system")

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.androidRelated

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "android"
            setPrefsIconRes(android.R.mipmap.sym_def_app_icon) { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryLine(
                getString(R.string.allow_untrusted_touch),
                getString(R.string.set_ltpo_refresh_rate_mode)
            )
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_gms_usage_restrictions)
                summary = arraySummaryLine(
                    getString(R.string.need_restart_system)
                )
                key = "remove_gms_usage_restrictions"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getBoolean(ModulePrefs, "remove_gms_usage_restrictions", false)) {
                add(EditTextPreference(this@loadPreferences).apply {
                    title = getString(R.string.custom_remote_provisioning_hostname)
                    dialogTitle = title
                    dialogMessage = arraySummaryLine(
                        getString(R.string.custom_remote_provisioning_hostname_summary),
                        getString(R.string.need_restart_system)
                    )
                    key = "custom_remote_provisioning_hostname"
                    setDefaultValue("remoteprovisioning.grapheneos.org")
                    setSummaryProvider(this)
                    isIconSpaceReserved = false
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.replace_system_root_state_detection)
                summary = getString(R.string.need_restart_system)
                key = "replace_system_root_state_detection"
                setDefaultValue(false)
                isVisible = SDK >= A12
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.allow_untrusted_touch)
                summary = getString(R.string.allow_untrusted_touch_summary)
                key = "allow_untrusted_touch"
                setDefaultValue(false)
                isVisible = SDK >= A12
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_temperature_control_listener)
                summary = getString(R.string.need_restart_system)
                key = "disable_temperature_control_listener"
                setDefaultValue(false)
                isVisible = false
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_long_press_home_key_start_speech_asssist)
                summary = getString(R.string.need_restart_system)
                key = "disable_long_press_home_key_start_speech_asssist"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.customized_gaussian_blur_effect_level)
                summary = arraySummaryLine(
                    getString(R.string.current_mode) + ": %s",
                    getString(R.string.need_restart_system)
                )
                key = "customized_gaussian_blur_effect_level"
                setEntries(R.array.customized_gaussian_blur_effect_level_entries)
                entryValues = arrayOf("-1", "0", "1", "2", "3")
                setDefaultValue("-1")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            //LTPO
            add(PreferenceCategory(this@loadPreferences).apply {
                title = "LTPO"
                key = "OplusLTPO"
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_ltpo_refresh_rate_mode)
                summary = arraySummaryLine(
                    getString(R.string.current_mode) + ": %s",
                    getString(R.string.need_restart_system)
                )
                key = "set_ltpo_refresh_rate_mode"
                setEntries(R.array.set_ltpo_refresh_rate_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getString(ModulePrefs, "set_ltpo_refresh_rate_mode", "0") == "1") {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.enable_full_brightness_refresh_rate_minimum_one)
                    summary = arraySummaryLine(
                        getString(R.string.enable_full_brightness_refresh_rate_minimum_one_summary),
                        getString(R.string.need_restart_system)
                    )
                    key = "enable_full_brightness_refresh_rate_minimum_one"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
        }
    }
}