package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import org.lsposed.corepatch.Config

object ApkSigningBlockUtilsHook : BaseHook() {
    override val name = "ApkSigningBlockUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        val apkSigningBlockUtilsClazz =
            "android.util.apk.ApkSigningBlockUtils".toClass()
        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/util/apk/ApkSigningBlockUtils.java;l=303
        val parseVerityDigestAndVerifySourceLengthMethod =
            apkSigningBlockUtilsClazz.declaredMethods.first { m -> m.name == "parseVerityDigestAndVerifySourceLength" }
        parseVerityDigestAndVerifySourceLengthMethod.hook {
            before {
                if (Config.isBypassVerificationEnabled()) {
                    result = (args[0] as ByteArray).copyOfRange(0, 32)
                }
            }
        }

        val verifyIntegrityForVerityBasedAlgorithmMethod =
            apkSigningBlockUtilsClazz.declaredMethods.first { m -> m.name == "verifyIntegrityForVerityBasedAlgorithm" }
        verifyIntegrityForVerityBasedAlgorithmMethod.hook {
            //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
            intercept {
                if (Config.isBypassVerificationEnabled()) {
                    null
                } else callOriginal()
            }
        }
    }
}
