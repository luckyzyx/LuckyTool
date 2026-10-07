package com.luckyzyx.luckytool.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getInt
import com.luckyzyx.luckytool.utils.getString
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * 主题模式（迁移自 KernelSU `ui/theme/Theme.kt` 的 ColorMode，取值完全一致）。
 *
 * LuckyTool 沿用既有偏好键 `SettingsPrefs["dark_theme"]`，其历史取值 0/1/2/3 与
 * KernelSU 的 SYSTEM / LIGHT / DARK / DARK_AMOLED 一一对应，因此升级不会改变既有观感。
 * KernelSU 的 MONET_*（3/4/5，动态取色跨明暗）在 LuckyTool 由 `use_dynamic_color`
 * 开关独立承担，故此处不再作为独立模式暴露，但 [fromValue] 仍可解析这些历史值。
 */
enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    DARK_AMOLED(3),
    MONET_SYSTEM(4),
    MONET_LIGHT(5),
    MONET_DARK(6),
    MONET_DARK_AMOLED(7);

    /** 是否跟随系统明暗 */
    val isSystem: Boolean
        get() = this == SYSTEM || this == MONET_SYSTEM

    /** 是否为深色 */
    val isDark: Boolean
        get() = this == DARK || this == MONET_DARK || this == DARK_AMOLED || this == MONET_DARK_AMOLED

    /** 是否为 AMOLED 纯黑深色 */
    val isAmoled: Boolean
        get() = this == DARK_AMOLED || this == MONET_DARK_AMOLED

    /** 是否走动态取色（Follow system color）分支 */
    val isMonet: Boolean
        get() = this == MONET_SYSTEM || this == MONET_LIGHT || this == MONET_DARK || this == MONET_DARK_AMOLED

    /** 去动态取色：保留明暗与 AMOLED 语义 */
    fun toNonMonetMode(): ColorMode = when (this) {
        MONET_SYSTEM -> SYSTEM
        MONET_LIGHT -> LIGHT
        MONET_DARK -> DARK
        MONET_DARK_AMOLED -> DARK_AMOLED
        else -> this
    }

    /** 加动态取色：保留明暗与 AMOLED 语义 */
    fun toMonetMode(): ColorMode = when (this) {
        SYSTEM -> MONET_SYSTEM
        LIGHT -> MONET_LIGHT
        DARK -> MONET_DARK
        DARK_AMOLED -> MONET_DARK_AMOLED
        else -> this
    }

    companion object {
        fun fromValue(value: Int): ColorMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

/**
 * SPEC_2025 只对部分调色风格实现了算法，其余风格需回退到 SPEC_2021。
 * 迁移自 KernelSU `PaletteStyle.supportsSpec2025` / `ColorSpec.SpecVersion.effectiveFor`。
 */
val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
        this == PaletteStyle.Neutral ||
        this == PaletteStyle.Vibrant ||
        this == PaletteStyle.Expressive

/** 取色规格按调色风格做有效值回退（对齐 KernelSU） */
fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

/** 主题相关的应用设置快照（迁移自 KernelSU `AppSettings`） */
data class AppSettings(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    /** 0 表示跟随系统取色 */
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val dynamicColor: Boolean = true,
)

/** 主题偏好读取（对应 KernelSU `ThemeController` 的 SettingsRepository 读取） */
object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val colorMode = ColorMode.fromValue(context.getString(SettingsPrefs, "dark_theme", "0")?.toIntOrNull() ?: 0)
        val keyColor = context.getInt(SettingsPrefs, "key_color", 0)
        val paletteStyle = PaletteStyle.entries.firstOrNull {
            it.name == context.getString(SettingsPrefs, "palette_style", PaletteStyle.TonalSpot.name)
        } ?: PaletteStyle.TonalSpot
        val colorSpec = ColorSpec.SpecVersion.entries.firstOrNull {
            it.name == context.getString(SettingsPrefs, "color_spec", ColorSpec.SpecVersion.SPEC_2025.name)
        } ?: ColorSpec.SpecVersion.SPEC_2025
        val dynamicColor = context.getBoolean(SettingsPrefs, "use_dynamic_color", true)
        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            dynamicColor = dynamicColor,
        )
    }
}

/**
 * 主题偏好变更通知：主题页写入偏好后自增 [revision]，让 [LuckyAppTheme] 重新读取主题设置，
 * 从而在页内即时预览配色，无需 recreate Activity（旧实现靠 recreate 生效）。
 */
object ThemePrefs {
    var revision by mutableStateOf(0)
        private set

    fun notifyChanged() {
        revision++
    }
}

/** 当前颜色模式（对齐 KernelSU `LocalColorMode`） */
val LocalColorMode = staticCompositionLocalOf { ColorMode.SYSTEM }

/** 组合期内读取当前颜色模式（对齐 KernelSU `isInDarkTheme`） */
val isInDarkTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalColorMode.current.isDark
