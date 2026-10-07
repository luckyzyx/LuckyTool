package com.luckyzyx.luckytool.ui.application

import android.app.Application
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.utils.PredictiveBackUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        XposedServiceBridge.init()
        // 预测性返回手势：把偏好同步到 ApplicationInfo（对齐 KernelSU 启动期行为，best-effort）
        PredictiveBackUtils.apply(this, ShellSettingsController.get(this).predictiveBack)
    }

    fun reloadAllActivities() {
        ActivityLifecycleManager.recreateAllActivities()
    }
}


























