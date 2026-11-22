package com.luckyzyx.luckytool.hook.scopes.systemui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.os.SystemProperties
import android.provider.Settings
import android.view.Surface
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.abs
import kotlin.math.roundToInt

private const val RING_FADE_MS = 200L

/** ColorOS describes the physical panel's camera as left,top:right,bottom. */
internal fun parseCameraBounds(value: String): Rect? {
    val parts = value.split(',', ':', limit = 5)
    if (parts.size != 4) return null
    val (left, top, right, bottom) = parts.map { it.trim().toIntOrNull() ?: return null }
    return Rect(left, top, right, bottom).takeIf { left >= 0 && top >= 0 && !it.isEmpty }
}

/** Arc coordinates in the status bar view; the stroke lies outside the real lens edge. */
internal fun cameraProgressArc(host: Rect, lens: RectF?, albumDiameter: Float, stroke: Float): RectF? {
    val center = host.exactCenterX()
    val centered = lens?.takeIf { !it.isEmpty && it.left <= center && center <= it.right }
    val x = (centered?.centerX() ?: center) - host.left
    val y = (centered?.centerY() ?: host.exactCenterY()) - host.top
    val radius = if (centered != null) (maxOf(centered.width(), centered.height()) + stroke) / 2f
        else (albumDiameter - stroke) / 2f
    val outer = radius + stroke / 2f
    if (radius <= 0f || x - outer < 0f || y - outer < 0f ||
        x + outer > host.width() || y + outer > host.height()
    ) return null
    return RectF(x - radius, y - radius, x + radius, y + radius)
}

/** A null fraction represents an indeterminate activity; a null state hides the ring. */
internal data class IslandProgress(val fraction: Float?) {
    companion object {
        fun from(position: Long, duration: Long): IslandProgress? {
            if (position < 0 || duration <= 0) return null
            return IslandProgress(position.coerceAtMost(duration).toFloat() / duration)
        }
    }
}

/** Sample the stock clock, including system animation scaling and interrupted transitions. */
internal class IslandRingTransition(val animator: AnimatorSet, val entering: Boolean, private val from: Int) {
    // Some scenes include delayed callbacks after the compact capsule has stopped moving.
    private val duration = (animator.totalDuration - animator.startDelay).coerceIn(0L, RING_FADE_MS)
    private val easing = AccelerateDecelerateInterpolator()

    fun opacity(): Int {
        val target = if (entering) 255 else 0
        if (duration == 0L || !ValueAnimator.areAnimatorsEnabled()) return target
        val fraction = (animator.currentPlayTime.toFloat() / duration).coerceIn(0f, 1f)
        return (from + (target - from) * easing.getInterpolation(fraction)).roundToInt()
    }
}

