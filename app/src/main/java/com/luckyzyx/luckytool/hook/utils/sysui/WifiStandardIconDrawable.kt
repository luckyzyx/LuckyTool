package com.luckyzyx.luckytool.hook.utils.sysui

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import org.lsposed.lsparanoid.Obfuscate

/**
 * 用系统字体绘制 Wi-Fi 标准数字（4 / 5 / 6 / 7），不再依赖模块里的矢量资源。
 *
 * 几何沿用被替换掉的 stat_signal_wifi_4/5/6/7 矢量：画布 22x17，数字水平中心 4.52、基线 15.1、
 * 字高约 5.5，这里换算成 22:17 视口内的相对位置；字号取高度的 0.45（系统字体数字高约 0.72em）。
 * 颜色跟随 ImageView 给的 tint，没有 tint 时用白色。
 */
@Obfuscate
class WifiStandardIconDrawable(
    private val digit: Char,
    private val widthPx: Int,
    private val heightPx: Int
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = DIGIT_TYPEFACE
        textAlign = Paint.Align.CENTER
    }

    private var tintList: ColorStateList? = null
    private var colorFilter: ColorFilter? = null
    private var alphaValue = 255

    override fun getIntrinsicWidth(): Int = widthPx

    override fun getIntrinsicHeight(): Int = heightPx

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // 保持 22:17 的绘制区域并居中，等效于旧矢量被 ImageView 缩放后的结果
        val boxWidth = bounds.width().toFloat()
        val boxHeight = bounds.height().toFloat()
        val scale = minOf(boxWidth / VIEWPORT_WIDTH, boxHeight / VIEWPORT_HEIGHT)
        val drawWidth = VIEWPORT_WIDTH * scale
        val drawHeight = VIEWPORT_HEIGHT * scale
        val left = bounds.left + (boxWidth - drawWidth) / 2f
        val top = bounds.top + (boxHeight - drawHeight) / 2f

        val tint = tintList
        val color = tint?.getColorForState(state, tint.defaultColor) ?: Color.WHITE
        paint.textSize = drawHeight * TEXT_SIZE_RATIO
        paint.color = color
        paint.alpha = alphaValue * Color.alpha(color) / 255
        paint.colorFilter = colorFilter
        canvas.drawText(
            digit.toString(),
            left + drawWidth * DIGIT_CENTER_X,
            top + drawHeight * DIGIT_BASELINE_Y,
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        alphaValue = alpha
        invalidateSelf()
    }

    override fun getAlpha(): Int = alphaValue

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getColorFilter(): ColorFilter? = colorFilter

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setTintList(tint: ColorStateList?) {
        tintList = tint
        invalidateSelf()
    }

    override fun isStateful(): Boolean = tintList?.isStateful == true

    override fun onStateChange(state: IntArray): Boolean {
        if (tintList?.isStateful != true) return false
        invalidateSelf()
        return true
    }

    private companion object {
        const val VIEWPORT_WIDTH = 22f
        const val VIEWPORT_HEIGHT = 17f
        const val TEXT_SIZE_RATIO = 0.45f
        const val DIGIT_CENTER_X = 4.52f / VIEWPORT_WIDTH
        const val DIGIT_BASELINE_Y = 15.1f / VIEWPORT_HEIGHT

        val DIGIT_TYPEFACE: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
}
