package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import java.util.Arrays

object SigningDetailsHook : BaseHook() {
    override val name = "SigningDetailsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        val signingDetailsClazz =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "android.content.pm.SigningDetails".toClass()
            } else {
                "android.content.pm.PackageParser\$SigningDetails".toClass()
            }

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5851
        // public boolean checkCapability(SigningDetails oldDetails, @CertCapabilities int flags)

        val checkCapabilityMethod = signingDetailsClazz.getDeclaredMethod(
            "checkCapability", signingDetailsClazz, Int::class.java
        )
        checkCapabilityMethod.hook {
            before {
                if (Config.isBypassDigestEnabled()) {
                    if (args[1] != 4 && args[1] != 16) {
                        result = true
                    }
                }
            }
        }

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5962
        // public boolean checkCapabilityRecover(SigningDetails oldDetails, @CertCapabilities int flags)
        // New package has a different signature
        val checkCapabilityRecoverMethod = signingDetailsClazz.getDeclaredMethod(
            "checkCapabilityRecover", signingDetailsClazz, Int::class.java
        )
        checkCapabilityRecoverMethod.hook {
            before {
                if (Config.isBypassDigestEnabled()) {
                    // Don't handle PERMISSION (grant SIGNATURE permissions to pkgs with this cert)
                    // Or applications will have all privileged permissions
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageParser.java;l=5947
                    if (args[1] != 4 && args[1] != 16) {
                        result = true
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // for SharedUser
            // "Package " + packageName + " has a signing lineage " + "that diverges from the lineage of the sharedUserId"
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=725
            val hasCommonAncestorMethod = signingDetailsClazz.getDeclaredMethod(
                "hasCommonAncestor", signingDetailsClazz
            )
            hasCommonAncestorMethod.hook {
                before {
                    if (Config.isBypassDigestEnabled() && Config.isBypassSharedUserEnabled()
                        // because of LSPosed's bug, we can't hook verifySignatures while deoptimize it
                        && Arrays.stream(
                            Thread.currentThread().stackTrace
                        ).anyMatch { o: StackTraceElement -> "verifySignatures" == o.methodName }
                    ) {
                        result = true
                    }
                }
            }
        }

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r61:frameworks/base/core/java/android/content/pm/PackageParser.java;l=6036
        val signaturesMatchExactlyMethod = signingDetailsClazz.getDeclaredMethod("signaturesMatchExactly", signingDetailsClazz)
        signaturesMatchExactlyMethod.hook {
            before {
                if (Config.isBypassExactSignatureMatch()) {
                    result = true
                }
            }
        }
    }
}
