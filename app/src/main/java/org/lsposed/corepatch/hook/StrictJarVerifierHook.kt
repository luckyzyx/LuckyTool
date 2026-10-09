package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import org.lsposed.corepatch.Config
import java.security.cert.Certificate

object StrictJarVerifierHook : BaseHook() {
    override val name = "StrictJarVerifierHook"

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    override fun hook() {
        val strictJarVerifierClazz = "android.util.jar.StrictJarVerifier".toClass()

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/util/jar/StrictJarVerifier.java;l=529
        // private static boolean verifyMessageDigest(byte[] expected, byte[] encodedActual)
        val verifyMessageDigestMethod =
            strictJarVerifierClazz.declaredMethods.first { m -> m.name == "verifyMessageDigest" && m.returnType == Boolean::class.java }
        verifyMessageDigestMethod.hook {
            before {
                if (Config.isBypassVerificationEnabled()) {
                    result = true
                }
            }
        }

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/util/jar/StrictJarVerifier.java;l=502
        // private boolean verify(
        //     Attributes attributes,
        //     String entry,
        //     byte[] data,
        //     int start,
        //     int end,
        //     boolean ignoreSecondEndline,
        //     boolean ignorable)
        val verifyMethod =
            strictJarVerifierClazz.declaredMethods.first { m -> m.name == "verify" && m.returnType == Boolean::class.java }
        verifyMethod.hook {
            before {
                if (Config.isBypassVerificationEnabled()) {
                    result = true
                }
            }
        }

        val strictJarVerifierConstructor = strictJarVerifierClazz.declaredConstructors.first()
        val signatureSchemeRollbackProtectionsEnforcedField =
            strictJarVerifierClazz.declaredFields.first { f -> f.name == "signatureSchemeRollbackProtectionsEnforced" }
        signatureSchemeRollbackProtectionsEnforcedField.isAccessible = true
        strictJarVerifierConstructor.hook {
            after {
                if (Config.isBypassVerificationEnabled()) {
                    signatureSchemeRollbackProtectionsEnforcedField.set(
                        instance, false
                    )
                }
            }
        }

        val pkcs7Clazz = "sun.security.pkcs.PKCS7".toClass()
        val pkcs7Constructor = pkcs7Clazz.declaredConstructors.first { c ->
            c.parameterTypes.size == 1 && c.parameterTypes[0] == ByteArray::class.java
        }
        val getSignerInfosMethod = pkcs7Clazz.getDeclaredMethod("getSignerInfos")
        val signerInfoClazz = "sun.security.pkcs.SignerInfo".toClass()
        val getCertificateChainMethod =
            signerInfoClazz.getDeclaredMethod("getCertificateChain", pkcs7Clazz)

        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/util/jar/StrictJarVerifier.java;l=324
        // static Certificate[] verifyBytes(byte[] blockBytes, byte[] sfBytes)
        val verifyBytesMethod = strictJarVerifierClazz.getDeclaredMethod(
            "verifyBytes", ByteArray::class.java, ByteArray::class.java
        )
        verifyBytesMethod.hook {
            after {
                if (Config.isBypassDigestEnabled() && !Config.isUsePreviousSignaturesEnabled()) {
                    val block = pkcs7Constructor.newInstance(args[0])
                    val signerInfo = getSignerInfosMethod.invoke(block) as Array<*>
                    if (signerInfo.isEmpty()) return@after
                    val signer = signerInfo[0]
                    // libcore 的 SignerInfo.getCertificateChain 返回 ArrayList<X509Certificate>（List 而非数组），
                    // 需转换为 Certificate[] 以匹配 verifyBytes 的声明返回类型，否则框架返回值类型校验会抛
                    // ClassCastException: Return value's type from hook callback does not match the hooked method
                    val certs = getCertificateChainMethod.invoke(signer, block) as List<*>
                    // 新 API 中给 result 赋值会同时清除 throwable，无需再显式置空
                    result = certs.map { it as Certificate }.toTypedArray()
                }
            }
        }
    }
}
