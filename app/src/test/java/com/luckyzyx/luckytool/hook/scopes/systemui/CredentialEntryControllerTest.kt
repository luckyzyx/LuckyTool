package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CredentialEntryControllerTest {
    private lateinit var entry: CredentialEntryController
    private lateinit var controller: InputController
    private lateinit var container: Container

    @Before
    fun setUp() {
        Monitor.userId = 0
        FakeStorageManager.ceStorageUnlocked = true
        FakeSwitchHelper.manager = FakeSwitchManager()
        FakeDependencyEx.sDependency.keyguardDependency = FakeKeyguardDependencyChild()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().visible().get()
        val view = View(activity)
        activity.setContentView(view)
        assertTrue(view.isAttachedToWindow)
        controller = InputController(view)
        container = Container(controller)
        container.mUpdateMonitor.mContext = activity
        entry = CredentialEntryController(::fakeFindClass)
        assertTrue(entry.prepareNativeStructure(Container::class.java, Monitor::class.java))
        entry.bindContainer(container)
    }

    private fun showCredentials(): Boolean = entry.showCredentials(
        showBouncer = { container.bouncerShown = true },
        isBouncerShowing = { container.bouncerShown },
    )

    @Test
    fun `synchronous native callback opens each credential mode once`() {
        for (mode in SecurityMode.entries) {
            container.mode = mode
            controller.mode = mode
            controller.state = InputState.STATE_BIOMETRIC
            entry.bindPasswordAction(controller.mView, controller.showPassword)
            val before = controller.showPassword.calls
            assertTrue(showCredentials())
            assertEquals(before + 1, controller.showPassword.calls)
            entry.onInputAttached(controller)
            assertEquals(before + 1, controller.showPassword.calls)
        }
    }

    @Test
    fun `first inflation waits for native listener without requesting another controller`() {
        container.delayed = true
        assertTrue(entry.isReady())
        assertFalse(entry.isActionReady(controller.mView))
        assertTrue(showCredentials())
        container.deliver()
        assertEquals(0, controller.showPassword.calls)
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        entry.onInputAttached(controller)
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
        assertEquals(1, controller.showPassword.calls)
        assertEquals(1, container.lookups)
    }

    @Test
    fun `listener registered after attach resumes on the next frame`() {
        assertTrue(showCredentials())
        entry.onInputAttached(controller)
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertEquals(0, controller.showPassword.calls)
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
        assertEquals(1, controller.showPassword.calls)
    }

    @Test
    fun `normal lockout and olock states are not changed by a late callback`() {
        for (state in listOf(InputState.STATE_NORMAL, InputState.STATE_LOCKOUT, InputState.STATE_OLOCK)) {
            controller.state = InputState.STATE_BIOMETRIC
            container.delayed = true
            entry.bindPasswordAction(controller.mView, controller.showPassword)
            assertTrue(showCredentials())
            controller.state = state
            container.deliver()
            assertEquals(0, controller.showPassword.calls)
            assertEquals(state, controller.state)
        }
    }

    @Test
    fun `user switch invalidates a pending native callback`() {
        container.delayed = true
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertTrue(showCredentials())
        Monitor.userId = 10
        container.deliver()
        assertEquals(0, controller.showPassword.calls)
    }

    @Test
    fun `keyguard exit cancels an action waiting for the native listener`() {
        assertTrue(showCredentials())
        container.mUpdateMonitor.mKeyguardGoingAway = true
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        entry.onInputAttached(controller)
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
        assertEquals(0, controller.showPassword.calls)
    }

    @Test
    fun `cancelling a request makes its old callback harmless`() {
        container.delayed = true
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertTrue(showCredentials())
        val oldCallback = requireNotNull(container.callback)
        entry.cancelPending()
        assertTrue(showCredentials())
        oldCallback.onViewInflated(controller)
        assertEquals(0, controller.showPassword.calls)
        container.deliver()
        assertEquals(1, controller.showPassword.calls)
    }

    @Test
    fun `detach discards the captured action and pending request`() {
        assertTrue(showCredentials())
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        entry.onInputDetached(controller)
        shadowOf(Looper.getMainLooper()).idleFor(32, TimeUnit.MILLISECONDS)
        entry.onInputAttached(controller)
        assertEquals(0, controller.showPassword.calls)
        assertFalse(entry.isActionReady(controller.mView))
    }

    @Test
    fun `missing native action makes readiness false and preserves gesture fallback`() {
        val incompatibleAction = Any()
        entry.bindPasswordAction(controller.mView, incompatibleAction)
        assertFalse(showCredentials())
        assertFalse(entry.isReady())
        assertEquals(0, controller.showPassword.calls)
    }

    @Test
    fun `live manager permits credentials before monitor receives the bouncer update`() {
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertFalse(container.mUpdateMonitor.mPrimaryBouncerIsOrWillBeShowing)
        assertTrue(showCredentials())
        assertTrue(container.bouncerShown)
        assertFalse(container.mUpdateMonitor.mPrimaryBouncerIsOrWillBeShowing)
        assertEquals(1, controller.showPassword.calls)
    }

    @Test
    fun `live manager hiding the bouncer invalidates a pending callback`() {
        container.delayed = true
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertTrue(showCredentials())
        container.bouncerShown = false
        container.deliver()
        container.bouncerShown = true
        entry.onInputAttached(controller)
        assertEquals(0, controller.showPassword.calls)
        assertEquals(InputState.STATE_BIOMETRIC, controller.state)
    }

    @Test
    fun `biometric gate reads live eligibility state`() {
        assertTrue(entry.canUseBiometric())
        container.mUpdateMonitor.mFingerprintLockedOut = true
        assertFalse(entry.canUseBiometric())
        container.mUpdateMonitor.mFingerprintLockedOut = false
        assertTrue(entry.canUseBiometric())
        FakeStorageManager.ceStorageUnlocked = false
        assertFalse(entry.canUseBiometric())
        FakeStorageManager.ceStorageUnlocked = true
        assertTrue(entry.canUseBiometric())
        Monitor.userId = 7
        assertFalse(entry.canUseBiometric())
        container.mCurrentUser = 7
        assertTrue(entry.canUseBiometric())
        Monitor.userId = 0
        assertFalse(entry.canUseBiometric())
        container.mCurrentUser = 0
        assertTrue(entry.canUseBiometric())
    }

    @Test
    fun `probe resolution failure falls back and keeps the entry usable`() {
        val bareContainer = GateContainer(BareGateMonitor())
        entry.bindContainer(bareContainer)
        assertFalse(entry.canUseBiometric())
        assertFalse(entry.canUseBiometric())
        entry.bindContainer(container)
        assertTrue(entry.canUseBiometric())
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertTrue(showCredentials())
        assertEquals(1, controller.showPassword.calls)
    }

    @Test
    fun `a temporarily hidden bouncer does not disable a later credential request`() {
        assertFalse(entry.showCredentials(showBouncer = {}, isBouncerShowing = { false }))
        assertTrue(entry.isReady())
        entry.bindPasswordAction(controller.mView, controller.showPassword)
        assertTrue(showCredentials())
        assertEquals(1, controller.showPassword.calls)
    }

    enum class SecurityMode { Pattern, PIN, Password }
    enum class InputState { STATE_BIOMETRIC, STATE_NORMAL, STATE_LOCKOUT, STATE_OLOCK }

    open class InputBase(@JvmField val mView: View) {
        var mode = SecurityMode.Pattern
        var state = InputState.STATE_BIOMETRIC
        fun getSecurityMode(): SecurityMode = mode
        fun getCurrentState(): InputState = state
    }

    class InputController(view: View) : InputBase(view) {
        val showPassword = PasswordAction(this)
    }

    class PasswordAction(private val controller: InputController) {
        var calls = 0
        fun invoke() {
            calls++
            controller.state = InputState.STATE_NORMAL
        }
    }

    fun interface OnViewInflatedCallback {
        fun onViewInflated(controller: InputController)
    }

    class Container(private val controller: InputController) {
        @JvmField val mUpdateMonitor = Monitor()
        @JvmField var mCurrentUser = 0
        var mode = SecurityMode.Pattern
        var delayed = false
        var callback: OnViewInflatedCallback? = null
        var lookups = 0
        var bouncerShown = false

        fun getCurrentSecurityMode(): SecurityMode = mode
        fun getCurrentSecurityController(callback: OnViewInflatedCallback) {
            lookups++
            if (delayed) this.callback = callback else callback.onViewInflated(controller)
        }
        fun deliver() {
            val action = requireNotNull(callback)
            callback = null
            action.onViewInflated(controller)
        }
    }

    class Monitor {
        @JvmField var mKeyguardShowing = true
        @JvmField var mKeyguardGoingAway = false
        @JvmField var mSwitchingUser = false
        @JvmField var mGoingToSleep = false
        @JvmField var mDeviceInteractive = true
        @JvmField var mPrimaryBouncerIsOrWillBeShowing = false
        @JvmField var mContext: Context? = null
        @JvmField var mFingerprintLockedOut = false
        @JvmField var mFingerprintLockedOutPermanent = false
        @JvmField var mBiometricAllowed = true
        @JvmField var mOplusKeyguardUpdateMonitor: GateExtension = GateExtension()
        fun isUnlockingWithBiometricAllowed(type: FakeBiometricSource): Boolean = mBiometricAllowed

        companion object {
            var userId = 0
            @JvmStatic fun getCurrentUser(): Int = userId
        }
    }
}
