package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object VerificationParamsHook: BaseHook() {
    override val name = "VerificationParamsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val verificationParamsClazz = "com.android.server.pm.VerificationParams".toClass()

        val isVerificationEnabledMethod = verificationParamsClazz.declaredMethods.first { m -> m.name == "isVerificationEnabled" }
        isVerificationEnabledMethod.hook {
            before {
                if (Config.isDisableVerificationAgentEnabled()) {
                    result = false
                }
            }
        }
    }
}
