package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * 颜色预览圆点：所选颜色 + 1dp 对比描边，用在设置条目右侧。
 *
 * 描边不能取主题色的 `outline`：主题色本身可以选到白色，此时列表行与弹层都是浅色，
 * 白色描边等于没有描边，而这两个颜色偏好的默认值正是 `#FFFFFFFF`，圆点会整块看不见。
 */
@Composable
fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Box(
        modifier
            .clip(shape)
            .background(color)
            .border(1.dp, contrastBorder(color), shape)
    )
}

/**
 * 与 [color] 形成对比的描边色（[ColorDot] 与颜色选择器的预览色块共用）。
 *
 * 只按颜色自身取反、不跟主题色走：浅色用深描边，深色用浅描边；半透明色一律用深描边，
 * 因为它底下透出的是浅色内容（选择器里是浅灰棋盘格）。这样无论主题色与所选颜色怎么组合，
 * 色块本体与轮廓至少有一个看得清 —— 「白色主题色 + 白色颜色」原先是两者都看不见的组合。
 */
internal fun contrastBorder(color: Color): Color =
    if (color.alpha < 0.5f || color.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }
