package org.lsposed.corepatch

import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.preference.YukiHookPreferences

object Config {
    const val BYPASS_DOWNGRADE = "downgrade"
    const val BYPASS_VERIFICATION = "bypass_verification"
    const val BYPASS_RESOURCE_ARSC_RESTRICTIONS = "bypass_resource_arsc_restrictions"
    const val BYPASS_DIGEST = "bypass_digest"
    const val BYPASS_EXACT_SIGNATURE_MATCH = "bypass_exact_sig_match"
    const val USE_PREVIOUS_SIGNATURES = "use_previous_signatures"
    const val ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS = "allow_hidden_apis_for_system_apps"
    const val BYPASS_SHARED_USER = "bypass_shared_user"
    const val DISABLE_VERIFICATION_AGENT = "disable_verification_agent"
    const val BYPASS_BLOCK = "bypass_block"

    private val allConfig = arrayOf(
        BYPASS_DOWNGRADE,
        BYPASS_VERIFICATION,
        BYPASS_RESOURCE_ARSC_RESTRICTIONS,
        BYPASS_DIGEST,
        USE_PREVIOUS_SIGNATURES,
        ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS,
        BYPASS_SHARED_USER,
        BYPASS_BLOCK
    )

    //LuckyTool：配置统一存 ModulePrefs 组，键名沿用上游，由 BaseHook 在 hook 前绑定
    @Volatile
    private var preferences: YukiHookPreferences? = null

    fun bind(preferences: YukiHookPreferences) {
        this.preferences = preferences
    }

    fun printAllConfig() {
        allConfig.forEach {
            YLog.debug("$it: ${getBoolean(it)}")
        }
    }

    private fun getBoolean(key: String): Boolean {
        return preferences?.getBoolean(key, false) ?: false
    }

    fun isBypassDowngradeEnabled(): Boolean {
        return getBoolean(BYPASS_DOWNGRADE)
    }

    fun isBypassVerificationEnabled(): Boolean {
        return getBoolean(BYPASS_VERIFICATION)
    }

    fun isBypassResourceArscRestrictionsEnabled(): Boolean {
        return getBoolean(BYPASS_RESOURCE_ARSC_RESTRICTIONS)
    }

    fun isBypassDigestEnabled(): Boolean {
        return getBoolean(BYPASS_DIGEST)
    }

    fun isBypassExactSignatureMatch(): Boolean {
        return getBoolean(BYPASS_EXACT_SIGNATURE_MATCH)
    }

    fun isUsePreviousSignaturesEnabled(): Boolean {
        return getBoolean(USE_PREVIOUS_SIGNATURES)
    }

    fun isAllowHiddenApisForSystemAppsEnabled(): Boolean {
        return getBoolean(ALLOW_HIDDEN_APIS_FOR_SYSTEM_APPS)
    }

    fun isBypassSharedUserEnabled(): Boolean {
        return getBoolean(BYPASS_SHARED_USER)
    }

    fun isDisableVerificationAgentEnabled(): Boolean {
        return getBoolean(DISABLE_VERIFICATION_AGENT)
    }

    fun isBypassBlockEnabled(): Boolean {
        return getBoolean(BYPASS_BLOCK)
    }
}
