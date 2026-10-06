package com.luckyzyx.luckytool.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import com.android.internal.hidden_from_bootclasspath.android.permission.flags.Flags
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.utils.CommandUtils.uid
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class EcmUtils(val context: Context) {

    val bindAccessibilityService = "android:bind_accessibility_service"

    fun autoUnlockRestrictedSettings(packName: String) {
        if (isEcmMode()) {
            if (isEcmRestricted(packName)) {
                if (!isClearRestrictionAllowed(packName)) {
                    setClearRestrictionAllowed(packName)
                }
                clearRestriction(packName)
            }
        } else {
            if (isAppopsRestricted(packName)) {
                setAppopsRestrict(packName, false)
            }
        }
    }

    fun isEcmMode(): Boolean {
//        return Flags.enhancedConfirmationModeApisEnabled() && com.android.internal.hidden_from_bootclasspath.android.security.Flags.extendEcmToAllSettings()
        return Flags.enhancedConfirmationModeApisEnabled()
    }

    fun isEcmRestricted(packName: String, type: String = bindAccessibilityService): Boolean {
        val ecm = context.getSystemService("ecm_enhanced_confirmation")
        return ecm.asResolver().firstMethod {
            name = "isRestricted"
            parameters(String::class, String::class)
            returnType = Boolean::class
        }.invoke<Boolean>(packName, type) ?: false
    }

    fun createRestrictedSettingDialogIntent(
        packName: String, type: String = bindAccessibilityService
    ): Intent? {
        val ecm = context.getSystemService("ecm_enhanced_confirmation")
        return ecm.asResolver().firstMethod {
            name = "createRestrictedSettingDialogIntent"
            parameters(String::class, String::class)
            returnType = Intent::class
        }.invoke<Intent>(packName, type)
    }

    fun isClearRestrictionAllowed(packName: String): Boolean {
        val ecm = context.getSystemService("ecm_enhanced_confirmation")
        return ecm.asResolver().firstMethod {
            name = "isClearRestrictionAllowed"
            parameters(String::class)
            returnType = Boolean::class
        }.invoke<Boolean>(packName) ?: false
    }

    fun setClearRestrictionAllowed(packName: String) {
        val ecm = context.getSystemService("ecm_enhanced_confirmation")
        ecm.asResolver().firstMethod {
            name = "setClearRestrictionAllowed"
            parameters(String::class)
            returnType = Void.TYPE
        }.invoke(packName)
    }

    fun clearRestriction(packName: String) {
        val ecm = context.getSystemService("ecm_enhanced_confirmation")
        ecm.asResolver().firstMethod {
            name = "clearRestriction"
            parameters(String::class)
            returnType = Void.TYPE
        }.invoke(packName)
    }

    fun isAppopsRestricted(packName: String): Boolean {
        val appOps = context.getSystemService(classOf<AppOpsManager>())
        val op = appOps.asResolver().firstField { name = "OPSTR_ACCESS_RESTRICTED_SETTINGS" }
            .get<String>() ?: return false
        val uid = PackageUtils(context.packageManager).getPackageUid(packName, 0) ?: return false
        val note = appOps.noteOpNoThrow(op, uid, packName, null, null)
        return !(note == 0 || note == 3)
    }

    fun setAppopsRestrict(packName: String, restrict: Boolean) {
        val appOps = context.getSystemService(classOf<AppOpsManager>())
        appOps.asResolver().firstMethod {
            name = "setMode"
            parameters(Int::class, Int::class, String::class, Int::class)
        }.invoke(119, uid, packName, if (restrict) 1 else 0)
    }


}