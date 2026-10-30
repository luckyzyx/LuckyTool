package com.luckyzyx.luckytool.hook.scopes.systemui

import android.os.Handler
import android.os.Looper
import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.WeakHashMap

internal class CredentialEntryController(
    findClass: (String, ClassLoader) -> Class<*> = { name, loader ->
        Class.forName(name, false, loader)
    },
) {
    private var securityContainer = WeakReference<Any>(null)
    private val showPassword = WeakHashMap<View, WeakReference<Any>>()
    private var getController: Method? = null
    private var getCurrentUser: Method? = null
    private var bridge: CredentialHookBridge? = null
    private var pending: Request? = null
    private var biometricFailureLogged = false
    private val gate = CredentialBiometricGate(
        onBroken = { what, error -> logBiometricFailure(what, error) },
        findClass = findClass,
    )

    private class Request(val userId: Int, val isBouncerShowing: () -> Boolean) {
        var mode: String? = null
        var controller = WeakReference<Any>(null)
        var view = WeakReference<View>(null)
        var failed = false
        var afterAttach: Runnable? = null
    }

    internal fun prepareNativeStructure(
        containerType: Class<*>,
        updateMonitorType: Class<*>,
    ): Boolean = runCatching {
        val method = containerType.methods.single {
            it.name == "getCurrentSecurityController" && it.parameterCount == 1
        }
        require(method.parameterTypes.single().isInterface)
        getCurrentUser = updateMonitorType.getMethod("getCurrentUser")
        getController = method
        true
    }.onFailure {
        getController = null
        getCurrentUser = null
        logFailure("resolve native entry", it)
    }.getOrDefault(false)

    fun bindContainer(controller: Any) {
        if (!isMainThread()) return
        if (securityContainer.get() !== controller) cancelPending()
        securityContainer = WeakReference(controller)
    }

    fun bindPasswordAction(view: View, action: Any?) {
        if (!isMainThread()) return
        if (action == null) showPassword.remove(view)
        else showPassword[view] = WeakReference(action)
        val request = pending ?: return
        if (action == null || request.view.get() !== view || !view.isAttachedToWindow ||
            request.afterAttach != null
        ) return
        // setter 可能在 onViewAttached 中执行，留到 attach 剩余逻辑完成后再切换。
        val resume = Runnable {
            request.afterAttach = null
            completeWhenReady(request)
        }
        request.afterAttach = resume
        view.postOnAnimation(resume)
    }

    fun onInputAttached(controller: Any) {
        if (!isMainThread()) return
        val request = pending ?: return
        if (request.controller.get() === controller) completeWhenReady(request)
    }

    fun onInputDetached(controller: Any) {
        if (!isMainThread()) return
        runCatching {
            val view = viewOf(controller) ?: return
            showPassword.remove(view)
            if (pending?.view?.get() === view) cancelPending()
        }.onFailure { cancelPending() }
    }

    fun cancelPending() {
        if (!isMainThread()) return
        pending?.let { finish(it, failed = true) }
    }

    fun isReady(): Boolean = getController != null && getCurrentUser != null &&
        securityContainer.get() != null

    fun isActionReady(view: View): Boolean = isMainThread() && isReady() &&
        view.isAttachedToWindow && showPassword[view]?.get() != null

    fun canUseBiometric(): Boolean = runCatching {
        if (!isMainThread() || !isReady()) return false
        val container = securityContainer.get() ?: return false
        val userId = getCurrentUser?.invoke(null) as? Int ?: return false
        gate.isBiometricEligible(container, userId)
    }.onFailure { logBiometricFailure("check fingerprint eligibility", it) }.getOrDefault(false)

    fun showCredentials(
        showBouncer: () -> Unit,
        isBouncerShowing: () -> Boolean,
    ): Boolean {
        if (!isMainThread()) return false
        cancelPending()
        val method = getController ?: return false
        val container = securityContainer.get() ?: return false
        return runCatching {
            val userId = getCurrentUser?.invoke(null) as? Int ?: return false
            if (!isKeyguardActive(container)) return false
            val request = Request(userId, isBouncerShowing)
            pending = request
            showBouncer()
            if (pending !== request) return false
            if (!request.isBouncerShowing()) {
                finish(request, failed = true)
                return false
            }
            request.mode = securityMode(container)
            if (request.mode !in setOf("Pattern", "PIN", "Password")) {
                finish(request, failed = true)
                return false
            }
            val callbackType = method.parameterTypes.single()
            val callback = Proxy.newProxyInstance(
                callbackType.classLoader, arrayOf(callbackType),
            ) { proxy, called, args ->
                when (called.name) {
                    "onViewInflated" -> {
                        if (isMainThread() && pending === request) {
                            runCatching {
                                val controller = requireNotNull(args?.firstOrNull())
                                request.controller = WeakReference(controller)
                                request.view = WeakReference(requireNotNull(viewOf(controller)))
                                completeWhenReady(request)
                            }.onFailure { fail(request, it) }
                        }
                        null
                    }
                    "toString" -> "LuckyTool credential view callback"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> null
                }
            }
            method.invoke(container, callback)
            !request.failed
        }.onFailure {
            cancelPending()
            getController = null
            logFailure("open credentials", it)
        }.getOrDefault(false)
    }

    private fun completeWhenReady(request: Request) {
        if (pending !== request) return
        runCatching {
            val container = securityContainer.get()
            if (container == null || getCurrentUser?.invoke(null) != request.userId ||
                container.asResolver().firstField { name = "mCurrentUser" }.get<Int>() != request.userId ||
                securityMode(container) != request.mode || !isKeyguardActive(container) ||
                !request.isBouncerShowing()
            ) {
                finish(request, failed = true)
                return
            }
            val controller = request.controller.get() ?: return
            val view = request.view.get() ?: return
            val resolver = controller.asResolver()
            val mode = resolver.firstMethod {
                name = "getSecurityMode"; emptyParameters(); superclass()
            }.invoke<Any>() as? Enum<*>
            if (mode?.name != request.mode || viewOf(controller) !== view) {
                finish(request, failed = true)
                return
            }
            val state = resolver.firstMethod {
                name = "getCurrentState"; emptyParameters(); superclass()
            }.invoke<Any>() as? Enum<*>
            when (state?.name) {
                "STATE_NORMAL" -> finish(request)
                "STATE_BIOMETRIC" -> {
                    if (!view.isAttachedToWindow) return
                    val action = showPassword[view]?.get() ?: return
                    val invoke = action.asResolver().firstMethod {
                        name = "invoke"; emptyParameters()
                    }
                    // 原生状态转换可能同步触发 attach、detach 或新的快捷请求。
                    finish(request)
                    invoke.invoke()
                }
                else -> finish(request, failed = true)
            }
        }.onFailure { fail(request, it) }
    }

    private fun finish(request: Request, failed: Boolean = false) {
        request.failed = request.failed || failed
        request.afterAttach?.let { request.view.get()?.removeCallbacks(it) }
        request.afterAttach = null
        if (pending === request) pending = null
    }

    private fun fail(request: Request, error: Throwable) {
        finish(request, failed = true)
        getController = null
        logFailure("show password", error)
    }

    private fun logFailure(action: String, error: Throwable) {
        runCatching { YLog.error("Credential shortcut: $action", error) }
    }

    // 同一进程只记录一次资格检查失败,避免触摸路径刷屏。
    private fun logBiometricFailure(what: String, error: Throwable?) {
        if (biometricFailureLogged) return
        biometricFailureLogged = true
        val action = "fingerprint eligibility: $what"
        if (error != null) logFailure(action, error)
        else runCatching { YLog.error("Credential shortcut: $action") }
    }

    private fun isKeyguardActive(container: Any): Boolean = gate.isKeyguardActive(container)

    private fun securityMode(container: Any): String? = (
        container.asResolver().firstMethod {
            name = "getCurrentSecurityMode"; emptyParameters()
        }.invoke<Any>() as? Enum<*>
    )?.name

    private fun viewOf(controller: Any): View? = controller.asResolver().firstField {
        name = "mView"; superclass()
    }.get<View>()

    private fun isMainThread(): Boolean = Looper.myLooper() == Looper.getMainLooper()

    class Hooks(private val entry: CredentialEntryController) : YukiBaseHooker() {
        override fun onHook() {
            val framework = LockScreenCredentialShortcut.framework ?: return
            val container = "com.android.keyguard.KeyguardSecurityContainerController"
                .toClassOrNull() ?: return
            val inputView = "com.oplus.keyguard.security.view.OplusKeyguardInputView"
                .toClassOrNull() ?: return
            val inputController = "com.oplus.keyguard.security.view.OplusKeyguardInputViewController"
                .toClassOrNull() ?: return
            val updateMonitor = "com.android.keyguard.KeyguardUpdateMonitor"
                .toClassOrNull() ?: return
            var disabled = false
            fun disable(error: Throwable) {
                entry.getController = null
                entry.getCurrentUser = null
                if (disabled) return
                disabled = true
                val cleanup = Runnable {
                    entry.cancelPending()
                    entry.bridge?.unhookAll()
                }
                if (entry.isMainThread()) cleanup.run()
                else Handler(Looper.getMainLooper()).post(cleanup)
                runCatching { YLog.error("Credential shortcut: hook native entry", error) }
            }
            runCatching {
                val constructor = container.resolve().firstConstructor()
                val pause = container.resolve().firstMethod { name = "onPause"; emptyParameters() }
                val setter = inputView.resolve().firstMethod {
                    name = "setOnShowPwdClickListener"; parameterCount = 1
                }
                val attached = inputController.resolve().firstMethod {
                    name = "onViewAttached"; emptyParameters()
                }
                val detached = inputController.resolve().firstMethod {
                    name = "onViewDetached"; emptyParameters()
                }
                val hooks = CredentialHookBridge(framework, ::disable)
                entry.bridge = hooks
                fun register(
                    member: java.lang.reflect.Member,
                    block: CredentialHookBridge.NativeHook.() -> Unit,
                ) {
                    checkNotNull(hooks.register(member, block))
                    LockScreenCredentialShortcut.hasNativeHooks = true
                }
                register(constructor.self) {
                    after { if (!hasThrowable) entry.bindContainer(checkNotNull(instance)) }
                }
                register(pause.self) {
                    before {
                        if (entry.securityContainer.get() === instance) entry.cancelPending()
                    }
                }
                register(setter.self) {
                    after {
                        if (!hasThrowable) entry.bindPasswordAction(instance as View, args[0])
                    }
                }
                register(attached.self) {
                    after { if (!hasThrowable) entry.onInputAttached(checkNotNull(instance)) }
                }
                register(detached.self) {
                    before { entry.onInputDetached(checkNotNull(instance)) }
                }
                check(!disabled && hooks.handleCount == 5)
                if (!entry.prepareNativeStructure(container, updateMonitor)) {
                    entry.cancelPending()
                    hooks.unhookAll()
                }
            }.onFailure(::disable)
        }
    }
}
