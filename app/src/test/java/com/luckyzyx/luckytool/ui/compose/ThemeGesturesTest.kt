package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import android.os.Bundle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.shell.FloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.LocalFloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.shell.LocalPagerInterceptionMode
import com.luckyzyx.luckytool.ui.shell.PagerModeCrossAxis
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.appPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThemeGesturesTest {
    @Test
    // 需要「低于预测性返回 API」的系统环境，返回手势才以开关呈现而不是系统提示。
    @Config(sdk = [32])
    fun `both appearances expose working gesture preferences below the predictive back API level`() {
        for (mode in UiMode.entries) {
            for (color in listOf(ColorMode.LIGHT, ColorMode.DARK)) {
                ThemeHost(mode, color).use { h ->
                    assertTrue(h.swipe)
                    assertEquals(PagerModeCrossAxis, h.pagerMode)
                    h.scrollTo("横移返回手势")
                    assertNull(h.ui.textNode("预测性返回手势"))
                    h.ui.clickText("横移返回手势")
                    h.ui.advance()
                    assertFalse(h.swipe)
                    assertFalse(ShellSettingsController.get(h.ui.activity).swipeDismiss)
                    h.scrollTo("页面切换手势")
                    h.ui.snapshot("theme-${mode.name.lowercase()}-${color.name.lowercase()}")
                    for ((index, label) in listOf(2 to "iOS 风格", 0 to "默认", 1 to "跨轴拦截")) {
                        h.ui.clickText("页面切换手势")
                        h.ui.advance()
                        h.ui.snapshot("theme-${mode.name.lowercase()}-${color.name.lowercase()}-menu")
                        h.ui.clickText(label)
                        h.ui.advance()
                        assertEquals(index, h.pagerMode)
                        assertEquals(index, ShellSettingsController.get(h.ui.activity).pagerInterceptionMode)
                    }
                    assertFalse("These preferences must not recreate the activity", h.ui.activity.isChangingConfigurations)
                }
            }
        }
    }

    @Test
    @Config(sdk = [35])
    fun `supported Android shows predictive back before swipe and pager mode`() {
        for (mode in UiMode.entries) {
            ThemeHost(mode, ColorMode.LIGHT).use { h ->
                h.scrollTo("预测性返回手势")
                assertNotNull(h.ui.textNode("横移返回手势"))
                assertNotNull(h.ui.textNode("页面切换手势"))
                val predictiveY = h.ui.textNode("预测性返回手势")!!.boundsInRoot.top
                val swipeY = h.ui.textNode("横移返回手势")!!.boundsInRoot.top
                val pagerY = h.ui.textNode("页面切换手势")!!.boundsInRoot.top
                assertTrue(predictiveY < swipeY && swipeY < pagerY)
            }
        }
    }

    @Test
    fun `gesture choices persist through activity recreation`() {
        for (mode in listOf(UiMode.Material)) {
            val saved: Bundle
            ThemeHost(mode, ColorMode.LIGHT).use { h ->
                h.scrollTo("横移返回手势")
                h.ui.clickText("横移返回手势")
                h.ui.advance()
                h.scrollTo("页面切换手势")
                h.ui.clickText("页面切换手势")
                h.ui.advance()
                h.ui.clickText("iOS 风格")
                h.ui.advance()
                saved = h.ui.saveAndClose()
            }
            ThemeHost(mode, ColorMode.LIGHT, saved).use { h ->
                assertFalse(h.swipe)
                assertEquals(2, h.pagerMode)
                h.scrollTo("页面切换手势")
                assertNotNull(h.ui.textNode("iOS 风格"))
            }
        }
    }

    @Test
    fun `floating styles are revealed by the switch and persist with independent global blur`() {
        val saved: Bundle
        ThemeHost(UiMode.Material, ColorMode.LIGHT).use { h ->
            h.scrollTo("悬浮底栏")
            assertNull(h.ui.textNode("悬浮底栏样式"))
            h.ui.clickText("悬浮底栏")
            h.ui.advance()
            h.scrollTo("悬浮底栏样式")
            for ((style, label) in listOf(
                FloatingBottomBarStyle.IosLike to "iOS 风格（液态玻璃）",
                FloatingBottomBarStyle.Classic to "经典",
                FloatingBottomBarStyle.IosLike to "iOS 风格（液态玻璃）",
            )) {
                h.ui.clickText("悬浮底栏样式")
                h.ui.advance()
                h.ui.snapshot("theme-floating-style-menu")
                h.ui.clickText(label)
                h.ui.advance()
                assertEquals(style, h.floatingStyle)
                assertEquals(style, ShellSettingsController.get(h.ui.activity).floatingBottomBarStyle)
                assertFalse("Style choice must not turn on global blur", h.blur)
            }
            h.ui.clickText("模糊")
            h.ui.advance()
            assertTrue(h.blur)
            assertEquals(FloatingBottomBarStyle.IosLike, h.floatingStyle)
            h.ui.snapshot("theme-floating-style-selected")
            saved = h.ui.saveAndClose()
        }
        ThemeHost(UiMode.Material, ColorMode.LIGHT, saved).use { h ->
            assertEquals(FloatingBottomBarStyle.IosLike, h.floatingStyle)
            assertTrue(h.blur)
            assertTrue(ShellSettingsController.get(h.ui.activity).enableFloatingBottomBar)
        }
    }

    @Test
    fun `coui owns its bottom navigation so the floating bar settings are hidden`() {
        ThemeHost(UiMode.Coui, ColorMode.LIGHT).use { h ->
            repeat(8) { h.ui.drag(420f, 1400f, 420f, 650f) }
            assertNull(h.ui.textNode("悬浮底栏"))
            assertNull(h.ui.textNode("悬浮底栏样式"))
            h.ui.snapshot("theme-coui-without-floating-bar")
        }
    }

    private class ThemeHost(mode: UiMode, color: ColorMode, saved: Bundle? = null) : AutoCloseable {
        val ui = ComposeGestureHost(saved)
        var swipe = true
        var pagerMode = -1
        var floatingStyle = FloatingBottomBarStyle.Classic
        var blur = false

        init {
            if (saved == null) {
                ui.activity.appPrefs(SettingsPrefs).edit().clear()
                    .putString(ThemeController.KEY_UI_MODE, mode.value)
                    .putString("dark_theme", color.value.toString())
                    .putBoolean("use_dynamic_color", false)
                    .commit()
            }
            ui.setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f)) {
                    AppTheme {
                        val enabled = LocalEnableSwipeDismiss.current
                        val interceptionMode = LocalPagerInterceptionMode.current
                        val style = LocalFloatingBottomBarStyle.current
                        val blurEnabled = LocalEnableBlur.current
                        SideEffect {
                            swipe = enabled
                            pagerMode = interceptionMode
                            floatingStyle = style
                            blur = blurEnabled
                        }
                        androidx.compose.material3.Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
                            ThemeScreen(onBack = {})
                        }
                    }
                }
            }
            ui.advance()
        }

        fun scrollTo(text: String) {
            repeat(8) {
                val node = ui.textNode(text)
                if (node != null && node.boundsInRoot.top in 250f..1200f) return
                ui.drag(420f, 1400f, 420f, 650f)
            }
            check(ui.textNode(text) != null) { "Could not scroll to $text" }
        }

        override fun close() = ui.close()
    }
}
