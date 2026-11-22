package com.luckyzyx.luckytool.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 冻结「重启/优化只覆盖可 force-stop 的应用作用域，system/android 不参与」这一语义：
 * system 单独成页时没有可重启作用域（重启菜单只显示重载 Hooker），
 * system 与应用作用域混合的页面仍保留应用那部分的重启入口。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestartMenuScopesTest {

    @Test
    fun `system only page has no restartable scope`() {
        assertEquals(emptyList<String>(), scopesToRestart(arrayOf("system")).toList())
    }

    @Test
    fun `mixed system and app page keeps app scope restartable`() {
        assertEquals(
            listOf("com.oplus.riderMode"),
            scopesToRestart(arrayOf("system", "com.oplus.riderMode")).toList(),
        )
    }

    @Test
    fun `android scope is not restartable`() {
        assertEquals(emptyList<String>(), scopesToRestart(arrayOf("android")).toList())
    }

    @Test
    fun `framework reload names cover legacy android and new system`() {
        assertEquals(
            setOf("system", "android"),
            scopesForReload(arrayOf("android")),
        )
        assertEquals(
            setOf("system", "android"),
            scopesForReload(arrayOf("system")),
        )
    }

    @Test
    fun `mixed page reload keeps app scope and framework aliases`() {
        assertEquals(
            setOf("system", "android", "com.oplus.riderMode"),
            scopesForReload(arrayOf("system", "com.oplus.riderMode")),
        )
    }
}
