package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object ScanPackageUtilsHook : BaseHook() {
    override val name = "ScanPackageUtilsHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val scanPackageUtilsClazz =
            "com.android.server.pm.ScanPackageUtils".toClass()
        val assertMinSignatureSchemeIsValidMethod =
            scanPackageUtilsClazz.declaredMethods.first { m -> m.name == "assertMinSignatureSchemeIsValid" }
        //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
        assertMinSignatureSchemeIsValidMethod.hook {
            intercept {
                if (Config.isBypassVerificationEnabled()) {
                    null
                } else callOriginal()
            }
        }
    }
}
