package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method

/** 仅缓存反射成员，资格值每次读取；由主线程访问，不持有宿主实例。 */
internal class CredentialBiometricGate(
    private val onBroken: (String, Throwable?) -> Unit = { _, _ -> },
    private val findClass: (String, ClassLoader) -> Class<*> = { name, loader ->
        Class.forName(name, false, loader)
    },
) {
    // null 表示成员缺失，避免在每次触摸时重复解析。
    private val members = HashMap<Class<*>, HashMap<String, Member?>>()
    private val statics = HashMap<ClassLoader, HostStatics?>()
    private val reported = HashSet<String>()

    fun isKeyguardActive(container: Any): Boolean {
        val monitor = updateMonitorOf(container) ?: return false
        if (booleanOf(monitor, "mKeyguardShowing") != true) return false
        if (booleanOf(monitor, "mKeyguardGoingAway") != false) return false
        if (booleanOf(monitor, "mSwitchingUser") != false) return false
        if (booleanOf(monitor, "mGoingToSleep") != false) return false
        return booleanOf(monitor, "mDeviceInteractive") == true
    }

    fun isBiometricEligible(container: Any, userId: Int): Boolean {
        if (!isKeyguardActive(container)) return false
        val monitor = updateMonitorOf(container) ?: return false
        val context = fieldOf(monitor, "mContext")?.get(monitor) as? Context ?: return false
        if ((fieldOf(container, "mCurrentUser")?.get(container) as? Int) != userId) return false
        val loader = monitor.javaClass.classLoader ?: return false
        val statics = staticsOf(loader) ?: return false
        if (statics.ceStorageUnlocked.invoke(null, userId) != true) return false
        if (booleanOf(monitor, "mFingerprintLockedOut") != false) return false
        if (booleanOf(monitor, "mFingerprintLockedOutPermanent") != false) return false
        val fingerprintAllowed = methodOf(monitor.javaClass, "isUnlockingWithBiometricAllowed") {
            monitor.asResolver().optional(silent = true).firstMethodOrNull {
                name = "isUnlockingWithBiometricAllowed"
                parameters(statics.fingerprint.javaClass)
            }?.self?.accessible()
        } ?: return false
        if (fingerprintAllowed.invoke(monitor, statics.fingerprint) != true) return false
        val extension = fieldOf(monitor, "mOplusKeyguardUpdateMonitor")?.get(monitor) ?: return false
        val unlockPossible = methodOf(extension.javaClass, "isFingerprintUnlockPossible") {
            extension.asResolver().optional(silent = true).firstMethodOrNull {
                name = "isFingerprintUnlockPossible"
                parameters(Int::class)
                superclass()
            }?.self?.accessible()
        } ?: return false
        if (unlockPossible.invoke(extension, userId) != true) return false
        val companion = statics.switchCompanion.get(null) ?: return false
        val getInstance = methodOf(companion.javaClass, "getInstance") {
            companion.asResolver().optional(silent = true).firstMethodOrNull {
                name = "getInstance"
                parameters(Context::class)
            }?.self?.accessible()
        } ?: return false
        val switches = getInstance.invoke(companion, context) ?: return false
        val switchEnabled = methodOf(switches.javaClass, "isFingerprintUnlockEnable") {
            switches.asResolver().optional(silent = true).firstMethodOrNull {
                name = "isFingerprintUnlockEnable"
                emptyParameters()
            }?.self?.accessible()
        } ?: return false
        if (switchEnabled.invoke(switches) != true) return false
        val dependency = statics.dependency.get(null) ?: return false
        val getDependency = methodOf(dependency.javaClass, "getDependency") {
            dependency.asResolver().optional(silent = true).firstMethodOrNull {
                name = "getDependency"
                parameters(Class::class)
            }?.self?.accessible()
        } ?: return false
        val keyguard = getDependency.invoke(dependency, statics.keyguardDependencyType) ?: return false
        val verifyManagerGetter = methodOf(keyguard.javaClass, "getOplusKeyguardVerifyManager") {
            keyguard.asResolver().optional(silent = true).firstMethodOrNull {
                name = "getOplusKeyguardVerifyManager"
                emptyParameters()
                superclass()
            }?.self?.accessible()
        } ?: return false
        val verifyManager = verifyManagerGetter.invoke(keyguard) ?: return false
        val needVerify = methodOf(verifyManager.javaClass, "needVerify") {
            verifyManager.asResolver().optional(silent = true).firstMethodOrNull {
                name = "needVerify"
                emptyParameters()
            }?.self?.accessible()
        } ?: return false
        return needVerify.invoke(verifyManager) == false
    }

    private class HostStatics(
        val ceStorageUnlocked: Method,
        val fingerprint: Any,
        val switchCompanion: Field,
        val dependency: Field,
        val keyguardDependencyType: Class<*>,
    )

    private fun staticsOf(loader: ClassLoader): HostStatics? {
        if (statics.containsKey(loader)) return statics[loader]
        val resolved = runCatching {
            val ceStorageUnlocked = findClass("android.os.storage.StorageManager", loader)
                .getMethod("isCeStorageUnlocked", Int::class.javaPrimitiveType)
            val fingerprintType = findClass("android.hardware.biometrics.BiometricSourceType", loader)
            val fingerprint = checkNotNull(fingerprintType.enumConstants)
                .first { (it as Enum<*>).name == "FINGERPRINT" }
            val switchCompanion = findClass(
                "com.oplus.systemui.biometrics.finger.FingerSwitchHelper", loader,
            ).getField("Companion")
            val dependency = findClass("com.android.systemui.DependencyEx", loader)
                .getField("sDependency")
            val keyguardDependencyType = findClass(
                "com.android.keyguard.OplusKeyguardDependencyEx", loader,
            )
            HostStatics(ceStorageUnlocked, fingerprint, switchCompanion, dependency, keyguardDependencyType)
        }.onFailure { report("host statics", it) }.getOrNull()
        statics[loader] = resolved
        return resolved
    }

    private fun memberOf(owner: Class<*>, key: String, resolve: () -> Member?): Member? {
        members[owner]?.let { if (it.containsKey(key)) return it[key] }
        val member = runCatching { resolve() }.getOrNull()
        members.getOrPut(owner) { HashMap() }[key] = member
        if (member == null) report("$owner.$key", null)
        return member
    }

    private fun fieldOf(instance: Any, fieldName: String): Field? = memberOf(
        instance.javaClass, fieldName,
    ) {
        instance.asResolver().optional(silent = true)
            .firstFieldOrNull { name = fieldName }?.self?.accessible()
    } as Field?

    private fun methodOf(owner: Class<*>, methodName: String, resolve: () -> Method?): Method? =
        memberOf(owner, methodName, resolve) as Method?

    private fun updateMonitorOf(container: Any): Any? =
        fieldOf(container, "mUpdateMonitor")?.get(container)

    private fun booleanOf(instance: Any, fieldName: String): Boolean? =
        fieldOf(instance, fieldName)?.get(instance) as? Boolean

    private fun report(what: String, error: Throwable?) {
        if (!reported.add(what)) return
        onBroken(what, error)
    }

    private fun <M : java.lang.reflect.AccessibleObject> M.accessible(): M = apply { isAccessible = true }
}
