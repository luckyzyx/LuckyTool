package com.luckyzyx.luckytool.ui.fragment.scopes.apps

import android.content.Context
import android.os.SystemProperties
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withDefault
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import com.topjohnwu.superuser.ShellUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusOTA : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.oplus.ota")

    override val isEnableRestartMenu: Boolean = true

    override val isEnableOpenMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.oplusOta

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.oplus.ota"
            setPrefsIconRes(key) { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryDot(
                getString(R.string.unlock_local_upgrade),
                getString(R.string.restore_ota_update_verity)
            )
            isVisible = checkPackName(key)
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_ota_notify_install_success)
                key = "remove_ota_notify_install_success"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_ota_auto_download_dialog)
                key = "remove_ota_auto_download_dialog"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            //OTA
            add(PreferenceCategory(this@loadPreferences).apply {
                title = "OTA"
                key = "OTAUpdate"
                isIconSpaceReserved = false
            })
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.get_ota_verify_result)
                val list = ShellUtils.fastCmd(
                    "${CommandUtils.getprop} ${CommandUtils.otaVerifyResult}"
                ).split(",")
                val imgs = formatStringAuto(list, ",", false)
                summary = getString(R.string.get_ota_verify_result_summary, imgs)
                key = "get_ota_verify_result"
                isPersistent = false
                isIconSpaceReserved = false
            })
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.unlock_local_upgrade)
                summary = getString(R.string.unlock_local_upgrade_summary)
                key = "unlock_local_upgrade"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    scopeLife {
                        val command = arrayOf(
                            "settings put global development_settings_enabled 1",
                            "pm clear com.oplus.ota",
                            "settings put global airplane_mode_on 1",
                            "am broadcast --user all -a android.intent.action.AIRPLANE_MODE --ez 'state' 'true'",
                            "am start com.oplus.ota/com.oplus.otaui.activity.EntryActivity"
                        )
                        withDefault { ShellUtils.fastCmd(*command) }
                    }
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_ota_local_update_verity)
                key = "remove_ota_local_update_verity"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.enable_opex_local_install)
                key = "enable_opex_local_install"
                setDefaultValue(false)
                isVisible = osCode >= 30 && SystemProperties.getBoolean("oplus.opex.merge", false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.disable_dm_verity_verification)
                key = "disable_dm_verity_verification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                val verityMode =
                    ShellUtils.fastCmd("${CommandUtils.getprop} ${CommandUtils.otaVerityMode}")
                val vbMetaState =
                    ShellUtils.fastCmd("${CommandUtils.getprop} ${CommandUtils.otaVbmetaState}")
                val status =
                    verityMode == "enforcing" || verityMode == "eio" && vbMetaState == "locked"

                title = getString(R.string.restore_ota_update_verity)
                summary = getString(R.string.restore_ota_update_verity_summary, status.toString())
                key = "restore_ota_update_verity"
                isEnabled = !status
                isChecked = status
                isPersistent = false
                isVisible = false
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue as Boolean) {
                        ShellUtils.fastCmd(
                            "${CommandUtils.resetprop} ${CommandUtils.otaVerityMode} enforcing",
                            "${CommandUtils.resetprop} ${CommandUtils.otaVbmetaState} locked",
                            "${CommandUtils.resetprop} ${CommandUtils.otaLockedState} 1"
                        )
                        (activity as MainActivity).restart()
                    }
                    true
                }
            })
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.extract_ota_information)
                summary = getString(R.string.extract_ota_information_summary)
                key = "extract_ota_information"
                setDefaultValue(false)
                isVisible = osCode < 37
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    findNavController().navigatePage(R.id.extractOTAFragment, title)
                    true
                }
            })
        }
    }

    override fun callOpenMenu() = IntentUtils(requireActivity()).jumpOTA()
}