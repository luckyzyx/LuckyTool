package com.luckyzyx.luckytool.ui

import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.utils.PredictiveBackUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class App : ModuleApplication() {

    override fun onCreate() {
        super.onCreate()

        // 激活检测走 XposedServiceBridge（订阅 YukiHook 的框架 service 观察）：
        // XposedServiceHelper 只有一个监听槽，YukiHook 的 LibXposedService 在 super.onCreate()
        // 里已经占过一次，这里再注册会把它顶掉，所以只订阅 observeFrameworkService、不注册监听
        XposedServiceBridge.init()
        // 预测性返回手势：启动时尝试把偏好同步到 ApplicationInfo。
        PredictiveBackUtils.apply(this, ShellSettingsController.get(this).predictiveBack)
    }

}


























