package com.luckyzyx.luckytool.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.CouiTypography

/**
 * COUI 外观线主题。
 *
 * 色板来源由主题页的 COUI 取色开关决定（[AppSettings.couiDynamicColor]）：
 * 关闭（默认）取 COUI 自带预设（[couiPresetColorScheme]），界面保持 COUI 自己的观感；
 * 打开才跑与 Material 线同一条 material-kolor 管线（种子色 / 壁纸动态取色、调色风格、
 * 色彩规格、AMOLED 刷黑、弹性过渡），把 M3 色板经 [couiColorScheme] 翻译成 COUI 语义色。
 *
 * 不使用 COUI 自带的 M3 适配（CouiMaterial3Style）：它把页面底色与卡片表面取成同一档，
 * 分组卡片会与页面撞色而看不见；这里改用 M3 的容器阶拉开层次。
 *
 * COUI 线里仍由 material3 渲染的弹层与底部弹窗读的是 M3 色板，
 * 因此用同一套 COUI 色板反推 M3 色板（见 [couiMaterialColorScheme]），避免两种强调色并排出现。
 *
 * 组合顺序有约束：[CouiTheme] 必须位于 [MaterialExpressiveTheme] 内部，
 * 这样两套组件读到的深浅色与取色结果一致。
 *
 * 不使用 Coui.initialize：它只能调用一次且是进程级默认值，
 * 与本项目「ui_mode 运行时可切换」的前提冲突；这里每次组合显式传入色板与字体。
 */
@Composable
fun CouiAppTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colorMode = appSettings.colorMode
    val darkTheme = colorMode.resolveDarkTheme()

    themeWindowAppearance(darkTheme)

    // 关闭取色时连取色管线一起跳过：预设配色不必读壁纸，也不必逐帧过渡。
    val couiColors = if (appSettings.couiDynamicColor) {
        val colorScheme = rememberAppColorScheme(
            seedColor = rememberSeedColor(appSettings = appSettings, isDark = darkTheme),
            isDark = darkTheme,
            isAmoled = colorMode.isAmoled,
            style = appSettings.paletteStyle,
            specVersion = appSettings.colorSpec,
        ).animateAsState()
        couiColorScheme(colorScheme, darkTheme, colorMode.isAmoled)
    } else {
        couiPresetColorScheme(darkTheme, colorMode.isAmoled)
    }
    val materialScheme = couiMaterialColorScheme(couiColors, darkTheme)

    CompositionLocalProvider(
        LocalColorMode provides colorMode,
        LocalAppColorScheme provides materialScheme,
    ) {
        MaterialExpressiveTheme(
            colorScheme = materialScheme,
            motionScheme = MotionScheme.expressive(),
        ) {
            CouiTheme(
                darkTheme = darkTheme,
                colorScheme = couiColors,
                typography = CouiTypography(),
                // 顶栏、底栏与弹层的材质染色跟随卡片色，与页面底色形成层次。
                materialTint = couiColors.card,
            ) {
                // 未包在 Surface / Button 里的内容（自绘弹层、顶栏 actions 等）读不到内容色，
                // 会退回 material3 默认的黑；深色模式下不可见，这里按 COUI 的 onSurface 兜底。
                CompositionLocalProvider(
                    LocalContentColor provides couiColors.onSurface,
                    content = content,
                )
            }
        }
    }
}
