package org.lsposed.corepatch.hook

import org.lsposed.corepatch.Config

object MessageDigestHook : BaseHook() {
    override val name = "MessageDigestHook"

    override fun hook() {
        val messageDigestClazz = "java.security.MessageDigest".toClass()
        // https://cs.android.com/android/platform/superproject/main/+/main:libcore/ojluni/src/main/java/java/security/MessageDigest.java;l=518
        // public static boolean isEqual(byte[] digesta, byte[] digestb)
        val isEqualMethod = messageDigestClazz.getDeclaredMethod(
            "isEqual", ByteArray::class.java, ByteArray::class.java
        )
        isEqualMethod.hook {
            before {
                if (Config.isBypassVerificationEnabled()) {
                    result = true
                }
            }
        }
    }
}
