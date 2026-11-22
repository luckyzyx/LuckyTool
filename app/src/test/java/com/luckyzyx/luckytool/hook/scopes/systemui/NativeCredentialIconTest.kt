package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeCredentialIconTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `fixture retains original native vector and its padded viewport`() {
        val icon = lock(context)
        assertEquals(46, icon.intrinsicWidth)
        assertEquals(65, icon.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(46, 65, Bitmap.Config.ARGB_8888)
        try {
            icon.setBounds(0, 0, bitmap.width, bitmap.height)
            icon.draw(Canvas(bitmap))
            val ink = ink(bitmap)
            assertTrue(ink.width() < icon.intrinsicWidth)
            assertTrue(ink.height() < icon.intrinsicHeight)
            assertTrue(ink.height() > ink.width())
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `unwrapped native vector reproduces the idle to mirror shrink`() {
        val view = view(context, Layout(48, 48), wrapped = false)
        val expected = centeredIdle(view)
        val mirror = mirror(view)
        try {
            assertTrue(ink(mirror).width() < ink(expected).width())
            assertTrue(ink(mirror).height() < ink(expected).height())
        } finally {
            expected.recycle()
            mirror.recycle()
        }
    }

    @Test
    fun `mirror preserves idle ink at each density layout and activation state`() {
        for (density in listOf(160, 280, 420, 560)) {
            val configured = context.createConfigurationContext(
                Configuration(context.resources.configuration).apply { densityDpi = density },
            )
            for (layout in listOf(Layout(48, 48), Layout(57, 63, 3, 5, 7, 2))) {
                val baseline = view(configured, layout, wrapped = false)
                val wrapped = view(configured, layout, wrapped = true)
                for (activated in listOf(false, true)) {
                    baseline.isActivated = activated
                    wrapped.isActivated = activated
                    val expected = centeredIdle(baseline)
                    val idle = centeredIdle(wrapped)
                    val mirror = mirror(wrapped)
                    try {
                        val label = "density=$density layout=$layout activated=$activated"
                        assertInkMatches("$label idle", expected, idle)
                        assertInkMatches("$label mirror", expected, mirror)
                    } finally {
                        expected.recycle()
                        idle.recycle()
                        mirror.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun `mirror follows a changed layout without first drawing another idle frame`() {
        val layout = Layout(48, 48)
        val baseline = view(context, layout, wrapped = false)
        val wrapped = view(context, layout, wrapped = true)
        centeredIdle(wrapped).recycle()
        for (view in listOf(baseline, wrapped)) {
            view.setPadding(4, 2, 6, 8)
            view.layout(0, 0, 71, 69)
        }
        val expected = centeredIdle(baseline)
        val mirror = mirror(wrapped)
        try {
            assertInkMatches("changed layout", expected, mirror)
        } finally {
            expected.recycle()
            mirror.recycle()
        }
    }

    @Test
    fun `native mirror restores drawable callback alpha bounds and subsequent idle`() {
        val view = view(context, Layout(48, 48), wrapped = true)
        val icon = view.drawable as NativeCredentialIcon
        val glyph = checkNotNull(icon.drawable)
        icon.alpha = 103
        val before = centeredIdle(view)
        val callback = icon.callback
        val bounds = Rect(icon.bounds)
        val childBounds = Rect(glyph.bounds)
        mirror(view).recycle()
        val after = centeredIdle(view)
        try {
            assertSame(callback, icon.callback)
            assertSame(icon, glyph.callback)
            assertEquals(103, icon.alpha)
            assertEquals(bounds, icon.bounds)
            assertEquals(childBounds, glyph.bounds)
            assertInkMatches("restored idle", before, after)
        } finally {
            before.recycle()
            after.recycle()
        }
    }

    @Test
    fun `lock clones cannot mutate each other or an existing camera layer`() {
        val cameraGlyph = lock(context)
        val camera = LayerDrawable(arrayOf(
            ColorDrawable(Color.TRANSPARENT), ColorDrawable(Color.TRANSPARENT), cameraGlyph,
        ))
        camera.setBounds(0, 0, 48, 48)
        val cameraBounds = Rect(cameraGlyph.bounds)
        val state = checkNotNull(cameraGlyph.constantState)
        val first = NativeCredentialIcon(state.newDrawable(context.resources), ImageView(context))
        val second = NativeCredentialIcon(state.newDrawable(context.resources), ImageView(context))
        first.alpha = 53
        first.setTint(Color.RED)
        first.setBounds(1, 2, 19, 23)
        assertNotSame(cameraGlyph, first.drawable)
        assertNotSame(first.drawable, second.drawable)
        assertNotSame(first.drawable?.constantState, second.drawable?.constantState)
        assertEquals(255, cameraGlyph.alpha)
        assertEquals(255, second.alpha)
        assertEquals(cameraBounds, cameraGlyph.bounds)
        val unmodified = lock(context)
        val expected = Bitmap.createBitmap(46, 65, Bitmap.Config.ARGB_8888)
        val actual = Bitmap.createBitmap(46, 65, Bitmap.Config.ARGB_8888)
        try {
            unmodified.setBounds(0, 0, 46, 65)
            unmodified.draw(Canvas(expected))
            second.setBounds(0, 0, 46, 65)
            second.draw(Canvas(actual))
            assertInkMatches("isolated glyph", expected, actual)
            val pixels = IntArray(actual.width * actual.height)
            actual.getPixels(pixels, 0, actual.width, 0, 0, actual.width, actual.height)
            assertFalse(pixels.any { Color.alpha(it) > 0 && Color.red(it) > 0 })
        } finally {
            expected.recycle()
            actual.recycle()
        }
    }

    private fun view(context: Context, layout: Layout, wrapped: Boolean): ClockBaseImageView {
        fun px(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
        return ClockBaseImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            cropToPadding = true
            setPadding(px(layout.left), px(layout.top), px(layout.right), px(layout.bottom))
            val glyph = lock(context)
            setImageDrawable(if (wrapped) NativeCredentialIcon(glyph, this) else glyph)
            layout(0, 0, px(layout.width), px(layout.height))
        }
    }

    private fun centeredIdle(view: ImageView): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width * 3, view.height * 3, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.translate((bitmap.width - view.width) / 2f, (bitmap.height - view.height) / 2f)
        view.draw(canvas)
        return bitmap
    }

    // SystemUI KeyguardAffordanceHelper.drawIconForTransaction 的前景绘制及恢复顺序。
    private fun mirror(view: ImageView): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width * 3, view.height * 3, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = view.drawable
        val width = if (view.isActivated) view.width else view.width / 2
        val height = if (view.isActivated) view.height else view.height / 2
        val callback = icon.callback
        val alpha = icon.alpha
        val bounds = Rect(icon.bounds)
        val saveCount = canvas.saveCount
        try {
            icon.callback = null
            icon.alpha = 255
            val save = canvas.save()
            try {
                canvas.translate((bitmap.width - width) / 2f, (bitmap.height - height) / 2f)
                icon.setBounds(0, 0, width, height)
                icon.draw(canvas)
            } finally {
                canvas.restoreToCount(save)
            }
        } finally {
            icon.bounds = bounds
            icon.alpha = alpha
            icon.callback = callback
        }
        assertEquals(saveCount, canvas.saveCount)
        return bitmap
    }

    private fun assertInkMatches(label: String, expected: Bitmap, actual: Bitmap) {
        val expectedInk = ink(expected)
        val actualInk = ink(actual)
        assertEquals("$label ink bounds", expectedInk, actualInk)
        assertEquals(
            "$label aspect", expectedInk.width().toFloat() / expectedInk.height(),
            actualInk.width().toFloat() / actualInk.height(), 0.01f,
        )
        var totalAlpha = 0L
        var difference = 0L
        for (y in 0 until expected.height) for (x in 0 until expected.width) {
            val alpha = Color.alpha(expected.getPixel(x, y))
            totalAlpha += alpha
            difference += abs(alpha - Color.alpha(actual.getPixel(x, y)))
        }
        assertTrue("$label alpha difference=$difference total=$totalAlpha", difference <= totalAlpha / 100)
    }

    private fun ink(bitmap: Bitmap): Rect {
        val bounds = Rect(bitmap.width, bitmap.height, 0, 0)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            if (Color.alpha(bitmap.getPixel(x, y)) > 32) {
                bounds.left = minOf(bounds.left, x)
                bounds.top = minOf(bounds.top, y)
                bounds.right = maxOf(bounds.right, x + 1)
                bounds.bottom = maxOf(bounds.bottom, y + 1)
            }
        }
        assertFalse("native lock must have visible ink", bounds.isEmpty)
        return bounds
    }

    private fun lock(context: Context): VectorDrawable {
        val xml = GZIPInputStream(ByteArrayInputStream(Base64.getMimeDecoder().decode(LOCK_XML))).use {
            it.readBytes()
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(xml).joinToString("") { "%02x".format(it) }
        check(digest == "17cbfc57ca05d8404c211fdce4f586c7b85159b9e6898b432677a8b4f971168a")
        val type = Class.forName("android.content.res.XmlBlock")
        val block = type.getDeclaredConstructor(ByteArray::class.java).apply { isAccessible = true }
            .newInstance(xml)
        val parser = type.getDeclaredMethod("newParser").invoke(block) as XmlResourceParser
        try {
            return Drawable.createFromXml(context.resources, parser) as VectorDrawable
        } finally {
            parser.close()
            type.getDeclaredMethod("close").invoke(block)
        }
    }

    // ClockBase 2e200811… OplusLaunchableImageView.onDraw 的普通 ImageView 分支。
    private class ClockBaseImageView(context: Context) : ImageView(context) {
        override fun onDraw(canvas: Canvas) {
            drawable?.let { icon ->
                val width = width - paddingLeft - paddingRight
                val height = height - paddingTop - paddingBottom
                val left = (icon.intrinsicWidth - width) / 2
                val top = (icon.intrinsicHeight - height) / 2
                icon.setBounds(left, top, left + width, top + height)
            }
            super.onDraw(canvas)
        }
    }

    private data class Layout(
        val width: Int, val height: Int,
        val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0,
    )

    private companion object {
        // SystemUI a06bf8e0… res/drawable/ic_lock.xml 原始编译 XML，gzip + Base64。
        const val LOCK_XML = """
            H4sIAAAAAAAC/72WXU/bVhjHj7FDAhRKgdKUAg3lZYNFTmwnDUOTVkirsZaiaSoqrJtSD7wmat6UeFS7i3qzVdrFvsCkfoRN3XaJ
            qNS79nYXm3azi6p3Fd9g+58XY8fYksfFjH45z/mf8/yfg/3EIJMEeTFEiEQmyXKMkDQRl0TIK+JecTAMkmAGLAEDfAA2wBegAmzw
            GPwAnoKfwQF4Cf4Ar8ERuA//t+DPHkL+Bm+BLBMyDXLgJrgLquBb8CN4Bg7B7+AI9CqETIIFoIJ74EugKHWzZpHe3rJVeVC2SSz2
            qLJnlyE0K/sNe9sJdhC0d82qte0EO2RwcL9iPWo2WvZdljM05MzXuVlf31eVarXYqDZaJJFomnb5ummbZGCgbbcaDy2+4My4x+Ag
            n21U6lbRbMLTnd9sVOqkv99umfV21bRxFM9k59hotdosm6I2jxMJGt/5pmmRWi11W1fzKS2fKmJMY6TDsnpVcwO2klf1XCqvGmm6
            fVkt5FlQ1HRVT3MHvkVzkpxAY678k3SedJ7QioU8vHTk51TNSLOQfqTplEdsT5GFmmoUUmxjlm6kKzwq0hUeZo83Zt3s7LEl25g/
            zqbRLk7zpvMmdTtH92rsLAV+Bwxm5gmKek41uMRPlFffd8ICVp2IB4bhBmLNwClS3MRwfA3q60SstmcsGupVvpcJLJ1F1FIEbKRV
            xcgX2NlYtibsNPq7Kkrp09JHaBE6lDZKWUxGRtxJ6To+PyllSTLpETdLWpZ+aKU7WBod9S+FqZpbSPMW0oIKaeGFtMBCWmAh3VtI
            DyqkhxfSAwvp3kLxuFnfazUqe3grPGg1vm6SpaWybTdXMpn2btmqmW1VbFB3G7WM2XyYaVntjJOkKPQbj9fFvrVr44tOOlKCbON9
            JhNJ2pIkaQf8Cn4Dc7IkzYMeRZJkoIAY6AVxMADOgM/APfASvALTMUki0jB7154D/+B6B+Mi6IFue3R6vYd4DD+KeFcv8tc3W08Q
            EpNWRa5XU7kWdzWkL69RLdGlPV6jNZehjXtqvitqSp6a9LrA8+UL4qz3hebPk315cyJvTmhnu87w1/NF8ffH1b5/7vhPRPBfEP4L
            gf4fr530t9cc/6kI/vPCfz7Q//OA8x8dOv6pCP5XhP+VQP/Dg5P+z9j9+QnarMd/SfjHff6zwn9WaP3ieVNtXGgDrv8kDal2pqsm
            uUa1QVcblkTuUIB2riu386Eset5/Xkf33ydHnwrRJ0J0fz86z0GN8Bwui3ty+RR9qkXwnxH+M6fo01wE/5TwT52iTwsR/KeE/1Sg
            /9NrJ/1/Oe7TlQh9Oi38pz19Oia0sf+5T1dC+rQQ0ne5EF0L0dWQPr0R4TlMiHsycYo+XY/gPyn8J0/Rp7ci+F8S/pf+Y5/SPtoM
            8Pf3UVL4J4Umd3llWX8oAVrM1wtU6w3Qus+bXT153s4BPe930LYC+j7mO+9Fcd6LQusL6HH6XTgv9p0X+0YCzjbq6XHi6betkH7e
            DOnPWyH6eoh+I0QfD9H9/+NIIf8T/Qsr8xlzxA4AAA==
        """
    }
}
