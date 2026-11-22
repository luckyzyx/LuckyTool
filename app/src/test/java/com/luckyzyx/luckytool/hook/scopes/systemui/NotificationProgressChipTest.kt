package com.luckyzyx.luckytool.hook.scopes.systemui

import android.animation.ValueAnimator
import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.View.MeasureSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationProgressChipTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun onlyTracksValidActiveProgress() {
        assertEquals(.4f, requireNotNull(task("a:1", "a", 80, 200).fraction), .0001f)
        assertEquals(0f, requireNotNull(task("a:1", "a", 0).fraction), .0001f)
        assertNull(NotificationProgressTask.from("a:1", "a", notification(100)))
        assertNull(NotificationProgressTask.from("a:1", "a", notification(0, max = 0)))
        assertNull(indeterminateTask("a:1", "a").fraction)
        assertNull(NotificationProgressTask.from("a:1", "a", notification(60).apply {
            flags = flags or Notification.FLAG_GROUP_SUMMARY
        }))
    }

    @Test
    fun keepsOneAppAndItsTaskOrderUntilThatAppFinishes() {
        val selection = NotificationProgressSelection()
        val first = indeterminateTask("a:1", "a")
        val second = task("a:2", "a", 58)
        val third = task("b:1", "b", 86)
        assertEquals(listOf(first, second), selection.update(listOf(first, second, third)))
        val updated = task("a:1", "a", 55)
        assertEquals(listOf(updated, second), selection.update(listOf(third, second, updated)))
        assertEquals(listOf(second), selection.update(listOf(third, second)))
        assertEquals(listOf(third), selection.update(listOf(third)))
    }

    @Test
    fun replacesOnlyTheChosenSlotAndRestoresStockInteraction() {
        val primary = View(context).apply {
            focusable = View.NOT_FOCUSABLE
            stateDescription = "Stock state"
        }
        val duplicate = View(context)
        val other = View(context)
        var clicked: View? = null
        val slot = NotificationProgressSlot().apply { onClick = { clicked = it } }

        slot.update(primary, 36, setOf(duplicate))
        assertEquals(36, slot.widthFor(primary))
        assertEquals(0, slot.widthFor(duplicate))
        assertNull(slot.widthFor(other))
        slot.describe("50% complete")
        assertEquals("50% complete", primary.stateDescription)
        assertTrue(primary.performClick())
        assertEquals(primary, clicked)

        var stockClicks = 0
        other.setOnClickListener { stockClicks++ }
        slot.update(other, 28)
        assertNull(slot.widthFor(primary))
        assertNull(slot.widthFor(duplicate))
        assertFalse(primary.isClickable)
        assertEquals(View.NOT_FOCUSABLE, primary.focusable)
        assertEquals("Stock state", primary.stateDescription)
        slot.update(null)
        assertNull(slot.widthFor(other))
        assertTrue(other.performClick())
        assertEquals(1, stockClicks)
    }

    @Test
    @Config(qualifiers = "mdpi")
    fun alignsBarsToVisibleArtworkWithATwoDpGap() {
        val themed = ContextThemeWrapper(context, android.R.style.Theme_Material_Light)
        val accent = themeAccent(themed)
        assertTrue(Color.alpha(accent) > 0)
        val paddedIcon = object : ColorDrawable(Color.MAGENTA) {
            private val paint = Paint().apply { color = Color.MAGENTA }
            override fun draw(canvas: Canvas) {
                canvas.drawRect(bounds.left + 1f, bounds.top + 3f, bounds.right - 3f, bounds.bottom - 1f, paint)
            }
        }
        shadowOf(themed.packageManager).setApplicationIcon(themed.packageName, paddedIcon)
        val chip = NotificationProgressChip(themed)
        chip.show(listOf(task("a:1", themed.packageName, 50)), "50% complete")
        chip.applyTint(Color.BLACK)
        val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        chip.measure(unspecified, MeasureSpec.makeMeasureSpec(24, MeasureSpec.EXACTLY))
        chip.layout(0, 0, chip.measuredWidth, chip.measuredHeight)

        val bitmap = Bitmap.createBitmap(chip.width, chip.height, Bitmap.Config.ARGB_8888)
        chip.draw(Canvas(bitmap))
        val iconRows = (0 until bitmap.height).filter { bitmap.getPixel(12, it) == Color.MAGENTA }
        assertEquals((7..18).toList(), iconRows)
        val pillColor = bitmap.getPixel(18, 3)
        assertTrue(Color.alpha(pillColor) > 0)
        assertEquals(Color.MAGENTA, bitmap.getPixel(16, 10))
        assertEquals(pillColor, bitmap.getPixel(17, 10))
        assertEquals(pillColor, bitmap.getPixel(18, 10))
        assertTrue(Color.alpha(bitmap.getPixel(19, 10)) > Color.alpha(pillColor))
        assertEquals(iconRows, (0 until bitmap.height).filter {
            Color.alpha(bitmap.getPixel(20, it)) > Color.alpha(pillColor)
        })
        assertEquals((13..18).toList(), (0 until bitmap.height).filter { bitmap.getPixel(20, it) == accent })
        assertEquals((2..21).toList(), (0 until bitmap.height).filter {
            Color.alpha(bitmap.getPixel(18, it)) != 0
        })
        assertEquals(0, bitmap.getPixel(20, 10) and 0x00ffffff)
    }

    @Test
    @Config(qualifiers = "mdpi")
    fun animatesOnlyTheIndeterminateTrackAndRespectsDisabledAnimations() {
        val themed = ContextThemeWrapper(context, android.R.style.Theme_Material_Light)
        val accent = themeAccent(themed)
        shadowOf(themed.packageManager).setApplicationIcon(themed.packageName, ColorDrawable(Color.MAGENTA))
        val chip = NotificationProgressChip(themed)
        chip.show(listOf(task("a:1", themed.packageName, 50), indeterminateTask("a:2", themed.packageName)), "Two tasks")
        chip.applyTint(Color.BLACK)
        chip.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(24, MeasureSpec.EXACTLY))
        chip.layout(0, 0, chip.measuredWidth, chip.measuredHeight)
        val bitmap = Bitmap.createBitmap(chip.width, chip.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        fun frame(): List<Int> {
            bitmap.eraseColor(Color.TRANSPARENT)
            chip.draw(canvas)
            assertEquals((12..19).toList(), (0 until bitmap.height).filter { bitmap.getPixel(23, it) == accent })
            return (0 until bitmap.height).filter { bitmap.getPixel(28, it) == accent }
        }

        val durationScale = ValueAnimator.getDurationScale()
        try {
            setAnimationScale(1f)
            val frames = List(4) {
                frame().also { ShadowSystemClock.advanceBy(Duration.ofMillis(300)) }
            }
            assertTrue(frames.filter { it.isNotEmpty() }.distinct().size > 1)

            setAnimationScale(0f)
            val still = frame()
            assertTrue(still.isNotEmpty())
            ShadowSystemClock.advanceBy(Duration.ofMillis(300))
            assertEquals(still, frame())
        } finally {
            setAnimationScale(durationScale)
        }
    }

    private fun themeAccent(context: Context): Int {
        val colors = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))
        return try {
            colors.getColor(0, Color.TRANSPARENT)
        } finally {
            colors.recycle()
        }
    }

    private fun setAnimationScale(scale: Float) = ReflectionHelpers.callStaticMethod<Unit>(
        ValueAnimator::class.java, "setDurationScale", ReflectionHelpers.ClassParameter.from(Float::class.javaPrimitiveType, scale),
    )

    private fun notification(value: Int, max: Int = 100, indeterminate: Boolean = false) =
        Notification.Builder(context, "progress")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, value, indeterminate)
            .build()

    private fun task(key: String, app: String, value: Int, max: Int = 100) =
        requireNotNull(NotificationProgressTask.from(key, app, notification(value, max)))

    private fun indeterminateTask(key: String, app: String) =
        requireNotNull(NotificationProgressTask.from(key, app, notification(0, max = 0, indeterminate = true)))
}
