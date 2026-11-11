package com.luckyzyx.luckytool.hook.scopes.systemui

import io.github.libxposed.api.XposedInterface
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CredentialHookBridgeTest {
    @Test
    fun `only successful registration retains a handle and registration failures are reported`() {
        for (failAtHook in listOf(true, false)) {
            val framework = Framework()
            assertEquals(0, framework.bridge.handleCount)
            val handle = framework.bridge.register(framework.method) {}
            assertSame(framework.handles.single(), handle)
            assertEquals(1, framework.bridge.handleCount)
            assertEquals(XposedInterface.ExceptionMode.PASSTHROUGH, framework.exceptionMode)
            assertTrue(framework.failures.isEmpty())

            val failure = IllegalStateException("registration failed")
            if (failAtHook) framework.hookFailure = failure else framework.interceptFailure = failure

            assertNull(framework.bridge.register(framework.method) {})

            assertEquals(1, framework.bridge.handleCount)
            assertEquals(listOf(failure), framework.failures)
        }
    }

    @Test
    fun `before failure restores original arguments and proceeds once despite a partial skip`() {
        val framework = Framework()
        val failure = IllegalStateException("before failed")
        val instance = Any()
        val nativeResult = Any()
        framework.bridge.register(framework.method) {
            before {
                assertSame(instance, this.instance)
                args[0] = "mutated"
                args = arrayOf("replacement")
                result = "skipped"
                throw failure
            }
        }

        val result = framework.call(instance, listOf("original")) { nativeResult }

        assertSame(nativeResult, result)
        assertArrayEquals(arrayOf<Any?>("original"), framework.proceededArgs)
        assertEquals(1, framework.proceedCalls)
        assertEquals(listOf(failure), framework.failures)
    }

    @Test
    fun `after failure preserves the native result or exception despite a partial replacement`() {
        for (nativeFailure in listOf(null, IllegalArgumentException("native failed"))) {
            val framework = Framework()
            val nativeResult = Any()
            val afterFailure = IllegalStateException("after failed")
            framework.bridge.register(framework.method) {
                after {
                    assertSame(nativeFailure, throwable)
                    assertEquals(nativeFailure != null, hasThrowable)
                    assertSame(if (nativeFailure == null) nativeResult else null, result)
                    result = "replacement"
                    throw afterFailure
                }
            }

            if (nativeFailure == null) {
                assertSame(nativeResult, framework.call { nativeResult })
            } else {
                val thrown = assertThrows(IllegalArgumentException::class.java) {
                    framework.call { throw nativeFailure }
                }
                assertSame(nativeFailure, thrown)
            }

            assertEquals(1, framework.proceedCalls)
            assertEquals(listOf(afterFailure), framework.failures)
        }
    }

    @Test
    fun `before result skips the original method and still runs after`() {
        val framework = Framework()
        var afterCalls = 0
        framework.bridge.register(framework.method) {
            before { result = "skipped" }
            after {
                assertEquals("skipped", result)
                afterCalls++
                result = "after result"
            }
        }

        val result = framework.call { error("The original method must be skipped") }

        assertEquals("after result", result)
        assertEquals(0, framework.proceedCalls)
        assertEquals(1, afterCalls)
        assertTrue(framework.failures.isEmpty())
    }

    @Test
    fun `unhookAll releases every retained handle and is safe to repeat`() {
        val framework = Framework()
        repeat(2) { framework.bridge.register(framework.method) {} }
        assertEquals(2, framework.bridge.handleCount)

        framework.bridge.unhookAll()

        assertEquals(0, framework.bridge.handleCount)
        assertEquals(2, framework.unhookCalls)
        framework.bridge.unhookAll()
        assertEquals(2, framework.unhookCalls)
        assertTrue(framework.failures.isEmpty())
    }

    @Test
    fun `failure reporter cannot replace the native outcome`() {
        for (nativeFailure in listOf(null, IllegalArgumentException("native failed"))) {
            val framework = Framework()
            framework.reporterFailure = IllegalStateException("reporter failed")
            framework.bridge.register(framework.method) {
                before {
                    args[0] = "mutated"
                    result = "skipped"
                    error("before failed")
                }
                after { error("after failed") }
            }
            if (nativeFailure == null) {
                assertEquals("native", framework.call { "native" })
            } else {
                assertSame(nativeFailure, assertThrows(IllegalArgumentException::class.java) {
                    framework.call { throw nativeFailure }
                })
            }
            assertArrayEquals(arrayOf<Any?>("original"), framework.proceededArgs)
            assertEquals(1, framework.proceedCalls)
            assertEquals(2, framework.failures.size)
        }
    }

    @Test
    fun `registration failure remains contained when reporting also fails`() {
        val framework = Framework()
        framework.hookFailure = IllegalArgumentException("hook unavailable")
        framework.reporterFailure = IllegalStateException("reporter failed")

        assertNull(framework.bridge.register(framework.method) {})
        assertEquals(0, framework.bridge.handleCount)
        assertEquals(listOf(framework.hookFailure), framework.failures)
    }

    private class Framework {
        val method: Method = String::class.java.getMethod("concat", String::class.java)
        val failures = mutableListOf<Throwable>()
        val handles = mutableListOf<XposedInterface.HookHandle>()
        var hookFailure: Throwable? = null
        var interceptFailure: Throwable? = null
        var reporterFailure: Throwable? = null
        var exceptionMode: XposedInterface.ExceptionMode? = null
        var unhookCalls = 0
        var proceedCalls = 0
        var proceededArgs: Array<Any?>? = null
        private lateinit var hooker: XposedInterface.Hooker

        private val builder = proxy<XposedInterface.HookBuilder> { self, method, args ->
            when (method.name) {
                "setExceptionMode" -> {
                    exceptionMode = args[0] as XposedInterface.ExceptionMode
                    self
                }
                "intercept" -> {
                    interceptFailure?.let { throw it }
                    hooker = args[0] as XposedInterface.Hooker
                    proxy<XposedInterface.HookHandle> { _, handleMethod, _ ->
                        when (handleMethod.name) {
                            "unhook" -> { unhookCalls++; null }
                            else -> error("Unexpected handle call: $handleMethod")
                        }
                    }.also { handles += it }
                }
                else -> error("Unexpected builder call: $method")
            }
        }

        val bridge = CredentialHookBridge(
            proxy<XposedInterface> { _, calledMethod, args ->
                check(calledMethod.name == "hook") { "Unexpected framework call: $calledMethod" }
                assertEquals(method, args[0])
                hookFailure?.let { throw it }
                builder
            },
            {
                failures += it
                reporterFailure?.let { error -> throw error }
            },
        )

        fun call(
            instance: Any? = null,
            args: List<Any?> = listOf("original"),
            proceed: (Array<Any?>) -> Any?,
        ): Any? = hooker.intercept(proxy<XposedInterface.Chain> { _, method, arguments ->
            when (method.name) {
                "getThisObject" -> instance
                "getArgs" -> args
                "proceed" -> {
                    assertEquals(1, arguments.size)
                    @Suppress("UNCHECKED_CAST")
                    val forwarded = arguments[0] as Array<Any?>
                    proceedCalls++
                    proceededArgs = forwarded
                    proceed(forwarded)
                }
                else -> error("Unexpected chain call: $method")
            }
        })
    }
}

private inline fun <reified T> proxy(
    crossinline answer: (Any, Method, Array<out Any?>) -> Any?,
): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { self, method, args ->
    when (method.name) {
        "equals" -> self === args?.get(0)
        "hashCode" -> System.identityHashCode(self)
        "toString" -> "Test ${T::class.java.simpleName}"
        else -> answer(self, method, args ?: emptyArray())
    }
} as T
