package com.luckyzyx.luckytool.ui.fragment.scopes.apps

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class OplusPhoneManager : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf("com.coloros.phonemanager")

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.oplusPhoneManager

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "com.coloros.phonemanager"
            setPrefsIconRes(key) { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = AppUtils(context).getAppLabel(key)
            summary = arraySummaryLine(
                getString(R.string.remove_secure_pay_found_virus_dialog),
                getString(R.string.remove_virus_risk_notification_in_phone_manager)
            )
            isVisible = checkPackName(key)
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_virus_risk_notification_in_phone_manager)
                key = "remove_virus_risk_notification_in_phone_manager"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_countdown_add_virus_app_whitelist)
                key = "remove_countdown_add_virus_app_whitelist"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
        }
    }
}