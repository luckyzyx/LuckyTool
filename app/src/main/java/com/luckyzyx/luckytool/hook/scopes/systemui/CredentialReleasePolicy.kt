package com.luckyzyx.luckytool.hook.scopes.systemui

internal class CredentialReleasePolicy(private val host: Host) {
    enum class Mode { NATIVE_FADE, DIRECTIONAL, IMMEDIATE_CLEANUP }

    interface Host {
        // Optional preparation must leave native fade usable on failure.
        fun prepareDirectional(): Boolean
        fun startNativeFade()
        fun openCredentials()
        fun cleanup()
        fun onModeStarted(mode: Mode, directionalError: Throwable?) {}
    }

    private var cleanupPending = false

    // Cleanup belongs to the same helper/session as start().
    fun start(candidate: Boolean, nativeFadeReady: Boolean): Mode? {
        if (!candidate) return null
        val prepared = if (nativeFadeReady) runCatching { host.prepareDirectional() } else null
        val mode = when {
            !nativeFadeReady -> Mode.IMMEDIATE_CLEANUP
            prepared?.getOrDefault(false) == true -> Mode.DIRECTIONAL
            else -> Mode.NATIVE_FADE
        }
        if (mode == Mode.IMMEDIATE_CLEANUP) {
            host.cleanup()
        } else {
            cleanupPending = true
            host.startNativeFade()
        }
        host.onModeStarted(mode, prepared?.exceptionOrNull())
        host.openCredentials()
        return mode
    }

    fun release() {
        if (!cleanupPending) return
        cleanupPending = false
        host.cleanup()
    }
}
