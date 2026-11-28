package com.luckyzyx.luckytool.ui.compose.components.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

/** 轻量 vibrancy 效果：提升饱和度，让透出的背景更鲜亮。 */
internal fun BackdropEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
