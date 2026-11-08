package com.luckyzyx.luckytool.ui.shell

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.appPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class)
class ShellSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = context.appPrefs(SettingsPrefs)

    @Before
    fun clearPreferences() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `new installs use the classic bar and legacy glass selects iOS without changing global blur`() {
        assertEquals(FloatingBottomBarStyle.Classic, ShellSettingsController.get(context).floatingBottomBarStyle)
        prefs.edit().putBoolean(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_BLUR, true).commit()
        assertEquals(FloatingBottomBarStyle.IosLike, ShellSettingsController.get(context).floatingBottomBarStyle)
        assertFalse(ShellSettingsController.get(context).enableBlur)
        prefs.edit().putBoolean(ShellSettingsController.KEY_ENABLE_BLUR, true).commit()
        assertTrue(ShellSettingsController.get(context).enableBlur)
        assertFalse("Reading preferences must not rewrite them", prefs.contains(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE))
    }

    @Test
    fun `explicit style takes precedence over the old liquid glass switch and legacy miuix value maps to classic`() {
        prefs.edit().putBoolean(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_BLUR, true)
            .putString(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE, "miuix").commit()
        assertEquals(FloatingBottomBarStyle.Classic, ShellSettingsController.get(context).floatingBottomBarStyle)
        prefs.edit().putBoolean(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_BLUR, false)
            .putString(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE, "ios_like").commit()
        assertEquals(FloatingBottomBarStyle.IosLike, ShellSettingsController.get(context).floatingBottomBarStyle)
    }

    @Test
    fun `unknown stored style falls back to the classic bar`() {
        prefs.edit().putString(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE, "unknown")
            .putBoolean(ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_BLUR, true).commit()
        assertEquals(FloatingBottomBarStyle.Classic, ShellSettingsController.get(context).floatingBottomBarStyle)
    }
}
