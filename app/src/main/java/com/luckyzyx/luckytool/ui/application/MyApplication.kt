package com.luckyzyx.luckytool.ui.application

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.utils.ThemeUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        XposedServiceBridge.init()

        applyThemeBasedOnPreferences()
    }


    private fun applyThemeBasedOnPreferences() {
        if (ThemeUtils.isDynamicColorsEnabled(this)) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }

    fun reloadAllActivities() {
        ActivityLifecycleManager.recreateAllActivities()
    }
}


























