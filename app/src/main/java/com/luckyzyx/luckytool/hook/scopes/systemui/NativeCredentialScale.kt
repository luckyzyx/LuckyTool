package com.luckyzyx.luckytool.hook.scopes.systemui

import android.os.Bundle
import android.view.View
import java.util.function.BiFunction

internal object NativeCredentialScale {
    fun read(view: View): Float? {
        val comm = nativeComm(view) ?: return null
        return try {
            val result = comm.apply("getImageScale", null) ?: return null
            if (!result.containsKey("imageScale")) return null
            result.getFloat("imageScale").takeIf { it.isFinite() && it > 0f }
        } catch (_: Throwable) {
            null
        }
    }

    fun write(view: View, scale: Float): Boolean {
        if (!scale.isFinite() || scale <= 0f) return false
        val comm = nativeComm(view) ?: return false
        return try {
            comm.apply("setImageScale", Bundle().apply {
                putFloat("imageScale", scale)
                putBoolean("animate", false)
                putFloat("velocityX", 0f)
                putFloat("velocityY", 0f)
            })
            read(view)?.compareTo(scale) == 0
        } catch (_: Throwable) {
            false
        }
    }

    fun animate(view: View, scale: Float): Boolean {
        if (!scale.isFinite() || scale <= 0f || read(view) == null) return false
        val comm = nativeComm(view) ?: return false
        return try {
            comm.apply("setImageScale", Bundle().apply {
                putFloat("imageScale", scale)
                putBoolean("animate", true)
                putFloat("velocityX", 0f)
                putFloat("velocityY", 0f)
            })
            true
        } catch (_: Throwable) {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun nativeComm(view: View): BiFunction<String, Bundle?, Bundle?>? =
        view as? BiFunction<String, Bundle?, Bundle?>
}
