package com.luckyzyx.luckytool.ui.compose

import android.app.Activity
import android.app.Application
import android.content.Intent
import com.luckyzyx.luckytool.ui.activity.PageActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppNavigationTest {
    @Test
    fun `activity intents retain destinations and search position when copied for restoration`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            for (route in listOf<AppRoute>(ThemeRoute, ScopeRoute("statusbar", "状态栏", "clock_style", 12))) {
                val intent = PageActivity.createIntent(controller.get(), route)
                assertEquals(PageActivity::class.java.name, intent.component?.className)
                assertEquals(route, PageActivity.readRoute(Intent(intent)))
                assertEquals(0, intent.flags)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `missing or invalid destinations are rejected`() {
        assertNull(PageActivity.readRoute(Intent()))
        assertNull(PageActivity.readRoute(Intent().putExtra("com.luckyzyx.luckytool.PAGE_ROUTE", "invalid")))
    }
}
