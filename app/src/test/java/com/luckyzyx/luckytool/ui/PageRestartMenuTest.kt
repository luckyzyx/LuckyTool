package com.luckyzyx.luckytool.ui

import com.luckyzyx.luckytool.ui.compose.PageRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结「重启作用域菜单默认显示、只有没有可重启作用域的页面才关闭」这一语义。
 *
 * [com.luckyzyx.luckytool.ui.compose.PageSpec.restartEnabled] 默认 true（opt-out）：
 * 顶栏按钮由 FunctionPage 按该开关渲染，而对话框对空作用域直接返回 —— 空作用域的页若留着默认值，
 * 按钮点下去不会有任何反应。所以这里要求作用域列表为空的功能页必须显式关闭，其余页面保持开启。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PageRestartMenuTest {

    @Test
    fun `pages without scopes must opt out of the restart menu`() {
        PageRegistry.all().forEach { spec ->
            assertFalse(
                "${spec.pageKey} 没有作用域却开着重启菜单：按钮点了不会有任何反应",
                spec.scopes.isEmpty() && spec.restartEnabled,
            )
        }
    }

    @Test
    fun `scope pages show the restart menu by default`() {
        // 天气页曾因每页手写 restartEnabled = false 而没有重启入口
        val weather = PageRegistry["oplus_weather"]
        assertNotNull("oplus_weather 应已注册", weather)
        assertTrue("天气页必须有可重启的作用域", weather!!.scopes.isNotEmpty())
        assertTrue("有作用域的页面默认显示重启入口", weather.restartEnabled)
    }
}
