package com.luckyzyx.luckytool.hook.scopes.systemui

import com.luckyzyx.luckytool.hook.scopes.systemui.CredentialReleasePolicy.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CredentialReleasePolicyTest {
    @Test
    fun `missing enhancement preserves fade before credentials and cleans up only on release`() {
        for (reason in listOf("comm_unsupported", "no_mirror", "prediction_missing")) {
            val host = FakeHost(unavailableReason = reason)
            val policy = CredentialReleasePolicy(host)

            assertEquals(Mode.NATIVE_FADE, policy.start(candidate = true, nativeFadeReady = true))
            val started = listOf("prepare:$reason", "fade", "mode:NATIVE_FADE", "open")
            assertEquals(started, host.calls)

            host.onCleanup = { policy.release() }
            policy.release()
            policy.release()
            assertEquals(started + "cleanup", host.calls)
        }
    }

    @Test
    fun `directional mode follows successful preparation and native fade start`() {
        for (releaseDuringOpen in listOf(false, true)) {
            val host = FakeHost()
            val policy = CredentialReleasePolicy(host)
            host.onCleanup = { policy.release() }
            if (releaseDuringOpen) host.onOpen = { policy.release() }

            assertEquals(Mode.DIRECTIONAL, policy.start(candidate = true, nativeFadeReady = true))
            val started = listOf("prepare:available", "fade", "mode:DIRECTIONAL", "open")
            val expected = if (releaseDuringOpen) started + "cleanup" else started
            assertEquals(expected, host.calls)
            assertNull(host.directionalError)

            policy.release()
            policy.release()
            assertEquals(started + "cleanup", host.calls)
        }
    }

    @Test
    fun `optional preparation failure keeps native fade and reports the actual fallback`() {
        val failure = IllegalStateException("Plugin scale entry unavailable")
        val host = FakeHost(preparationError = failure)
        val policy = CredentialReleasePolicy(host)

        assertEquals(Mode.NATIVE_FADE, policy.start(candidate = true, nativeFadeReady = true))
        val started = listOf("prepare:available", "fade", "mode:NATIVE_FADE", "open")
        assertEquals(started, host.calls)
        assertSame(failure, host.directionalError)

        policy.release()
        assertEquals(started + "cleanup", host.calls)
    }

    @Test
    fun `unavailable native fade cleans up before credentials without preparing direction`() {
        val host = FakeHost()
        val policy = CredentialReleasePolicy(host)

        assertEquals(Mode.IMMEDIATE_CLEANUP, policy.start(candidate = true, nativeFadeReady = false))
        policy.release()
        assertEquals(listOf("cleanup", "mode:IMMEDIATE_CLEANUP", "open"), host.calls)
    }

    @Test
    fun `short cancellation and camera candidates leave original handling untouched`() {
        for (gesture in listOf("short_cancel", "camera")) {
            for (nativeFadeReady in listOf(false, true)) {
                val host = FakeHost(unavailableReason = gesture)
                val policy = CredentialReleasePolicy(host)

                assertNull(policy.start(candidate = false, nativeFadeReady = nativeFadeReady))
                policy.release()
                assertEquals(emptyList<String>(), host.calls)
            }
        }
    }

    private class FakeHost(
        private val unavailableReason: String? = null,
        private val preparationError: Throwable? = null,
    ) : CredentialReleasePolicy.Host {
        val calls = mutableListOf<String>()
        var directionalError: Throwable? = null
            private set
        var onCleanup: (() -> Unit)? = null
        var onOpen: (() -> Unit)? = null

        override fun prepareDirectional(): Boolean {
            calls += "prepare:${unavailableReason ?: "available"}"
            preparationError?.let { throw it }
            return unavailableReason == null
        }

        override fun startNativeFade() { calls += "fade" }

        override fun openCredentials() {
            calls += "open"
            onOpen?.invoke()
        }

        override fun cleanup() {
            calls += "cleanup"
            onCleanup?.invoke()
        }

        override fun onModeStarted(mode: Mode, directionalError: Throwable?) {
            calls += "mode:$mode"
            this.directionalError = directionalError
        }
    }
}
