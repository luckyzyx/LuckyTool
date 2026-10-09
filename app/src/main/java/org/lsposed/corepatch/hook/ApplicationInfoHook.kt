package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import org.lsposed.corepatch.Config

object ApplicationInfoHook : BaseHook() {
    override val name = "ApplicationInfoHook"

    @SuppressLint("SoonBlockedPrivateApi")
    override fun hook() {
        val applicationInfoClazz = "android.content.pm.ApplicationInfo".toClass()

        // if app is system app, allow to use hidden api, even if app not using a system signature
        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/ApplicationInfo.java;l=1678
        // private boolean isPackageWhitelistedForHiddenApis()
        val isPackageWhitelistedForHiddenApisMethod =
            applicationInfoClazz.getDeclaredMethod("isPackageWhitelistedForHiddenApis")
        isPackageWhitelistedForHiddenApisMethod.hook {
            before {
                if (Config.isAllowHiddenApisForSystemAppsEnabled()) {
                    val applicationInfo = instance as ApplicationInfo
                    if (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 || applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) {
                        result = true
                    }
                }
            }
        }
    }
}
