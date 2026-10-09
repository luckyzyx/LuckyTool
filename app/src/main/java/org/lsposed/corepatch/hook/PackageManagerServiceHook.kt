package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object PackageManagerServiceHook : BaseHook() {
    override val name = "PackageManagerServiceHook"

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            return
        }

        val packageManagerServiceClazz =
            "com.android.server.pm.PackageManagerService".toClass()

        // https://cs.android.com/android/platform/superproject/+/android-5.1.0_r5:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=13604
        // private static void checkDowngrade(PackageParser.Package before, PackageInfoLite after)
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/pm/PackageManagerService.java;l=23832
        // private static void checkDowngrade(AndroidPackage before, PackageInfoLite after)
        val checkDowngradeVoidMethod =
            packageManagerServiceClazz.declaredMethods.first { m -> m.name == "checkDowngrade" && m.returnType == Void.TYPE }
        //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
        checkDowngradeVoidMethod.hook {
            intercept {
                if (Config.isBypassDowngradeEnabled()) {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        val before = args[0]!!
                        val packageParserPackageClazz = before.javaClass
                        val mVersionCodeField =
                            packageParserPackageClazz.declaredFields.first { f -> f.name == "mVersionCode" }
                        val mVersionCodeMajorField =
                            packageParserPackageClazz.declaredFields.first { f -> f.name == "mVersionCodeMajor" }
                        mVersionCodeField.set(before, 0)
                        mVersionCodeMajorField.set(before, 0)
                    }
                    null
                } else callOriginal()
            }
        }

        val isVerificationEnabledMethod =
            packageManagerServiceClazz.declaredMethods.first { m -> m.name == "isVerificationEnabled" }
        isVerificationEnabledMethod.hook {
            before {
                if (Config.isDisableVerificationAgentEnabled()) {
                    result = false
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val doesSignatureMatchForPermissionsMethod =
                packageManagerServiceClazz.declaredMethods.first { m -> m.name == "doesSignatureMatchForPermissions" }
            doesSignatureMatchForPermissionsMethod.hook {
                after {
                    if (Config.isBypassDigestEnabled() && Config.isUsePreviousSignaturesEnabled()) {
                        if (result == false) {
                            val getPackageNameMethod =
                                args[1]!!.javaClass.declaredMethods.first { m -> m.name == "getPackageName" }
                            val packageName =
                                getPackageNameMethod.invoke(args[1]) as String
                            if (packageName == args[0] as String) {
                                result = true
                            }
                        }
                    }
                }
            }
        }

        // exists on flyme 9(Android 11) only
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && isFlyme()) {
            val checkDowngradeBooleanMethod =
                packageManagerServiceClazz.declaredMethods.first { m -> m.name == "checkDowngrade" && m.returnType == Boolean::class.java }
            checkDowngradeBooleanMethod.hook {
                before {
                    if (Config.isBypassDowngradeEnabled()) {
                        result = true
                    }
                }
            }
        }
    }

    private fun isFlyme(): Boolean {
        return try {
            Build::class.java.getMethod("hasSmartBar")
            true
        } catch (e: NoSuchMethodException) {
            false
        }
    }
}
