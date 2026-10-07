package com.luckyzyx.luckytool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController as MiuixThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Miuix 外观线主题（迁移自 KernelSU `ui/theme/MiuixTheme.kt`）。
 *
 * 取色管线与 KernelSU 完全一致：
 * - 直接把 `ThemeController(色板模式, keyColor, isDark, paletteStyle, colorSpec)` 交给 `MiuixTheme`，
 *   Miuix 自己生成整套色板（不构造 MiuixColorScheme）；
 * - `key_color != 0` 时用它当种子；`keyColor == 0` 且走 Monet 时借用 Material3 的系统动态主色；
 * - paletteStyle / colorSpec 按名称映射到 miuix 的枚举，SPEC_2025 在不支持的风格上回退 SPEC_2021；
 * - 深色切换同步系统栏前景色。
 *
 * 两处刻意的差异：
 * 1. `ThemeController` 用 `remember` 缓存（KernelSU 每次重组都新建，属于浪费）；
 * 2. Miuix 主题内部再套一层 Material3 Expressive 主题（同一套种子色），
 *    因为 LuckyTool 目前只有主题页 / 底部导航等部分界面是 Miuix 实现，
 *    其余 Material 页面需要继续跟随用户配色，否则 Miuix 模式下会出现两套色板。
 */
@Composable
fun MiuixLuckyTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorMode = appSettings.colorMode
    val darkTheme = colorMode.resolveDarkTheme()

    val miuixPaletteStyle = remember(appSettings.paletteStyle) {
        runCatching { ThemePaletteStyle.valueOf(appSettings.paletteStyle.name) }
            .getOrDefault(ThemePaletteStyle.TonalSpot)
    }
    val miuixColorSpec =
        if (appSettings.colorSpec.effectiveFor(appSettings.paletteStyle) == ColorSpec.SpecVersion.SPEC_2025) {
            ThemeColorSpec.Spec2025
        } else {
            ThemeColorSpec.Spec2021
        }

    val resolvedKeyColor: Color? = when {
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        colorMode.isMonet -> if (darkTheme) {
            dynamicDarkColorScheme(context).primary
        } else {
            dynamicLightColorScheme(context).primary
        }

        else -> null
    }

    val controller = remember(appSettings, darkTheme) {
        MiuixThemeController(
            when (colorMode) {
                ColorMode.SYSTEM -> ColorSchemeMode.System
                ColorMode.LIGHT -> ColorSchemeMode.Light
                ColorMode.DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.Dark
                ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
                ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
                ColorMode.MONET_DARK, ColorMode.MONET_DARK_AMOLED -> ColorSchemeMode.MonetDark
            },
            keyColor = resolvedKeyColor,
            isDark = darkTheme,
            paletteStyle = miuixPaletteStyle,
            colorSpec = miuixColorSpec,
        )
    }

    themeWindowAppearance(darkTheme)

    // Monet 模式下 Material 层同样走动态取色，保证两套色板同源
    val materialSettings = if (colorMode.isMonet) {
        appSettings.copy(dynamicColor = true)
    } else {
        appSettings
    }

    MiuixTheme(
        controller = controller,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            ) {
                MaterialExpressiveTheme(
                    colorScheme = rememberLuckyColorScheme(
                        seedColor = rememberSeedColor(appSettings = materialSettings, isDark = darkTheme),
                        isDark = darkTheme,
                        isAmoled = colorMode.isAmoled,
                        style = appSettings.paletteStyle,
                        specVersion = appSettings.colorSpec,
                    ).animateAsState(),
                    motionScheme = MotionScheme.expressive(),
                    typography = Typography,
                    content = content,
                )
            }
        },
    )
}

/** 供 Miuix 主题复用的深色判断（跟随系统时读取系统配置） */
@Composable
internal fun isSystemDarkNow(): Boolean = isSystemInDarkTheme()
