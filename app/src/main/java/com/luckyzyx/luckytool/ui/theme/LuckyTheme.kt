@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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

/** 关闭动态取色时的种子色：M3 基线紫，观感对齐旧 light/darkColorScheme() 默认值。 */
private val DefaultSeedColor = Color(0xFF6750A4)

/**
 * LuckyTool 统一 Compose 主题：Material 3 Expressive（material3 1.5.0-alpha29，
 * Google 官方 Expressive 线；1.4.0 稳定版不含公开 Expressive API，故按用户确认选 alpha29）。
 *
 * 取色管线对齐 KernelSU（material-kolor 5.0.1，TonalSpot + SPEC_2025）：
 * - use_dynamic_color 开启且 Android 12+ 时以系统 primary 为种子动态取色；
 * - 否则使用默认种子色生成稳定色板；
 * - 主题切换时全色板弹性动画过渡，并同步系统栏前景色（对齐 KernelSU MaterialKernelSUTheme）。
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
    val colorScheme = rememberLuckyColorScheme(
        seedColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Color.Unspecified
        } else {
            DefaultSeedColor
        },
        isDark = darkTheme,
    )
    val animatedColorScheme = colorScheme.animateAsState()

    // 对齐 KernelSU：深色切换时同步状态栏 / 导航栏前景色
    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // material3 1.5.0-alpha29：Expressive 公开入口（1.4.0 稳定版中该 API 为 internal 不可用）
    MaterialExpressiveTheme(
        colorScheme = animatedColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
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
