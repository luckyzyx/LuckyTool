package com.luckyzyx.luckytool.ui.compose.components

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import com.luckyzyx.luckytool.ui.compose.ComposeGestureHost
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.ui.theme.ColorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 顶栏「更多」弹层的宽度约束：弹层是独立窗口、按 WRAP_CONTENT 测量，
 * 条目行又是 fillMaxWidth，所以列必须先按最宽条目定宽，否则整行会铺满屏幕。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopBarOverflowMenuWidthTest {

    private val labels = listOf(
        "全选分享意图",
        "全选长按文本意图",
        "全选打开方式意图",
        "全选浏览器意图",
        "清除所有数据",
    )

    @Test
    fun overflowMenuColumnKeepsIntrinsicWidth() {
        ComposeGestureHost().use { host ->
            host.setContent {
                AppTheme(AppSettings(colorMode = ColorMode.LIGHT, dynamicColor = false)) {
                    Box(Modifier.fillMaxSize()) {
                        TopBarOverflowMenu(
                            items = labels.map { TopBarOverflowItem(it) {} },
                            contentDescription = "更多",
                        )
                    }
                }
            }
            host.advance()
            host.clickLabel("更多")
            host.advance()

            // 动作键的点击面也带 OnClick 语义，条目行的 top 在它下面。
            val rows = host.nodes().filter { node ->
                node.config.getOrNull(SemanticsActions.OnClick) != null &&
                    node.boundsInRoot.width > 0f &&
                    node.boundsInRoot.top > 0f
            }
            assertEquals("弹层条目数", labels.size, rows.size)
            rows.forEach { row ->
                // 160dp 的最小列宽在 xhdpi 下是 320px，且必须窄于窗口（822px）。
                assertEquals("条目行宽度", 320f, row.boundsInRoot.width)
                assertTrue("条目行不得铺满窗口", row.boundsInRoot.width < host.width)
            }
        }
    }
}
