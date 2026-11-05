package com.luckyzyx.luckytool.hook.scopes.systemui

import android.os.Looper
import android.view.MotionEvent
import java.lang.reflect.Method
import java.util.WeakHashMap

internal class NotificationSwipeFilter(
    private val hooks: CredentialHookBridge,
    private val host: PanelHost,
    private val enabled: () -> Boolean,
    private val eligible: () -> Boolean,
    private val affordanceHit: (MotionEvent) -> Boolean?,
    private val onFailure: (Throwable) -> Unit,
) {
    internal interface PanelHost {
        fun stable(handler: Any): Boolean
        fun keyguardActive(handler: Any): Boolean
        fun touchSlop(handler: Any): Float
        fun initializesPanelDown(handler: Any, event: MotionEvent): Boolean
        fun canCancelPanel(handler: Any): Boolean
    }

    private class Stream(down: MotionEvent) {
        private val startedAt = down.eventTime
        private val deviceId = down.deviceId
        private val source = down.source
        private var latestEventTime = startedAt
        private var hasMovement = false
        private var terminalTime: Long? = null
        private var terminalAction: Int? = null
        var session: NotificationSwipeSession? = null
        var nativeDown: MotionEvent? = null
        var initializingPanel = false

        fun sameDown(event: MotionEvent): Boolean = !hasMovement && terminalTime == null &&
            event.eventTime == startedAt && event.deviceId == deviceId && event.source == source

        fun accepts(event: MotionEvent): Boolean {
            if (event.deviceId != deviceId || event.source != source || event.eventTime < latestEventTime) return false
            return terminalTime == null ||
                (event.eventTime == terminalTime && event.actionMasked == terminalAction)
        }

        fun observe(event: MotionEvent) {
            latestEventTime = event.eventTime
            hasMovement = true
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                terminalTime = event.eventTime
                terminalAction = event.actionMasked
            }
        }
    }

    private val streams = WeakHashMap<Any, Stream>()
    private lateinit var handleTouch: Method
    private var active = false

    fun install(intercept: Method, touch: Method, dispatch: Method? = null, outerTouch: Method? = null): Boolean {
        handleTouch = touch
        val first = hooks.register(intercept) {
            before { filter(this, entry = "intercept") }
        }
        val second = hooks.register(touch) {
            before { filter(this, entry = "touch") }
            after {
                if (!active || Looper.myLooper() != Looper.getMainLooper()) return@after
                val handler = instance ?: return@after
                val event = args.firstOrNull() as? MotionEvent ?: return@after
                val stream = streams[handler]?.takeIf {
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) it.sameDown(event) else it.accepts(event)
                } ?: return@after
                if (event.actionMasked == MotionEvent.ACTION_DOWN && !skipped && !hasThrowable &&
                    stream.session != null && stream.initializingPanel
                ) {
                    stream.nativeDown?.recycle()
                    stream.nativeDown = MotionEvent.obtain(event)
                } else if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    stream.nativeDown?.recycle()
                    stream.nativeDown = null
                }
            }
        }
        val observer = dispatch?.let { method ->
            hooks.register(method) {
                before { filter(this, entry = "dispatch", observeOnly = true) }
            }
        }
        val outer = outerTouch?.let { method ->
            hooks.register(method) {
                before {
                    val action = (args.firstOrNull() as? MotionEvent)?.actionMasked
                    // UP/CANCEL 继续原生 outer 收尾；handleTouch 仍按同一流阻止解锁。
                    filter(this, entry = "outer",
                        observeOnly = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
                }
            }
        }
        active = first != null && second != null &&
            (dispatch == null || observer != null) && (outerTouch == null || outer != null)
        if (!active) hooks.unhookAll()
        return active
    }

    private fun filter(call: CredentialHookBridge.Invocation, entry: String, observeOnly: Boolean = false) {
        if (!active || Looper.myLooper() != Looper.getMainLooper()) return
        val handler = call.instance ?: return
        val event = call.args.firstOrNull() as? MotionEvent ?: return
        runCatching {
            var stream = streams[handler]
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (stream?.sameDown(event) != true) {
                    stream?.let { cancelPanel(handler, it) }
                    stream = Stream(event)
                    streams[handler] = stream
                }
                if (stream.session == null && event.pointerCount == 1 && enabled() &&
                    host.stable(handler) && eligible()
                ) {
                    val affordance = affordanceHit(event)
                    if (affordance == false) {
                        stream.session = NotificationSwipeSession(
                            event.getPointerId(0), event.rawX, event.rawY, host.touchSlop(handler),
                        )
                    }
                }
                if (entry == "touch") stream.initializingPanel =
                    stream.session != null && host.initializesPanelDown(handler, event)
                return@runCatching
            }
            if (stream == null || !stream.accepts(event)) return@runCatching
            stream.observe(event)
            val session = stream.session ?: return@runCatching
            if (session.decided && !session.blocksPanel) return@runCatching
            if (!enabled() || !host.keyguardActive(handler)) {
                cancelPanel(handler, stream, event)
                stream.session = null
                return@runCatching
            }
            if (event.actionMasked == MotionEvent.ACTION_CANCEL && !session.blocksPanel) return@runCatching
            val alreadyDecided = session.decided
            if (!session.shouldBlock(event.rawX, event.rawY, event.getPointerId(0), event.pointerCount)) {
                return@runCatching
            }
            if (!alreadyDecided) cancelPanel(handler, stream, event)
            if (!observeOnly) call.result = false
        }.onFailure { error ->
            active = false
            streams.forEach { (owner, stream) -> runCatching { cancelPanel(owner, stream) } }
            streams.clear()
            onFailure(error)
        }
    }

    private fun cancelPanel(handler: Any, stream: Stream, event: MotionEvent? = null) {
        val down = stream.nativeDown ?: return
        if (!host.canCancelPanel(handler)) return
        stream.nativeDown = null
        try {
            val sample = event ?: down
            val properties = Array(sample.pointerCount) { index ->
                MotionEvent.PointerProperties().apply { sample.getPointerProperties(index, this) }
            }
            val coordinates = Array(sample.pointerCount) { index ->
                MotionEvent.PointerCoords().apply { sample.getPointerCoords(index, this) }
            }
            val cancel = MotionEvent.obtain(
                down.downTime, sample.eventTime, MotionEvent.ACTION_CANCEL,
                sample.pointerCount, properties, coordinates, sample.metaState, sample.buttonState,
                sample.xPrecision, sample.yPrecision, sample.deviceId, sample.edgeFlags, sample.source, sample.flags,
            )
            try {
                hooks.invokeOriginal(handleTouch, handler, cancel)
            } finally {
                cancel.recycle()
            }
        } finally {
            down.recycle()
        }
    }
}
