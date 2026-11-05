package com.luckyzyx.luckytool.hook.scopes.systemui

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

internal object NativeReleaseParameters {
    fun velocity(position: Float, target: Float, velocity: Float, stiffness: Float): Float {
        if (!position.isFinite() || !target.isFinite() || !velocity.isFinite() ||
            !stiffness.isFinite() || stiffness <= 0f
        ) return velocity
        val distance = target - position
        if (distance == 0f || velocity * distance <= 0f) return 0f
        // 临界阻尼在 v <= ωd 时不会越过终点，再被弹簧拉回。
        val maximum = sqrt(stiffness) * abs(distance)
        return sign(distance) * minOf(abs(velocity), maximum)
    }
}
