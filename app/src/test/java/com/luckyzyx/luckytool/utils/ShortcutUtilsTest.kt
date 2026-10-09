package com.luckyzyx.luckytool.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShortcutUtilsTest {

    private lateinit var context: Context
    private lateinit var utils: ShortcutUtils

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        utils = ShortcutUtils(context)
    }

    private fun enabledIds(): Set<String> = utils.getEnabledShortcutList().map { it.id }.toSet()

    @Test
    fun `enabling one shortcut enables only that shortcut`() {
        val beans = utils.getDefaultShortcutBean()
        assertTrue(beans.isNotEmpty())

        val first = beans.first()
        utils.setShortcutStatus(beans, first, true)

        assertEquals(setOf(first.key), enabledIds())
    }

    @Test
    fun `enabling multiple shortcuts preserves already enabled ones`() {
        val beans = utils.getDefaultShortcutBean()
        assertTrue(beans.size >= 2)

        val first = beans[0]
        val second = beans[1]
        utils.setShortcutStatus(beans, first, true)
        utils.setShortcutStatus(beans, second, true)

        assertEquals(setOf(first.key, second.key), enabledIds())
    }

    @Test
    fun `unchecking a shortcut removes only that shortcut`() {
        val beans = utils.getDefaultShortcutBean()
        beans.forEach { utils.setShortcutStatus(beans, it, true) }
        assertEquals(beans.map { it.key }.toSet(), enabledIds())

        val first = beans.first()
        utils.setShortcutStatus(beans, first, false)

        assertFalse(enabledIds().contains(first.key))
        assertEquals(beans.drop(1).map { it.key }.toSet(), enabledIds())
    }

    @Test
    fun `unchecking all shortcuts removes every shortcut`() {
        val beans = utils.getDefaultShortcutBean()
        beans.forEach { utils.setShortcutStatus(beans, it, true) }
        assertEquals(beans.map { it.key }.toSet(), enabledIds())

        // 模拟用户取消勾选所有模块快捷方式后点击确定：status=false 会走删除分支
        beans.forEach { utils.setShortcutStatus(beans, it, false) }

        assertTrue(enabledIds().isEmpty())
    }
}
