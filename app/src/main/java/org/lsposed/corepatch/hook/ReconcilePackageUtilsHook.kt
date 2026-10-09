package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.corepatch.Config

object ReconcilePackageUtilsHook : BaseHook() {
    override val name = "ReconcilePackageUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r75:frameworks/base/services/core/java/com/android/server/pm/ReconcilePackageUtils.java
        val reconcilePackageUtilsClazz =
            "com.android.server.pm.ReconcilePackageUtils".toClass()
        val reconcilePackagesMethod =
            reconcilePackageUtilsClazz.declaredMethods.first { m -> m.name == "reconcilePackages" }
        if (!reconcilePackagesMethod.deoptimize()) YLog.debug("failed to deoptimize reconcilePackages")

        if (Config.isBypassDigestEnabled() && Config.isBypassSharedUserEnabled()) {
            reconcilePackageUtilsClazz.declaredFields.firstOrNull { field -> field.name == "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS" }
                ?.let { field ->
                    UnsafeField.setStaticBoolean(field, true)
                }
        }
    }
}
