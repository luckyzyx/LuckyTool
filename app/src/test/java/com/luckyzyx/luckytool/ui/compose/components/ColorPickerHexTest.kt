package com.luckyzyx.luckytool.ui.compose.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 锁定颜色选择器十六进制输入框的解析与过滤语义。
 *
 * 存储与回调契约是 `#AARRGGBB`，但输入框允许更宽松的写法：
 * `#` 可省略、大小写不限、6 位按不透明补 `FF`、非十六进制字符直接被过滤掉。
 * 回归背景：若把 6 位输入当成 alpha 缺失而不补 `FF`，颜色会变成全透明（alpha=0）而看不见；
 * 若过滤时保留中间的 `#` 或按 9 字符硬截断，无 `#` 的 8 位输入会被截成 9 位而永远解析失败。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorPickerHexTest {

    @Test
    fun `six digit input is treated as opaque`() {
        assertEquals(0xFFFF0000.toInt(), parseHex("#FF0000"))
        assertEquals(0xFFFF0000.toInt(), parseHex("FF0000"))
        assertEquals(0xFF00FF80.toInt(), parseHex("#00ff80"))
    }

    @Test
    fun `eight digit input keeps its alpha`() {
        assertEquals(0x80FF0000.toInt(), parseHex("#80FF0000"))
        assertEquals(0x80FF0000.toInt(), parseHex("80ff0000"))
        assertEquals(0x00112233.toInt(), parseHex("#00112233"))
    }

    @Test
    fun `unparsable input returns null`() {
        assertNull(parseHex(""))
        assertNull(parseHex("#"))
        assertNull(parseHex("#FFF"))
        assertNull(parseHex("#FF0000FF00"))
        assertNull(parseHex("#GGGGGG"))
        assertNull(parseHex("#FF00-0"))
    }

    @Test
    fun `filter keeps the leading hash and hex digits only`() {
        assertEquals("#ff0000", filterHexInput("#ff0000"))
        assertEquals("ff0000", filterHexInput("f f 0 0 0 0"))
        // 中间的 '#' 不是合法输入，直接丢弃
        assertEquals("ab", filterHexInput("a#b"))
        // 粘贴带空白的完整值
        assertEquals("#80FF0000", filterHexInput("  #80FF0000  "))
    }

    @Test
    fun `filter truncates to the maximal length`() {
        assertEquals("#FF0000FF", filterHexInput("#FF0000FF00"))
        assertEquals("FF0000FF", filterHexInput("FF0000FF00"))
        // 长度上限内的内容原样保留（含大小写）
        assertEquals("#AbCdEf", filterHexInput("#AbCdEf"))
    }
}
