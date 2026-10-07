package com.luckyzyx.luckytool.ui.shell

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getFloat
import com.luckyzyx.luckytool.utils.getInt

/** 页面切换手势（对齐 KernelSU 使用的 miuix `PagerInterceptionMode` 序号） */
const val PagerModeNative = 0
const val PagerModeCrossAxis = 1
const val PagerModeIosLike = 2

/** 界面缩放下限 / 上限（对齐 KernelSU 滑块轨道 0.8f..1.1f） */
const val PageScaleMin = 0.8f
const val PageScaleMax = 1.1f

/** 模块描述行数下限 / 上限（对齐 KernelSU 滑块 1f..5f，steps = 3） */
const val ModuleLinesMin = 1
const val ModuleLinesMax = 5

/**
 * 外壳行为偏好（迁移 KernelSU 主题页中与配色无关的项）。
 *
 * 偏好键与 KernelSU 保持一致，默认值取其仓库默认值：
 * enable_navigation_badge = true、enable_predictive_back = false、enable_swipe_dismiss = true、
 * pager_interception_mode = 1、page_scale = 1.0f、module_description_max_lines = 4。
 */
@Immutable
data class ShellSettings(
    val navigationBadge: Boolean = true,
    val predictiveBack: Boolean = false,
    val swipeDismiss: Boolean = true,
    val pagerInterceptionMode: Int = PagerModeCrossAxis,
    val pageScale: Float = 1.0f,
    val moduleDescriptionMaxLines: Int = 4,
)

object ShellSettingsController {
    const val KEY_NAVIGATION_BADGE = "enable_navigation_badge"
    const val KEY_PREDICTIVE_BACK = "enable_predictive_back"
    const val KEY_SWIPE_DISMISS = "enable_swipe_dismiss"
    const val KEY_PAGER_MODE = "pager_interception_mode"
    const val KEY_PAGE_SCALE = "page_scale"
    const val KEY_MODULE_LINES = "module_description_max_lines"

    fun get(context: Context): ShellSettings = ShellSettings(
        navigationBadge = context.getBoolean(SettingsPrefs, KEY_NAVIGATION_BADGE, true),
        predictiveBack = context.getBoolean(SettingsPrefs, KEY_PREDICTIVE_BACK, false),
        swipeDismiss = context.getBoolean(SettingsPrefs, KEY_SWIPE_DISMISS, true),
        pagerInterceptionMode = context.getInt(SettingsPrefs, KEY_PAGER_MODE, PagerModeCrossAxis)
            .coerceIn(PagerModeNative, PagerModeIosLike),
        pageScale = context.getFloat(SettingsPrefs, KEY_PAGE_SCALE, 1.0f)
            .coerceIn(PageScaleMin, PageScaleMax),
        moduleDescriptionMaxLines = context.getInt(SettingsPrefs, KEY_MODULE_LINES, 4)
            .coerceIn(ModuleLinesMin, ModuleLinesMax),
    )
}

/** 底部导航角标开关 */
val LocalEnableNavigationBadge = staticCompositionLocalOf { true }

/** 边缘横移返回手势开关 */
val LocalEnableSwipeDismiss = staticCompositionLocalOf { true }

/** 页面切换手势模式（LuckyTool 无横向分页，仅透传偏好） */
val LocalPagerInterceptionMode = staticCompositionLocalOf { PagerModeCrossAxis }

/** 模块描述最大行数 */
val LocalModuleDescriptionMaxLines = staticCompositionLocalOf { 4 }

/**
 * 底部导航角标数据源。
 *
 * KernelSU 用「已授权应用数 / 已启用模块数」驱动角标；LuckyTool 没有这两类计数，
 * 这里用主页更新检查的结果驱动：发现新版本时在「主页」tab 上显示角标。
 */
@Stable
object ShellBadgeState {
    var updateAvailable by mutableStateOf(false)
}
