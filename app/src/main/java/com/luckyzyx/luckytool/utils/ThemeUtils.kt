@file:Suppress("unused")

package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.luckyzyx.luckytool.ui.theme.ColorMode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ThemeUtils {

    fun isDynamicColorsEnabled(context: Context): Boolean {
        val enable = context.getBoolean(SettingsPrefs, "use_dynamic_color", true)
        return enable && Build.VERSION.SDK_INT >= 31
    }

    fun setDynamicColorsEnabled(context: Context, enabled: Boolean) {
        context.putBoolean(SettingsPrefs, "use_dynamic_color", enabled)
    }

    /**
     * 是否为夜间模式
     */
    val Context.isNightMode get() = isNightMode(resources.configuration)

    /**
     * 是否为夜间模式
     * @param configuration Configuration
     * @return Boolean
     */
    fun isNightMode(configuration: Configuration): Boolean {
        return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 存量 `dark_theme` 取值 → AppCompatDelegate 夜间模式
     *
     * 取值表与 Compose 侧 [ColorMode] 共用一份（0 跟随系统 / 1 深色 / 2 浅色 /
     * 3 深色 AMOLED，4-7 为 Monet 变体），避免此处再写一套映射导致平台主题与
     * Compose 界面的明暗错位（曾造成深色模式下图标不可见）。
     * @param stored String 偏好中存储的取值
     * @return Int AppCompatDelegate.MODE_NIGHT_*
     */
    fun nightModeFor(stored: String): Int {
        val mode = ColorMode.fromValue(stored.toIntOrNull() ?: 0)
        return when {
            mode.isSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            mode.isDark -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    /**
     * 初始化设置主题模式
     * @param context Context
     */
    fun initTheme(context: Context) {
        val stored = context.getString(SettingsPrefs, "dark_theme", "0")
        AppCompatDelegate.setDefaultNightMode(nightModeFor(stored))
    }
}