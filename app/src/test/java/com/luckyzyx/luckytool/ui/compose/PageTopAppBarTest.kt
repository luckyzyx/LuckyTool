package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 32, 33, 34, 35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageTopAppBarTest {
    @Test
    fun `function list collapses title and restores it only after scrolling to the top`() {
        checkScrolling(UiMode.Coui, scopeList = false)
    }

    @Test
    fun `shared settings list drives the native title through overscroll`() {
        checkScrolling(UiMode.Coui, scopeList = true)
    }

    @Test
    fun `material function list uses the native expressive collapse behavior`() {
        checkScrolling(UiMode.Material, scopeList = false)
    }

    @Test
    fun `material shared settings list collapses and restores its title`() {
        checkScrolling(UiMode.Material, scopeList = true)
    }

    private fun checkScrolling(mode: UiMode, scopeList: Boolean) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup().visible()
        val activity = controller.get()
        val view = ComposeView(activity)
        val mainLooper = shadowOf(Looper.getMainLooper())
        var topInset = 0f
        val name = "${mode.name.lowercase()}-${if (scopeList) "settings" else "function"}"
        val rows = List(40) { FunctionRow("row-$it", "应用 ${it + 1}", "android", "android") }
        val prefs = PrefState.of(activity, "top-app-bar-test")
        activity.setContentView(view)
        view.setContent {
            CompositionLocalProvider(LocalUiMode provides mode, LocalDensity provides Density(2f)) {
                AppTheme(AppSettings(colorMode = ColorMode.LIGHT, dynamicColor = false), uiMode = mode) {
                    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                        PageScaffold(
                            title = if (scopeList) "系统设置" else "功能",
                            onBack = if (scopeList) ({}) else null,
                        ) { padding ->
                            val top = padding.calculateTopPadding().value
                            SideEffect { topInset = top }
                            if (scopeList) {
                                CompositionLocalProvider(LocalScopeTopInset provides padding.calculateTopPadding()) {
                                    val listModifier = if (mode == UiMode.Coui) {
                                        Modifier.fillMaxSize()
                                    } else {
                                        Modifier.fillMaxSize().padding(padding)
                                    }
                                    ScopeScreen(state = prefs, modifier = listModifier) {
                                        repeat(40) { index ->
                                            click(title = "设置 ${index + 1}", summary = "系统功能设置", onClick = {})
                                        }
                                    }
                                }
                            } else {
                                FunctionList(rows, padding, onOpenPage = { _, _ -> })
                            }
                        }
                    }
                }
            }
        }
        fun settle() {
            mainLooper.idleFor(1, TimeUnit.SECONDS)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(822, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1760, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, 822, 1760)
            mainLooper.idleFor(1, TimeUnit.SECONDS)
        }
        fun snapshot(state: String) {
            val output = System.getProperty("header.preview.output") ?: return
            val file = File(output, "$name-$state.png").apply { parentFile?.mkdirs() }
            val bitmap = Bitmap.createBitmap(822, 1760, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        fun swipe(fromY: Float, toY: Float) {
            val downTime = SystemClock.uptimeMillis()
            fun send(action: Int, y: Float) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 400f, y, 0)
                view.dispatchTouchEvent(event)
                event.recycle()
                mainLooper.idleFor(20, TimeUnit.MILLISECONDS)
            }
            send(MotionEvent.ACTION_DOWN, fromY)
            for (step in 1..20) send(MotionEvent.ACTION_MOVE, fromY + (toY - fromY) * step / 20f)
            // End without extra fling velocity so each assertion observes the settled scroll position.
            mainLooper.idleFor(300, TimeUnit.MILLISECONDS)
            send(MotionEvent.ACTION_UP, toY)
            settle()
        }
        try {
            settle()
            val expanded = topInset
            assertTrue("Header must be measured", expanded > 0)
            snapshot("expanded")
            swipe(1300f, 450f)
            val collapsed = topInset
            snapshot("collapsed")
            assertTrue("Native title should collapse: $expanded -> $collapsed", collapsed < expanded - 30f)
            swipe(450f, 650f)
            assertEquals("Scrolling down mid-list must keep the title collapsed", collapsed, topInset, 1f)
            swipe(400f, 1650f)
            snapshot("restored")
            assertEquals("Reaching the top restores the large title", expanded, topInset, 1f)
        } finally {
            controller.pause().stop().destroy()
            mainLooper.idle()
        }
    }
}
