package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Application
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import io.github.libxposed.api.XposedInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NotificationSwipeFilterTest {
    @Test
    fun `notification owns the complete upward stream without panel down or cancel`() {
        val f = Fixture(notification = true)
        f.dispatch(ACTION_DOWN)
        f.notification = false
        f.dispatch(ACTION_MOVE, dy = -2f)
        f.dispatch(ACTION_UP, dx = 2f, dy = -4f)

        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.childEvents)
        assertTrue(f.handler.nativeTouch.isEmpty())
        assertEquals(listOf(ACTION_DOWN), f.handler.nativeIntercept)
        assertEquals(0, f.handler.panelTracking)
        assertEquals(0, f.handler.downMarker)
        assertEquals(0, f.framework.originCalls)
        f.assertHealthy()
    }

    @Test
    fun `blank upward stream cancels initialized panel once and shares blocked release`() {
        val f = Fixture()
        f.gesture()

        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(listOf(ACTION_DOWN), f.handler.nativeIntercept)
        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.touchEntries)
        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.interceptEntries)
        assertEquals(1, f.handler.downMarker)
        assertEquals(1, f.framework.originCalls)
        assertEquals(0, f.handler.panelTracking)
        assertTrue(f.childEvents.isEmpty())
        f.assertHealthy()
    }

    @Test
    fun `initialized native down returning false still receives cancel when the millisecond marker is unchanged`() {
        val f = Fixture()
        f.handler.keepDownMarker = true
        f.handler.downResult = false
        f.gesture()

        assertEquals(0, f.handler.downMarker)
        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(1, f.framework.originCalls)
        assertEquals(0, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `a native down rejected before initialization never receives a synthetic cancel`() {
        val f = Fixture()
        f.handler.rejectDown = true
        f.gesture()

        assertEquals(listOf(ACTION_DOWN), f.handler.nativeTouch)
        assertEquals(0, f.framework.originCalls)
        assertEquals(0, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `downward and horizontal streams retain native tracking and release`() {
        for ((dx, dy) in listOf(0f to 2f, 2f to 0f, -2f to 0f)) {
            val f = Fixture()
            f.gesture(dx, dy)

            assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.nativeTouch)
            assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.nativeIntercept)
            assertEquals(1, f.handler.panelTracking)
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `camera affordance keeps outer consumption and unknown hit retains native behavior`() {
        for (hit in listOf<Boolean?>(true, null)) {
            val f = Fixture()
            f.affordance = hit
            f.gesture()

            val events = listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP)
            assertEquals(events, f.handler.nativeIntercept)
            assertEquals(if (hit == true) events else emptyList<Int>(), f.outerEvents)
            assertEquals(if (hit == true) emptyList<Int>() else events, f.handler.nativeTouch)
            assertEquals(if (hit == true) 0 else 1, f.handler.panelTracking)
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `disabled or ineligible first upward stream uses native handling`() {
        for (disabled in listOf(true, false)) {
            val f = Fixture()
            f.enabled = !disabled
            f.eligible = disabled
            f.gesture()

            assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.nativeTouch)
            assertEquals(1, f.handler.panelTracking)
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `a second down entry can qualify the same stream after transient panel instability`() {
        val f = Fixture()
        f.stable = false
        f.afterIntercept = { action ->
            if (action == ACTION_DOWN) f.stable = true
        }
        f.gesture()

        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(listOf(ACTION_DOWN), f.handler.nativeIntercept)
        assertEquals(1, f.framework.originCalls)
        assertEquals(0, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `an eligible upward stream survives panel instability before its direction is decided`() {
        val f = Fixture()
        f.dispatch(ACTION_DOWN)
        f.dispatch(ACTION_MOVE, dy = -0.5f)
        f.stable = false
        f.dispatch(ACTION_MOVE, dy = -2f)
        f.stable = true
        f.dispatch(ACTION_UP, dy = -4f)

        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE), f.handler.nativeIntercept)
        assertEquals(1, f.framework.originCalls)
        assertEquals(0, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `side empty handoff cannot reach the native early intercept after an eligible down`() {
        for (startX in listOf(0.001f, 100f)) {
            val f = Fixture()
            f.gesture(dx = 0f, startX = startX, beforeMove = {
                f.stable = false
                f.handler.sideEmpty = true
            })

            assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
            assertEquals(listOf(ACTION_DOWN), f.handler.nativeIntercept)
            assertEquals(0, f.handler.sideEmptyIntercepts)
            assertEquals(1, f.framework.originCalls)
            assertEquals(0, f.handler.panelTracking)
            f.assertHealthy()
        }
    }

    @Test
    fun `dispatch observes upward movement while a child prevents parent interception`() {
        val f = Fixture(observeDispatch = true)
        f.dispatch(ACTION_DOWN)
        f.stable = false
        f.handler.sideEmpty = true
        f.childPreventsIntercept = true
        f.dispatch(ACTION_MOVE, dy = -2f)
        f.childPreventsIntercept = false
        f.dispatch(ACTION_UP, dy = -4f)

        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.customEvents)
        assertEquals(listOf(ACTION_DOWN), f.handler.nativeIntercept)
        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(0, f.handler.sideEmptyIntercepts)
        assertEquals(0, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `dispatch observation preserves notification child movement and shortcut ownership`() {
        for (affordance in listOf(false, true)) {
            val f = Fixture(notification = !affordance, observeDispatch = true)
            f.affordance = affordance
            f.dispatch(ACTION_DOWN)
            f.childPreventsIntercept = true
            f.dispatch(ACTION_MOVE, dy = -2f)
            f.dispatch(ACTION_UP, dy = -4f)

            val events = listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP)
            assertEquals(events, f.handler.customEvents)
            assertEquals(if (affordance) emptyList<Int>() else events, f.childEvents)
            assertEquals(if (affordance) events else emptyList<Int>(), f.outerEvents)
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `four entries block an upward stream before outer side effects and retain release cleanup`() {
        val f = Fixture(observeDispatch = true, filterOuter = true)
        f.handler.earlyOuterConsume = true
        f.gesture()

        assertEquals(listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.customEvents)
        assertEquals(listOf(ACTION_DOWN, ACTION_UP), f.handler.nativeOuter)
        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
        assertEquals(0, f.handler.earlyOuterMoves)
        assertEquals(1, f.handler.outerTerminalCleanups)
        assertEquals(1, f.framework.originCalls)
        f.assertHealthy()
    }

    @Test
    fun `four entries preserve camera consumption and notification child delivery`() {
        for (affordance in listOf(false, true)) {
            val f = Fixture(notification = !affordance, observeDispatch = true, filterOuter = true)
            f.affordance = affordance
            f.gesture()

            val events = listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP)
            assertEquals(events, f.handler.customEvents)
            assertEquals(if (affordance) events else emptyList<Int>(), f.handler.nativeOuter)
            assertEquals(if (affordance) events else emptyList<Int>(), f.outerEvents)
            assertEquals(if (affordance) emptyList<Int>() else events, f.childEvents)
            assertTrue(f.handler.nativeTouch.isEmpty())
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `rewritten down time retains the active gesture across all four entries`() {
        for (rewrite in listOf(1L, 10L, 1000L)) {
            val f = Fixture(observeDispatch = true, filterOuter = true)
            f.dispatch(ACTION_DOWN, time = 100L)
            f.dispatch(ACTION_MOVE, dy = -2f, time = 100L, reportedDownTime = 100L + rewrite)
            f.dispatch(ACTION_UP, dy = -4f, time = 100L, reportedDownTime = 100L + rewrite)

            assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL), f.handler.nativeTouch)
            assertEquals(listOf(ACTION_DOWN, ACTION_UP), f.handler.nativeOuter)
            assertEquals(listOf(100L, 100L), f.handler.nativeTouchDownTimes)
            assertEquals(1, f.framework.originCalls)
            assertEquals(0, f.handler.panelTracking)
            f.assertHealthy()
        }
    }

    @Test
    fun `an event after release cannot inherit a blocked gesture without another down`() {
        val f = Fixture(observeDispatch = true, filterOuter = true)
        f.gesture(time = 100L)
        f.dispatch(ACTION_MOVE, dy = 2f, time = 100L)

        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL, ACTION_MOVE), f.handler.nativeTouch)
        assertEquals(1, f.handler.panelTracking)
        f.gesture(dy = 2f, time = 200L)
        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL, ACTION_MOVE, ACTION_DOWN, ACTION_MOVE, ACTION_UP),
            f.handler.nativeTouch)
        assertEquals(2, f.handler.panelTracking)
        assertEquals(1, f.framework.originCalls)
        f.assertHealthy()
    }

    @Test
    fun `different device or source does not reuse another active down`() {
        for (differentSource in listOf(false, true)) {
            val f = Fixture(observeDispatch = true, filterOuter = true)
            f.dispatch(ACTION_DOWN, time = 100L)
            f.dispatch(ACTION_MOVE, dy = -2f, time = 100L,
                deviceId = if (differentSource) 0 else 8,
                source = if (differentSource) InputDevice.SOURCE_MOUSE else InputDevice.SOURCE_TOUCHSCREEN)

            assertEquals(listOf(ACTION_DOWN, ACTION_MOVE), f.handler.nativeTouch)
            assertEquals(0, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    @Test
    fun `new down replaces the completed stream even when reported down time is reused`() {
        val f = Fixture(observeDispatch = true, filterOuter = true)
        f.gesture(time = 100L)
        f.gesture(dy = 2f, time = 100L)

        assertEquals(listOf(ACTION_DOWN, ACTION_CANCEL, ACTION_DOWN, ACTION_MOVE, ACTION_UP), f.handler.nativeTouch)
        assertEquals(1, f.handler.panelTracking)
        f.assertHealthy()
    }

    @Test
    fun `next down time reevaluates direction after a blocked upward stream`() {
        val f = Fixture()
        f.gesture(time = 100L)
        f.gesture(dy = 2f, time = 200L)

        assertEquals(
            listOf(ACTION_DOWN, ACTION_CANCEL, ACTION_DOWN, ACTION_MOVE, ACTION_UP),
            f.handler.nativeTouch,
        )
        assertEquals(2, f.handler.downMarker)
        assertEquals(1, f.handler.panelTracking)
        assertEquals(1, f.framework.originCalls)
        f.assertHealthy()
    }

    @Test
    fun `keyguard departure or disabling cancels panel state then resumes native handling`() {
        for (leaveKeyguard in listOf(true, false)) {
            val f = Fixture()
            f.dispatch(ACTION_DOWN)
            if (leaveKeyguard) f.keyguard = false else f.enabled = false
            f.dispatch(ACTION_MOVE, dy = -2f)
            f.dispatch(ACTION_UP, dy = -4f)

            assertEquals(
                if (leaveKeyguard) listOf(ACTION_DOWN, ACTION_MOVE, ACTION_UP)
                else listOf(ACTION_DOWN, ACTION_CANCEL, ACTION_MOVE, ACTION_UP),
                f.handler.nativeTouch,
            )
            assertEquals(1, f.handler.panelTracking)
            assertEquals(if (leaveKeyguard) 0 else 1, f.framework.originCalls)
            f.assertHealthy()
        }
    }

    private class Fixture(
        var notification: Boolean = false,
        val observeDispatch: Boolean = false,
        val filterOuter: Boolean = false,
    ) : NotificationSwipeFilter.PanelHost {
        val framework = Framework()
        val handler = FakeHandler()
        val childEvents = mutableListOf<Int>()
        val outerEvents = mutableListOf<Int>()
        val interceptEntries = mutableListOf<Int>()
        val touchEntries = mutableListOf<Int>()
        var enabled = true
        var eligible = true
        var keyguard = true
        var stable = true
        var affordance: Boolean? = false
        var afterIntercept: ((Int) -> Unit)? = null
        var childPreventsIntercept = false
        private var childOwns = false
        private var clock = 0L
        private val intercept = FakeHandler::class.java.getMethod("onInterceptTouchEvent", MotionEvent::class.java)
        private val touch = FakeHandler::class.java.getMethod("handleTouch", MotionEvent::class.java)
        private val dispatchCustom = FakeHandler::class.java.getMethod("dispatchCustomTouchEvent", MotionEvent::class.java)
        private val outer = FakeHandler::class.java.getMethod("onTouchEvent", MotionEvent::class.java)
        private val filter = NotificationSwipeFilter(
            framework.bridge, this, { enabled }, { eligible }, { affordance }, framework.failures::add,
        )

        init {
            assertTrue(filter.install(intercept, touch, if (observeDispatch) dispatchCustom else null,
                if (filterOuter) outer else null))
            val expected = mutableSetOf(intercept, touch)
            if (observeDispatch) expected += dispatchCustom
            if (filterOuter) expected += outer
            assertEquals(expected, framework.callbacks.keys)
            handler.touchDispatch = { event ->
                touchEntries += event.actionMasked
                framework.call(touch, handler, event)
            }
            handler.affordanceDispatch = { event ->
                if (affordance == true) outerEvents += event.actionMasked
                affordance == true
            }
        }

        fun gesture(dx: Float = 0f, dy: Float = -2f, time: Long = 100L,
            startX: Float = 12f, beforeMove: () -> Unit = {}) {
            dispatch(ACTION_DOWN, time = time, startX = startX)
            beforeMove()
            dispatch(ACTION_MOVE, dx, dy, time, startX)
            dispatch(ACTION_UP, dx * 2f, dy * 2f, time, startX)
        }

        fun dispatch(action: Int, dx: Float = 0f, dy: Float = 0f, time: Long = 100L, startX: Float = 12f,
            reportedDownTime: Long = time, deviceId: Int = 0, source: Int = InputDevice.SOURCE_TOUCHSCREEN) {
            val event = MotionEvent.obtain(reportedDownTime, maxOf(time, reportedDownTime) + clock++, action,
                (startX + dx) * SLOP, (24f + dy) * SLOP, 1f, 1f, 0, 1f, 1f, deviceId, 0)
            event.source = source
            try {
                if (action == ACTION_DOWN) childOwns = notification
                if (observeDispatch) framework.call(dispatchCustom, handler, event)
                val intercepted = if (childPreventsIntercept) false else {
                    interceptEntries += action
                    framework.call(intercept, handler, event).also { afterIntercept?.invoke(action) }
                }
                if (childOwns && !intercepted) {
                    childEvents += action
                } else {
                    if (childOwns) childEvents += ACTION_CANCEL
                    childOwns = false
                    if (filterOuter) framework.call(outer, handler, event)
                    else if (affordance == true) outerEvents += action else {
                        touchEntries += action
                        framework.call(touch, handler, event)
                    }
                }
            } finally {
                event.recycle()
            }
        }

        fun assertHealthy() = assertTrue(framework.failures.toString(), framework.failures.isEmpty())
        override fun stable(handler: Any) = stable
        override fun keyguardActive(handler: Any) = keyguard
        override fun touchSlop(handler: Any) = SLOP
        override fun canCancelPanel(handler: Any) = keyguard
        override fun initializesPanelDown(handler: Any, event: MotionEvent) = !this.handler.rejectDown
    }

    class FakeHandler {
        val customEvents = mutableListOf<Int>()
        val nativeIntercept = mutableListOf<Int>()
        val nativeTouch = mutableListOf<Int>()
        val nativeTouchDownTimes = mutableListOf<Long>()
        val nativeOuter = mutableListOf<Int>()
        var touchDispatch: ((MotionEvent) -> Boolean)? = null
        var affordanceDispatch: ((MotionEvent) -> Boolean)? = null
        var earlyOuterConsume = false
        var earlyOuterMoves = 0
        var outerTerminalCleanups = 0
        var downMarker = 0
        var keepDownMarker = false
        var rejectDown = false
        var downResult = true
        var panelTracking = 0
        var sideEmpty = false
        var sideEmptyIntercepts = 0

        fun dispatchCustomTouchEvent(event: MotionEvent) {
            customEvents += event.actionMasked
        }

        fun onTouchEvent(event: MotionEvent): Boolean {
            nativeOuter += event.actionMasked
            if (event.actionMasked == ACTION_UP || event.actionMasked == ACTION_CANCEL) {
                outerTerminalCleanups++
            }
            if (affordanceDispatch?.invoke(event) == true) return true
            if (earlyOuterConsume && event.actionMasked == ACTION_MOVE) {
                earlyOuterMoves++
                return true
            }
            return touchDispatch?.invoke(event) ?: handleTouch(event)
        }

        fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            nativeIntercept += event.actionMasked
            if (sideEmpty) {
                sideEmptyIntercepts++
                return true
            }
            return event.actionMasked == ACTION_MOVE && moved(event)
        }

        fun handleTouch(event: MotionEvent): Boolean {
            nativeTouch += event.actionMasked
            nativeTouchDownTimes += event.downTime
            if (event.actionMasked == ACTION_DOWN) {
                if (rejectDown) return false
                if (!keepDownMarker) downMarker++
                return downResult
            }
            if (event.actionMasked == ACTION_MOVE && moved(event)) panelTracking++
            return true
        }

        private fun moved(event: MotionEvent) =
            abs(event.rawX - 12f * SLOP) > SLOP || abs(event.rawY - 24f * SLOP) > SLOP
    }

    private class Framework {
        val callbacks = mutableMapOf<Method, XposedInterface.Hooker>()
        val failures = mutableListOf<Throwable>()
        var originCalls = 0
        val bridge = CredentialHookBridge(proxy(XposedInterface::class.java) { _, method, args ->
            val target = args[0] as Method
            when (method.name) {
                "hook" -> proxy(XposedInterface.HookBuilder::class.java) { self, operation, values ->
                    when (operation.name) {
                        "setExceptionMode" -> self
                        "intercept" -> {
                            callbacks[target] = values[0] as XposedInterface.Hooker
                            proxy(XposedInterface.HookHandle::class.java) { _, _, _ -> null }
                        }
                        else -> error("Unexpected builder call: $operation")
                    }
                }
                "getInvoker" -> proxy(method.returnType) { self, operation, values ->
                    when (operation.name) {
                        "setType" -> {
                            assertEquals(XposedInterface.Invoker.Type.ORIGIN, values[0])
                            self
                        }
                        "invoke" -> {
                            originCalls++
                            target.invoke(values[0], *(values[1] as Array<*>))
                        }
                        else -> error("Unexpected invoker call: $operation")
                    }
                }
                else -> error("Unexpected framework call: $method")
            }
        }, failures::add)

        fun call(target: Method, handler: FakeHandler, event: MotionEvent): Boolean =
            callbacks.getValue(target).intercept(proxy(XposedInterface.Chain::class.java) { _, method, args ->
                when (method.name) {
                    "getThisObject" -> handler
                    "getArgs" -> listOf(event)
                    "proceed" -> target.invoke(handler, *(args[0] as Array<*>))
                    else -> error("Unexpected chain call: $method")
                }
            }) as? Boolean ?: false
    }

    private companion object {
        const val SLOP = 8f

        fun <T : Any> proxy(type: Class<T>, answer: (Any, Method, Array<out Any?>) -> Any?): T =
            requireNotNull(type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { self, method, args ->
                when (method.name) {
                    "equals" -> self === args?.get(0)
                    "hashCode" -> System.identityHashCode(self)
                    "toString" -> "Test ${type.simpleName}"
                    else -> answer(self, method, args ?: emptyArray())
                }
            }))
    }
}
