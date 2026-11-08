package com.luckyzyx.luckytool.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.itos.coui.theme.CouiColorScheme
import com.itos.coui.theme.CouiPresets

/**
 * COUI 自带预设色板：浅色灰底白卡、深色黑底浅色浮层，两档明度差由预设负责，
 * 分组卡片因此有明确边界。
 *
 * 深色与 AMOLED 的差别在页面底色：深色用预设浮层灰做底，AMOLED 保持纯黑底，
 * 这样主题页四个明暗按钮在 COUI 线上也能看出区别。
 *
 * COUI 线的取色开关关闭时用这一套（默认），界面回到 COUI 自己的观感。
 */
internal fun couiPresetColorScheme(dark: Boolean, amoled: Boolean = false): CouiColorScheme = when {
    !dark -> CouiPresets.Light
    amoled -> CouiPresets.Dark
    else -> CouiPresets.Dark.copy(background = CouiPresets.Dark.surface)
}

/**
 * 把 M3 色板翻译成 COUI 的语义色：COUI 线跟随 Material 取色时与 Material 线共用同一套取色结果。
 *
 * COUI 的 [CouiColorScheme.background] 是页面底色、[CouiColorScheme.card] 是列表卡片，
 * 两者必须拉开明度差，分组卡片才看得见；M3 的 background 与 surface 是同一档，
 * 直接取同名字段会让卡片和页面融成一片。这里改用容器阶：
 * 浅色页取 surfaceContainer、卡取 surfaceContainerLow、弹层取 surfaceContainerHigh；
 * 深色整体上移一档（页 surfaceContainerLow / 卡 surfaceContainerHigh / 弹层 surfaceContainerHighest），
 * 让深色页底明显浅于 AMOLED 纯黑。
 *
 * AMOLED 模式下容器阶被刷成纯黑（见 [amoledBackground]），卡片改用半透明白叠在纯黑上，
 * 保持「黑底 + 浅色浮层」的层次。
 */
internal fun couiColorScheme(
    colors: ColorScheme,
    dark: Boolean,
    amoled: Boolean = false,
): CouiColorScheme {
    val accent = colors.primary
    val card = when {
        !amoled -> if (dark) colors.surfaceContainerHigh else colors.surfaceContainerLow
        else -> Color.White.copy(alpha = 0.10f)
    }
    // 深色下 primary 直接铺在浮层上对比度不足，用 inversePrimary 提亮。
    val checkboxAccent = if (dark) colors.inversePrimary else accent
    return CouiColorScheme(
        accent = accent,
        onAccent = colors.onPrimary,
        surface = card,
        container = card,
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariant,
        outlineVariant = colors.outlineVariant,
        accentOnSurface = accent,
        menuSurface = when {
            amoled -> Color.White.copy(alpha = 0.14f)
            dark -> colors.surfaceContainerHighest
            else -> colors.surfaceContainerHigh
        },
        checkboxAccent = checkboxAccent,
        menuCheckboxAccent = checkboxAccent,
        background = when {
            amoled -> Color.Black
            dark -> colors.surfaceContainerLow
            else -> colors.surfaceContainer
        },
        card = card,
    )
}

/**
 * COUI 线里仍由 material3 渲染的部分（弹层、底部弹窗、输入框、滑块）读的是 M3 色板。
 * 这里用 COUI 色板反推出同源 M3 色板：强调色即 COUI accent，容器阶与 COUI 的卡片、菜单表面一一对应，
 * 同一屏因此不会出现两种强调色。
 */
internal fun couiMaterialColorScheme(colors: CouiColorScheme, dark: Boolean): ColorScheme {
    val accentContainer = lerp(colors.surface, colors.accent, if (dark) 0.24f else 0.12f)
    val onAccentContainer = if (dark) colors.onSurface else colors.accent
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = accentContainer,
        onPrimaryContainer = onAccentContainer,
        inversePrimary = colors.accentOnSurface,
        secondary = colors.accent,
        onSecondary = colors.onAccent,
        secondaryContainer = accentContainer,
        onSecondaryContainer = onAccentContainer,
        background = colors.background,
        onBackground = colors.onSurface,
        surface = colors.card,
        onSurface = colors.onSurface,
        surfaceVariant = colors.container,
        onSurfaceVariant = colors.onSurfaceVariant,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.card,
        surfaceContainer = colors.card,
        surfaceContainerHigh = colors.menuSurface,
        surfaceContainerHighest = colors.menuSurface,
        surfaceBright = colors.card,
        surfaceDim = colors.background,
        outline = colors.onSurfaceVariant,
        outlineVariant = colors.outlineVariant,
    )
}
