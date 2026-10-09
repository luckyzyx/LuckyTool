package com.luckyzyx.luckytool.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
 * 把 `enable_predictive_back` 偏好同步到 ApplicationInfo。默认开启（opt-out）：偏好为 true 时
 * 保持系统默认的预测性返回，false 时回落旧返回。开关切换用 [applyImmediately] 即时生效。
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

    /**
     * 即时生效：反射改写 ApplicationInfo 后重建当前 Activity。
     *
     * 该 flag 在 [android.view.Window] 构造时只读一次并缓存到 final 字段，
     * 因此仅改写 ApplicationInfo 不会影响已存在的窗口；必须重建 Activity 让新窗口
     * 重新读取更新后的 flag。仅用于设置页开关切换，进程启动期同步用 [apply]。
     */
    fun applyImmediately(context: Context, enable: Boolean) {
        if (!isSupported()) return
        apply(context, enable)
        context.findActivity()?.recreate()
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
