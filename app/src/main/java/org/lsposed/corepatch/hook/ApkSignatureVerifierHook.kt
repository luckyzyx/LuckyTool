package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.Constant

@SuppressLint(
    "PrivateApi",
    "SoonBlockedPrivateApi",
    "BlockedPrivateApi",
    "DiscouragedPrivateApi",
)
object ApkSignatureVerifierHook : BaseHook() {
    override val name = "ApkSignatureVerifierHook"

    override fun hook() {
        val apkSignatureVerifierClazz =
            "android.util.apk.ApkSignatureVerifier".toClass()

        val signingDetailsClazz =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "android.content.pm.SigningDetails".toClass()
            } else {
                "android.content.pm.PackageParser\$SigningDetails".toClass()
            }
        // SigningDetails(Signature[] signatures, int signatureSchemeVersion)
        val signingDetailsConstructor =
            signingDetailsClazz.declaredConstructors.first { constructor ->
                constructor.parameterCount == 2 &&
                    constructor.parameterTypes[0].componentType == Signature::class.java &&
                    constructor.parameterTypes[1] == Int::class.java
            }.apply { isAccessible = true }

        val packageParserExceptionClazz =
            "android.content.pm.PackageParser\$PackageParserException".toClass()
        val errorField = packageParserExceptionClazz.getDeclaredField("error").apply {
            isAccessible = true
        }

        val strictJarFileClazz = "android.util.jar.StrictJarFile".toClass()
        val strictJarFileConstructor = strictJarFileClazz.getDeclaredConstructor(
            String::class.java, Boolean::class.java, Boolean::class.java
        ).apply { isAccessible = true }
        val findEntryMethod = strictJarFileClazz.declaredMethods.first { method ->
            method.name == "findEntry" &&
                method.parameterCount == 1 &&
                method.parameterTypes[0] == String::class.java
        }.apply { isAccessible = true }
        val closeMethod = strictJarFileClazz.getDeclaredMethod("close").apply {
            isAccessible = true
        }
        val convertToSignaturesMethod =
            apkSignatureVerifierClazz.declaredMethods.first { method ->
                method.name == "convertToSignatures" && method.parameterCount == 1
            }.apply { isAccessible = true }

        val parseResultClazz = "android.content.pm.parsing.result.ParseResult".toClassOrNull()
        val parseResultIsErrorMethod = parseResultClazz?.getMethod("isError")
        val parseResultGetErrorCodeMethod = parseResultClazz?.getMethod("getErrorCode")
        val parseResultGetResultMethod = parseResultClazz?.getMethod("getResult")
        val signingDetailsWithDigestsClazz =
            "android.util.apk.ApkSignatureVerifier\$SigningDetailsWithDigests".toClassOrNull()
        val signingDetailsWithDigestsConstructor = signingDetailsWithDigestsClazz
            ?.getDeclaredConstructor(signingDetailsClazz, Map::class.java)
            ?.apply { isAccessible = true }

