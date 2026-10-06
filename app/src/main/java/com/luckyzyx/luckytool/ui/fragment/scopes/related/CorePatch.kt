package com.luckyzyx.luckytool.ui.fragment.scopes.related

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dialogCentered
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class CorePatch : BaseScopePreferenceFeagment() {

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = R.id.corePatch

    override fun Context.loadRootPreference(): Preference {
        return Preference(this).apply {
            title = getString(R.string.corepatch)
            summary = getString(R.string.corepatch_summary, "11-17")
            key = "CorePatch"
            isIconSpaceReserved = false
        }
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        return ArrayList<Preference>().apply {
            add(Preference(this@loadPreferences).apply {
                title = getString(R.string.ColorOSCorePatchTip)
                key = "ColorOSCorePatchTip"
                isIconSpaceReserved = false
            })
            add(PreferenceCategory(this@loadPreferences).apply {
                setTitle(R.string.corepatch)
                key = "CorePatch"
                isIconSpaceReserved = false
            })
            //上游 org.lsposed.corepatch 配置键（ModulePrefs 组，键名与默认值对齐上游）
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_downgrade)
                setSummary(R.string.bypass_downgrade_summary)
                key = "downgrade"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_verification)
                setSummary(R.string.bypass_verification_summary)
                key = "bypass_verification"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_resource_arsc_restrictions)
                setSummary(R.string.bypass_resource_arsc_restrictions_summary)
                key = "bypass_resource_arsc_restrictions"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_digest)
                setSummary(R.string.bypass_digest_summary)
                key = "bypass_digest"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_exact_signature_match)
                setSummary(R.string.bypass_exact_signature_match_summary)
                key = "bypass_exact_sig_match"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.use_previous_signatures)
                setSummary(R.string.use_previous_signatures_summary)
                key = "use_previous_signatures"
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue == true) {
                        MaterialAlertDialogBuilder(this@loadPreferences, dialogCentered).apply {
                            setMessage(R.string.use_previous_signatures_warning)
                            setPositiveButton(android.R.string.ok, null)
                            show()
                        }
                    }
                    true
                }
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.allow_hidden_apis_for_system_apps)
                setSummary(R.string.allow_hidden_apis_for_system_apps_summary)
                key = "allow_hidden_apis_for_system_apps"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_shared_user)
                setSummary(R.string.bypass_shared_user_summary)
                key = "bypass_shared_user"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.disable_verification_agent)
                setSummary(R.string.disable_verification_agent_summary)
                key = "disable_verification_agent"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
            add(SwitchPreference(this@loadPreferences).apply {
                setTitle(R.string.bypass_block)
                setSummary(R.string.bypass_block_summary)
                key = "bypass_block"
                setDefaultValue(false)
                isIconSpaceReserved = false
            })
        }
    }
}