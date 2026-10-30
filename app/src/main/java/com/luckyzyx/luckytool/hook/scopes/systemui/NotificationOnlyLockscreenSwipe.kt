package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.MotionEvent
import android.view.InputDevice
import android.view.View
import android.view.ViewConfiguration
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.reflect.Field

internal class NotificationOnlyLockscreenSwipe(
    private val entry: CredentialEntryController,
    private val shortcut: NativeCredentialShortcut,
    private val enabled: () -> Boolean,
) : YukiBaseHooker() {
    private var filter: NotificationSwipeFilter? = null
    private var loggedFailure = false

    override fun onHook() {
        val api = runCatching { Host(checkNotNull(hostClassLoader)) }
            .getOrElse { logOnce(it); return }
        val framework = LockScreenCredentialShortcut.framework ?: return
        val hooks = CredentialHookBridge(framework, ::logOnce)
        val swipeFilter = NotificationSwipeFilter(
            hooks, api, enabled,
            { shortcut.isReady() && entry.canUseBiometric() },
            shortcut::hitsAffordance,
            ::logOnce,
        )
        filter = swipeFilter
        if (swipeFilter.install(api.intercept, api.touch, api.dispatch, api.outerTouch)) {
            LockScreenCredentialShortcut.hasNativeHooks = true
        }
    }

    private fun logOnce(error: Throwable) {
        if (loggedFailure) return
        loggedFailure = true
        runCatching { YLog.error("Credential shortcut: notification swipe unavailable", error) }
    }

    private class Host(private val loader: ClassLoader) : NotificationSwipeFilter.PanelHost {
        private fun type(name: String) = Class.forName(name, false, loader)
        private fun field(owner: Class<*>, name: String): Field =
            owner.getDeclaredField(name).apply { isAccessible = true }
        private val controller = type("com.android.systemui.shade.NotificationPanelViewController")
        private val handler = type("${controller.name}\$TouchHandler")
        private val keyguard = type("com.android.systemui.statusbar.policy.KeyguardStateControllerImpl")
        private val manager = type("com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager")
        val panel = field(handler, "this\$0")
        val view = field(controller, "mView")
        val touchSlop = field(controller, "mTouchSlop")
        val intercept = handler.getDeclaredMethod("onInterceptTouchEvent", MotionEvent::class.java)
        val touch = handler.getDeclaredMethod("handleTouch", MotionEvent::class.java)
        val dispatch = handler.getDeclaredMethod("dispatchCustomTouchEvent", MotionEvent::class.java)
        val outerTouch = handler.getDeclaredMethod("onTouchEvent", MotionEvent::class.java)
        private val barState = field(controller, "mBarState")
        private val fraction = field(controller, "mExpandedFraction")
        private val animator = field(controller, "mHeightAnimator")
        private val instantExpanding = field(controller, "mInstantExpanding")
        private val touchDisabled = field(controller, "mTouchDisabled")
        private val notificationsDragEnabled = field(controller, "mNotificationsDragEnabled")
        private val dozing = field(controller, "mDozing")
        private val pulsing = field(controller, "mPulsing")
        private val bouncer = field(controller, "mBouncerShowing")
        private val state = field(controller, "mKeyguardStateController")
        private val viewManager = field(controller, "mStatusBarKeyguardViewManager")
        private val showing = field(keyguard, "mShowing")
        private val occluded = field(keyguard, "mOccluded")
        private val goingAway = field(keyguard, "mKeyguardGoingAway")
        private val tracking = controller.getDeclaredMethod("isTracking")
        private val bouncerShowing = manager.getDeclaredMethod("primaryBouncerIsOrWillBeShowing")

        init {
            require(intercept.returnType == Boolean::class.javaPrimitiveType)
            require(touch.returnType == Boolean::class.javaPrimitiveType)
            listOf(intercept, touch, dispatch, outerTouch, tracking, bouncerShowing)
                .forEach { it.isAccessible = true }
        }

        override fun keyguardActive(handler: Any): Boolean {
            val panel = checkNotNull(this.panel.get(handler))
            val current = state.get(panel) ?: return false
            return showing.getBoolean(current) && !occluded.getBoolean(current) &&
                !goingAway.getBoolean(current) && !dozing.getBoolean(panel) && !pulsing.getBoolean(panel) &&
                !bouncer.getBoolean(panel) && bouncerShowing.invoke(viewManager.get(panel)) == false
        }

        override fun stable(handler: Any): Boolean {
            val panel = checkNotNull(this.panel.get(handler))
            return keyguardActive(handler) && barState.getInt(panel) == 1 &&
                fraction.getFloat(panel) == 1f && animator.get(panel) == null && tracking.invoke(panel) == false
        }

        override fun canCancelPanel(handler: Any): Boolean {
            val panel = checkNotNull(this.panel.get(handler))
            return keyguardActive(handler) && barState.getInt(panel) == 1 && fraction.getFloat(panel) == 1f
        }

        override fun touchSlop(handler: Any): Float {
            val panel = checkNotNull(this.panel.get(handler))
            val context = (view.get(panel) as View).context
            return (touchSlop.getInt(panel).takeIf { it > 0 }
                ?: ViewConfiguration.get(context).scaledTouchSlop).toFloat()
        }

        override fun initializesPanelDown(handler: Any, event: MotionEvent): Boolean {
            val panel = checkNotNull(this.panel.get(handler))
            return keyguardActive(handler) && !instantExpanding.getBoolean(panel) &&
                !touchDisabled.getBoolean(panel) && notificationsDragEnabled.getBoolean(panel) &&
                event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)
        }
    }
}
