package com.luckyzyx.luckytool.ui.compose.pages

import androidx.compose.runtime.Composable
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 主题与配色页分发（对齐 KernelSU `ColorPaletteScreen`）：按当前界面风格选择实现。
 */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> ThemeScreenMiuix(onBack = onBack)
        UiMode.Material -> ThemeScreenMaterial(onBack = onBack)
    }
}
