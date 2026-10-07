package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.lsparanoid.Obfuscate

/**
 * 预测性返回手势开关（迁移 KernelSU `KernelSUApplication.setEnableOnBackInvokedCallback`）。
 *
 * 该 API 未公开：KernelSU 依赖 HiddenApiBypass 添加豁免后再反射调用。
 * LuckyTool 未引入该依赖，这里用 runCatching 做 best-effort（失败即静默忽略）。
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
            val method = ApplicationInfo::class.java.getDeclaredMethod(
                "setEnableOnBackInvokedCallback",
                java.lang.Boolean.TYPE,
            )
            method.isAccessible = true
            method.invoke(context.applicationInfo, enable)
        }
    }
}
