package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.hook.scopes.systemui.CredentialShortcutPolicy.Slot
import io.github.libxposed.api.XposedInterface
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Proxy
import java.util.WeakHashMap
import kotlin.math.hypot
import kotlin.math.min

internal class NativeCredentialShortcut(
    private val entry: CredentialEntryController,
    private val enabled: () -> Boolean,
) : YukiBaseHooker() {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val bindings = WeakHashMap<ImageView, Binding>()
    private val gestures = WeakHashMap<Any, Gesture>()
    private val fading = WeakHashMap<Any, Handoff>()
    private var host: Host? = null
    private var bridge: CredentialHookBridge? = null
    private var animationBridge: CredentialHookBridge? = null
    private var motionBridge: CredentialHookBridge? = null
    private var animationReady = false
    private var nativeMotionReady = false
    private var failed = false
    private var loggedFailure = false
    private var rebindScheduled = false

    override fun onHook() {
        val api = runCatching { Host(checkNotNull(hostClassLoader)) }
            .getOrElse { logOnce(it); return }
        val framework = LockScreenCredentialShortcut.framework ?: return
        bridge = CredentialHookBridge(framework, ::disable)
        host = api
        register(api.binder) {
            before {
                if (!hooksReady()) return@before
                runCatching { prepareButton(api, args) }.onFailure { disable(it) }
            }
            after {
                if (!isMainThread()) return@after
                val view = args.getOrNull(2) as? ImageView ?: return@after
                val binding = bindings[view] ?: return@after
                if (!binding.applied) return@after
                runCatching {
                    if (hasThrowable) {
                        restore(api, binding)
                        checkNotNull(bridge).invokeOriginal(api.binder, null, *binding.arguments(view))
                        result = null
                        error("Native credential button binding failed")
                    }
                    check(view.drawable != null)
                    view.setOnClickListener(binding.click)
                }.onFailure { disable(it) }
            }
        }
        register(api.touch) {
            before {
                if (!isMainThread()) return@before
                val gesture = gestures[instance] ?: return@before
                val event = args[0] as? MotionEvent ?: return@before
                gesture.dispatching = true
                gesture.action = event.actionMasked
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    gesture.singlePointer = event.pointerCount == 1
                    gesture.pointerId = event.getPointerId(0)
                    gesture.pending = false
                } else if (event.pointerCount != 1 ||
                    event.getPointerId(0) != gesture.pointerId ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL ||
                    event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
                ) {
                    gesture.singlePointer = false
                }
            }
            after {
                if (!isMainThread()) return@after
                gestures[instance]?.dispatching = false
            }
        }
        register(api.endMotion) {
            before {
                if (!isMainThread()) return@before
                val gesture = gestures[instance] ?: return@before
                val binding = gesture.binding.get() ?: return@before
                if (!binding.applied) return@before
                runCatching {
                    if (api.target.get(instance) !== binding.view.get()) return@runCatching
                    val validUp = gesture.dispatching && gesture.singlePointer &&
                        gesture.action == MotionEvent.ACTION_UP
                    if (!validUp && gesture.dispatching) args[2] = true
                    gesture.completing = validUp && args[2] == false && usable(api, binding)
                }.onFailure { disable(it) }
            }
            after {
                if (!isMainThread()) return@after
                val gesture = gestures[instance] ?: return@after
                gesture.completing = false
                if (!gesture.pending) return@after
                gesture.pending = false
                val binding = gesture.binding.get() ?: return@after
                runCatching {
                    if (hasThrowable || gesture.completionSession != api.session.getLong(instance)) {
                        return@runCatching
                    }
                    val helper = checkNotNull(instance)
                    val handoff = Handoff(gesture.completionSession)
                    val policy = CredentialReleasePolicy(object : CredentialReleasePolicy.Host {
                        override fun prepareDirectional(): Boolean {
                            val view = binding.view.get() ?: return false
                            if (NativeCredentialScale.read(view) == null) return false
                            handoff.scaleView = WeakReference(view)
                            return NativeCredentialScale.animate(view, NATIVE_PRESS_SCALE)
                        }

                        override fun startNativeFade() {
                            fading[helper] = handoff
                            api.clearSurface.invoke(helper, true)
                        }

                        override fun openCredentials() {
                            if (!openCredentials(api, binding)) {
                                cleanup(api, helper)
                                restore(api, binding)
                                rebind(api, binding)
                            }
                        }

                        override fun cleanup() {
                            cleanup(api, helper)
                        }
                    })
                    handoff.policy = policy
                    policy.start(candidate = true, nativeFadeReady = animationReady)
                }.onFailure { disable(it) }
            }
        }
        register(api.sideStarted) {
            before {
                if (!hooksReady()) return@before
                runCatching {
                    val controller = api.callbackController.get(instance)
                    val helper = api.helper.get(controller) ?: return@runCatching
                    val gesture = gestures[helper] ?: return@runCatching
                    val binding = gesture.binding.get() ?: return@runCatching
                    if (args[0] != (binding.slot == Slot.END) ||
                        !gesture.completing || !usable(api, binding) ||
                        api.target.get(helper) !== binding.view.get()
                    ) return@runCatching
                    gesture.completionSession = api.session.getLong(helper)
                    gesture.pending = true
                }.onFailure { disable(it) }
            }
            after {
                if (!isMainThread()) return@after
                runCatching {
                    val controller = api.callbackController.get(instance)
                    val helper = api.helper.get(controller) ?: return@runCatching
                    val gesture = gestures[helper] ?: return@runCatching
                    val binding = gesture.binding.get() ?: return@runCatching
                    if (gesture.pending && (hasThrowable ||
                            !api.launchTransitionRunning.getBoolean(controller) ||
                            api.valueGet.invoke(api.canLaunch[binding.slot.ordinal].get(controller)) != true)
                    ) gesture.pending = false
                }.onFailure { disable(it) }
            }
        }
        register(api.setIds) {
            before {
                if (!isMainThread()) return@before
                val gesture = gestures[instance] ?: return@before
                runCatching {
                    val pair = args[0] ?: return@runCatching
                    val ids = arrayOf(api.pairFirst.invoke(pair), api.pairSecond.invoke(pair))
                    Slot.entries.forEach { slot ->
                        if (ids[slot.ordinal] != CONFIG_KEY) {
                            gesture.originalIds[slot.ordinal] = ids[slot.ordinal] as? String
                        }
                    }
                    val binding = gesture.binding.get()
                    if (binding?.applied == true) {
                        if (binding.slot != credentialSlot(gesture) || !enabled() || failed) {
                            restore(api, binding)
                        } else {
                            ids[binding.slot.ordinal] = CONFIG_KEY
                            args[0] = api.pairConstructor.newInstance(*ids)
                        }
                    }
                }.onFailure { disable(it) }
            }
            after {
                if (isMainThread() && gestures.containsKey(instance)) scheduleRebind()
            }
        }
        Slot.entries.forEach { slot ->
            register(api.setSelections[slot.ordinal]) {
                before {
                    if (!isMainThread()) return@before
                    val binding = bindings.values.firstOrNull {
                        it.controller.get() === instance && it.slot == slot
                    } ?: return@before
                    if (args[0] == CONFIG_KEY) return@before
                    binding.originalSelection = args[0]
                    if (binding.applied && enabled() && !failed) args[0] = CONFIG_KEY
                }
            }
        }
        register(api.releaseSurface) {
            after {
                if (!isMainThread()) return@after
                val handoff = fading[instance] ?: return@after
                runCatching {
                    if (api.session.getLong(instance) != handoff.session) {
                        fading.remove(instance)?.dispose()
                    } else if (!hasThrowable && api.surfaceHost.get(instance) == null) {
                        handoff.policy?.release()
                    }
                }.onFailure { disable(it) }
            }
        }
        register(api.clearSurface) {
            before {
                if (!isMainThread()) return@before
                val handoff = fading[instance] ?: return@before
                if (api.session.getLong(instance) != handoff.session) {
                    fading.remove(instance)?.dispose()
                }
            }
            after {
                if (!isMainThread() || args[0] != false || hasThrowable) return@after
                val helper = instance ?: return@after
                if (fading[helper]?.session == api.session.getLong(helper)) {
                    runCatching { cleanup(api, helper) }.onFailure { disable(it) }
                }
            }
        }
        register(api.entryConstructor) {
            after { if (isMainThread() && !hasThrowable) scheduleRebind() }
        }
        installAnimationHandoff(api, framework)
    }

    private fun installAnimationHandoff(api: Host, framework: XposedInterface) {
        val animation = api.animation ?: return
        val hooks = CredentialHookBridge(framework) {
            animationReady = false
            mainHandler.post {
                fading.keys.toList().forEach { helper ->
                    runCatching { cleanup(api, helper) }.onFailure { disable(it) }
                }
                animationBridge?.unhookAll()
            }
        }
        animationBridge = hooks
        animationReady = hooks.register(animation.forceReset) {
            before {
                if (!isMainThread()) return@before
                val handoff = fading[instance] ?: return@before
                if (animationReady && api.session.getLong(instance) == handoff.session) {
                    // 原生惯性和镜像淡出结束后再归零，避免把仍可见的按钮拉回原位。
                    result = null
                }
            }
        } != null
        installNativeMotion(api, framework, animation)
    }

    private fun installNativeMotion(api: Host, framework: XposedInterface, animation: AnimationAccess) {
        val motion = animation.motion ?: return
        val hooks = CredentialHookBridge(framework) {
            nativeMotionReady = false
            mainHandler.post { motionBridge?.unhookAll() }
        }
        motionBridge = hooks
        val followHook = hooks.register(animation.followDrag) {
            before {
                if (!isMainThread() || !animationReady || !nativeMotionReady) return@before
                val helper = instance ?: return@before
                val gesture = gestures[helper] ?: return@before
                val binding = gesture.binding.get() ?: return@before
                if (!gesture.pending || !gesture.completing ||
                    gesture.completionSession != api.session.getLong(helper) ||
                    args[2] !== binding.view.get()
                ) return@before
                val view = binding.view.get() ?: return@before
                val x = animation.dragX.getFloat(helper)
                val y = animation.dragY.getFloat(helper)
                val dx = (args[0] as Float) - x
                val dy = (args[1] as Float) - y
                val distance = hypot(dx, dy)
                val contentSize = min(view.width - view.paddingLeft - view.paddingRight,
                    view.height - view.paddingTop - view.paddingBottom)
                // 只限制原生成功目标的追加距离，位移和速度仍由原生弹簧推进。
                if (contentSize > 0 && distance > contentSize / 2f) {
                    val factor = contentSize / (2f * distance)
                    args[0] = x + dx * factor
                    args[1] = y + dy * factor
                }
            }
        }
        val springHook = hooks.register(motion.getSpringMotion) {
            after {
                if (!isMainThread() || !nativeMotionReady || hasThrowable) return@after
                val helper = instance ?: return@after
                val gesture = gestures[helper] ?: return@after
                val binding = gesture.binding.get() ?: return@after
                if (!gesture.pending || !gesture.completing ||
                    gesture.completionSession != api.session.getLong(helper) || args[0] !== binding.view.get()
                ) return@after
                val spring = result ?: return@after
                val force = motion.getForce.invoke(spring) ?: return@after
                motion.setBounce.invoke(force, 0f)
                val position = motion.value.getFloat(spring)
                val velocity = motion.velocity.getFloat(spring)
                val stiffness = motion.getStiffness.invoke(force) as Float
                val target = args[2] as Float
                motion.setTarget.invoke(force, target)
                val adjusted = NativeReleaseParameters.velocity(position, target, velocity, stiffness)
                if (adjusted != velocity) motion.setVelocity.invoke(spring, adjusted)
            }
        }
        nativeMotionReady = followHook != null && springHook != null
        if (!nativeMotionReady) hooks.unhookAll()
    }

    private fun unhookAnimationHandoff() {
        animationReady = false
        nativeMotionReady = false
        animationBridge?.unhookAll()
        motionBridge?.unhookAll()
    }

    fun isReady(): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper() || !hooksReady() ||
            !enabled() || !entry.isReady()
        ) return false
        val api = host ?: return false
        return runCatching { bindings.values.any { usable(api, it, requireShown = false) } }
            .onFailure { disable(it) }.getOrDefault(false)
    }

    fun hitsAffordance(event: MotionEvent): Boolean? {
        val api = host ?: return null
        if (!hooksReady()) return null
        return runCatching {
            val helpers = bindings.values.mapNotNull { binding ->
                binding.controller.get()?.let { api.helper.get(it) }
            }.distinct()
            if (helpers.isEmpty()) return null
            helpers.any { helper ->
                val root = api.bottomArea.get(helper) as? View ?: return null
                val location = IntArray(2)
                root.getLocationOnScreen(location)
                api.iconAt.invoke(helper, event.rawX - location[0], event.rawY - location[1]) != null
            }
        }.onFailure { disable(it) }.getOrNull()
    }

    fun onPreferenceChanged() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { onPreferenceChanged() }
            return
        }
        val api = host ?: return
        if (!enabled() || failed) entry.cancelPending()
        bindings.values.toList().forEach { binding ->
            runCatching {
                if (!enabled() || failed) {
                    val helper = binding.helper.get()
                    if (helper != null && (api.target.get(helper) === binding.view.get() ||
                            fading.containsKey(helper))
                    ) {
                        cleanup(api, helper)
                    }
                    restore(api, binding)
                }
                rebind(api, binding)
            }.onFailure { disable(it) }
        }
    }

    private fun prepareButton(api: Host, arguments: Array<Any?>) {
        val model = arguments[3] ?: return
        val slot = Slot.entries.firstOrNull {
            it.id == api.modelFields.getValue("slotId").get(model)
        } ?: return
        val view = arguments[2] as? ImageView ?: return
        val controller = arguments[5] ?: return
        var binding = bindings[view]
        if (binding == null || binding.controller.get() !== controller || binding.slot != slot) {
            binding?.let {
                restore(api, it)
                view.removeOnAttachStateChangeListener(it.detach)
            }
            val nativeClick = readClickListener(view)
            binding = Binding(view, controller, slot, arguments, nativeClick)
            val reference = WeakReference(binding)
            binding.click = View.OnClickListener {
                reference.get()?.let { current ->
                    runCatching {
                        if (usable(api, current)) {
                            current.helper.get()?.let { cleanup(api, it) }
                            openCredentials(api, current, deduplicateGesture = false)
                        }
                    }.onFailure { disable(it) }
                }
            }
            bindings[view] = binding
            binding.detach = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = Unit

                override fun onViewDetachedFromWindow(view: View) {
                    reference.get()?.let { current ->
                        runCatching { restore(api, current) }.onFailure { disable(it) }
                        current.helper.get()?.let { helper ->
                            if (gestures[helper]?.binding?.get() === current) gestures.remove(helper)
                        }
                    }
                    bindings.remove(view)
                    view.removeOnAttachStateChangeListener(this)
                }
            }
            view.addOnAttachStateChangeListener(binding.detach)
        } else {
            val currentClick = readClickListener(view)
            if (currentClick !== binding.click) binding.nativeClick = currentClick
            binding.saveArguments(arguments)
        }
        val helper = api.helper.get(controller) ?: return
        if (binding.helper.get() !== helper) restore(api, binding)
        binding.helper = WeakReference(helper)
        val gesture = gestures.getOrPut(helper) {
            Gesture(Array(2) { api.ids[it].get(helper) as? String })
        }
        if (!enabled() || !entry.isReady() || slot != credentialSlot(gesture) ||
            !mainLockscreen(api, controller)
        ) {
            restore(api, binding)
            return
        }
        check(api.findButtons[slot.ordinal].invoke(controller) === view)
        val manager = viewManager(api, controller) ?: return
        binding.manager = WeakReference(manager)
        val context = arguments[1] as Context
        val description = module.resources.getString(R.string.lock_screen_credential_shortcut)
        val drawable = NativeCredentialIcon.create(context, view)
        val icon = api.iconConstructor.newInstance(
            drawable,
            api.descriptionConstructor.newInstance(description),
        )
        val reference = WeakReference(binding)
        val onClicked = api.function(api.function1) { values ->
            val current = reference.get()
            val parameters = values?.firstOrNull()
            if (current != null && parameters != null &&
                api.clickedAction.get(parameters) === api.startApp && usable(api, current)
            ) {
                val gesture = gestures[current.helper.get()]
                if (gesture != null && !gesture.pending && !gesture.dispatching &&
                    gesture.singlePointer && gesture.action == MotionEvent.ACTION_UP
                ) {
                    runCatching { openCredentials(api, current) }.onFailure { disable(it) }
                }
            }
        }
        val clone = api.modelConstructor.newInstance(
            CONFIG_KEY, true, api.modelFields.getValue("animateReveal").get(model), icon,
            onClicked, true, false, api.modelFields.getValue("isSelected").get(model),
            false, api.modelFields.getValue("isDimmed").get(model), description, slot.id,
            api.modelFields.getValue("extendAction").get(model),
            api.function(api.function2) {},
        )
        gesture.binding = WeakReference(binding)
        val id = api.ids[slot.ordinal].get(helper)
        if (id != CONFIG_KEY) gesture.originalIds[slot.ordinal] = id as? String
        val selection = api.selections[slot.ordinal].get(controller)
        if (selection != CONFIG_KEY) binding.originalSelection = selection
        binding.applied = true
        api.ids[slot.ordinal].set(helper, CONFIG_KEY)
        api.selections[slot.ordinal].set(controller, CONFIG_KEY)
        // 跨 ClassLoader 的普通 ImageView 分支不会读取 Icon.Loaded 的 drawable。
        view.setImageDrawable(drawable)
        arguments[3] = clone
    }

    private fun usable(api: Host, binding: Binding, requireShown: Boolean = true): Boolean {
        if (!hooksReady() || !enabled() || !entry.isReady() || !binding.applied) return false
        val view = binding.view.get() ?: return false
        val controller = binding.controller.get() ?: return false
        return view.isAttachedToWindow && (!requireShown || view.isShown && view.alpha > 0f) &&
            view.width > 0 && view.height > 0 && view.drawable != null &&
            api.helper.get(controller) === binding.helper.get() &&
            gestures[binding.helper.get()]?.let { credentialSlot(it) == binding.slot } == true &&
            binding.manager.get() != null && mainLockscreen(api, controller)
    }

    private fun mainLockscreen(api: Host, controller: Any): Boolean {
        val mediator = api.lazyGet.invoke(api.mediator.get(controller)) ?: return false
        if (!api.showing.getBoolean(mediator) || api.dozing.getBoolean(mediator) ||
            api.aod.getBoolean(mediator) || api.occluded.getBoolean(mediator)
        ) return false
        val viewModel = api.bottomViewModel.get(controller) ?: return false
        val preview = api.valueGet.invoke(api.previewMode.get(viewModel)) ?: return false
        return !api.inPreview.getBoolean(preview)
    }

    private fun viewManager(api: Host, controller: Any): Any? {
        val mediator = api.lazyGet.invoke(api.mediator.get(controller)) ?: return null
        return api.lazyGet.invoke(api.manager.get(mediator))?.takeIf {
            api.showBouncer.declaringClass.isInstance(it)
        }
    }

    private fun credentialSlot(gesture: Gesture): Slot? = CredentialShortcutPolicy.credentialSlot(
        gesture.originalIds[0], gesture.originalIds[1],
    )

    private fun cleanup(api: Host, helper: Any) {
        fading.remove(helper)?.let { handoff ->
            handoff.dispose()
            if (api.session.getLong(helper) == handoff.session) {
                handoff.scaleView?.get()?.let { NativeCredentialScale.write(it, 1f) }
            }
        }
        api.reset.invoke(helper, false, false)
        api.clearSurface.invoke(helper, false)
        api.clearSurface.invoke(helper, true)
        api.clearSurface.invoke(helper, false)
    }

    private fun openCredentials(
        api: Host,
        binding: Binding,
        deduplicateGesture: Boolean = true,
    ): Boolean {
        val manager = binding.manager.get() ?: return false
        val helper = binding.helper.get() ?: return false
        val gesture = gestures[helper] ?: return false
        val session = api.session.getLong(helper)
        if (deduplicateGesture) {
            if (gesture.openedSession == session) return true
            gesture.openedSession = session
        }
        val managerRef = WeakReference(manager)
        val accepted = entry.showCredentials(
            showBouncer = {
                managerRef.get()?.let {
                    api.showBouncer.invoke(it, "LuckyTool credential shortcut", true)
                }
            },
            isBouncerShowing = {
                managerRef.get()?.let { api.bouncerShowing.invoke(it) as? Boolean } == true
            },
        )
        if (!accepted) {
            gesture.openedSession = -1L
            if (!entry.isReady()) disable(IllegalStateException("Credential entry is unavailable"))
        }
        return accepted
    }

    private fun restore(api: Host, binding: Binding) {
        if (!binding.applied) return
        binding.applied = false
        binding.helper.get()?.let { helper ->
            gestures[helper]?.let { gesture ->
                gesture.pending = false
                gesture.completing = false
                val id = api.ids[binding.slot.ordinal]
                if (id.get(helper) == CONFIG_KEY) {
                    id.set(helper, gesture.originalIds[binding.slot.ordinal])
                }
            }
        }
        binding.controller.get()?.let { controller ->
            val selection = api.selections[binding.slot.ordinal]
            if (selection.get(controller) == CONFIG_KEY) {
                selection.set(controller, binding.originalSelection)
            }
        }
        binding.view.get()?.let { view ->
            if (readClickListener(view) === binding.click) view.setOnClickListener(binding.nativeClick)
        }
    }

    private fun rebind(api: Host, binding: Binding) {
        val view = binding.view.get() ?: return
        if (binding.controller.get() == null) return
        val arguments = binding.arguments(view)
        arguments[6] = api.modelFields.getValue("extendAction").get(arguments[3])
        api.binder.invoke(null, *arguments)
    }

    private fun register(
        method: Member,
        block: CredentialHookBridge.NativeHook.() -> Unit,
    ) {
        if (failed) return
        val hooks = bridge ?: return
        if (hooks.register(method, block) == null) return
        LockScreenCredentialShortcut.hasNativeHooks = true
        if (hooks.handleCount == HOOK_COUNT) scheduleRebind()
    }

    private fun hooksReady() = isMainThread() && bridge?.handleCount == HOOK_COUNT && !failed

    private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

    private fun scheduleRebind() {
        if (rebindScheduled) return
        rebindScheduled = true
        mainHandler.post {
            rebindScheduled = false
            onPreferenceChanged()
        }
    }

    private fun disable(error: Throwable) {
        logOnce(error)
        if (failed) return
        failed = true
        mainHandler.post {
            try {
                onPreferenceChanged()
            } finally {
                bridge?.unhookAll()
                unhookAnimationHandoff()
            }
        }
    }

    private fun logOnce(error: Throwable) {
        if (loggedFailure) return
        loggedFailure = true
        YLog.error("Credential shortcut: native affordance unavailable", error)
    }

    private fun readClickListener(view: View): View.OnClickListener? {
        val info = View::class.java.getDeclaredField("mListenerInfo")
            .apply { isAccessible = true }.get(view) ?: return null
        return info.javaClass.getDeclaredField("mOnClickListener")
            .apply { isAccessible = true }.get(info) as? View.OnClickListener
    }

    private class Binding(
        view: ImageView,
        controller: Any,
        val slot: Slot,
        arguments: Array<Any?>,
        var nativeClick: View.OnClickListener?,
    ) {
        val view = WeakReference(view)
        val controller = WeakReference(controller)
        var helper = WeakReference<Any>(null)
        var manager = WeakReference<Any>(null)
        var originalSelection: Any? = null
        var applied = false
        var click: View.OnClickListener? = null
        var detach: View.OnAttachStateChangeListener? = null
        private var savedArguments = emptyArray<Any?>()

        init { saveArguments(arguments) }

        fun saveArguments(arguments: Array<Any?>) {
            savedArguments = arguments.copyOf().apply { this[2] = null; this[5] = null }
        }

        fun arguments(view: ImageView): Array<Any?> = savedArguments.copyOf().apply {
            this[2] = view
            this[5] = controller.get()
        }
    }

    private class Gesture(val originalIds: Array<String?>) {
        var binding = WeakReference<Binding>(null)
        var dispatching = false
        var singlePointer = false
        var pointerId = -1
        var action = -1
        var completing = false
        var pending = false
        var completionSession = -1L
        var openedSession = -1L
    }

    private class Handoff(val session: Long) {
        var policy: CredentialReleasePolicy? = null
        var scaleView: WeakReference<View>? = null

        fun dispose() {
            policy = null
        }
    }

    private class Host(private val loader: ClassLoader) {
        private fun type(name: String) = Class.forName(name, false, loader)
        private fun field(type: Class<*>, name: String): Field =
            type.getDeclaredField(name).apply { isAccessible = true }
        private val controller = type("com.oplus.systemui.keyguard.OplusKeyguardBottomAreaController")
        private val helperClass = type("com.oplus.systemui.keyguard.KeyguardAffordanceHelper")
        private val callback = type("${controller.name}\$InnerKeyguardAffordanceCallback")
        private val binderClass = type("com.oplus.systemui.keyguard.ui.binder.OplusKeyguardBottomAreaViewBinder")
        private val model = type("com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordanceViewModel")
        private val icon = type("com.android.systemui.common.shared.model.Icon")
        private val description = type("com.android.systemui.common.shared.model.ContentDescription")
        private val action = type("com.android.systemui.keyguard.shared.quickaffordance.ExtendAction")
        private val mediatorClass = type("com.android.systemui.keyguard.KeyguardViewMediator")
        private val bottomModel = type("com.oplus.systemui.keyguard.ui.viewmodel.OplusKeyguardBottomAreaViewModel")
        val function1 = type("kotlin.jvm.functions.Function1")
        val function2 = type("kotlin.jvm.functions.Function2")
        private val unit = type("kotlin.Unit").getField("INSTANCE").get(null)
        val modelFields = model.declaredFields.associateBy { it.name }.also { fields ->
            listOf("slotId", "animateReveal", "isSelected", "isDimmed", "extendAction")
                .forEach { fields.getValue(it).isAccessible = true }
        }
        val modelConstructor = model.getDeclaredConstructor(
            String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            icon, function1, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType, String::class.java, String::class.java, action, function2,
        )
        val iconConstructor = type("${icon.name}\$Loaded").getConstructor(Drawable::class.java, description)
        val descriptionConstructor = type("${description.name}\$Loaded").getConstructor(String::class.java)
        val clickedAction = field(type("${model.name}\$OnClickedParameters"), "action")
        val startApp = type("${action.name}\$StartApp").getField("INSTANCE").get(null)
        val binder = binderClass.getDeclaredMethod(
            "access\$updateButton", binderClass, Context::class.java, ImageView::class.java, model,
            type("com.oplus.keyguard.OplusKeyguardUnlockAnimationControllerExImpl"), controller, action,
        ).apply { isAccessible = true }
        val touch = helperClass.getDeclaredMethod("onTouchEvent", MotionEvent::class.java)
        val endMotion = helperClass.getDeclaredMethod(
            "endMotion", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
        )
        val sideStarted = callback.getDeclaredMethod("onAnimationToSideStarted", Boolean::class.javaPrimitiveType)
        private val pair = type("kotlin.Pair")
        val pairConstructor = pair.getConstructor(Any::class.java, Any::class.java)
        val pairFirst = pair.getMethod("getFirst")
        val pairSecond = pair.getMethod("getSecond")
        val setIds = helperClass.getDeclaredMethod("setStartEndAffordanceId", pair)
        val setSelections = arrayOf("setCurrentStartSelection", "setCurrentEndSelection").map {
            controller.getDeclaredMethod(it, String::class.java)
        }
        val releaseSurface = helperClass.getDeclaredMethod("releaseQuickAffordanceSurface", Boolean::class.javaPrimitiveType)
        val reset = helperClass.getDeclaredMethod("reset", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
        val clearSurface = helperClass.getDeclaredMethod("clearLaunchAnimViewAndCleanSurface", Boolean::class.javaPrimitiveType)
        val findButtons = arrayOf("findStartButton", "findEndButton").map { controller.getDeclaredMethod(it) }
        val callbackController = field(callback, "this\$0")
        val helper = field(controller, "keyguardAffordanceHelper")
        val mediator = field(controller, "keyguardViewMediator")
        val bottomViewModel = field(controller, "viewModel")
        val selections = arrayOf("mCurrentStartSelection", "mCurrentEndSelection").map { field(controller, it) }
        val canLaunch = arrayOf("canLaunchStart", "canLaunchEnd").map { field(controller, it) }
        val launchTransitionRunning = field(controller, "mIsLaunchTransitionRunning")
        val ids = arrayOf("startAffordanceId", "endAffordanceId").map { field(helperClass, it) }
        val session = field(helperClass, "activeQuickAffordanceSessionId")
        val surfaceHost = field(helperClass, "quickAffordanceSurfaceHost")
        val target = field(helperClass, "mTargetedView")
        val bottomArea = field(helperClass, "keyguardBottomAreaView")
        val iconAt = helperClass.getDeclaredMethod(
            "getIconAtPosition", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
        ).apply { isAccessible = true }
        val animation = runCatching { AnimationAccess(helperClass, loader) }.getOrNull()
        val manager = field(mediatorClass, "mKeyguardViewControllerLazy")
        val showing = field(mediatorClass, "mShowing")
        val dozing = field(mediatorClass, "mDozing")
        val aod = field(mediatorClass, "mAodShowing")
        val occluded = field(mediatorClass, "mOccluded")
        val previewMode = field(bottomModel, "previewMode")
        val inPreview = field(type("${bottomModel.name}\$PreviewMode"), "isInPreviewMode")
        val lazyGet = type("dagger.Lazy").getMethod("get")
        val valueGet = type("kotlinx.coroutines.flow.StateFlow").getMethod("getValue")
        val showBouncer = type("com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager")
            .getMethod("showPrimaryBouncer", String::class.java, Boolean::class.javaPrimitiveType)
        val bouncerShowing = showBouncer.declaringClass.getMethod("primaryBouncerIsOrWillBeShowing")
        val entryConstructor = type("com.android.keyguard.KeyguardSecurityContainerController")
            .declaredConstructors.single { !it.isSynthetic }

        fun function(type: Class<*>, action: (Array<out Any?>?) -> Unit): Any =
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, arguments ->
                when (method.name) {
                    "invoke" -> { action(arguments); unit }
                    "equals" -> proxy === arguments?.firstOrNull()
                    "hashCode" -> System.identityHashCode(proxy)
                    "toString" -> "LuckyTool credential shortcut"
                    else -> null
                }
            }
    }

    private class AnimationAccess(helperClass: Class<*>, loader: ClassLoader) {
        val forceReset = helperClass.declaredMethods.single {
            it.name.startsWith("forceResetAffordanceButtonTranslation") &&
                it.parameterCount == 0 && it.returnType == Void.TYPE
        }.apply { isAccessible = true }
        val followDrag = helperClass.getDeclaredMethod(
            "updateDragTranslationWithFollowSpring", Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, View::class.java,
        ).apply { isAccessible = true }
        val dragX = helperClass.getDeclaredField("rtDragTranslationX").apply { isAccessible = true }
        val dragY = helperClass.getDeclaredField("rtDragTranslationY").apply { isAccessible = true }
        val motion = runCatching { MotionAccess(helperClass, loader) }.getOrNull()
    }

    private class MotionAccess(helperClass: Class<*>, loader: ClassLoader) {
        private val motion = Class.forName("com.coui.appcompat.animation.dynamicanimation.SpringMotion", false, loader)
        private val dynamic = Class.forName("com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation", false, loader)
        private val force = Class.forName("com.coui.appcompat.animation.dynamicanimation.COUISpringForce", false, loader)
        val getSpringMotion = helperClass.declaredMethods.single {
            it.name == "getOrCreateDragFollowSpring" && it.parameterCount == 3
        }.apply { isAccessible = true }
        val getForce = motion.getMethod("getSpring")
        val setBounce = force.getMethod("setBounce", Float::class.javaPrimitiveType)
        val setTarget = force.getMethod("setFinalPosition", Float::class.javaPrimitiveType)
        val getStiffness = force.getMethod("getStiffness")
        val setVelocity = dynamic.getMethod("setStartVelocity", Float::class.javaPrimitiveType)
        val value = dynamic.getDeclaredField("mValue").apply {
            check(type == Float::class.javaPrimitiveType)
            isAccessible = true
        }
        val velocity = dynamic.getDeclaredField("mVelocity").apply {
            check(type == Float::class.javaPrimitiveType)
            isAccessible = true
        }
    }

    private companion object {
        const val HOOK_COUNT = 10
        const val CONFIG_KEY = "luckytool_credential_shortcut"
        const val NATIVE_PRESS_SCALE = 1.08f
    }
}
