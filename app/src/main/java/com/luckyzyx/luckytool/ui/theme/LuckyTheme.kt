@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.getString

/**
 * 深色模式偏好（SettingsPrefs["dark_theme"]），与旧 View 时代 ThemeUtils.initTheme 语义一致：
 * "0" 跟随系统 / "1" 强制夜间 / "2" 强制白天。
 */
enum class LuckyDarkTheme(val prefValue: String) {
    FollowSystem("0"),
    On("1"),
    Off("2");

    companion object {
        fun from(context: Context): LuckyDarkTheme =
            when (context.getString(SettingsPrefs, "dark_theme", FollowSystem.prefValue)) {
                On.prefValue -> On
                Off.prefValue -> Off
                else -> FollowSystem
            }
    }
}

/**
 * LuckyTool 统一 Compose 主题：Material 3 Expressive（material3 1.5.0-alpha29，
 * Google 官方 Expressive 线；1.4.0 稳定版不含公开 Expressive API，故按用户确认选 alpha29）。
 *
 * - 动态取色沿用 View 时代偏好 use_dynamic_color（默认开），Android 12+ 可用时走
 *   dynamicXxxColorScheme，否则回落到 M3 基线色（与 res/values/colors.xml 的 md_theme_*
 *   基线紫调色板一致，即默认 light/darkColorScheme()）。
 * - 深色模式沿用 dark_theme 偏好；不再依赖 AppCompat 的 setDefaultNightMode 代理。
 *
 * 注意：dark_theme / use_dynamic_color 修改后按既有约定触发 Activity recreate（见 SettingsScreen），
 * 因此主题参数在每次组合树重建时重新读取偏好即可，无需运行时监听。
 */
@Composable
fun LuckyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = ThemeUtils.isDynamicColorsEnabled(LocalContext.current),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    // material3 1.5.0-alpha29：Expressive 公开入口（1.4.0 稳定版中该 API 为 internal 不可用）
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

/**
 * 跟随应用偏好（dark_theme + use_dynamic_color）的 LuckyTheme。
 * Compose 界面的统一入口：P1 试点 ComposeView、P2 主壳 setContent 均包一层 LuckyAppTheme。
 */
@Composable
fun LuckyAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkPref = remember(context) { LuckyDarkTheme.from(context) }
    val dynamicColor = remember(context) { ThemeUtils.isDynamicColorsEnabled(context) }
    val darkTheme = when (darkPref) {
        LuckyDarkTheme.On -> true
        LuckyDarkTheme.Off -> false
        LuckyDarkTheme.FollowSystem -> isSystemInDarkTheme()
    }
    LuckyTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
