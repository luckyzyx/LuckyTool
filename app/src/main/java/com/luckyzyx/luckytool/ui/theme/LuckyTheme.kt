@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBar
import com.luckyzyx.luckytool.ui.shell.LocalEnableFloatingBottomBarBlur
import com.luckyzyx.luckytool.ui.shell.LocalEnableNavigationBadge
import com.luckyzyx.luckytool.ui.shell.LocalEnableSwipeDismiss
import com.luckyzyx.luckytool.ui.shell.LocalModuleDescriptionMaxLines
import com.luckyzyx.luckytool.ui.shell.LocalPagerInterceptionMode
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController

/** 关闭动态取色、且未选自定义主题色时的种子色：M3 基线紫，观感对齐旧 light/darkColorScheme() 默认值。 */
private val DefaultSeedColor = Color(0xFF6750A4)

/**
 * 给定模式解析本次组合应使用的种子色（对齐 KernelSU MaterialKernelSUTheme）：
 * `Color.Unspecified` 表示跟随系统动态取色（需 Android 12+）；否则用自定义主题色或默认种子色。
 */
@Composable
fun rememberSeedColor(
    appSettings: AppSettings,
    isDark: Boolean,
): Color {
    val context = LocalContext.current
    val dynamicAvailable = appSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return remember(context, appSettings.keyColor, dynamicAvailable, isDark) {
        when {
            dynamicAvailable -> Color.Unspecified
            appSettings.keyColor != 0 -> Color(appSettings.keyColor)
            else -> DefaultSeedColor
        }
    }
}

/** 解析当前模式下的明暗（跟随系统时需要系统配置参与判断） */
@Composable
fun ColorMode.resolveDarkTheme(): Boolean = when {
    isDark -> true
    isSystem -> isSystemInDarkTheme()
    else -> false
}

/**
 * LuckyTool 统一 Compose 主题：Material 3 Expressive（material3 1.5.0-alpha29，
 * Google 官方 Expressive 线；1.4.0 稳定版不含公开 Expressive API，故按用户确认选 alpha29）。
 *
 * 取色管线对齐 KernelSU（material-kolor 5.0.1）：
 * - dynamicColor 开启且 Android 12+ 时以系统 primary 为种子动态取色；
 * - 否则使用自定义主题色（key_color）或默认种子色（0xFF6750A4）；
 * - palette_style / color_spec 偏好控制取色风格与色彩规格，SPEC_2025 在不支持的风格上回退 SPEC_2021；
 * - AMOLED 模式（ColorMode.DARK_AMOLED）下所有背景槽位纯黑；
 * - 主题切换时全色板弹性动画过渡，并同步系统栏前景色（对齐 KernelSU MaterialKernelSUTheme）。
 */
@Composable
fun MaterialLuckyTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colorMode = appSettings.colorMode
    val darkTheme = colorMode.resolveDarkTheme()
    val seedColor = rememberSeedColor(appSettings = appSettings, isDark = darkTheme)

    val colorScheme = rememberLuckyColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = colorMode.isAmoled,
        style = appSettings.paletteStyle,
        specVersion = appSettings.colorSpec,
    )
    val animatedColorScheme = colorScheme.animateAsState()

    themeWindowAppearance(darkTheme)

    // 模式与色板下发到组合树（对齐 KernelSU LocalColorMode / 色板本地化读取）
    CompositionLocalProvider(
        LocalColorMode provides colorMode,
        LocalLuckyColorScheme provides animatedColorScheme,
    ) {
        // material3 1.5.0-alpha29：Expressive 公开入口（1.4.0 稳定版中该 API 为 internal 不可用）
        MaterialExpressiveTheme(
            colorScheme = animatedColorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/**
 * 主题分发（对齐 KernelSU `KernelSUTheme`）：按 [uiMode] 选择 Miuix 或 Material 外观线。
 */
@Composable
fun LuckyTheme(
    appSettings: AppSettings,
    uiMode: UiMode = LocalUiMode.current,
    content: @Composable () -> Unit,
) {
    // 切换 ui_mode 时两条外观线位于不同的组合位置：直接切换会销毁重建整棵内容子树
    //（NavHost / NavController / 返回栈 / 页面内 remember 全部丢失，观感等同重启应用）。
    // movableContentOf 把同一份内容在两条外观线之间「搬移」：槽表（状态）随内容走，只有主题被替换。
    // content 通过 rememberUpdatedState 间接化，避免把首次组合的 lambda 实例永久固化。
    val currentContent = rememberUpdatedState(content)
    val movableContent = remember { movableContentOf { currentContent.value() } }
    when (uiMode) {
        UiMode.Miuix -> MiuixLuckyTheme(appSettings = appSettings, content = movableContent)
        UiMode.Material -> MaterialLuckyTheme(appSettings = appSettings, content = movableContent)
    }
}

/** 深色切换时同步状态栏 / 导航栏前景色（对齐 KernelSU） */
@Composable
internal fun themeWindowAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    LaunchedEffect(darkTheme) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

/**
 * 跟随应用偏好的 LuckyTheme 入口：主题（dark_theme / use_dynamic_color / key_color /
 * palette_style / color_spec）与外壳行为（ShellSettings）。
 * Compose 界面的统一入口：P1 试点 ComposeView、P2 主壳 setContent 均包一层 LuckyAppTheme。
 *
 * 主题页（ThemeScreen）写入偏好后会调用 [ThemePrefs.notifyChanged]，revision 自增即重读设置，
 * 全应用配色与外壳行为均无需 recreate Activity 即时生效；明暗模式另外要经
 * `ThemeUtils.initTheme(context)` 同步平台 DayNight 主题（AppCompat 在明暗实际变化时
 * 会重建 Activity），否则矢量图的 `?attr/colorControlNormal` 仍按旧明暗解析而不可见。
 *
 * 界面缩放（page_scale）对齐 KernelSU：覆盖 [LocalDensity]，只缩放 density，fontScale 原样透传。
 */
@Composable
fun LuckyAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // revision 变化表示主题页刚写入偏好：重新读取设置即可即时生效
    val revision = ThemePrefs.revision
    val appSettings = remember(context, revision) { ThemeController.getAppSettings(context) }
    val shellSettings = remember(context, revision) { ShellSettingsController.get(context) }
    val uiMode = remember(context, revision) { ThemeController.getUiMode(context) }

    val systemDensity = LocalDensity.current
    val density = remember(systemDensity, shellSettings.pageScale) {
        Density(systemDensity.density * shellSettings.pageScale, systemDensity.fontScale)
    }

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalUiMode provides uiMode,
        LocalEnableNavigationBadge provides shellSettings.navigationBadge,
        LocalEnableBlur provides shellSettings.enableBlur,
        LocalEnableFloatingBottomBar provides shellSettings.enableFloatingBottomBar,
        LocalEnableFloatingBottomBarBlur provides shellSettings.enableFloatingBottomBarBlur,
        LocalEnableSwipeDismiss provides shellSettings.swipeDismiss,
        LocalPagerInterceptionMode provides shellSettings.pagerInterceptionMode,
        LocalModuleDescriptionMaxLines provides shellSettings.moduleDescriptionMaxLines,
    ) {
        LuckyTheme(appSettings = appSettings, uiMode = uiMode, content = content)
    }
}

/** 当前色板（主题预览卡片等需要显式拿到色板的场景使用） */
val LocalLuckyColorScheme = androidx.compose.runtime.staticCompositionLocalOf<ColorScheme> {
    error("LocalLuckyColorScheme not provided")
}
