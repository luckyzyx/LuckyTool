package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

/** The viewport and corner contours are from the stock four-bar ColorOS vector. */
internal object StockSignalGeometry {
    const val WIDTH = 19f
    const val HEIGHT = 16f
    const val BAR_TOP = 2f
    const val BAR_BOTTOM = 14f
    const val START_X = 3.6f
    const val PITCH = 3.6f
    const val BLOCK_SIZE = 1.8f
    const val BLOCK_COUNT = 4
    const val BAR_RIGHT = START_X + (BLOCK_COUNT - 1) * PITCH + BLOCK_SIZE
    const val SECOND_ROW_Y = BAR_BOTTOM - BLOCK_SIZE
    const val ROTATED_SCALE = (BAR_BOTTOM - BAR_TOP) / (BAR_RIGHT - START_X)
    private const val ROW_GAP = .9f
    const val TOP_ROW_SCALE = (SECOND_ROW_Y - ROW_GAP - BAR_TOP) / (BAR_BOTTOM - BAR_TOP)

    fun block() = Path().apply {
        moveTo(0f, .8f)
        cubicTo(0f, .52f, 0f, .38f, .05f, .27f)
        cubicTo(.1f, .17f, .18f, .1f, .27f, .05f)
        cubicTo(.38f, 0f, .52f, 0f, .8f, 0f)
        lineTo(1f, 0f)
        cubicTo(1.28f, 0f, 1.42f, 0f, 1.53f, .05f)
        cubicTo(1.63f, .1f, 1.7f, .18f, 1.75f, .27f)
        cubicTo(1.8f, .38f, 1.8f, .52f, 1.8f, .8f)
        lineTo(1.8f, 1f)
        cubicTo(1.8f, 1.28f, 1.8f, 1.42f, 1.75f, 1.53f)
        cubicTo(1.7f, 1.63f, 1.62f, 1.7f, 1.53f, 1.75f)
        cubicTo(1.42f, 1.8f, 1.28f, 1.8f, 1f, 1.8f)
        lineTo(.8f, 1.8f)
        cubicTo(.52f, 1.8f, .38f, 1.8f, .27f, 1.75f)
        cubicTo(.17f, 1.7f, .1f, 1.62f, .05f, 1.53f)
        cubicTo(0f, 1.42f, 0f, 1.28f, 0f, 1f)
        close()
    }
}

/** Fits the native bars and the other SIM's blocks inside the original visible signal height. */
internal class StackedSignalDrawable(
    val stock: Drawable,
    level: Int,
    private val rotated: Boolean = false,
) : Drawable() {
    private val block = StockSignalGeometry.block()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var tint: ColorStateList? = null
    private var opacity = stock.alpha

    var secondaryLevel: Int = level
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val sx = bounds.width() / StockSignalGeometry.WIDTH
        val sy = bounds.height() / StockSignalGeometry.HEIGHT
        val orientationSave = canvas.save()
        if (rotated) {
            val right = bounds.left + StockSignalGeometry.BAR_RIGHT * sx
            // Rotate around the visible glyph and keep its right edge and visible height.
            canvas.translate(right, bounds.top + StockSignalGeometry.BAR_TOP * sy)
            canvas.scale(StockSignalGeometry.ROTATED_SCALE * sx / sy, StockSignalGeometry.ROTATED_SCALE * sy / sx)
            canvas.rotate(-90f)
            canvas.translate(-right, -bounds.top - StockSignalGeometry.BAR_BOTTOM * sy)
        }
        val stockSave = canvas.save()
        // Keep the tallest bar's top fixed and make room below the bars.
        canvas.scale(1f, StockSignalGeometry.TOP_ROW_SCALE, 0f, bounds.top + StockSignalGeometry.BAR_TOP * sy)
        stock.draw(canvas)
        canvas.restoreToCount(stockSave)

        canvas.translate(bounds.left + StockSignalGeometry.START_X * sx, bounds.top + StockSignalGeometry.SECOND_ROW_Y * sy)
        canvas.scale(sx, sy)
        paint.color = tint?.getColorForState(state, Color.BLACK) ?: Color.BLACK
        val alpha = Color.alpha(paint.color) * opacity / 255f
        repeat(StockSignalGeometry.BLOCK_COUNT) { index ->
            paint.alpha = (alpha * if (index < secondaryLevel) 1f else .2f).roundToInt()
            canvas.drawPath(block, paint)
            canvas.translate(StockSignalGeometry.PITCH, 0f)
        }
        canvas.restoreToCount(orientationSave)
    }

    override fun getIntrinsicWidth() = stock.intrinsicWidth
    override fun getIntrinsicHeight() = stock.intrinsicHeight
    override fun onBoundsChange(bounds: Rect) { stock.bounds = bounds }
    override fun getAlpha() = opacity
    override fun setAlpha(alpha: Int) { opacity = alpha; stock.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter; stock.colorFilter = filter; invalidateSelf() }
    override fun setTintList(tint: ColorStateList?) { this.tint = tint; stock.setTintList(tint); invalidateSelf() }
    override fun setTintBlendMode(mode: BlendMode?) { stock.setTintBlendMode(mode); invalidateSelf() }
    override fun isStateful() = stock.isStateful || tint?.isStateful == true
    override fun onStateChange(state: IntArray): Boolean { stock.state = state; return true }
    @Deprecated("Deprecated by Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
