package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.lsposed.lsparanoid.Obfuscate

/**
 * 预测性返回手势开关（迁移 KernelSU `KernelSUApplication.setEnableOnBackInvokedCallback`）。
 *
 * `ApplicationInfo.setEnableOnBackInvokedCallback` 不是公开 API，反射调用前需要经 HiddenApiBypass
 * 添加豁免；豁免申请失败时退回 best-effort（失败即静默忽略）。
 *
 * 自 targetSdk 提升到 37（>= 33）后，系统默认即为应用派发预测性返回回调；本工具据此在启动期
 * 把 `enable_predictive_back` 偏好同步到 ApplicationInfo，实现「开 → 启用、关 → 回落旧返回」，
 * 与 KernelSU 启动期行为一致（偏好变更生效于下次启动）。
 */
@Obfuscate
object PredictiveBackUtils {

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun apply(context: Context, enable: Boolean) {
        if (!isSupported()) return
        runCatching {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;")
            val method = ApplicationInfo::class.java.getDeclaredMethod(
                "setEnableOnBackInvokedCallback",
                java.lang.Boolean.TYPE,
            )
            method.isAccessible = true
            method.invoke(context.applicationInfo, enable)
        }
    }
}
