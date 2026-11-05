package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Application
import android.os.Bundle
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.function.BiFunction

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class NativeCredentialScaleTest {
    @Test
    fun `animated request delegates the target to the plugin native spring`() {
        val view = NativeImageView()

        assertTrue(NativeCredentialScale.animate(view, 1.08f))

        assertEquals(listOf("getImageScale", "setImageScale"), view.calls.map { it.command })
        val request = requireNotNull(view.calls.last().arguments)
        assertTrue(request.getBoolean("animate"))
        assertEquals(1.08f, request.getFloat("imageScale"), 0f)
        assertEquals(1.2f, requireNotNull(NativeCredentialScale.read(view)), 0f)
        assertTrue(view.spring.running)
        assertEquals(0, view.spring.cancelCalls)
    }

    @Test
    fun `animated request without native communication leaves the view untouched`() {
        val view = View(RuntimeEnvironment.getApplication())
        assertFalse(NativeCredentialScale.animate(view, 1.08f))
        assertEquals(1f, view.scaleX, 0f)
        assertEquals(1f, view.scaleY, 0f)
    }

    @Test
    fun `read uses the native scalar and sends a null argument`() {
        val view = NativeImageView(1.3f)
        view.scaleX = 0.7f
        view.scaleY = 0.8f

        assertEquals(1.3f, requireNotNull(NativeCredentialScale.read(view)), 0f)
        assertEquals(listOf("getImageScale"), view.calls.map { it.command })
        assertNull(view.calls.single().arguments)
        assertTrue(view.spring.running)
        assertEquals(0.7f, view.scaleX, 0f)
        assertEquals(0.8f, view.scaleY, 0f)
    }

    @Test
    fun `write accepts a void reply and preserves the requested scalar without animation`() {
        val view = NativeImageView()

        assertTrue(NativeCredentialScale.write(view, 1.3f))

        assertEquals(listOf("setImageScale", "getImageScale"), view.calls.map { it.command })
        val arguments = requireNotNull(view.calls.first().arguments)
        assertEquals(setOf("imageScale", "animate", "velocityX", "velocityY"), arguments.keySet())
        assertEquals(1.3f, arguments.getFloat("imageScale"), 0f)
        assertFalse(arguments.getBoolean("animate"))
        assertEquals(0f, arguments.getFloat("velocityX"), 0f)
        assertEquals(0f, arguments.getFloat("velocityY"), 0f)
        assertNull(view.calls.last().arguments)
        assertFalse(view.spring.running)
        assertEquals(1, view.spring.cancelCalls)
        assertEquals(1.3f, view.scaleX, 0f)
        assertEquals(1.3f, view.scaleY, 0f)
        assertEquals(1.3f, requireNotNull(NativeCredentialScale.read(view)), 0f)
    }

    @Test
    fun `successive frames forward every positive finite scalar to the same delegate`() {
        val view = NativeImageView()
        val scales = listOf(1.3f, 1.2f, 0.75f, Float.MIN_VALUE, Float.MAX_VALUE, 1f)

        for (scale in scales) {
            assertTrue("scale=$scale", NativeCredentialScale.write(view, scale))
            assertEquals(scale, view.scaleX, 0f)
            assertEquals(scale, view.scaleY, 0f)
        }

        assertEquals(scales.flatMap { listOf("setImageScale", "getImageScale") },
            view.calls.map { it.command })
        assertEquals(scales, view.calls.filter { it.command == "setImageScale" }
            .map { requireNotNull(it.arguments).getFloat("imageScale") })
        assertEquals(scales.size, view.spring.cancelCalls)
    }

    @Test
    fun `each view supplies its own current native state`() {
        val first = NativeImageView(1.1f)
        val second = NativeImageView(1.7f)

        assertEquals(1.1f, requireNotNull(NativeCredentialScale.read(first)), 0f)
        assertEquals(1.7f, requireNotNull(NativeCredentialScale.read(second)), 0f)
        assertTrue(NativeCredentialScale.write(first, 1.3f))
        assertEquals(1.7f, requireNotNull(NativeCredentialScale.read(second)), 0f)
        assertEquals(1.3f, requireNotNull(NativeCredentialScale.read(first)), 0f)
        assertTrue(second.spring.running)
    }

    @Test
    fun `a view without the native delegate is unsupported and is left alone`() {
        val view = View(RuntimeEnvironment.getApplication())
        view.scaleX = 0.7f
        view.scaleY = 0.8f

        assertNull(NativeCredentialScale.read(view))
        assertFalse(NativeCredentialScale.write(view, 1.3f))
        assertEquals(0.7f, view.scaleX, 0f)
        assertEquals(0.8f, view.scaleY, 0f)
    }

    @Test
    fun `missing malformed and invalid getter replies fail both read and write verification`() {
        val replies = listOf(
            null,
            "not a Bundle",
            Bundle(),
            Bundle().apply { putFloat("anotherScale", 1.3f) },
            Bundle().apply { putString("imageScale", "1.3") },
        ) + listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f, -0f, -1f)
            .map { scale -> Bundle().apply { putFloat("imageScale", scale) } }

        for (reply in replies) {
            val view = NativeImageView().apply { getter = { reply } }

            assertNull(NativeCredentialScale.read(view))
            assertFalse(NativeCredentialScale.write(view, 1.3f))
            assertEquals(listOf("getImageScale", "setImageScale", "getImageScale"),
                view.calls.map { it.command })
        }
    }

    @Test
    fun `invalid requested scales never invoke native communication`() {
        val view = NativeImageView()

        for (scale in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f, -0f, -1f)) {
            assertFalse("scale=$scale", NativeCredentialScale.write(view, scale))
        }

        assertTrue(view.calls.isEmpty())
        assertTrue(view.spring.running)
        assertEquals(1f, view.scaleX, 0f)
        assertEquals(1f, view.scaleY, 0f)
    }

    @Test
    fun `getter failures are contained during reads and write verification`() {
        for (failure in listOf(IllegalStateException("getter failed"), LinkageError("delegate unavailable"))) {
            val view = NativeImageView().apply {
                throwOn = "getImageScale"
                this.failure = failure
            }

            assertNull(NativeCredentialScale.read(view))
            assertFalse(NativeCredentialScale.write(view, 1.3f))
            assertEquals(listOf("getImageScale", "setImageScale", "getImageScale"),
                view.calls.map { it.command })
        }
    }

    @Test
    fun `setter failures are contained without another native call`() {
        for (failure in listOf(IllegalStateException("setter failed"), LinkageError("delegate unavailable"))) {
            val view = NativeImageView().apply {
                throwOn = "setImageScale"
                this.failure = failure
            }

            assertFalse(NativeCredentialScale.write(view, 1.3f))
            assertEquals(listOf("setImageScale"), view.calls.map { it.command })
            assertTrue(view.spring.running)
            assertEquals(1f, view.scaleX, 0f)
            assertEquals(1f, view.scaleY, 0f)
        }
    }

    @Test
    fun `an unsupported void setter is detected by reading back the unchanged scalar`() {
        val view = NativeImageView(1.1f).apply { supportsSet = false }

        assertFalse(NativeCredentialScale.write(view, 1.3f))

        assertEquals(listOf("setImageScale", "getImageScale"), view.calls.map { it.command })
        assertTrue(view.spring.running)
        assertEquals(1.1f, view.scaleX, 0f)
        assertEquals(1.1f, view.scaleY, 0f)
    }

    @Test
    fun `a different finite readback fails even when it is the next representable float`() {
        val requested = 1.3f
        val view = NativeImageView().apply {
            getter = { Bundle().apply { putFloat("imageScale", Math.nextUp(requested)) } }
        }

        assertFalse(NativeCredentialScale.write(view, requested))
        assertEquals(requested, view.scaleX, 0f)
        assertEquals(requested, view.scaleY, 0f)
    }

    private data class Call(val command: String, val arguments: Bundle?)

    private class NativeImageView(initialScale: Float = 1f) :
        View(RuntimeEnvironment.getApplication()), BiFunction<String, Bundle?, Any?> {
        private var c = initialScale
        val calls = mutableListOf<Call>()
        val spring = StubSpring()
        var supportsSet = true
        var throwOn: String? = null
        var failure: Throwable = IllegalStateException("native communication failed")
        var getter: () -> Any? = { Bundle().apply { putFloat("imageScale", c) } }

        init {
            scaleX = c
            scaleY = c
        }

        override fun apply(command: String, arguments: Bundle?): Any? {
            calls += Call(command, arguments?.let { Bundle(it) })
            if (command == throwOn) throw failure
            return when (command) {
                "getImageScale" -> getter()
                "setImageScale" -> {
                    if (supportsSet) {
                        val input = requireNotNull(arguments)
                        val animate = input.getBoolean("animate")
                        if (!animate) spring.cancel()
                        c = if (animate) 1.2f else input.getFloat("imageScale")
                        scaleX = c
                        scaleY = c
                        invalidate()
                    }
                    null
                }
                else -> null
            }
        }
    }

    private class StubSpring {
        var running = true
            private set
        var cancelCalls = 0
            private set

        fun cancel() {
            running = false
            cancelCalls++
        }
    }
}
