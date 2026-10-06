package com.luckyzyx.luckytool.ui.fragment.scopes.related

import android.content.Context
import android.util.ArraySet
import androidx.navigation.fragment.findNavController
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
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.fixIconSize
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.sendPrefsKey
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class ApplicationRelated : BaseScopePreferenceFeagment() {

    override val scopes = arrayOf(
        "com.oplus.battery",
        "com.oplus.safecenter",
        "com.coloros.safecenter",
        "com.android.settings",
        "com.android.packageinstaller",
        "com.oplus.multiapp"
    )

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.application

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.android.packageinstaller"
            setPrefsIconRes(key) { resource, show ->
                icon = fixIconSize(resource)
                isIconSpaceReserved = show
            }
            title = getString(R.string.Application)
            summary = arraySummaryDot(
                getString(R.string.skip_apk_scan),
                getString(R.string.unlock_startup_limit)
            )
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            //应用启动
            if (SDK >= A13) {
                add(PreferenceCategory(this@loadPreferences).apply {
                    title = getString(R.string.AppStartupRelated)
                    key = "AppStartupRelated"
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.disable_splash_screen)
                    summary = arraySummaryLine(
                        getString(R.string.need_restart_system),
                        getString(R.string.disable_splash_screen_summary)
                    )
                    key = "disable_splash_screen"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.disable_preload_splash)
                    summary = getString(R.string.need_restart_system)
                    key = "disable_preload_splash"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            //应用列表
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.APPRelatedList)
                key = "APPRelatedList"
                isIconSpaceReserved = false
            })
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.custom_config_app_intent_list)
                summary = getString(R.string.need_restart_system)
                key = "custom_config_app_intent_list"
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    findNavController().navigatePage(R.id.hideAppIntentFragment, title)
                    true
                }
            })
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.dark_mode_support_list)
                summary = getString(R.string.zoom_window_support_list_summary)
                key = "dark_mode_support_list"
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    findNavController().navigatePage(R.id.darkModeFragment, title)
                    true
                }
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_multi_app_support_mode)
                key = "set_multi_app_support_mode"
                summary = arraySummaryLine(
                    getString(R.string.current_mode) + ": %s",
                    getString(R.string.need_restart_system),
                    getString(R.string.set_multi_app_support_mode_tips)
                )
                setEntries(
                    if (osCode < 27) R.array.set_multi_app_support_mode_low_entries
                    else R.array.set_multi_app_support_mode_entries
                )
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    sendPrefsValue("com.oplus.multiapp", key, newValue)
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getString(ModulePrefs, "set_multi_app_support_mode", "0") == "1") {
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.multi_app_custom_list)
                    summary = getString(R.string.multi_app_custom_list_summary)
                    key = "multi_app_custom_list"
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigatePage(R.id.multiAppFragment, title)
                        true
                    }
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_multi_app_blacklist)
                summary = getString(R.string.need_restart_system)
                key = "remove_multi_app_blacklist"
                setDefaultValue(false)
                isVisible = osCode >= 31
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_multi_app_created_num_limit)
                summary = arraySummaryLine(
                    getString(R.string.remove_multi_app_created_num_limit_for_users),
                    getString(R.string.need_restart_system)
                )
                key = "remove_multi_app_created_num_limit_for_users"
                setDefaultValue(false)
                isVisible = osCode >= 38
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_multi_app_created_num_limit)
                summary = arraySummaryLine(
                    getString(R.string.remove_multi_app_created_num_limit_for_apps),
                    getString(R.string.need_restart_system)
                )
                key = "remove_multi_app_created_num_limit_for_apps"
                setDefaultValue(false)
                isVisible = osCode >= 31
                isIconSpaceReserved = false
            })
            add(DropDownPreference(this@loadPreferences).apply {
                title = getString(R.string.set_wlan_sla_whitelist_mode)
                key = "set_wlan_sla_whitelist_mode"
                summary = arraySummaryLine(
                    getString(R.string.current_mode) + ": %s",
                    getString(R.string.need_restart_system)
                )
                setEntries(R.array.set_wlan_sla_whitelist_mode_entries)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    (activity as MainActivity).restart()
                    true
                }
            })
            if (getString(ModulePrefs, "set_wlan_sla_whitelist_mode", "0") != "0") {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.remove_wlan_sla_blacklist)
                    key = "remove_wlan_sla_blacklist"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, newValue ->
                        sendPrefsValue("android", key, newValue)
                        true
                    }
                })
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.custom_wlan_sla_whitelist)
                    key = "custom_wlan_sla_whitelist"
                    val value = getStringSet(ModulePrefs, key, ArraySet())
                    summary = value.toString()
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
                                    sendPrefsKey("android", key)
                                    (activity as MainActivity).restart()
                                }
                            })
                            show()
                        }
                        true
                    }
                })
                add(Preference(this@loadPreferences).apply {
                    title = getString(R.string.custom_wlan_sla_game_whitelist)
                    key = "custom_wlan_sla_game_whitelist"
                    val value = getStringSet(ModulePrefs, key, ArraySet())
                    summary = value.toString()
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
                                    sendPrefsKey("android", key)
                                    (activity as MainActivity).restart()
                                }
                            })
                            show()
                        }
                        true
                    }
                })
            }
            //应用安装
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.AppInstallationRelated)
                summary = getString(R.string.PackageInstaller_summary)
                key = "PackageInstaller"
                isIconSpaceReserved = false
            })
            add(CorePatch().getRootPreference(this@loadPreferences))
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_enable_32_bit_support)
                summary = getString(R.string.need_restart_system)
                key = "force_enable_32_bit_support"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.fix_install_button_display_exception)
                summary = getString(R.string.fix_install_button_display_exception_summary)
                key = "fix_install_button_display_exception"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_start_app_detail)
                summary = getString(R.string.disable_start_app_detail_summary)
                key = "disable_start_app_detail"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.skip_apk_scan)
                summary = getString(R.string.skip_apk_scan_summary)
                key = "skip_apk_scan"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.allow_downgrade_install)
                summary = getString(R.string.allow_downgrade_install_summary)
                key = "allow_downgrade_install"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_install_ads)
                summary = getString(R.string.remove_install_ads_summary)
                key = "remove_install_ads"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.auto_click_install_button)
                key = "auto_click_install_button"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.auto_click_uninstall_button)
                key = "auto_click_uninstall_button"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_more_apk_package_information)
                summary = getString(R.string.show_more_apk_package_information_summary)
                key = "show_more_apk_package_information"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.replase_aosp_installer)
                summary = getString(R.string.replase_aosp_installer_summary)
                key = "replase_aosp_installer"
                setDefaultValue(false)
                isVisible = false
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_adb_install_confirm)
                summary = getString(R.string.remove_adb_install_confirm_summary)
                key = "remove_adb_install_confirm"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //其他限制
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.ApplyOtherRestrictions)
                key = "ApplyOtherRestrictions"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.unlock_startup_limit)
                summary = getString(R.string.unlock_startup_limit_summary)
                key = "unlock_startup_limit"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //应用详情相关
            add(PreferenceCategory(this@loadPreferences).apply {
                title = getString(R.string.AppDetailsRelated)
                key = "AppDetailsRelated"
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_package_name_in_app_details)
                key = "show_package_name_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_sdk_in_app_details)
                key = "show_sdk_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_first_install_time_in_app_details)
                key = "show_first_install_time_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_last_update_time_in_app_details)
                key = "show_last_update_time_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_install_source_in_app_details)
                key = "show_install_source_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_long_press_to_copy_in_app_details)
                key = "enable_long_press_to_copy_in_app_details"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_quick_open_market_page)
                key = "enable_quick_open_market_page"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_app_clone_quick_jump)
                key = "enable_app_clone_quick_jump"
                setDefaultValue(false)
                isVisible = osCode >= 27
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.allow_disabling_system_apps)
                summary = getString(R.string.allow_disabling_system_apps_summary)
                key = "allow_disabling_system_apps"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_app_uninstall_button_blacklist)
                key = "remove_app_uninstall_button_blacklist"
                setDefaultValue(false)
                isVisible = SDK >= A13
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_custom_app_language)
                key = "enable_custom_app_language"
                setDefaultValue(false)
                isVisible = SDK >= A14
                isIconSpaceReserved = false
            })
            //《自动释放应用空间》默认关闭（空间不足时自动卸载应用但保留数据）
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_app_archiving)
                summary = getString(R.string.disable_app_archiving_summary)
                key = "disable_app_archiving"
                setDefaultValue(true)
                isVisible = osCode >= 36
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    sendPrefsValue("android", key, newValue)
                    sendPrefsValue("com.android.settings", key, newValue)
                    true
                }
            })
        }
    }
}