package com.luckyzyx.luckytool.ui.application

import android.app.Application
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        XposedServiceBridge.init()
    }

    fun reloadAllActivities() {
        ActivityLifecycleManager.recreateAllActivities()
    }
}


























