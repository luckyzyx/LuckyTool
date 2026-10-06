package com.luckyzyx.luckytool.ui.fragment.scopes.related

import android.content.Context
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.arraySummaryDot
import com.luckyzyx.luckytool.utils.setPrefsIconRes
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class Miscellaneous : BaseScopePreferenceFeagment() {
    override val scopes = arrayOf(
        "com.android.systemui",
        "com.android.externalstorage",
        "com.oplus.exsystemservice"
    )

    override val isEnableRestartMenu: Boolean = true

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.miscellaneous

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            key = "Miscellaneous"
            setPrefsIconRes("com.android.systemui") { resource, show ->
                icon = resource
                isIconSpaceReserved = show
            }
            title = getString(R.string.Miscellaneous)
            summary =
                arraySummaryDot(getString(R.string.Miscellaneous_summary))
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(DialogRelated().getRootPreference(this@loadPreferences))
            add(FingerPrintRelated().getRootPreference(this@loadPreferences))
            add(SoundRelated().getRootPreference(this@loadPreferences))
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_charging_ripple)
                summary = getString(R.string.show_charging_ripple_summary)
                key = "show_charging_ripple"
                setDefaultValue(false)
                isVisible = SDK >= A12
                isIconSpaceReserved = false
            })
            if (osCode < 30) {
                add(SwitchPreference(this@loadPreferences).apply {
                    title = getString(R.string.disable_otg_auto_off)
                    summary = getString(R.string.disable_otg_auto_off_summary)
                    key = "disable_otg_auto_off"
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                })
            }
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_storage_limit)
                summary = getString(R.string.remove_storage_limit_summary)
                key = "remove_storage_limit"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.force_enable_systemui_blur_feature)
                key = "force_enable_systemui_blur_feature"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.show_manual_lock_button_power_menu)
                key = "show_manual_lock_button_power_menu"
                setDefaultValue(false)
                isVisible = SDK >= A14
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                title = getString(R.string.remove_power_menu_sos_button)
                key = "remove_power_menu_sos_button"
                setDefaultValue(false)
                isVisible = SDK >= A13
                isIconSpaceReserved = false
            })
        }
    }
}