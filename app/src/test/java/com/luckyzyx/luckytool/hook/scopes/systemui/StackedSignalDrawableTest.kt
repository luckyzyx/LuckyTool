package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.luckyzyx.luckytool.utils.MobileSignalDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StackedSignalDrawableTest {
    @Test
    fun fitsBothRowsInsideTheNativeInkBoundsAndRendersIndependentSignalLevels() {
        val drawable = StackedSignalDrawable(stock(), 2).apply {
            setBounds(0, 0, 190, 160)
            setTintList(ColorStateList.valueOf(Color.WHITE))
        }
        val image = Bitmap.createBitmap(190, 160, Bitmap.Config.ARGB_8888)
        drawable.draw(Canvas(image))

        assertEquals(190, drawable.intrinsicWidth)
        assertEquals(160, drawable.intrinsicHeight)
        val inkRows = (0 until image.height).filter { y ->
            (0 until image.width).any { x -> Color.alpha(image.getPixel(x, y)) != 0 }
        }
        assertEquals(20, inkRows.first())
        assertEquals(139, inkRows.last())
        assertEquals(Color.RED, image.getPixel(45, 112))
        assertEquals(Color.TRANSPARENT, image.getPixel(45, 117))
        assertEquals(Color.WHITE, image.getPixel(45, 131))
        assertEquals(Color.WHITE, image.getPixel(81, 131))
        assertEquals(51, Color.alpha(image.getPixel(117, 131)))
        assertEquals(51, Color.alpha(image.getPixel(153, 131)))
        assertEquals(Color.TRANSPARENT, image.getPixel(60, 131))
        assertTrue(Color.alpha(image.getPixel(36, 122)) < 20)

        image.eraseColor(Color.TRANSPARENT)
        drawable.secondaryLevel = 4
        drawable.setTintList(ColorStateList.valueOf(Color.BLUE))
        drawable.draw(Canvas(image))
        assertEquals(Color.BLUE, image.getPixel(153, 131))
    }

    @Test
    fun rotatedGlyphKeepsTheLongestBarAtTheTopAndTheBlocksOnTheRight() {
        val image = Bitmap.createBitmap(190, 160, Bitmap.Config.ARGB_8888)
        StackedSignalDrawable(stock(), 2, rotated = true).apply {
            setBounds(0, 0, 190, 160)
            setTintList(ColorStateList.valueOf(Color.WHITE))
        }.draw(Canvas(image))

        assertEquals(Color.RED, image.getPixel(70, 28))
        assertEquals(Color.TRANSPARENT, image.getPixel(70, 131))
        assertEquals(Color.RED, image.getPixel(120, 131))
        assertEquals(Color.TRANSPARENT, image.getPixel(141, 28))
        assertEquals(51, Color.alpha(image.getPixel(153, 28)))
        assertEquals(51, Color.alpha(image.getPixel(153, 63)))
        assertEquals(Color.WHITE, image.getPixel(153, 97))
        assertEquals(Color.WHITE, image.getPixel(153, 131))
        assertEquals(Color.TRANSPARENT, image.getPixel(70, 19))
        assertEquals(Color.TRANSPARENT, image.getPixel(153, 140))
    }

    @Test
    fun onlyRecognizesTheStockFourBarDrawables() {
        for (level in 0..4) assertEquals(level, MobileSignalDisplay.stockLevel("stat_signal_signal_lte_single_$level"))
        assertNull(MobileSignalDisplay.stockLevel("stat_signal_signal_lte_single_5"))
        assertNull(MobileSignalDisplay.stockLevel("stat_sys_signal_4_fully"))
        assertNull(MobileSignalDisplay.stockLevel("stat_signal_noservice_lte"))
    }

    private fun stock() = object : Drawable() {
        private val paint = Paint().apply { color = Color.RED }
        override fun draw(canvas: Canvas) {
            canvas.drawRect(36f, 94f, 54f, 140f, paint)
            canvas.drawRect(144f, 20f, 162f, 140f, paint)
        }
        override fun getIntrinsicWidth() = 190
        override fun getIntrinsicHeight() = 160
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
        @Deprecated("Deprecated by Android")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
