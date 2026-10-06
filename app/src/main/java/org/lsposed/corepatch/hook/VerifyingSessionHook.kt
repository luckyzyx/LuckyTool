package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.hostClassLoader

object VerifyingSessionHook : BaseHook() {
    override val name = "VerifyingSessionHook"

    private const val INSTALL_DISABLE_VERIFICATION = 0x00080000

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }

        val verifyingSessionClazz =
            hostClassLoader.loadClass("com.android.server.pm.VerifyingSession")

        val installFlagsField =
            verifyingSessionClazz.getDeclaredField("mInstallFlags").apply { isAccessible = true }
        val handleStartVerifyMethod =
            verifyingSessionClazz.declaredMethods.first { m -> m.name == "handleStartVerify" }
        hookBefore(handleStartVerifyMethod) { callback ->
            if (Config.isDisableVerificationAgentEnabled()) {
                val session = callback.thisObject ?: return@hookBefore
                installFlagsField.setInt(
                    session,
                    installFlagsField.getInt(session) or INSTALL_DISABLE_VERIFICATION
                )
            }
        }

        val isAdbVerificationEnabledMethod =
            verifyingSessionClazz.declaredMethods.first { m ->
                m.name == "isAdbVerificationEnabled" &&
                    m.returnType == Boolean::class.java
            }

        hookBefore(isAdbVerificationEnabledMethod) { callback ->
            if (Config.isDisableVerificationAgentEnabled()) {
                callback.returnAndSkip(false)
            }
        }
    }
}
