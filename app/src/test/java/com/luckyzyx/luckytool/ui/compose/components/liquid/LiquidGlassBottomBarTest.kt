package com.luckyzyx.luckytool.ui.compose.components.liquid

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.ComposeGestureHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
class LiquidGlassBottomBarTest {

    companion object {
        private const val INDICATOR_TAG = "floatingBarIndicator"
        private val LABELS = listOf("概览", "功能", "日志", "设置", "关于")

        private val dotIcon: ImageVector = ImageVector.Builder(
            name = "dot",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathData {
                moveTo(4f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                close()
            },
            fill = SolidColor(Color.Black),
        ).build()

        private val items = LABELS.map { LiquidGlassItem(it, dotIcon) }
    }

    @Composable
    private fun Bar(
        selectedIndex: Int,
        onSelected: (Int) -> Unit = {},
        isBlurActive: Boolean = false,
    ) {
        MaterialTheme {
            LiquidGlassBottomBar(
                items = items,
                selectedIndex = selectedIndex,
                onSelected = onSelected,
                backdrop = null,
                isBlurActive = isBlurActive,
            )
        }
    }

    private fun ComposeGestureHost.tabs() = nodes().filter {
        it.boundsInRoot.width > 0 && it.config.getOrNull(SemanticsProperties.Role) == Role.Tab
    }

    /** 无 backdrop 时退化路径；指定 backdrop 但着色器不可用时也必须不崩。 */
    @Test
    fun rendersWithBlurInactiveAndActive() {
        listOf(false, true).forEach { blur ->
            ComposeGestureHost().use { host ->
                host.setContent { Bar(selectedIndex = 0, isBlurActive = blur) }
                assertEquals("isBlurActive=$blur", LABELS.size, host.tabs().size)
            }
        }
    }

    /** 每个标签都是可选中的 Tab，且滑块中心与选中标签中心对齐。 */
    @Test
    fun selectionAndIndicatorFollowSelectedIndex() {
        listOf(0, 2, LABELS.lastIndex).forEach { selected ->
            ComposeGestureHost().use { host ->
                host.setContent { Bar(selectedIndex = selected) }
                val tabs = host.tabs()
                assertEquals(LABELS.size, tabs.size)

                val selectedTabs = tabs.filter { it.config.getOrNull(SemanticsProperties.Selected) == true }
                assertEquals("selectedIndex=$selected", 1, selectedTabs.size)

                val indicator = host.nodes().firstOrNull {
                    it.config.getOrNull(SemanticsProperties.TestTag) == INDICATOR_TAG
                }
                assertNotNull("selectedIndex=$selected 缺少 $INDICATOR_TAG", indicator)
                assertEquals(
                    "selectedIndex=$selected",
                    selectedTabs.single().boundsInRoot.center.x,
                    requireNotNull(indicator).boundsInRoot.center.x,
                    1f,
                )
            }
        }
    }

    /** 点击第三个标签回报下标 2。 */
    @Test
    fun clickingThirdTabReportsIndexTwo() {
        var clicked = -1
        ComposeGestureHost().use { host ->
            host.setContent { Bar(selectedIndex = 0, onSelected = { clicked = it }) }
            host.clickLabel(LABELS[2])
            assertEquals(2, clicked)
        }
    }
}
