package com.luckyzyx.luckytool.hook.scopes.systemui

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method

internal class CredentialHookBridge(
    private val framework: XposedInterface,
    private val onFailure: (Throwable) -> Unit,
) {
    private val handles = mutableListOf<XposedInterface.HookHandle>()
    val handleCount: Int get() = handles.size

    fun register(member: Member, block: NativeHook.() -> Unit): XposedInterface.HookHandle? =
        try {
            val callbacks = NativeHook().apply(block)
            framework.hook(member as Executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .intercept { chain ->
                    val originalArgs = chain.args.toTypedArray()
                    val call = Invocation(chain.thisObject, originalArgs.copyOf())
                    try {
                        callbacks.before?.invoke(call)
                    } catch (error: Throwable) {
                        call.args = originalArgs
                        call.skipped = false
                        call.complete(null, null)
                        reportFailure(error)
                    }
                    if (!call.skipped) {
                        try {
                            call.complete(chain.proceed(call.args), null)
                        } catch (error: Throwable) {
                            call.complete(null, error)
                        }
                    }
                    val nativeResult = call.result
                    val nativeError = call.throwable
                    try {
                        callbacks.after?.invoke(call)
                    } catch (error: Throwable) {
                        call.complete(nativeResult, nativeError)
                        reportFailure(error)
                    }
                    call.throwable?.let { throw it }
                    call.result
                }.also { handles += it }
        } catch (error: Throwable) {
            reportFailure(error)
            null
        }

    fun invokeOriginal(method: Method, instance: Any?, vararg args: Any?): Any? = try {
        framework.getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN)
            .invoke(instance, *args)
    } catch (error: InvocationTargetException) {
        throw error.targetException
    }

    fun unhookAll() {
        handles.toList().forEach { handle ->
            runCatching { handle.unhook() }.onSuccess { handles.remove(handle) }
                .onFailure(::reportFailure)
        }
    }

    private fun reportFailure(error: Throwable) {
        // 恢复或日志回调自身失败不能覆盖宿主结果，也不能中断其余钩子的释放。
        runCatching { onFailure(error) }
    }

    class NativeHook {
        internal var before: (Invocation.() -> Unit)? = null
        internal var after: (Invocation.() -> Unit)? = null
        fun before(block: Invocation.() -> Unit) { before = block }
        fun after(block: Invocation.() -> Unit) { after = block }
    }

    class Invocation(val instance: Any?, var args: Array<Any?>) {
        internal var skipped = false
        private var value: Any? = null
        var throwable: Throwable? = null
            private set
        val hasThrowable: Boolean get() = throwable != null
        var result: Any?
            get() = value
            set(value) {
                this.value = value
                throwable = null
                skipped = true
            }

        internal fun complete(result: Any?, error: Throwable?) {
            value = result
            throwable = error
        }
    }
}
