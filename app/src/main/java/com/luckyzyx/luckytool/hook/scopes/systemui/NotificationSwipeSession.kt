package com.luckyzyx.luckytool.hook.scopes.systemui

import kotlin.math.abs

internal class NotificationSwipeSession(
    private val pointerId: Int,
    private val downX: Float,
    private val downY: Float,
    private val slop: Float,
) {
    private var decision: Boolean? = null
    val decided: Boolean get() = decision != null
    val blocksPanel: Boolean get() = decision == true

    fun shouldBlock(x: Float, y: Float, pointerId: Int, pointerCount: Int): Boolean {
        decision?.let { return it }
        if (pointerCount != 1 || pointerId != this.pointerId ||
            !x.isFinite() || !y.isFinite() || !slop.isFinite() || slop < 0f
        ) {
            decision = false
            return false
        }
        val dx = x - downX
        val dy = y - downY
        if (abs(dx) <= slop && abs(dy) <= slop) return false
        decision = dy < -slop && abs(dy) > abs(dx)
        return blocksPanel
    }
}
