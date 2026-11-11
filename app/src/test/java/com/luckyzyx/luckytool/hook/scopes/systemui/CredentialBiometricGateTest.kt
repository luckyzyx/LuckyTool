package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Activity
import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// canUseBiometric 探针的宿主替身:gate 的 findClass 把宿主类名映射到下面这些类。
enum class FakeBiometricSource { FINGERPRINT, FACE }

class FakeStorageManager {
    companion object {
        var ceStorageUnlocked = false

        @JvmStatic
        fun isCeStorageUnlocked(userId: Int): Boolean = ceStorageUnlocked
    }
}

class FakeSwitchManager {
    var enabled = true
    fun isFingerprintUnlockEnable(): Boolean = enabled
}

class FakeSwitchHelper private constructor() {
    companion object {
        var manager = FakeSwitchManager()

        @JvmStatic
        fun getInstance(context: Context): FakeSwitchManager = manager
    }
}

class FakeDependencyEx {
    companion object {
        @JvmField
        val sDependency = FakeDependencyEx()
    }
    var keyguardDependency: FakeKeyguardDependencyEx? = null
    fun getDependency(type: Class<*>): Any? = keyguardDependency
}

class FakeVerifyManager {
    var needVerify = false
    fun needVerify(): Boolean = needVerify
}

open class FakeKeyguardDependencyEx {
    var verifyManager = FakeVerifyManager()
    open fun getOplusKeyguardVerifyManager(): FakeVerifyManager = verifyManager
}

class FakeKeyguardDependencyChild : FakeKeyguardDependencyEx()

open class GateExtension {
    var unlockPossible = true
    fun isFingerprintUnlockPossible(userId: Int): Boolean = unlockPossible
}

class GateExtensionChild : GateExtension()

class GateMonitor {
    @JvmField var mKeyguardShowing = true
    @JvmField var mKeyguardGoingAway = false
    @JvmField var mSwitchingUser = false
    @JvmField var mGoingToSleep = false
    @JvmField var mDeviceInteractive = true
    @JvmField var mContext: Context? = null
    @JvmField var mFingerprintLockedOut = false
    @JvmField var mFingerprintLockedOutPermanent = false
    @JvmField var mBiometricAllowed = true
    @JvmField var mOplusKeyguardUpdateMonitor: GateExtension = GateExtension()
    fun isUnlockingWithBiometricAllowed(type: FakeBiometricSource): Boolean = mBiometricAllowed
}

class BareGateMonitor {
    @JvmField var mKeyguardShowing = true
    @JvmField var mKeyguardGoingAway = false
    @JvmField var mSwitchingUser = false
    @JvmField var mGoingToSleep = false
    @JvmField var mDeviceInteractive = true
}

class GateContainer(monitor: Any) {
    @JvmField var mCurrentUser = 0
    @JvmField val mUpdateMonitor: Any = monitor
}