        // https://cs.android.com/android/platform/superproject/+/android-9.0.0_r59:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=162
        // private static PackageParser.SigningDetails verifyV1Signature(String apkPath, boolean verifyFull)
        // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=355
        // private static SigningDetailsWithDigests verifyV1Signature(String apkPath, boolean verifyFull)
        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r74:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=362
        // private static ParseResult<SigningDetailsWithDigests> verifyV1Signature(ParseInput input, String apkPath, boolean verifyFull)
        apkSignatureVerifierClazz.declaredMethods
            .filter { method -> method.name == "verifyV1Signature" }
            .forEach { verifyV1SignatureMethod ->
                verifyV1SignatureMethod.hook {
                    after {
                        if (Config.isBypassVerificationEnabled()) {
                            val throwable = this.throwable
                            var parseError: Int? = null
                            if (parseResultClazz != null &&
                                verifyV1SignatureMethod.returnType == parseResultClazz
                            ) {
                                val parseResult = result
                                if (parseResult != null &&
                                    parseResultIsErrorMethod?.invoke(parseResult) == true
                                ) {
                                    parseError = parseResultGetErrorCodeMethod?.invoke(parseResult) as Int
                                }
                            }

                            if (throwable != null || parseError != null) {
                                var signaturesBefore: Any? = null
                                // use previous signatures, get from package manager
                                if (Config.isUsePreviousSignaturesEnabled()) {
                                    try {
                                        val activityThreadClazz =
                                            "android.app.ActivityThread".toClass()
                                        val currentApplicationMethod =
                                            activityThreadClazz.getDeclaredMethod("currentApplication")
                                        val application =
                                            currentApplicationMethod.invoke(null) as Application
                                        val packageManager = application.packageManager
                                        if (packageManager == null) {
                                            YLog.debug("Cannot get the Package Manager... Are you using MiUI?")
                                        } else {
                                            val packageInfo = packageManager.getPackageArchiveInfo(
                                                args[if (parseError == null) 0 else 1] as String,
                                                0
                                            )
                                            packageInfo?.let { info ->
                                                val installedPackageInfo = packageManager.getPackageInfo(
                                                    info.packageName,
                                                    PackageManager.GET_SIGNING_CERTIFICATES
                                                )
                                                signaturesBefore = installedPackageInfo.signingInfo
                                                    ?.signingCertificateHistory
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        YLog.debug("cannot get signatures from installed package: ${t.message}")
                                    }
                                }
                                // if previous signatures not found, parse it from apk
                                if (signaturesBefore == null && Config.isBypassDigestEnabled()) {
                                    try {
                                        // verifyV1Signature(String apkPath, boolean verifyFull)
                                        // verifyV1Signature(ParseInput input, String apkPath, boolean verifyFull) // Android 13
                                        val originalJarFile = strictJarFileConstructor.newInstance(
                                            args[if (parseError == null) 0 else 1],
                                            true,
                                            false
                                        )
                                        try {
                                            val manifestEntry = findEntryMethod.invoke(
                                                originalJarFile, "AndroidManifest.xml"
                                            )

                                            //  9 private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry)
                                            // 13 private static ParseResult<Certificate[][]> loadCertificates(
                                            //     ParseInput input, StrictJarFile jarFile, ZipEntry entry)
                                            val loadCertificatesMethod =
                                                apkSignatureVerifierClazz.declaredMethods.first { method ->
                                                    method.name == "loadCertificates" &&
                                                        method.parameterCount ==
                                                        if (parseError == null) 2 else 3
                                                }.apply { isAccessible = true }
                                            val lastCerts = if (parseError == null) {
                                                loadCertificatesMethod.invoke(
                                                    null, originalJarFile, manifestEntry
                                                )
                                            } else {
                                                val certs = requireNotNull(
                                                    loadCertificatesMethod.invoke(
                                                        null,
                                                        args[0],
                                                        originalJarFile,
                                                        manifestEntry
                                                    )
                                                )
                                                parseResultGetResultMethod?.invoke(certs)
                                            }
                                            signaturesBefore = convertToSignaturesMethod.invoke(
                                                null, lastCerts
                                            )
                                        } finally {
                                            runCatching { closeMethod.invoke(originalJarFile) }
                                        }
                                    } catch (t: Throwable) {
                                        YLog.error("Unexpected error while parsing signatures", t)
                                    }
                                }

                                val signingDetailsArgs: Array<Any> = arrayOf(
                                    signaturesBefore ?: arrayOf(Signature(Constant.SIGNATURE)),
                                    1,
                                )
                                var newResult =
                                    signingDetailsConstructor.newInstance(*signingDetailsArgs)

                                // 修复 java.lang.ClassCastException: Cannot cast
                                // PackageParser$SigningDetails to SigningDetailsWithDigests
                                if (signingDetailsWithDigestsConstructor != null) {
                                    newResult = signingDetailsWithDigestsConstructor.newInstance(
                                        newResult, null
                                    )
                                }

                                if (throwable != null) {
                                    val cause = throwable.cause
                                    if (throwable.javaClass == packageParserExceptionClazz &&
                                        errorField.getInt(throwable) == -103
                                    ) {
                                        // 新 API 中给 result 赋值会同时清除 throwable，无需再显式置空
                                        result = newResult
                                    }
                                    if (cause?.javaClass == packageParserExceptionClazz &&
                                        errorField.getInt(cause) == -103
                                    ) {
                                        result = newResult
                                    }
                                }
                                if (parseError == -103) {
                                    val input = args[0]!!
                                    val resetMethod = input.javaClass.getMethod("reset")
                                    val successMethod = input.javaClass.getMethod(
                                        "success", Any::class.java
                                    )
                                    resetMethod.invoke(input)
                                    result = successMethod.invoke(input, newResult)
                                }
                            }
                        }
                    }
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // No signature found in package of version " + minSignatureSchemeVersion
            // + " or newer for package " + apkPath
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/core/java/android/util/apk/ApkSignatureVerifier.java;l=460
            // public static int getMinimumSignatureSchemeVersionForTargetSdk(int targetSdk)
            val getMinimumSignatureSchemeVersionForTargetSdkMethod =
                apkSignatureVerifierClazz.getDeclaredMethod(
                    "getMinimumSignatureSchemeVersionForTargetSdk", Int::class.java
                )
            getMinimumSignatureSchemeVersionForTargetSdkMethod.hook {
                before {
                    if (Config.isBypassVerificationEnabled()) {
                        result = 0
                    }
                }
            }
        }
    }

}
