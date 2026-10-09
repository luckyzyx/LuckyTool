package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.corepatch.Config

object PackageManagerServiceUtilsHook : BaseHook() {
    override val name = "PackageManagerServiceUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        val packageManagerServiceUtilsClazz =
            "com.android.server.pm.PackageManagerServiceUtils".toClass()

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=552
        // public static boolean verifySignatures(
        //     PackageSetting pkgSetting,
        //     PackageSetting disabledPkgSetting,
        //     PackageParser.SigningDetails parsedSignatures,
        //     boolean compareCompat,
        //     boolean compareRecover)
        // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=625
        // public static boolean verifySignatures(
        //     PackageSetting pkgSetting,
        //     PackageSetting disabledPkgSetting,
        //     PackageParser.SigningDetails parsedSignatures,
        //     boolean compareCompat,
        //     boolean compareRecover,
        //     boolean isRollback)
        val verifySignaturesMethod =
            packageManagerServiceUtilsClazz.declaredMethods.first { m -> m.name == "verifySignatures" && m.returnType == Boolean::class.java }
        if (!verifySignaturesMethod.deoptimize()) YLog.debug("failed to deoptimize verifySignatures")
        verifySignaturesMethod.hook {
            before {
                if (Config.isBypassVerificationEnabled()) {
                    result = false
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=1375
            // public static void checkDowngrade(com.android.server.pm.parsing.pkg.AndroidPackage before, PackageInfoLite after)
            // https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=1499
            // public static void checkDowngrade(com.android.server.pm.pkg.AndroidPackage before, PackageInfoLite after)
            // OneUI inlines the checkDowngrade methods into one, so we need to hook all methods with the same name and parameter types
            packageManagerServiceUtilsClazz.declaredMethods
                .filter { it.name == "checkDowngrade" && it.returnType == Void.TYPE }
                .filter {
                    it.parameterTypes.lastOrNull()?.name ==
                        "android.content.pm.PackageInfoLite"
                }
                .forEach { checkDowngradeMethod ->
                    //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
                    checkDowngradeMethod.hook {
                        intercept {
                            if (Config.isBypassDowngradeEnabled()) null else callOriginal()
                        }
                    }
                }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ensure verifySignatures success
            // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=621
            val canJoinSharedUserIdMethod =
                packageManagerServiceUtilsClazz.declaredMethods.first { m -> m.name == "canJoinSharedUserId" }
            if (!canJoinSharedUserIdMethod.deoptimize()) YLog.debug("failed to deoptimize canJoinSharedUserId")
        }
    }
}
