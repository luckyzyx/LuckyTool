package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppNavigationTest {
    private val activities = mutableListOf<ActivityController<ComponentActivity>>()

    @After
    fun tearDown() {
        activities.forEach { it.pause().stop().destroy() }
        drainFrames()
    }

    @Test
    fun `all destinations are serializable as AppRoute`() {
        val routes: List<AppRoute> = listOf(
            MainRoute,
            ThemeRoute,
            ScopeRoute("statusbar", "状态栏", "clock_style", 12),
        )
        assertEquals(routes, Json.decodeFromString<List<AppRoute>>(Json.encodeToString(routes)))
    }

    @Test
    fun `navigating to the page already on top keeps a single entry`() {
        val host = createHost()
        assertEquals(listOf<AppRoute>(MainRoute), host.nav.routes())

        val page = ScopeRoute("statusbar", "状态栏")
        host.nav.navigatePage(ThemeRoute)
        host.nav.navigatePage(page)
        host.nav.navigatePage(page)
        drainFrames()
        assertEquals(listOf(MainRoute, ThemeRoute, page), host.nav.routes())

        host.nav.popBackStack()
        drainFrames()
        assertEquals(listOf(MainRoute, ThemeRoute), host.nav.routes())
        host.nav.popBackStack()
        drainFrames()
        assertEquals(listOf(MainRoute), host.nav.routes())
        // 根目的地不出栈：再次返回只会让系统退出应用
        assertEquals(false, host.nav.popBackStack())
    }

    @Test
    fun `navigating to a different scope page pushes instead of replacing the top`() {
        val host = createHost()
        val category = ScopeRoute("statusbar", "状态栏相关")
        val page = ScopeRoute("statusbar_icon", "状态栏图标")
        host.nav.navigatePage(category)
        host.nav.navigatePage(page)
        drainFrames()
        // 所有 ScopeRoute 共用一个目的地，singleTop 若只按目的地类判断会把分类页原地替换
        assertEquals(listOf(MainRoute, category, page), host.nav.routes())

        host.nav.popBackStack()
        drainFrames()
        // 返回应先回到分类页，而不是直接跳回主界面
        assertEquals(listOf(MainRoute, category), host.nav.routes())
    }

    @Test
    fun `activity recreation returns to the same page with its arguments`() {
        val host = createHost()
        val target = ScopeRoute("statusbar", "状态栏", "clock_style", 12)
        host.nav.navigatePage(ThemeRoute)
        host.nav.navigatePage(target)
        drainFrames()
        assertEquals(listOf(MainRoute, ThemeRoute, target), host.nav.routes())

        val savedState = Bundle()
        host.controller.pause().saveInstanceState(savedState).stop().destroy()
        activities.remove(host.controller)
        drainFrames()

        // 页面栈由 NavHost 自己保存：重建后仍是同一页面，路由参数原样带回
        val restored = createHost(savedState)
        assertTrue(restored.nav !== host.nav)
        assertEquals(listOf(MainRoute, ThemeRoute, target), restored.nav.routes())
    }

    private fun createHost(savedState: Bundle? = null): Host {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java)
            .create(savedState).start().resume().visible()
        activities += controller
        val host = Host()
        host.controller = controller
        controller.get().setContent {
            val nav = rememberNavController()
            SideEffect { host.nav = nav }
            AppNavHost(
                navController = nav,
                modifier = Modifier.fillMaxSize(),
                mainContent = { host.Page() },
                themeContent = { host.Page() },
                scopeContent = { host.Page() },
            )
        }
        drainFrames()
        return host
    }

    private class Host {
        lateinit var nav: NavHostController
        lateinit var controller: ActivityController<ComponentActivity>

        @androidx.compose.runtime.Composable
        fun Page() {
            Box(Modifier.fillMaxSize())
        }
    }

    /** 把返回栈读回成路由列表：目的地是类型安全路由，按具体类型还原参数 */
    private fun NavController.routes(): List<AppRoute> = currentBackStack.value.mapNotNull { entry ->
        when {
            entry.destination.hasRoute<MainRoute>() -> MainRoute
            entry.destination.hasRoute<ThemeRoute>() -> ThemeRoute
            entry.destination.hasRoute<ScopeRoute>() -> entry.toRoute<ScopeRoute>()
            else -> null
        }
    }

    private fun drainFrames() {
        Snapshot.sendApplyNotifications()
        shadowOf(Looper.getMainLooper()).idleFor(2, TimeUnit.SECONDS)
    }
}
