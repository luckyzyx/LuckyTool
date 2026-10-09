package com.luckyzyx.luckytool.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.theme.ColorMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 锁定 `dark_theme` 的存量取值表，以及「平台 DayNight 主题必须与 Compose 侧 ColorMode 同明暗」。
 *
 * 回归背景：Compose 迁移时把 1/2 的语义写反（1 当成浅色、2 当成深色），而
 * [ThemeUtils.initTheme] 一直按旧版「1 深色 / 2 浅色」设置平台主题，于是选深色时
 * Compose 界面是深色、平台主题却是浅色，矢量图的 `?attr/colorControlNormal` 解析成
 * 黑色 → 深色模式下菜单图标不可见。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `stored values keep the legacy order`() {
        // 与旧版 res/values/arrays.xml 的 dark_theme 顺序一致：0 跟随系统 / 1 深色 / 2 浅色 / 3 深色 AMOLED
        assertEquals(ColorMode.SYSTEM, ColorMode.fromValue(0))
        assertEquals(ColorMode.DARK, ColorMode.fromValue(1))
        assertEquals(ColorMode.LIGHT, ColorMode.fromValue(2))
        assertEquals(ColorMode.DARK_AMOLED, ColorMode.fromValue(3))
        // 4-7 为 Monet 变体，规则为「非 Monet 值 + 4」
        assertEquals(ColorMode.MONET_SYSTEM, ColorMode.fromValue(4))
        assertEquals(ColorMode.MONET_DARK, ColorMode.fromValue(5))
        assertEquals(ColorMode.MONET_LIGHT, ColorMode.fromValue(6))
        assertEquals(ColorMode.MONET_DARK_AMOLED, ColorMode.fromValue(7))
    }

    @Test
    fun `platform night mode agrees with the compose color mode`() {
        for (value in 0..7) {
            val mode = ColorMode.fromValue(value)
            val nightMode = ThemeUtils.nightModeFor(value.toString())
            val expected = when {
                mode.isSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                mode.isDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
            assertEquals("dark_theme=$value ($mode)", expected, nightMode)
        }
    }

    @Test
    fun `unknown or missing stored value falls back to follow system`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeUtils.nightModeFor(""))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeUtils.nightModeFor("abc"))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeUtils.nightModeFor("99"))
    }

    @Test
    fun `initTheme applies the stored dark mode to the platform theme`() {
        // 选「浅色(2)」→ 平台主题必须是浅色，选「深色(1)」→ 必须是深色
        context.putString(SettingsPrefs, "dark_theme", "2")
        ThemeUtils.initTheme(context)
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())

        context.putString(SettingsPrefs, "dark_theme", "1")
        ThemeUtils.initTheme(context)
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.getDefaultNightMode())
    }
}
