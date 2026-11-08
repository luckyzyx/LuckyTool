package com.luckyzyx.luckytool.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getInt
import com.luckyzyx.luckytool.utils.getString
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * 主题模式。
 *
 * 注意 [value] 不是枚举声明顺序，而是偏好键 `SettingsPrefs["dark_theme"]` 的存量取值：
 * 0 跟随系统 / 1 深色 / 2 浅色 / 3 深色(AMOLED) 沿用旧版 `res/values/arrays.xml` 的
 * `dark_theme` 条目顺序（旧版 DropDownPreference 的 entryValues 即 0/1/2，见提交
 * 24a68742b「修改暗黑模式配置的存储值」），4-7 为动态取色(Monet)的同义变体，规则为
 * 「非 Monet 值 + 4」（4 跟随系统 / 5 深色 / 6 浅色 / 7 深色 AMOLED）。
 * 因此升级不会改变既有观感：旧版存下的「总是开启(1)」仍解析为深色。
 *
 * 该取值表同时被 [com.luckyzyx.luckytool.utils.ThemeUtils] 用于设置 Activity 的
 * DayNight 模式。两侧必须保持同一明暗：Compose 界面为深色而平台主题为浅色时，
 * 矢量图的 `?attr/colorControlNormal` 会解析成浅色主题下的黑色，深色界面上图标不可见。
 */
enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(2),
    DARK(1),
    DARK_AMOLED(3),
    MONET_SYSTEM(4),
    MONET_LIGHT(6),
    MONET_DARK(5),
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
 */
val PaletteStyle.supportsSpec2025: Boolean
    get() = this == PaletteStyle.TonalSpot ||
            this == PaletteStyle.Neutral ||
            this == PaletteStyle.Vibrant ||
            this == PaletteStyle.Expressive

/** 取色规格按调色风格做有效值回退 */
fun ColorSpec.SpecVersion.effectiveFor(style: PaletteStyle): ColorSpec.SpecVersion =
    if (this == ColorSpec.SpecVersion.SPEC_2025 && !style.supportsSpec2025) {
        ColorSpec.SpecVersion.SPEC_2021
    } else {
        this
    }

/** 主题相关的应用设置快照 */
data class AppSettings(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    /** 0 表示跟随系统取色 */
    val keyColor: Int = 0,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
    val dynamicColor: Boolean = true,
    /** COUI 线是否跟随 Material 取色；false 表示使用 COUI 自带预设配色 */
    val couiDynamicColor: Boolean = false,
)

/** 主题偏好读取。 */
object ThemeController {

    /** 界面风格偏好键：ui_mode，取值 "coui" / "material"。 */
    const val KEY_UI_MODE = "ui_mode"

    /** COUI 线专用取色开关：开启后跟随 Material 取色设置，关闭则使用 COUI 预设配色。 */
    const val KEY_COUI_DYNAMIC_COLOR = "coui_dynamic_color"

    fun getUiMode(context: Context): UiMode = UiMode.fromValue(
        context.getString(SettingsPrefs, KEY_UI_MODE, UiMode.DEFAULT_VALUE)
    ).coerceAvailable()

    /**
     * 读取主题设置。
     *
     * Material 线的取色由 key_color / use_dynamic_color 决定；COUI 线由 coui_dynamic_color
     * 决定使用 COUI 预设配色还是跟随同一份取色设置，跟随时才把结果翻译成 COUI 语义色。
     */
    fun getAppSettings(context: Context): AppSettings {
        val colorMode = ColorMode.fromValue(
            context.getString(SettingsPrefs, "dark_theme", "0").toIntOrNull() ?: 0
        )
        val keyColor = context.getInt(SettingsPrefs, "key_color", 0)
        val paletteStyle = PaletteStyle.entries.firstOrNull {
            it.name == context.getString(
                SettingsPrefs,
                "palette_style",
                PaletteStyle.TonalSpot.name
            )
        } ?: PaletteStyle.TonalSpot
        val colorSpec = ColorSpec.SpecVersion.entries.firstOrNull {
            it.name == context.getString(
                SettingsPrefs,
                "color_spec",
                ColorSpec.SpecVersion.SPEC_2025.name
            )
        } ?: ColorSpec.SpecVersion.SPEC_2025
        val dynamicColor = context.getBoolean(SettingsPrefs, "use_dynamic_color", true)
        val couiDynamicColor = context.getBoolean(SettingsPrefs, KEY_COUI_DYNAMIC_COLOR, false)
        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            dynamicColor = dynamicColor,
            couiDynamicColor = couiDynamicColor,
        )
    }
}

/**
 * 主题偏好变更通知：主题页写入偏好后自增 [revision]，让 [AppTheme] 重新读取主题设置，
 * 从而在页内即时预览配色，不必 recreate Activity 即生效。
 */
object ThemePrefs {
    var revision by mutableIntStateOf(0)
        private set

    fun notifyChanged() {
        revision++
    }
}

/** 当前颜色模式 */
val LocalColorMode = staticCompositionLocalOf { ColorMode.SYSTEM }

/** 组合期内读取当前颜色模式 */
val isInDarkTheme: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalColorMode.current.isDark
