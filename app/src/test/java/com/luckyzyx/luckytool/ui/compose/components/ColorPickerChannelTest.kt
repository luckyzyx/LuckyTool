package com.luckyzyx.luckytool.ui.compose.components

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 锁定颜色选择器 ARGB 模式四通道滑杆的位运算语义。
 *
 * 回归背景：拖动 A/R/G/B 任一滑杆只能替换该通道字节，其余三个通道（含 alpha）必须原样保留 ——
 * 掩码若写成 `0xFF shl shift` 而漏了取反，或滑杆越界值不收敛到 0..255，都会把颜色改成错误的值。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorPickerChannelTest {

    @Test
    fun `setting one channel keeps the other three`() {
        val color = 0x80112233.toInt()
        assertEquals(0xFF112233.toInt(), withChannel(color, ALPHA_SHIFT, 0xFF))
        assertEquals(0x80442233.toInt(), withChannel(color, RED_SHIFT, 0x44))
        assertEquals(0x8011AA33.toInt(), withChannel(color, GREEN_SHIFT, 0xAA))
        assertEquals(0x80112255.toInt(), withChannel(color, BLUE_SHIFT, 0x55))
    }

    @Test
    fun `channel value is clamped to the byte range`() {
        val color = 0x00112233.toInt()
        assertEquals(0x00112233.toInt(), withChannel(color, ALPHA_SHIFT, -1))
        assertEquals(0xFF112233.toInt(), withChannel(color, ALPHA_SHIFT, 999))
    }
}
