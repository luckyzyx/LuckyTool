package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrefStateTest {

    private lateinit var context: Context
    private lateinit var raw: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        raw = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        PrefState.invalidate("test_prefs")
    }

    private fun state(): PrefState = PrefState.ofProvider { raw }

    @Test
    fun `initial values come from stored prefs`() {
        raw.edit().putString("str", "hello").putBoolean("bool", true)
            .putInt("int", 42).putLong("long", 1L).putStringSet("set", setOf("a", "b"))
            .commit()

        val state = state()
        assertEquals("hello", state.getString("str", "dft"))
        assertTrue(state.getBoolean("bool", false))
        assertEquals(42, state.getInt("int", 0))
        assertEquals(1L, state.getLong("long", 0L))
        assertEquals(setOf("a", "b"), state.getStringSet("set", emptySet()))
    }

    @Test
    fun `missing keys fall back to default`() {
        val state = state()
        assertEquals("dft", state.getString("str", "dft"))
        assertTrue(state.getBoolean("bool", true))
        assertEquals(7, state.getInt("int", 7))
        assertNull(state.getString("nullable", null))
    }

    @Test
    fun `set persists with commit and is visible synchronously`() {
        val state = state()
        assertTrue(state.set("str", "new"))
        assertTrue(state.set("bool", false))
        assertTrue(state.set("int", 9))
        assertTrue(state.set("long", 9L))
        assertTrue(state.set("set", setOf("x")))

        assertEquals("new", raw.getString("str", null))
        assertFalse(raw.getBoolean("bool", true))
        assertEquals(9, raw.getInt("int", 0))
        assertEquals(9L, raw.getLong("long", 0L))
        assertEquals(setOf("x"), raw.getStringSet("set", null))
        assertEquals("new", state.getString("str", "dft"))
    }

    @Test
    fun `set null removes the key`() {
        raw.edit().putString("str", "old").commit()
        val state = state()
        assertTrue(state.set("str", null))
        assertFalse(raw.contains("str"))
        assertNull(state.getString("str", null))
    }

    @Test
    fun `listener mirrors external commits into flows`() = runBlocking {
        val state = state()
        val flow = state.stringFlow("str", "dft")
        assertEquals("dft", flow.first())

        raw.edit().putString("str", "from-outside").commit()
        assertEquals("from-outside", flow.first())
        assertEquals("from-outside", state.getString("str", null))
    }

    @Test
    fun `all flow types mirror external commits`() = runBlocking {
        val state = state()
        val s = state.stringFlow("s")
        val b = state.booleanFlow("b", false)
        val i = state.intFlow("i", 0)
        val l = state.longFlow("l", 0L)
        val set = state.stringSetFlow("set", emptySet())

        raw.edit().putString("s", "v").putBoolean("b", true).putInt("i", 3)
            .putLong("l", 4L).putStringSet("set", setOf("z")).commit()

        assertEquals("v", s.first())
        assertTrue(b.first())
        assertEquals(3, i.first())
        assertEquals(4L, l.first())
        assertEquals(setOf("z"), set.first())
    }

    @Test
    fun `flow keeps last committed value from state set`() = runBlocking {
        val state = state()
        val flow = state.stringFlow("str", "dft")
        state.set("str", "first")
        state.set("str", "second")
        assertEquals("second", flow.first())
    }

    @Test
    fun `provider switch refreshes all flows and listener registration`() = runBlocking {
        val secondRaw = context.getSharedPreferences("other_prefs", Context.MODE_PRIVATE)
        secondRaw.edit().clear().putString("str", "in-other").commit()

        val state = state()
        val flow = state.stringFlow("str", "dft")
        assertEquals("dft", flow.first())

        // 重新绑定 provider（模拟 XposedServiceBridge 绑定/解绑切换数据源）
        val switched = PrefState.ofProvider { secondRaw }
        assertEquals("in-other", switched.stringFlow("str", "dft").first())
    }

    @Test
    fun `typed flows fall back to default on a mismatched stored value`() = runBlocking {
        // 历史脏数据：集合键被写成了字符串 → 读集合的页面不崩、回落默认值
        raw.edit().putString("set", "{com.a, com.b}").commit()
        val state = state()
        assertEquals(emptySet<String>(), state.stringSetFlow("set", emptySet()).first())
        assertEquals(emptySet<String>(), state.getStringSet("set", emptySet()))

        // 之后写回正确类型，同一条流要能恢复成真实数据
        assertTrue(state.set("set", setOf("com.a")))
        assertEquals(setOf("com.a"), raw.getStringSet("set", null))
        assertEquals(setOf("com.a"), state.stringSetFlow("set", emptySet()).first())

        // 反向：字符串键被写成了集合 → 读字符串回落默认值
        raw.edit().putStringSet("str", setOf("a")).commit()
        assertEquals("dft", state.stringFlow("str", "dft").first())
        assertEquals("dft", state.getString("str", "dft"))
    }
}
