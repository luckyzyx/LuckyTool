package com.luckyzyx.luckytool.ui.compose.components.miuix

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.ComposeGestureHost
import com.luckyzyx.luckytool.ui.theme.LuckyAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.basic.Text

/**
 * 锁定「正文可滚动 + 底部操作行固定」这条不变量。
 *
 * 回归背景：miuix OverlayDialog 在手机上底对齐且正文不滚动，键盘弹出后可用高度只剩「屏幕 − 键盘」，
 * Column 把固定高度的子项逐项压扁，靠后的按钮行被压成 0 高度（详见 MiuixDialogScrollBody 的注释）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32], application = Application::class, qualifiers = "zh-rCN-w411dp-h880dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiuixDialogScrollBodyTest {

    @Test
    fun `actions stay visible when the body overflows a short container`() {
        ComposeGestureHost().use { host ->
            val sizes = MeasuredSizes()
            host.setContent {
                LuckyAppTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(SHORT_CONTAINER)
                                .onSizeChanged { sizes.container = it.height },
                        ) {
                            MiuixDialogScrollBody(
                                actions = { Text(ACTION_LABEL) },
                            ) {
                                repeat(40) { index -> Text("正文第 $index 行，用足够的行数把容器撑满") }
                            }
                        }
                    }
                }
            }
            host.advance()

            // xhdpi 下 200dp = 400px
            assertEquals(400, sizes.container)

            val actionNode = host.textNode(ACTION_LABEL)
            assertNotNull("正文溢出时按钮行必须仍然存在", actionNode)
            val bounds = actionNode!!.boundsInRoot
            assertTrue("按钮行不能被压成 0 高度：$bounds", bounds.height > 0f)
            assertTrue("按钮行被挤到 200dp 容器之外：$bounds / 容器高 ${sizes.container}px", bounds.top >= 0f && bounds.bottom <= sizes.container.toFloat())
            // 正文仍然完整组合（可滚动），不是被裁掉或丢弃
            assertNotNull("正文内容应保持组合", host.textNode("正文第 0 行，用足够的行数把容器撑满"))
        }
    }

    @Test
    fun `short content keeps the natural height instead of filling the parent`() {
        ComposeGestureHost().use { host ->
            val sizes = MeasuredSizes()
            host.setContent {
                LuckyAppTheme {
                    Box(Modifier.fillMaxSize()) {
                        MiuixDialogScrollBody(
                            modifier = Modifier.onSizeChanged { sizes.body = it.height },
                            actions = { Text(ACTION_LABEL) },
                        ) {
                            Text("一行短正文")
                        }
                    }
                }
            }
            host.advance()

            // fill = false：内容短时只占自身高度，不能被撑满 880dp 的父高度
            assertTrue("正文很短时容器高度应远小于父高度，实际 ${sizes.body}px", sizes.body in 1 until 400)
        }
    }

    @Test
    fun `lazy list content keeps a bounded height when scrollable is false`() {
        ComposeGestureHost().use { host ->
            val sizes = MeasuredSizes()
            host.setContent {
                LuckyAppTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(TALL_CONTAINER)
                                .onSizeChanged { sizes.container = it.height },
                        ) {
                            MiuixDialogScrollBody(
                                scrollable = false,
                                actions = { Text(ACTION_LABEL) },
                            ) {
                                // 与 AppPickerDialog 同形：正文里是 weighted 的 Card + LazyColumn
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {
                                    items(20) { index -> Text("应用列表第 $index 项") }
                                }
                            }
                        }
                    }
                }
            }
            // 套了 verticalScroll 的话这里会抛 infinity max height constraints，组合 + 布局直接失败
            host.advance()

            // xhdpi 下 400dp = 800px
            assertEquals(800, sizes.container)

            val actionNode = host.textNode(ACTION_LABEL)
            assertNotNull("惰性列表正文下按钮行必须仍然存在", actionNode)
            val bounds = actionNode!!.boundsInRoot
            assertTrue("按钮行不能被压成 0 高度：$bounds", bounds.height > 0f)
            assertTrue("按钮行被挤到容器之外：$bounds / 容器高 ${sizes.container}px", bounds.top >= 0f && bounds.bottom <= sizes.container.toFloat())
            assertNotNull("LazyColumn 至少应组合出第一项", host.textNode("应用列表第 0 项"))
        }
    }

    private class MeasuredSizes {
        var container = 0
        var body = 0
    }

    private companion object {
        val SHORT_CONTAINER = 200.dp
        val TALL_CONTAINER = 400.dp
        const val ACTION_LABEL = "确定"
    }
}
