package com.luckyzyx.luckytool.ui.fragment.others

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.lsposed.lsparanoid.Obfuscate
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.checkResolveActivity
import com.topjohnwu.superuser.ShellUtils

@Obfuscate
class QuickEntryFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            //系统调试相关
            addPreference(PreferenceCategory(context).apply {
                title = getString(R.string.SystemDebuggingRelated)
                key = "SystemDebuggingRelated"
                isIconSpaceReserved = false
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.engineering_mode)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpEngineermode()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.charging_test)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpBatteryInfo()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.developer_option)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpSettingsDev()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.system_interface_adjustment)
                isVisible = context.checkResolveActivity(
                    Intent().setClassName(
                        "com.android.systemui", "com.android.systemui.DemoMode"
                    )
                )
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpSystemUIDemoMode()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.AOSPSettingsPage)
                isVisible = context.checkResolveActivity(
                    Intent().setClassName(
                        "com.android.settings",
                        "com.android.settings.homepage.DeepLinkHomepageActivityInternal"
                    )
                )
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    ShellUtils.fastCmd(
                        "am start -n com.android.settings/.homepage.DeepLinkHomepageActivityInternal"
                    )
                    true
                }
            })
            //隐藏页面相关
            addPreference(PreferenceCategory(context).apply {
                title = getString(R.string.HidePageRelated)
                key = "HidePageRelated"
                isIconSpaceReserved = false
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.process_manager)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpRunningApp()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.very_dark_mode)
                isVisible = context.checkResolveActivity(
                    Intent().setClassName(
                        "com.android.settings",
                        "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity"
                    )
                )
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    IntentUtils(context).jumpVeryDarkMode()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.open_battery_health)
                isVisible = context.checkResolveActivity(
                    Intent().setClassName(
                        "com.oplus.battery", "com.oplus.powermanager.fuelgaue.BatteryHealthActivity"
                    )
                )
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    ShellUtils.fastCmd(
                        "am start -n com.oplus.battery/com.oplus.powermanager.fuelgaue.BatteryHealthActivity"
                    )
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.battery_optimization)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        startActivity(this)
                    }
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.camera_algo_page)
                isVisible = context.checkResolveActivity(
                    Intent().setClassName(
                        "com.oplus.camera", "com.oplus.camera.ui.menu.algoswitch.AlgoSwitchActivity"
                    )
                )
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    ShellUtils.fastCmd(
                        "am start -n com.oplus.camera/.ui.menu.algoswitch.AlgoSwitchActivity"
                    )
                    true
                }
            })
        }
    }
}
