package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 锁定颜色预览色块/圆点的描边取色规则。
 *
 * 回归背景：描边原先取主题色的 `outline`。主题色可以选到白色，此时列表行与弹层都是浅色，
 * 白色描边等于没有描边，而两个颜色偏好的默认值正是 `#FFFFFFFF`，圆点会整块看不见。
 * 规则改为只跟色块自身对比后，任何「主题色 × 所选颜色」组合都要么色块可见、要么描边可见。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColorDotTest {

    @Test
    fun `light color gets a dark border`() {
        // 白色正是默认值，也是原来看不见的那个组合
        assertDarkBorder(contrastBorder(Color.White))
        assertDarkBorder(contrastBorder(Color(0xFFF0F0F0)))
    }

    @Test
    fun `dark color gets a light border`() {
        assertLightBorder(contrastBorder(Color.Black))
        assertLightBorder(contrastBorder(Color(0xFF202020)))
    }

    @Test
    fun `translucent color always gets a dark border`() {
        // 半透明时底下透出的是浅色内容（选择器里是浅灰棋盘格），深描边才看得见
        assertDarkBorder(contrastBorder(Color.White.copy(alpha = 0.3f)))
        assertDarkBorder(contrastBorder(Color.Black.copy(alpha = 0.0f)))
    }

    @Test
    fun `border always differs from the color itself`() {
        val samples = listOf(
            Color.White,
            Color.Black,
            Color(0xFF808080),
            Color(0xFFFFEB3B),
            Color(0xFF2196F3),
            Color.White.copy(alpha = 0.4f),
        )
        samples.forEach { color ->
            assert(color != contrastBorder(color)) { "描边与颜色相同，色块会糊在一起：$color" }
        }
    }

    private fun assertDarkBorder(border: Color) {
        assertEquals(0f, border.red, 0.001f)
        assertEquals(0f, border.green, 0.001f)
        assertEquals(0f, border.blue, 0.001f)
        assertEquals(0.28f, border.alpha, 0.01f)
    }

    private fun assertLightBorder(border: Color) {
        assertEquals(1f, border.red, 0.001f)
        assertEquals(1f, border.green, 0.001f)
        assertEquals(1f, border.blue, 0.001f)
        assertEquals(0.38f, border.alpha, 0.01f)
    }
}