/** A draw-only overlay so the stock island continues to own touch handling. */
internal class DynamicIslandRing(private val host: View) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val nextFrame = Runnable { invalidateSelf() }
    private val location = IntArray(2)
    private var accent = readStatusBarProgressAccent(host.context)
    private var albumDiameter = readAlbumDiameter()
    private var arc: RectF? = null
    private var progress: IslandProgress? = null
    private var targetVisible = false
    var fadeAlpha = 0
        private set
    private var fade: ValueAnimator? = null
    val isPositioned get() = arc != null

    fun position() {
        val display = host.display
        val density = host.resources.displayMetrics.density
        paint.strokeWidth = 1.5f * density
        host.getLocationOnScreen(location)
        val frame = Rect(location[0], location[1], location[0] + host.width, location[1] + host.height)
        val next = if (display?.rotation != Surface.ROTATION_0) null else {
            val nativeWidth = display.supportedModes.maxOfOrNull { it.physicalWidth }?.takeIf { it > 0 }
            val scale = nativeWidth?.let { host.resources.displayMetrics.widthPixels.toFloat() / it }
            val lens = if (scale == null) null else readLens()?.let {
                RectF(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale)
            }
            // A tall DisplayCutout rect can reserve the entire status row, rather than describe the lens.
            val cutout = display.cutout?.boundingRectTop?.takeIf {
                it.width() > 0 && it.height().toFloat() / it.width() in .8f..1.25f
            }
            cameraProgressArc(frame, lens ?: cutout?.let(::RectF), albumDiameter, paint.strokeWidth)
        }
        if (arc == next) return
        invalidateSelf()
        arc = next
        val limits = Rect()
        next?.let {
            RectF(it).apply { inset(-paint.strokeWidth / 2f, -paint.strokeWidth / 2f) }.roundOut(limits)
        }
        bounds = limits
        if (next == null) hideImmediately()
        invalidateSelf()
    }

    fun updateConfiguration() {
        accent = readStatusBarProgressAccent(host.context)
        albumDiameter = readAlbumDiameter()
        position()
        invalidateSelf()
    }

    fun show(next: IslandProgress?, stockOpacity: Int? = null) {
        if (!isPositioned) {
            hideImmediately()
            return
        }
        if (next != null && progress != next) {
            progress = next
            if (next.fraction != null) host.removeCallbacks(nextFrame)
            invalidateSelf()
        }
        val visible = next != null
        if (stockOpacity != null) {
            targetVisible = visible
            fade?.cancel()
            fade = null
            if (!visible) host.removeCallbacks(nextFrame)
            updateOpacity(if (progress == null) 0 else stockOpacity)
            return
        }
        if (targetVisible == visible) return
        targetVisible = visible
        if (!visible) host.removeCallbacks(nextFrame)
        fade?.cancel()
        fade = null
        val target = if (visible) 255 else 0
        if (fadeAlpha == target || !ValueAnimator.areAnimatorsEnabled()) {
            updateOpacity(target)
            return
        }
        fade = ValueAnimator.ofInt(fadeAlpha, target).apply {
            duration = RING_FADE_MS * abs(target - fadeAlpha) / 255
            addUpdateListener { updateOpacity(it.animatedValue as Int) }
            start()
        }
    }

    fun hideImmediately() {
        if (progress == null && !targetVisible) return
        targetVisible = false
        fade?.cancel()
        fade = null
        host.removeCallbacks(nextFrame)
        updateOpacity(0)
    }

    private fun updateOpacity(value: Int) {
        if (value == 0 && !targetVisible) progress = null
        if (fadeAlpha == value) return
        fadeAlpha = value
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val area = arc ?: return
        val progress = progress ?: return
        if (fadeAlpha == 0) return
        val layer = if (fadeAlpha == 255) null else canvas.saveLayerAlpha(
            bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), fadeAlpha,
        )
        paint.color = TRACK_COLOR
        canvas.drawOval(area, paint)
        val fraction = progress.fraction
        val animate = fraction == null && ValueAnimator.areAnimatorsEnabled() && host.isShown && host.windowVisibility == View.VISIBLE
        val start = if (animate) (SystemClock.uptimeMillis() % 1200L) * .3f - 90f else -90f
        val sweep = fraction?.times(360f) ?: 90f
        if (sweep > 0f) {
            paint.color = accent
            canvas.drawArc(area, start, sweep, false, paint)
        }
        layer?.let(canvas::restoreToCount)
        if (animate && targetVisible) {
            host.removeCallbacks(nextFrame)
            host.postOnAnimation(nextFrame)
        }
    }

    // The overlay owns its paint; the status bar never applies a drawable tint or alpha to it.
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    @Deprecated("Deprecated by Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    private fun readLens(): Rect? = runCatching {
        val secondary = if (Settings.Global.getInt(host.context.contentResolver, "oplus_system_folding_mode", -1) == 0) {
            SystemProperties.get("ro.oplus.display.secondary.screenhole.position")
        } else ""
        // Preserve the vendor's spelling: this is the property its stock camera animation reads.
        parseCameraBounds(secondary.ifBlank { SystemProperties.get("ro.oplus.display.screenhole.positon") })
    }.getOrNull()

    private fun readAlbumDiameter(): Float = runCatching {
        val resources = host.context.packageManager.getResourcesForApplication(PLUGIN)
        val id = resources.getIdentifier("capsule_icon_size", "dimen", PLUGIN)
        if (id == 0) null else resources.getDimension(id).takeIf { it > 0f }
    }.getOrNull() ?: (20f * host.resources.displayMetrics.density)

    private companion object {
        const val PLUGIN = "com.oplus.systemui.plugins"
        const val TRACK_COLOR = 0x4cffffff
    }
}
