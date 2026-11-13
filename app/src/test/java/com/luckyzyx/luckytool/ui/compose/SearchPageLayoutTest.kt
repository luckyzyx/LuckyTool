package com.luckyzyx.luckytool.ui.compose

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.luckyzyx.luckytool.ui.compose.pages.SearchPage
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
// Robolectric 无法构造 API 34 的 MeshSpecification，使用库的普通背景路径验证布局。
@Config(sdk = [32], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchPageLayoutTest {
    @Test
    fun `search header reserves space above history and categories`() {
        ComposeGestureHost().use { ui ->
            System.setProperty("pager.preview.output", "/tmp/lucky-search-preview")
            ui.setContent {
                CompositionLocalProvider(LocalDensity provides Density(2f), LocalUiMode provides UiMode.Coui) {
                    AppTheme(AppSettings(colorMode = ColorMode.LIGHT, dynamicColor = false), uiMode = UiMode.Coui) {
                        SearchPage(isCurrentPage = true, onOpen = {})
                    }
                }
            }
            ui.advance()
            ui.snapshot("landing")
            val field = ui.nodes().first { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("搜索菜单") == true }.boundsInRoot
            val empty = requireNotNull(ui.textNode("暂无最近搜索")).boundsInRoot
            val categories = requireNotNull(ui.textNode("功能分类")).boundsInRoot
            assertTrue("History must be below the search field", empty.top > field.bottom)
            assertTrue("Categories must follow history", categories.top > empty.bottom)
            ui.clickLabel("搜索菜单")
            val openingPositions = mutableListOf<Float>()
            repeat(12) { frame ->
                ui.advance(16)
                openingPositions += ui.nodes().first {
                    it.config.getOrNull(SemanticsProperties.EditableText) != null ||
                        it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("搜索菜单") == true
                }.boundsInRoot.top
                if (frame in listOf(0, 3, 7, 11)) ui.snapshot("opening-$frame")
            }
            ui.advance()
            val input = ui.nodes().first { it.config.getOrNull(SemanticsProperties.EditableText) != null }.boundsInRoot
            assertTrue("Opening must pass through intermediate positions instead of jumping",
                openingPositions.any { it > input.top + 2f && it < field.top - 2f })
            val expandedHistory = requireNotNull(ui.textNode("暂无最近搜索")).boundsInRoot
            assertTrue("Expanded search must reserve space above history", expandedHistory.top > input.bottom)
            ui.snapshot("expanded")
            ui.clickText("取消")
            val closingPositions = mutableListOf<Float>()
            repeat(12) { frame ->
                ui.advance(16)
                closingPositions += ui.nodes().first {
                    it.config.getOrNull(SemanticsProperties.EditableText) != null ||
                        it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("搜索菜单") == true
                }.boundsInRoot.top
                if (frame in listOf(0, 3, 7, 11)) ui.snapshot("closing-$frame")
            }
            ui.advance()
            assertTrue("Cancel must animate back through intermediate positions",
                closingPositions.any { it > input.top + 2f && it < field.top - 2f })
            assertTrue(ui.textNode("搜索") != null)
            System.clearProperty("pager.preview.output")
        }
    }
}
