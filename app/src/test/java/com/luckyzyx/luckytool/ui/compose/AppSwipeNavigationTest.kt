package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.UiMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class, qualifiers = "w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSwipeNavigationTest {
    @Test
    fun `theme and scope routes cancel short swipes and complete a full swipe exactly once`() {
        Host().use { h ->
            for (route in listOf<AppRoute>(ThemeRoute, ScopeRoute("quick_entry"))) {
                h.nav.navigatePage(route)
                h.ui.advance()
                h.ui.drag(24f, 800f, 150f, 800f)
                assertEquals(listOf(MainRoute, route), h.nav.routes())
                h.ui.drag(24f, 800f, 750f, 800f)
                assertEquals(listOf(MainRoute), h.nav.routes())
            }
        }
    }

    @Test
    fun `turning off swipe updates an existing entry without disabling system back`() {
        Host().use { h ->
            h.nav.navigatePage(ThemeRoute)
            h.ui.advance()
            h.swipeEnabled = false
            h.ui.advance()
            h.ui.drag(24f, 800f, 750f, 800f)
            assertEquals(listOf(MainRoute, ThemeRoute), h.nav.routes())

            val input = DirectNavigationEventInput()
            h.ui.activity.navigationEventDispatcher.addInput(input)
            input.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0f, touchY = 800f))
            input.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.8f, touchY = 800f))
            h.ui.advance(64)
            input.backCompleted()
            h.ui.advance()
            assertEquals(listOf(MainRoute), h.nav.routes())
            h.ui.activity.navigationEventDispatcher.removeInput(input)

            h.nav.navigatePage(ThemeRoute)
            h.ui.advance()
            h.swipeEnabled = true
            h.ui.advance()
            h.ui.drag(24f, 800f, 750f, 800f)
            assertEquals(listOf(MainRoute), h.nav.routes())
        }
    }

    @Test
    fun `RTL reverses swipe direction and returns to the source main tab`() {
        Host().use { h ->
            h.direction = LayoutDirection.Rtl
            h.mainState.animateToPage(0)
            h.ui.advance()
            h.nav.navigatePage(ScopeRoute("quick_entry"))
            h.ui.advance()
            // 从左侧往右拖在 RTL 下是「前进方向」，不返回
            h.ui.drag(24f, 800f, 750f, 800f)
            assertEquals(2, h.nav.routes().size)
            h.ui.drag(798f, 800f, 72f, 800f)
            assertEquals(listOf(MainRoute), h.nav.routes())
            assertEquals(0, h.mainState.pagerState.settledPage)
            val input = DirectNavigationEventInput()
            h.ui.activity.navigationEventDispatcher.addInput(input)
            input.backCompleted()
            h.ui.advance()
            assertEquals(2, h.mainState.pagerState.settledPage)
            assertEquals(listOf(MainRoute), h.nav.routes())
            h.ui.activity.navigationEventDispatcher.removeInput(input)
        }
    }

    private class Host : AutoCloseable {
        val ui = ComposeGestureHost()
        lateinit var nav: NavHostController
        lateinit var mainState: MainPagerState
        var swipeEnabled by mutableStateOf(true)
        var direction by mutableStateOf(LayoutDirection.Ltr)

        init {
            ui.setContent {
                CompositionLocalProvider(
                    LocalEnableSwipeDismiss provides swipeEnabled,
                    LocalLayoutDirection provides direction,
                ) {
                    AppTheme(AppSettings(colorMode = ColorMode.LIGHT, dynamicColor = false), uiMode = UiMode.Material) {
                        val navigator = rememberNavController()
                        val pager = rememberPagerState(initialPage = 2) { MainTab.entries.size }
                        val state = rememberMainPagerState(pager)
                        val atMain = navigator.currentDestination?.hasRoute<MainRoute>() == true
                        SideEffect { nav = navigator; mainState = state }
                        MainPagerBackHandler(state, enabled = atMain)
                        AppNavHost(
                            navController = navigator,
                            modifier = Modifier.fillMaxSize(),
                            mainContent = {
                                MainTabs(state, isActive = atMain) { _, _ ->
                                    Box(Modifier.fillMaxSize())
                                }
                            },
                            themeContent = { Box(Modifier.fillMaxSize()) },
                            scopeContent = { Box(Modifier.fillMaxSize()) },
                        )
                    }
                }
            }
            ui.advance()
        }

        override fun close() = ui.close()
    }

    private fun NavController.routes(): List<AppRoute> = currentBackStack.value.mapNotNull { entry ->
        when {
            entry.destination.hasRoute<MainRoute>() -> MainRoute
            entry.destination.hasRoute<ThemeRoute>() -> ThemeRoute
            entry.destination.hasRoute<ScopeRoute>() -> entry.toRoute<ScopeRoute>()
            else -> null
        }
    }
}
