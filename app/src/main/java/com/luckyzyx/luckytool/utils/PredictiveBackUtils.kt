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
 * 另注意：应用 targetSdk 为 28，系统不会为它派发预测性返回回调，
 * 因此本项在本工程内属于「开关与偏好已就位、需 targetSdk >= 33 才真正生效」。
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
