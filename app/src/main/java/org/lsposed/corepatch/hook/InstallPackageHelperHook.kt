package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object InstallPackageHelperHook : BaseHook() {
    override val name = "InstallPackageHelperHook"

    @SuppressLint("PrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val installPackageHelperClazz =
            "com.android.server.pm.InstallPackageHelper".toClass()
        val doesSignatureMatchForPermissionsMethod =
            installPackageHelperClazz.declaredMethods.first { m -> m.name == "doesSignatureMatchForPermissions" }
        doesSignatureMatchForPermissionsMethod.hook {
            after {
                if (Config.isBypassDigestEnabled() && Config.isUsePreviousSignaturesEnabled()) {
                    // If we decide to crack this then at least make sure they are same apks, avoid another one that tries to impersonate.
                    if (result == false) {
                        val getPackageNameMethod =
                            args[1]!!.javaClass.declaredMethods.first { m -> m.name == "getPackageName" }
                        val packageName = getPackageNameMethod.invoke(args[1]) as String
                        if (packageName == args[0] as String) {
                            result = true
                        }
                    }
                }
            }
        }
    }
}
