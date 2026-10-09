package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object VerifyingSessionHook : BaseHook() {
    override val name = "VerifyingSessionHook"

    private const val INSTALL_DISABLE_VERIFICATION = 0x00080000

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        val verifyingSessionClazz =
            "com.android.server.pm.VerifyingSession".toClass()

        val installFlagsField =
            verifyingSessionClazz.getDeclaredField("mInstallFlags").apply { isAccessible = true }
        val handleStartVerifyMethod =
            verifyingSessionClazz.declaredMethods.first { m -> m.name == "handleStartVerify" }
        handleStartVerifyMethod.hook {
            before {
                if (Config.isDisableVerificationAgentEnabled()) {
                    val session = instance
                    installFlagsField.setInt(
                        session,
                        installFlagsField.getInt(session) or INSTALL_DISABLE_VERIFICATION
                    )
                }
            }
        }

        val isAdbVerificationEnabledMethod =
            verifyingSessionClazz.declaredMethods.first { m ->
                m.name == "isAdbVerificationEnabled" &&
                    m.returnType == Boolean::class.java
            }

        isAdbVerificationEnabledMethod.hook {
            before {
                if (Config.isDisableVerificationAgentEnabled()) {
                    result = false
                }
            }
        }
    }
}
