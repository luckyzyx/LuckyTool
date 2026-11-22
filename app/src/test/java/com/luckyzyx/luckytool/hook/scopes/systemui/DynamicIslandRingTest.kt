package com.luckyzyx.luckytool.hook.scopes.systemui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicIslandRingTest {
    @Test
    fun reportsElapsedActivityProgressAndHidesWhenDurationIsUnavailable() {
        assertEquals(IslandProgress(.25f), IslandProgress.from(30_000, 120_000))
        assertEquals(IslandProgress(1f), IslandProgress.from(130_000, 120_000))
        assertNull(IslandProgress.from(-1, 120_000))
        assertNull(IslandProgress.from(30_000, 0))
    }

    @Test
    @Config(qualifiers = "mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun fadesTheRingWithoutRestartingOnProgressUpdates() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        val host = FrameLayout(activity.get())
        activity.get().setContentView(host)
        host.layout(0, 0, 200, 100)
        val ring = DynamicIslandRing(host)
        ring.position()
        assertTrue(ring.isPositioned)
        fun fade() = ReflectionHelpers.getField<ValueAnimator>(ring, "fade")
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)

        fun frameAlpha(): Int {
            bitmap.eraseColor(Color.TRANSPARENT)
            ring.draw(Canvas(bitmap))
            return (0 until bitmap.width).maxOf { x ->
                (0 until bitmap.height).maxOf { y -> Color.alpha(bitmap.getPixel(x, y)) }
            }
        }

        val originalScale = ValueAnimator.getDurationScale()
        try {
            setAnimationScale(1f)
            ring.show(IslandProgress(.25f))
            assertEquals(0, frameAlpha())
            fade().setCurrentFraction(.5f)
            val entering = frameAlpha()
            assertTrue("Fade-in alpha: $entering", entering in 1..254)
            ring.show(IslandProgress(.5f))
            assertEquals(entering, frameAlpha())
            fade().end()
            assertEquals(255, frameAlpha())

            ring.show(null)
            fade().setCurrentFraction(.5f)
            val leaving = frameAlpha()
            assertTrue("Fade-out alpha: $leaving", leaving in 1..254)
            ring.show(IslandProgress(.75f))
            assertEquals(leaving, frameAlpha())
            fade().end()
            assertEquals(255, frameAlpha())
            ring.show(null)
            fade().end()
            assertEquals(0, frameAlpha())

            ring.show(IslandProgress(.5f))
            val interrupted = fade()
            interrupted.setCurrentFraction(.5f)
            ring.hideImmediately()
            assertFalse(interrupted.isStarted)
            assertEquals(0, frameAlpha())

            val stock = AnimatorSet().apply {
                playTogether(ValueAnimator.ofFloat(0f, 1f).apply { duration = 160L })
                startDelay = 40L
            }
            val enteringMotion = IslandRingTransition(stock, entering = true, from = ring.fadeAlpha)
            stock.currentPlayTime = 80L
            ring.show(IslandProgress(.5f), stockOpacity = enteringMotion.opacity())
            assertEquals(128, frameAlpha())
            stock.currentPlayTime = 160L
            ring.show(IslandProgress(.5f), stockOpacity = enteringMotion.opacity())
            assertEquals(255, frameAlpha())

            stock.currentPlayTime = 0L
            val leavingMotion = IslandRingTransition(stock, entering = false, from = ring.fadeAlpha)
            stock.currentPlayTime = 80L
            ring.show(null, stockOpacity = leavingMotion.opacity())
            assertEquals(128, frameAlpha())
            val reversed = IslandRingTransition(stock, entering = true, from = ring.fadeAlpha)
            stock.currentPlayTime = 0L
            ring.show(IslandProgress(.5f), stockOpacity = reversed.opacity())
            assertEquals(128, frameAlpha())
            stock.currentPlayTime = 160L
            ring.show(null, stockOpacity = leavingMotion.opacity())
            assertEquals(0, frameAlpha())

            val slowStock = AnimatorSet().apply {
                playTogether(ValueAnimator.ofFloat(0f, 1f).apply { duration = 600L })
                currentPlayTime = 200L
            }
            assertEquals(255, IslandRingTransition(slowStock, entering = true, from = 0).opacity())

            setAnimationScale(0f)
            ring.show(IslandProgress(.5f))
            assertEquals(255, frameAlpha())
            ring.show(null)
            assertEquals(0, frameAlpha())
        } finally {
            ring.hideImmediately()
            setAnimationScale(originalScale)
            activity.close()
            bitmap.recycle()
        }
    }

    private fun setAnimationScale(scale: Float) = ReflectionHelpers.callStaticMethod<Unit>(
        ValueAnimator::class.java, "setDurationScale", ReflectionHelpers.ClassParameter.from(Float::class.javaPrimitiveType, scale),
    )

    @Test
    fun parsesTheVendorCameraRectangleAndRejectsInvalidBounds() {
        assertEquals(Rect(470, 20, 530, 80), parseCameraBounds("470,20:530,80"))
        assertNull(parseCameraBounds(""))
        assertNull(parseCameraBounds("470,20:abc,80"))
        assertNull(parseCameraBounds("530,80:470,20"))
    }

    @Test
    fun centersTheRingOnTheLensAndKeepsTheStrokeOutsideItsEdge() {
        val host = Rect(10, 5, 1010, 105)
        val lens = RectF(481f, 24f, 541f, 84f)
        val arc = requireNotNull(cameraProgressArc(host, lens, albumDiameter = 54f, stroke = 4f))
        assertEquals(501f, arc.centerX(), .001f)
        assertEquals(49f, arc.centerY(), .001f)
        assertEquals(lens.width(), arc.width() - 4f, .001f)
    }

    @Test
    fun usesTheAlbumDiameterAtTheCenterWhenThereIsNoCenteredLens() {
        val host = Rect(0, 0, 1000, 100)
        val sideLens = RectF(60f, 20f, 120f, 80f)
        val fallback = RectF(475f, 25f, 525f, 75f)
        assertEquals(fallback, cameraProgressArc(host, null, albumDiameter = 54f, stroke = 4f))
        assertEquals(fallback, cameraProgressArc(host, sideLens, albumDiameter = 54f, stroke = 4f))
        assertNull(cameraProgressArc(Rect(0, 0, 1000, 48), null, albumDiameter = 54f, stroke = 4f))
    }
}
