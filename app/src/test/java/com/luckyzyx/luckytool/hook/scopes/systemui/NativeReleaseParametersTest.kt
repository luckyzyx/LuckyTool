package com.luckyzyx.luckytool.hook.scopes.systemui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class NativeReleaseParametersTest {
    @Test
    fun `normal forward velocity is preserved and only overshooting velocity is reduced`() {
        assertEquals(120f, NativeReleaseParameters.velocity(10f, 30f, 120f, 100f), 0f)
        assertEquals(200f, NativeReleaseParameters.velocity(10f, 30f, 900f, 100f), 0f)
        assertEquals(-200f, NativeReleaseParameters.velocity(30f, 10f, -900f, 100f), 0f)
    }

    @Test
    fun `reverse velocity and a completed axis cannot cause reverse motion`() {
        assertEquals(0f, NativeReleaseParameters.velocity(10f, 30f, -120f, 100f), 0f)
        assertEquals(0f, NativeReleaseParameters.velocity(30f, 10f, 120f, 100f), 0f)
        assertEquals(0f, NativeReleaseParameters.velocity(10f, 10f, 120f, 100f), 0f)
    }

    @Test
    fun `critical spring solution reaches its native target without reversing for large input velocities`() {
        for (target in listOf(-24f, 24f)) for (input in listOf(-10000f, -100f, 0f, 100f, 10000f)) {
            val velocity = NativeReleaseParameters.velocity(0f, target, input, 100f)
            val omega = 10f
            var previousDistance = abs(target)
            for (millisecond in 0..1000) {
                val t = millisecond / 1000f
                val displacement = (-target + (velocity - omega * target) * t) * exp(-omega * t)
                assertTrue("target=$target input=$input t=$t", abs(displacement) <= previousDistance + 0.0001f)
                assertTrue(displacement * target <= 0f)
                previousDistance = abs(displacement)
            }
        }
    }
}
