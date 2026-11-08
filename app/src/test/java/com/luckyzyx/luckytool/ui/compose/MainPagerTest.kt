package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import com.luckyzyx.luckytool.ui.compose.components.MainPageActivationEffect
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.shell.FloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBar
import com.luckyzyx.luckytool.ui.shell.LocalFloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.LocalPagerInterceptionMode
import com.luckyzyx.luckytool.ui.shell.PagerModeCrossAxis
import com.luckyzyx.luckytool.ui.shell.PagerModeIosLike
import com.luckyzyx.luckytool.ui.shell.PagerModeNative
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.UiMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainPagerTest {
    @Test
    fun `all bottom bars coordinate rapid navigation without an obsolete target winning`() {
        for ((mode, floating, style) in listOf(
            Triple(UiMode.Material, false, FloatingBottomBarStyle.Classic),
            Triple(UiMode.Material, true, FloatingBottomBarStyle.Classic),
            Triple(UiMode.Material, true, FloatingBottomBarStyle.IosLike),
            Triple(UiMode.Coui, false, FloatingBottomBarStyle.Classic),
            Triple(UiMode.Coui, true, FloatingBottomBarStyle.Classic),
            Triple(UiMode.Coui, true, FloatingBottomBarStyle.IosLike),
        )) {
            PagerHost(mode, floating, floatingStyle = style).use { h ->
                assertEquals(2, h.state.pagerState.settledPage)
                h.ui.clickLabel("设置")
                assertEquals(4, h.state.selectedPage)
                h.ui.advance(80)
                h.ui.clickLabel("其他")
                assertEquals(0, h.state.selectedPage)
                h.ui.advance(80)
                h.ui.clickLabel("功能")
                h.ui.advance()
                h.assertSettled(1)
                h.ui.clickLabel("功能")
                h.ui.advance()
                h.assertSettled(1)
                h.ui.snapshot("${mode.name.lowercase()}-${if (floating) style.name else "bottom"}")
            }
        }
    }

    @Test
    fun `dragging takes over an in-flight navigation and a cancelled short drag restores the page`() {
        PagerHost().use { h ->
            h.state.animateToPage(4)
            h.ui.advance(80)
            h.ui.drag(180f, 800f, 740f, 800f)
            assertTrue("Gesture must interrupt the old destination", h.state.pagerState.settledPage < 4)
            h.assertSettled(h.state.pagerState.settledPage)
            val before = h.state.pagerState.settledPage
            h.ui.drag(600f, 800f, 520f, 800f, cancel = true)
            h.assertSettled(before)
        }
    }

    @Test
    @Config(sdk = [35])
    fun `liquid glass bottom bar shares the same pager navigation`() {
        PagerHost(UiMode.Coui, floating = true, floatingStyle = FloatingBottomBarStyle.IosLike, blur = true).use { h ->
            h.ui.clickLabel("其他")
            h.ui.advance()
            h.assertSettled(0)
            h.ui.drag(700f, 800f, 120f, 800f)
            h.assertSettled(1)
            h.ui.clickLabel("设置")
            h.ui.advance()
            h.assertSettled(4)
        }
    }

    @Test
    fun `all gesture modes allow resting horizontal paging and leave vertical scroll to the list`() {
        for (mode in listOf(PagerModeNative, PagerModeCrossAxis, PagerModeIosLike)) {
            PagerHost().use { h ->
                h.gestureMode = mode
                h.ui.advance()
                h.ui.drag(410f, 300f, 410f, 300f)
                assertEquals("Paging must not consume row taps", 1, h.clicks[MainTab.Home])
                h.ui.drag(410f, 1300f, 410f, 450f)
                h.assertSettled(2)
                assertTrue(h.lists.getValue(MainTab.Home).firstVisibleItemIndex > 0)
                h.ui.drag(700f, 800f, 120f, 800f)
                h.assertSettled(3)
                h.ui.drag(120f, 800f, 700f, 800f)
                h.assertSettled(2)
                assertTrue("Returning must preserve the list position", h.lists.getValue(MainTab.Home).firstVisibleItemIndex > 0)
            }
        }
    }

    @Test
    fun `cross-axis interrupts list momentum while iOS mode requires a second horizontal swipe`() {
        for (appearance in UiMode.entries) {
            for (mode in listOf(PagerModeCrossAxis, PagerModeIosLike)) {
                PagerHost(appearance).use { h ->
                    h.gestureMode = mode
                    h.ui.advance()
                    h.ui.drag(410f, 1400f, 410f, 350f, holdBeforeUp = false, settle = false)
                    assertTrue("The scenario must start with list momentum", h.lists.getValue(MainTab.Home).isScrollInProgress)
                    h.ui.drag(700f, 800f, 120f, 800f)
                    if (mode == PagerModeCrossAxis) {
                        h.assertSettled(3)
                    } else {
                        h.assertSettled(2)
                        assertFalse(h.lists.getValue(MainTab.Home).isScrollInProgress)
                        h.ui.drag(700f, 800f, 120f, 800f)
                        h.assertSettled(3)
                    }
                }
            }
        }
    }

    @Test
    fun `cross-axis can change pages while a Coui list rebounds at its top edge`() {
        PagerHost(UiMode.Coui).use { h ->
            h.ui.drag(410f, 450f, 410f, 1300f, holdBeforeUp = false, settle = false)
            h.ui.drag(700f, 800f, 120f, 800f)
            h.assertSettled(3)
        }
    }

    @Test
    fun `preloaded pages load only on activation and only the current page refreshes on resume`() {
        PagerHost().use { h ->
            assertEquals(5, h.instances.size)
            assertEquals(listOf(0, 0, 1, 0, 0), MainTab.entries.map { h.loads[it] ?: 0 })
            val initialInstances = h.instances.toMap()
            h.ui.clickLabel("其他")
            h.ui.advance()
            assertEquals(1, h.loads[MainTab.Other])
            h.ui.clickLabel("主页")
            h.ui.advance()
            assertEquals(1, h.loads[MainTab.Home])
            MainTab.entries.forEach { assertSame(initialInstances[it], h.instances[it]) }
            h.ui.controller.pause().stop().start().resume()
            h.ui.advance()
            assertEquals(1, h.resumes[MainTab.Home])
            assertEquals(0, h.resumes[MainTab.Other] ?: 0)
        }
    }

    @Test
    fun `theme changes and recreation retain the settled page and scroll position`() {
        val saved: Bundle
        val position: Int
        PagerHost(withHeaders = true).use { h ->
            h.ui.clickLabel("设置")
            h.ui.advance()
            h.ui.drag(410f, 1300f, 410f, 450f)
            position = h.lists.getValue(MainTab.Setting).firstVisibleItemIndex
            assertTrue(position > 0)
            for (appearance in listOf(UiMode.Coui, UiMode.Material, UiMode.Coui)) {
                h.mode = appearance
                h.floating = appearance == UiMode.Material
                h.ui.advance()
                h.assertSettled(4)
                assertEquals(position, h.lists.getValue(MainTab.Setting).firstVisibleItemIndex)
                assertTrue(
                    "Switching appearance must keep a scrolled page's header collapsed",
                    h.topInsets.getValue(MainTab.Setting) < h.topInsets.getValue(MainTab.Home) - 30f,
                )
            }
            saved = h.ui.saveAndClose()
        }
        PagerHost(UiMode.Coui, floating = true, saved = saved, withHeaders = true).use { h ->
            h.assertSettled(4)
            assertEquals(position, h.lists.getValue(MainTab.Setting).firstVisibleItemIndex)
        }
    }

    @Test
    fun `collapsed headers remain independent across page changes and recreation`() {
        for (appearance in UiMode.entries) {
            val saved: Bundle
            val collapsed: Float
            PagerHost(appearance, withHeaders = true).use { h ->
                val expanded = h.topInsets.getValue(MainTab.Home)
                h.ui.drag(410f, 1300f, 410f, 450f)
                collapsed = h.topInsets.getValue(MainTab.Home)
                h.ui.snapshot("header-${appearance.name.lowercase()}-collapsed")
                assertTrue("$appearance header must collapse: $expanded -> $collapsed", collapsed < expanded - 30f)
                h.ui.clickLabel("设置")
                h.ui.advance()
                assertEquals(expanded, h.topInsets.getValue(MainTab.Setting), 1f)
                h.ui.clickLabel("主页")
                h.ui.advance()
                assertEquals(collapsed, h.topInsets.getValue(MainTab.Home), 1f)
                saved = h.ui.saveAndClose()
            }
            PagerHost(appearance, saved = saved, withHeaders = true).use { h ->
                assertEquals(collapsed, h.topInsets.getValue(MainTab.Home), 1f)
            }
        }
    }

    @Test
    fun `back returns to home while a blocking handler takes priority over paging`() {
        PagerHost().use { h ->
            h.ui.clickLabel("设置")
            h.ui.advance()
            val input = DirectNavigationEventInput()
            h.ui.activity.navigationEventDispatcher.addInput(input)
            h.active = false
            h.ui.advance()
            h.ui.drag(160f, 800f, 720f, 800f)
            h.ui.clickLabel("其他")
            h.ui.advance()
            input.backCompleted()
            h.ui.advance()
            assertEquals(1, h.blockedBacks)
            h.assertSettled(4)
            h.active = true
            h.ui.advance()
            input.backCompleted()
            h.ui.advance()
            h.assertSettled(2)
            h.ui.activity.navigationEventDispatcher.removeInput(input)
        }
    }

    @Test
    fun `floating bars keep five accessible targets at maximum scale on narrow screens`() {
        for ((appearance, style) in listOf(
            UiMode.Material to FloatingBottomBarStyle.Classic,
            UiMode.Material to FloatingBottomBarStyle.IosLike,
            UiMode.Coui to FloatingBottomBarStyle.Classic,
            UiMode.Coui to FloatingBottomBarStyle.IosLike,
        )) {
            for (direction in LayoutDirection.entries) {
                for (color in listOf(ColorMode.LIGHT, ColorMode.DARK)) {
                    PagerHost(
                        appearance, floating = true, floatingStyle = style,
                        width = 720, density = 2.2f, direction = direction, color = color,
                    ).use { h ->
                        val tabs = h.ui.nodes().filter { it.config.getOrNull(SemanticsProperties.Role) == Role.Tab &&
                            it.config.getOrNull(SemanticsActions.OnClick)?.action != null }
                        assertEquals(5, tabs.size)
                        for (tab in tabs) {
                            val bounds = tab.boundsInRoot
                            assertTrue("$style target width: $bounds", bounds.width >= 48 * 2.2f - 1)
                            assertTrue("$style target height: $bounds", bounds.height >= 48 * 2.2f - 1)
                            assertTrue("$style target clipped: $bounds", bounds.left >= 0 && bounds.right <= 720)
                        }
                        for (tab in MainTab.entries) {
                            val label = h.ui.activity.getString(tab.labelRes)
                            if (appearance == UiMode.Coui) {
                                // COUI 底栏始终显示页签标题，图标本身不带描述，所以按文字定位后点它的中心。
                                val title = h.ui.nodes().firstOrNull {
                                    it.boundsInRoot.top > 1200f &&
                                        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
                                }
                                assertNotNull("COUI navigation labels its tabs", title)
                                val at = requireNotNull(title).boundsInRoot.center
                                h.ui.drag(at.x, at.y, at.x, at.y)
                            } else {
                                assertTrue("Floating labels must be icon descriptions", h.ui.textNode(label) == null)
                                h.ui.clickLabel(label)
                            }
                            h.ui.advance()
                            h.assertSettled(tab.ordinal)
                            assertEquals(1, h.ui.nodes().count {
                                it.config.getOrNull(SemanticsProperties.Selected) == true &&
                                    it.config.getOrNull(SemanticsActions.OnClick)?.action != null
                            })
                        }
                        h.ui.snapshot("floating-${appearance.name}-${style.name}-${direction.name}-${color.name}-110")
                    }
                }
            }
        }
    }

    @Test
    fun `floating style and blur changes retain page state and badges`() {
        for (mode in listOf(UiMode.Material, UiMode.Coui)) {
            PagerHost(mode, floating = true, withHeaders = true).use { h ->
                h.ui.clickLabel("设置")
                h.ui.advance()
                h.ui.drag(410f, 1300f, 410f, 450f)
                val position = h.lists.getValue(MainTab.Setting).firstVisibleItemIndex
                val instances = h.instances.toMap()
                try {
                    // COUI 底栏是库自带的导航条，没有角标槽位，只验证外观切换不重建页面。
                    ShellBadgeState.updateAvailable = true
                    for (style in FloatingBottomBarStyle.entries) {
                        for (blur in listOf(true, false)) {
                            h.floatingStyle = style
                            h.blur = blur
                            h.ui.advance()
                            h.assertSettled(4)
                            assertEquals(position, h.lists.getValue(MainTab.Setting).firstVisibleItemIndex)
                            MainTab.entries.forEach { assertSame(instances[it], h.instances[it]) }
                            if (mode == UiMode.Material) {
                                assertTrue("Update badge must survive the style switch", h.ui.textNode("1") != null)
                            }
                        }
                    }
                    val input = DirectNavigationEventInput()
                    h.ui.activity.navigationEventDispatcher.addInput(input)
                    input.backCompleted()
                    h.ui.advance()
                    h.assertSettled(2)
                    h.ui.activity.navigationEventDispatcher.removeInput(input)
                } finally {
                    ShellBadgeState.updateAvailable = false
                }
            }
        }
    }
    @Test
    @Config(sdk = [31, 32, 35])
    fun `floating pointer taps and iOS drag selection work across effect support levels`() {
        for ((mode, style) in listOf(
            UiMode.Material to FloatingBottomBarStyle.Classic,
            UiMode.Material to FloatingBottomBarStyle.IosLike,
            UiMode.Coui to FloatingBottomBarStyle.Classic,
            UiMode.Coui to FloatingBottomBarStyle.IosLike,
        )) {
            PagerHost(mode, floating = true, floatingStyle = style, blur = true).use { h ->
                // COUI 页签只有标题文字，Material 悬浮底栏只有图标描述；页面上方的同名标题也要排除。
                fun center(label: String) = h.ui.nodes().first {
                    it.boundsInRoot.width > 0 && it.boundsInRoot.top > 1200f &&
                        (it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true ||
                            it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true)
                }.boundsInRoot.center
                val settings = center("设置")
                h.ui.drag(settings.x, settings.y, settings.x, settings.y)
                h.assertSettled(4)
                // 跨项拖拽选择是悬浮底栏自己的手势；COUI 底栏的滑动选择在库里实现，这里只验点击。
                if (mode == UiMode.Material) {
                    val other = center("其他")
                    h.ui.drag(settings.x, settings.y, other.x, other.y)
                    h.assertSettled(0)
                    h.ui.drag(other.x, other.y, settings.x, settings.y, cancel = true)
                    h.assertSettled(0)
                }
            }
        }
    }

    @Test
    fun `Material slider travels between tabs and follows pointer selection in both layout directions`() {
        for (direction in LayoutDirection.entries) {
            PagerHost(floating = true, direction = direction).use { h ->
                fun sliderX() = h.ui.nodes().first {
                    it.config.getOrNull(SemanticsProperties.TestTag) == "materialFloatingIndicator"
                }.boundsInRoot.center.x
                fun tabCenter(label: String) = h.ui.nodes().first {
                    it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true &&
                        it.boundsInRoot.width > 0
                }.boundsInRoot.center
                val startX = sliderX()
                h.ui.clickLabel("设置")
                h.ui.advance(48)
                val movingX = sliderX()
                h.ui.snapshot("material-slider-${direction.name}-moving")
                h.ui.advance()
                val endX = sliderX()
                assertTrue("Slider must animate through intermediate positions: $startX -> $movingX -> $endX", movingX > minOf(startX, endX) && movingX < maxOf(startX, endX))
                val settings = tabCenter("设置")
                val other = tabCenter("其他")
                h.ui.drag(settings.x, settings.y, other.x, other.y)
                h.assertSettled(0)
                h.ui.drag(other.x, other.y, settings.x, settings.y, cancel = true)
                h.assertSettled(0)
                h.ui.snapshot("material-slider-${direction.name}-cancelled")
            }
        }
    }

    private class PagerHost(
        mode: UiMode = UiMode.Material,
        floating: Boolean = false,
        saved: Bundle? = null,
        withHeaders: Boolean = false,
        floatingStyle: FloatingBottomBarStyle = FloatingBottomBarStyle.Classic,
        blur: Boolean = false,
        width: Int = 822,
        density: Float = 2f,
        direction: LayoutDirection = LayoutDirection.Ltr,
        color: ColorMode = ColorMode.LIGHT,
    ) : AutoCloseable {
        val ui = ComposeGestureHost(saved, width = width)
        var floatingStyle by mutableStateOf(floatingStyle)
        var blur by mutableStateOf(blur)
        var mode by mutableStateOf(mode)
        var floating by mutableStateOf(floating)
        var gestureMode by mutableIntStateOf(PagerModeCrossAxis)
        var active by mutableStateOf(true)
        lateinit var state: MainPagerState
        val lists = mutableMapOf<MainTab, LazyListState>()
        val instances = mutableMapOf<MainTab, Any>()
        val loads = mutableMapOf<MainTab, Int>()
        val resumes = mutableMapOf<MainTab, Int>()
        val clicks = mutableMapOf<MainTab, Int>()
        val topInsets = mutableMapOf<MainTab, Float>()
        var blockedBacks = 0

        init {
            ui.setContent {
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalLayoutDirection provides direction,
                    LocalUiMode provides this.mode,
                    LocalEnableFloatingBottomBar provides this.floating,
                    LocalFloatingBottomBarStyle provides this.floatingStyle,
                    LocalEnableBlur provides this.blur,
                    LocalPagerInterceptionMode provides gestureMode,
                ) {
                    AppTheme(AppSettings(colorMode = color, dynamicColor = false), uiMode = this.mode) {
                        val pager = rememberPagerState(initialPage = 2) { MainTab.entries.size }
                        val mainState = rememberMainPagerState(pager)
                        SideEffect { state = mainState }
                        MainPagerBackHandler(mainState, enabled = active)
                        MainTabs(mainState, isActive = active) { tab, current ->
                            val list = rememberLazyListState()
                            val instance = remember { Any() }
                            SideEffect { lists[tab] = list; instances[tab] = instance }
                            MainPageActivationEffect(
                                current,
                                onFirstActive = { loads[tab] = (loads[tab] ?: 0) + 1 },
                                onResume = { resumes[tab] = (resumes[tab] ?: 0) + 1 },
                            )
                            val rows: @Composable (PaddingValues) -> Unit = { padding ->
                                val top = padding.calculateTopPadding().value
                                SideEffect { topInsets[tab] = top }
                                val listItems: LazyListScope.() -> Unit = {
                                    items(150) { row ->
                                        Text(
                                            "${tab.name} row $row",
                                            Modifier.fillMaxWidth().height(64.dp).clickable {
                                                clicks[tab] = (clicks[tab] ?: 0) + 1
                                            }.padding(16.dp),
                                        )
                                    }
                                }
                                // Match ScopeScreen's separate list nodes while retaining their shared state.
                                if (this.mode == UiMode.Coui) {
                                    LazyColumn(
                                        state = list,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = padding,
                                        overscrollEffect = if (withHeaders) null else rememberOverscrollEffect(),
                                        content = listItems,
                                    )
                                } else {
                                    LazyColumn(
                                        state = list,
                                        modifier = Modifier.fillMaxSize().padding(padding),
                                        content = listItems,
                                    )
                                }
                            }
                            if (withHeaders) PageScaffold(title = tab.name, content = rows)
                            else rows(PaddingValues())
                        }
                        androidx.activity.compose.BackHandler(enabled = !active) { blockedBacks++ }
                    }
                }
            }
            ui.advance()
        }

        fun assertSettled(page: Int) {
            assertFalse(state.isNavigating)
            assertFalse(state.pagerState.isScrollInProgress)
            assertEquals(page, state.selectedPage)
            assertEquals(page, state.pagerState.currentPage)
            assertEquals(page, state.pagerState.settledPage)
        }

        override fun close() = ui.close()
    }
}