internal fun fakeFindClass(name: String, loader: ClassLoader): Class<*> = when (name) {
    "android.os.storage.StorageManager" -> FakeStorageManager::class.java
    "android.hardware.biometrics.BiometricSourceType" -> FakeBiometricSource::class.java
    "com.oplus.systemui.biometrics.finger.FingerSwitchHelper" -> FakeSwitchHelper::class.java
    "com.android.systemui.DependencyEx" -> FakeDependencyEx::class.java
    "com.android.keyguard.OplusKeyguardDependencyEx" -> FakeKeyguardDependencyEx::class.java
    else -> throw ClassNotFoundException(name)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CredentialBiometricGateTest {
    private lateinit var container: GateContainer
    private val broken = mutableListOf<String>()
    private var findClassCalls = 0

    @Before
    fun setUp() {
        FakeStorageManager.ceStorageUnlocked = true
        FakeSwitchHelper.manager = FakeSwitchManager()
        FakeDependencyEx.sDependency.keyguardDependency = FakeKeyguardDependencyChild()
        container = GateContainer(GateMonitor().apply {
            mContext = Robolectric.buildActivity(Activity::class.java).setup().visible().get()
        })
    }

    private fun gate(failLookup: Boolean = false) = CredentialBiometricGate(
        onBroken = { what, _ -> broken += what },
        findClass = { name, loader ->
            findClassCalls++
            if (failLookup) throw ClassNotFoundException(name)
            fakeFindClass(name, loader)
        },
    )

    private fun monitor(): GateMonitor = container.mUpdateMonitor as GateMonitor

    @Test
    fun `all native conditions met grants biometric`() {
        val probe = gate()
        assertTrue(probe.isKeyguardActive(container))
        assertTrue(probe.isBiometricEligible(container, 0))
    }

    @Test
    fun `eligibility values are read live on every check`() {
        val probe = gate()
        val monitor = monitor()
        assertTrue(probe.isBiometricEligible(container, 0))
        monitor.mFingerprintLockedOut = true
        assertFalse(probe.isBiometricEligible(container, 0))
        monitor.mFingerprintLockedOut = false
        assertTrue(probe.isBiometricEligible(container, 0))
        monitor.mFingerprintLockedOutPermanent = true
        assertFalse(probe.isBiometricEligible(container, 0))
        monitor.mFingerprintLockedOutPermanent = false
        assertTrue(probe.isBiometricEligible(container, 0))
        monitor.mBiometricAllowed = false
        assertFalse(probe.isBiometricEligible(container, 0))
        monitor.mBiometricAllowed = true
        assertTrue(probe.isBiometricEligible(container, 0))
        monitor.mOplusKeyguardUpdateMonitor.unlockPossible = false
        assertFalse(probe.isBiometricEligible(container, 0))
        monitor.mOplusKeyguardUpdateMonitor.unlockPossible = true
        assertTrue(probe.isBiometricEligible(container, 0))
        FakeSwitchHelper.manager.enabled = false
        assertFalse(probe.isBiometricEligible(container, 0))
        FakeSwitchHelper.manager.enabled = true
        assertTrue(probe.isBiometricEligible(container, 0))
        FakeDependencyEx.sDependency.keyguardDependency!!.verifyManager.needVerify = true
        assertFalse(probe.isBiometricEligible(container, 0))
        FakeDependencyEx.sDependency.keyguardDependency!!.verifyManager.needVerify = false
        assertTrue(probe.isBiometricEligible(container, 0))
        container.mCurrentUser = 5
        assertFalse(probe.isBiometricEligible(container, 0))
        container.mCurrentUser = 0
        assertTrue(probe.isBiometricEligible(container, 0))
        monitor.mKeyguardShowing = false
        assertFalse(probe.isBiometricEligible(container, 0))
        monitor.mKeyguardShowing = true
        assertTrue(probe.isBiometricEligible(container, 0))
        FakeStorageManager.ceStorageUnlocked = false
        assertFalse(probe.isBiometricEligible(container, 0))
        FakeStorageManager.ceStorageUnlocked = true
        assertTrue(probe.isBiometricEligible(container, 0))
        assertEquals(0, broken.size)
    }

    @Test
    fun `keyguard exit short-circuits before host class lookup`() {
        monitor().mKeyguardGoingAway = true
        assertFalse(gate().isBiometricEligible(container, 0))
        assertEquals(0, findClassCalls)
    }

    @Test
    fun `static members resolve once and stay live afterwards`() {
        val probe = gate()
        assertTrue(probe.isBiometricEligible(container, 0))
        val afterFirst = findClassCalls
        assertEquals(5, afterFirst)
        FakeStorageManager.ceStorageUnlocked = false
        assertFalse(probe.isBiometricEligible(container, 0))
        FakeStorageManager.ceStorageUnlocked = true
        assertTrue(probe.isBiometricEligible(container, 0))
        assertEquals(afterFirst, findClassCalls)
    }

    @Test
    fun `missing member falls back to false and reports once`() {
        val bare = GateContainer(BareGateMonitor())
        val probe = gate()
        assertFalse(probe.isBiometricEligible(bare, 0))
        assertFalse(probe.isBiometricEligible(bare, 0))
        assertEquals(1, broken.size)
        assertTrue(broken.single().endsWith(".mContext"))
    }

    @Test
    fun `host class lookup failure falls back to false and reports once`() {
        val probe = gate(failLookup = true)
        assertFalse(probe.isBiometricEligible(container, 0))
        assertFalse(probe.isBiometricEligible(container, 0))
        assertEquals(listOf("host statics"), broken)
    }

    @Test
    fun `superclass members are resolved on the runtime class`() {
        monitor().mOplusKeyguardUpdateMonitor = GateExtensionChild()
        assertTrue(gate().isBiometricEligible(container, 0))
    }
}
