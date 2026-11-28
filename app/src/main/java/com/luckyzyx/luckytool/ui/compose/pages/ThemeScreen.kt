package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppCard
import com.luckyzyx.luckytool.ui.compose.components.AppSlider
import com.luckyzyx.luckytool.ui.compose.components.LocalPageTopInset
import com.luckyzyx.luckytool.ui.compose.components.PageScaffold
import com.luckyzyx.luckytool.ui.compose.components.PageScreen
import com.luckyzyx.luckytool.ui.compose.components.PrefIcon
import com.luckyzyx.luckytool.ui.compose.components.PrefNote
import com.luckyzyx.luckytool.ui.compose.components.PrefPageBuilder
import com.luckyzyx.luckytool.ui.compose.components.PrefRowTitle
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialToggleButton
import com.luckyzyx.luckytool.ui.compose.components.pageInset
import com.luckyzyx.luckytool.ui.compose.isRenderEffectSupported
import com.luckyzyx.luckytool.ui.shell.FloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.PageScaleMax
import com.luckyzyx.luckytool.ui.shell.PageScaleMin
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.ui.theme.keyColorOptions
import com.luckyzyx.luckytool.ui.theme.rememberAppColorScheme
import com.luckyzyx.luckytool.ui.theme.rememberSeedColor
import com.luckyzyx.luckytool.ui.theme.resolveDarkTheme
import com.luckyzyx.luckytool.utils.PredictiveBackUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getFloat
import com.luckyzyx.luckytool.utils.getInt
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putFloat
import com.luckyzyx.luckytool.utils.putInt
import com.luckyzyx.luckytool.utils.putString
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.math.roundToInt
import com.composables.icons.lucide.R as LucideR
import com.itos.coui.text.Text as CouiText

/**
 * 主题与配色页入口。
 *
 * COUI 与 Material 两种外观共用本页：配色、取色、外壳手势与悬浮底栏都在这里配置。
 * 状态在两条线之外只建一份，外观切换时页内读数与滑条中间态都不回退。
 *
 * 页面组成：主题预览卡片 → 主题色选择（跟随系统 + 15 预设）→ 主题模式按钮组
 * → 调色风格 / 色彩规格下拉 → 动态取色开关 →（COUI 线）跟随 Material 配色开关 → 预测性返回手势。
 *
 * 两条外观线共用同一份列表与偏好；COUI 线额外显示「跟随 Material 配色」开关
 * （coui_dynamic_color，默认关，用 COUI 预设配色），并隐藏本线无意义的悬浮底栏项。
 *
 * 骨架与其余页面一致：外壳用 [PageScaffold]，列表与行交给 [PageScreen]，行声明走 DSL；
 * 只有预览卡、取色横滑、模式选择与缩放滑条用 [PrefPageBuilder.custom] 的 `raw = true` 原样排版。
 *
 * 偏好键沿用 LuckyTool 既有约定（SettingsPrefs）：
 * dark_theme（0 跟随系统 / 1 深色 / 2 浅色 / 3 深色 AMOLED）、key_color、
 * palette_style、color_spec、use_dynamic_color、coui_dynamic_color 与
 * coui_contour_glow（COUI 线专用）、
 * enable_predictive_back、enable_swipe_dismiss。
 * 写入后通过 [ThemePrefs.notifyChanged] 让应用主题即时重算，无需 recreate。
 */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state = rememberThemeScreenState()
    val isCouiLine = LocalUiMode.current == UiMode.Coui
    val isDark = state.colorMode.resolveDarkTheme()
    // 行文案在这里一次取好：DSL 的行 lambda 不是 composable，stringResource 不能在里面读
    val text = themeScreenText()
    val appSettings = AppSettings(
        colorMode = state.colorMode,
        keyColor = state.keyColor,
        paletteStyle = state.paletteStyle,
        colorSpec = state.colorSpec,
        dynamicColor = state.dynamicColor,
        couiDynamicColor = state.couiDynamicColor,
        couiContourGlow = state.couiContourGlow,
    )
    // 本页偏好文件（SettingsPrefs）：行由 DSL 统一读写，页面只声明行与页面级状态
    val prefsState = remember { PrefState.of(context.applicationContext, SettingsPrefs) }

    PageScaffold(
        title = stringResource(R.string.settings_theme),
        onBack = onBack,
        // 主题页不是导航页：顶栏保持收起样式，两条线都不挂折叠滚动连接
        collapseEnabled = false,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        // 外壳没含的底部系统栏高度经 [LocalShellBottomInset] 交给 PageScreen，末尾留白与其余页面
        // 同一口径（PageScreen 收 8dp + 该值）：本页原来的 16dp + 未含系统栏高度因此保持不变
        val systemBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
        val leftoverBottom = 8.dp + (systemBarBottom - padding.calculateBottomPadding())
            .coerceAtLeast(0.dp)
        CompositionLocalProvider(
            LocalPageTopInset provides padding.calculateTopPadding(),
            LocalShellBottomInset provides leftoverBottom,
        ) {
            PageScreen(
                state = prefsState,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    ),
            ) {
                themeScreenRows(
                    state = state,
                    text = text,
                    isCouiLine = isCouiLine,
                    isDark = isDark,
                    appSettings = appSettings,
                    context = context,
                )
            }
        }
    }
}

/** 外壳（模糊 / 悬浮底栏 / 分页手势）相关偏好键 */
private const val KeyEnableBlur = "enable_blur"
private const val KeyEnableFloatingBottomBar = "enable_floating_bottom_bar"
private const val KeyPaletteStyle = "palette_style"
private const val KeyColorSpec = "color_spec"
private const val KeyKeyColor = "key_color"
private const val KeyDarkTheme = "dark_theme"

/**
 * 主题页的页内状态。
 *
 * 两条外观线共用同一份：外观切换时页内读数不回退，滑条拖动中的临时值也不会丢。
 * 这里只管取值与页内更新，落盘与主题重算由各设置项在调用点完成——
 * 每个键的生效时机不同（有的要立即重算主题，有的要松手才提交）。
 */
private class ThemeScreenState(context: Context) {

    var colorMode by mutableStateOf(
        ColorMode.fromValue(
            context.getString(
                SettingsPrefs,
                "dark_theme",
                "0"
            ).toIntOrNull() ?: 0
        )
    )

    var keyColor by mutableIntStateOf(context.getInt(SettingsPrefs, "key_color", 0))

    var dynamicColor by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            "use_dynamic_color",
            true
        )
    )

    /** COUI 线是否跟随 Material 取色；默认关，界面用 COUI 自带预设配色 */
    var couiDynamicColor by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            ThemeController.KEY_COUI_DYNAMIC_COLOR,
            false
        )
    )

    /** COUI 线轮廓光（组件表面描边与内光晕）；默认开，与 COUI 默认观感一致 */
    var couiContourGlow by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            ThemeController.KEY_COUI_CONTOUR_GLOW,
            true
        )
    )

    var paletteStyle by mutableStateOf(
        PaletteStyle.entries.firstOrNull {
            it.name == context.getString(
                SettingsPrefs,
                "palette_style",
                PaletteStyle.TonalSpot.name
            )
        } ?: PaletteStyle.TonalSpot
    )

    var colorSpec by mutableStateOf(
        ColorSpec.SpecVersion.entries.firstOrNull {
            it.name == context.getString(
                SettingsPrefs,
                "color_spec",
                ColorSpec.SpecVersion.SPEC_2025.name
            )
        } ?: ColorSpec.SpecVersion.SPEC_2025
    )

    var enableBlur by mutableStateOf(context.getBoolean(SettingsPrefs, KeyEnableBlur, false))

    var enableFloatingBottomBar by mutableStateOf(
        context.getBoolean(SettingsPrefs, KeyEnableFloatingBottomBar, false)
    )

    var floatingBottomBarStyle by mutableStateOf(
        ShellSettingsController.get(context).floatingBottomBarStyle
    )

    /** 预测式返回手势（需 Android 14+，反射 ApplicationInfo.setEnableOnBackInvokedCallback） */
    var predictiveBack by mutableStateOf(
        context.getBoolean(SettingsPrefs, ShellSettingsController.KEY_PREDICTIVE_BACK, true)
    )

    var categoryBadgeColors by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            ShellSettingsController.KEY_CATEGORY_BADGE_COLORS,
            true
        )
    )

    var pageScale by mutableFloatStateOf(
        context.getFloat(SettingsPrefs, ShellSettingsController.KEY_PAGE_SCALE, 1.0f)
            .coerceIn(PageScaleMin, PageScaleMax)
    )
}

/** 页内状态与页面同生命周期，只在进入页面时读一次偏好 */
@Composable
private fun rememberThemeScreenState(): ThemeScreenState {
    val context = LocalContext.current
    return remember(context) { ThemeScreenState(context) }
}

/**
 * 主题页行的静态文案。
 *
 * DSL 的行 lambda 不是 composable，[stringResource] 不能在里面读，所以文案在 [themeScreenText] 里
 * 一次取好，再按字段传给各个行。
 */
private class ThemeScreenText(
    val paletteStyle: String,
    val colorSpec: String,
    val couiFollowMaterial: String,
    val couiFollowMaterialSummary: String,
    val couiContourGlow: String,
    val couiContourGlowSummary: String,
    val useDynamicColor: String,
    val useDynamicColorSummary: String,
    val enableBlur: String,
    val enableBlurSummary: String,
    val floatingBottomBar: String,
    val floatingBottomBarSummary: String,
    val floatingBottomBarStyle: String,
    val floatingStyleClassic: String,
    val floatingStyleLiquidGlass: String,
    val categoryBadgeColors: String,
    val categoryBadgeColorsSummary: String,
    val predictiveBack: String,
    val predictiveBackSummary: String,
    val pageScale: String,
    val pageScaleSummary: String,
)

@Composable
private fun themeScreenText(): ThemeScreenText = ThemeScreenText(
    paletteStyle = stringResource(R.string.palette_style_title),
    colorSpec = stringResource(R.string.color_spec_title),
    couiFollowMaterial = stringResource(R.string.coui_follow_material_color),
    couiFollowMaterialSummary = stringResource(R.string.coui_follow_material_color_summary),
    couiContourGlow = stringResource(R.string.coui_contour_glow),
    couiContourGlowSummary = stringResource(R.string.coui_contour_glow_summary),
    useDynamicColor = stringResource(R.string.use_dynamic_color),
    useDynamicColorSummary = stringResource(R.string.use_dynamic_color_summary),
    enableBlur = stringResource(R.string.settings_enable_blur),
    enableBlurSummary = stringResource(R.string.settings_enable_blur_summary),
    floatingBottomBar = stringResource(R.string.settings_floating_bottom_bar),
    floatingBottomBarSummary = stringResource(R.string.settings_floating_bottom_bar_summary),
    floatingBottomBarStyle = stringResource(R.string.settings_floating_bottom_bar_style),
    floatingStyleClassic = stringResource(R.string.settings_floating_style_classic),
    floatingStyleLiquidGlass = stringResource(R.string.settings_floating_style_liquid_glass),
    categoryBadgeColors = stringResource(R.string.settings_category_badge_colors),
    categoryBadgeColorsSummary = stringResource(R.string.settings_category_badge_colors_summary),
    predictiveBack = stringResource(R.string.settings_enable_predictive_back),
    predictiveBackSummary = stringResource(R.string.settings_enable_predictive_back_summary),
    pageScale = stringResource(R.string.settings_page_scale),
    pageScaleSummary = stringResource(R.string.settings_page_scale_summary),
)

/**
 * 主题页正文：两条外观线共用一份行声明，行写法与其他页面一致（DSL 的 switch / list）。
 *
 * 预览卡、取色横滑、模式选择与缩放滑条在 DSL 里没有对应行，用 [PrefPageBuilder.custom] 的
 * `raw = true` 原样排版：不套卡片、不参与分组，位置由这里的声明顺序决定。
 */
private fun PrefPageBuilder.themeScreenRows(
    state: ThemeScreenState,
    text: ThemeScreenText,
    isCouiLine: Boolean,
    isDark: Boolean,
    appSettings: AppSettings,
    context: Context,
) {
    custom(raw = true) {
        ThemePreviewCard(
            appSettings = appSettings,
            isDark = isDark,
        )
    }

    // 首个按钮是「跟随壁纸取色」，其余为固定主题色；COUI 未开启「跟随 Material 配色」时整行隐藏。
    custom(raw = true) {
        AnimatedVisibility(
            visible = !isCouiLine || state.couiDynamicColor,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    ColorButtonMaterial(
                        color = Color.Unspecified,
                        isSelected = state.dynamicColor,
                        appSettings = appSettings,
                        isDark = isDark,
                        onClick = {
                            state.keyColor = 0
                            context.putInt(SettingsPrefs, "key_color", 0)
                            state.dynamicColor = true
                            context.putBoolean(SettingsPrefs, "use_dynamic_color", true)
                            ThemePrefs.notifyChanged()
                        },
                    )
                }
                items(keyColorOptions) { color ->
                    ColorButtonMaterial(
                        color = Color(color),
                        isSelected = !state.dynamicColor && state.keyColor == color,
                        appSettings = appSettings,
                        isDark = isDark,
                        onClick = {
                            state.keyColor = color
                            context.putInt(SettingsPrefs, "key_color", color)
                            state.dynamicColor = false
                            context.putBoolean(SettingsPrefs, "use_dynamic_color", false)
                            ThemePrefs.notifyChanged()
                        },
                    )
                }
            }
        }
    }

    custom(raw = true) { ThemeModeSelector(state = state) }

    // 调色风格 / 色彩规格两条外观线共用；COUI 未开启「跟随 Material 配色」时整组隐藏。
    if (!isCouiLine || state.couiDynamicColor) {
        group {
            val styles = PaletteStyle.entries
            list(
                key = KeyPaletteStyle,
                title = text.paletteStyle,
                entries = styles.map { it.name }.toTypedArray(),
                entryValues = styles.map { it.name }.toTypedArray(),
                default = PaletteStyle.TonalSpot.name,
                leading = {
                    PrefIcon(
                        icon = Icons.Rounded.Style,
                        painter = painterResource(LucideR.drawable.lucide_ic_paintbrush),
                    )
                },
                onChange = { name ->
                    state.paletteStyle = styles.firstOrNull { it.name == name } ?: PaletteStyle.TonalSpot
                    ThemePrefs.notifyChanged()
                },
            )
            val specs = ColorSpec.SpecVersion.entries
            list(
                key = KeyColorSpec,
                title = text.colorSpec,
                entries = specs.map { it.name }.toTypedArray(),
                entryValues = specs.map { it.name }.toTypedArray(),
                default = ColorSpec.SpecVersion.SPEC_2025.name,
                leading = {
                    PrefIcon(
                        icon = Icons.Rounded.DesignServices,
                        painter = painterResource(LucideR.drawable.lucide_ic_pencil_ruler),
                    )
                },
                onChange = { name ->
                    state.colorSpec =
                        specs.firstOrNull { it.name == name } ?: ColorSpec.SpecVersion.SPEC_2025
                    ThemePrefs.notifyChanged()
                },
            )
        }
    }

    // 动态取色依赖 API 31（Monet）；COUI 线的「跟随 Material 配色」开关不依赖取色能力，API 31 以下也要出现。
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isCouiLine) {
        group {
            // COUI 线单独决定用预设配色还是跟随 Material 的取色设置；放在动态取色上方
            if (isCouiLine) {
                switch(
                    key = ThemeController.KEY_COUI_DYNAMIC_COLOR,
                    title = text.couiFollowMaterial,
                    summary = text.couiFollowMaterialSummary,
                    default = false,
                    leading = {
                        PrefIcon(
                            icon = Icons.Rounded.Colorize,
                            painter = painterResource(LucideR.drawable.lucide_ic_pipette),
                        )
                    },
                    onChange = {
                        state.couiDynamicColor = it
                        ThemePrefs.notifyChanged()
                    },
                )
                // 轮廓光只作用在 COUI 组件表面的描边与内光晕上，与配色选择互不影响
                switch(
                    key = ThemeController.KEY_COUI_CONTOUR_GLOW,
                    title = text.couiContourGlow,
                    summary = text.couiContourGlowSummary,
                    default = true,
                    leading = { PrefIcon(Icons.Rounded.Flare) },
                    onChange = {
                        state.couiContourGlow = it
                        ThemePrefs.notifyChanged()
                    },
                )
            }
            // COUI 未开启「跟随 Material 配色」时收起动态取色；API 31 以下同样不出现
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                switch(
                    key = "use_dynamic_color",
                    title = text.useDynamicColor,
                    summary = text.useDynamicColorSummary,
                    default = true,
                    visible = !isCouiLine || state.couiDynamicColor,
                    leading = {
                        PrefIcon(
                            icon = Icons.Rounded.Style,
                            painter = painterResource(LucideR.drawable.lucide_ic_paintbrush),
                        )
                    },
                    onChange = { enabled ->
                        state.dynamicColor = enabled
                        // 开启动态取色时把自定义主题色归零，保证"跟随系统"色板与开关状态一致
                        if (enabled) {
                            state.keyColor = 0
                            context.putInt(SettingsPrefs, "key_color", 0)
                        }
                        ThemePrefs.notifyChanged()
                    },
                )
            }
        }
    }

    group {
        // 模糊开关只对 Material 线生效：低于 API 31 没有 RenderEffect，
        // COUI 底栏的玻璃质感由自身渲染器提供，两种情况都不出现
        if (isRenderEffectSupported() && !isCouiLine) {
            switch(
                key = KeyEnableBlur,
                title = text.enableBlur,
                summary = text.enableBlurSummary,
                default = false,
                leading = { PrefIcon(Icons.Rounded.BlurOn) },
                onChange = {
                    state.enableBlur = it
                    ThemePrefs.notifyChanged()
                },
            )
        }
        // COUI 底栏本身就是浮起胶囊，悬浮底栏开关只对 Material 线有意义
        if (!isCouiLine) {
            switch(
                key = KeyEnableFloatingBottomBar,
                title = text.floatingBottomBar,
                summary = text.floatingBottomBarSummary,
                default = false,
                leading = { PrefIcon(Icons.Rounded.CallToAction) },
                onChange = {
                    state.enableFloatingBottomBar = it
                    ThemePrefs.notifyChanged()
                },
            )
            // 悬浮形态只在开启悬浮底栏时才有意义；存的不一定是当前生效形态（旧键回流），默认值取生效值
            if (state.enableFloatingBottomBar) {
                list(
                    key = ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE,
                    title = text.floatingBottomBarStyle,
                    entries = arrayOf(text.floatingStyleClassic, text.floatingStyleLiquidGlass),
                    entryValues = FloatingBottomBarStyle.entries.map { it.value }.toTypedArray(),
                    default = state.floatingBottomBarStyle.value,
                    leading = { PrefIcon(Icons.Rounded.Style) },
                    onChange = { value ->
                        state.floatingBottomBarStyle = FloatingBottomBarStyle.fromValue(value)
                        ThemePrefs.notifyChanged()
                    },
                )
            }
        }
        // 多彩徽标是 COUI 线的观感，Material 线固定用主题色单色徽标，所以开关只在 COUI 线出现
        if (isCouiLine) {
            switch(
                key = ShellSettingsController.KEY_CATEGORY_BADGE_COLORS,
                title = text.categoryBadgeColors,
                summary = text.categoryBadgeColorsSummary,
                default = true,
                leading = {
                    PrefIcon(
                        icon = Icons.Rounded.Palette,
                        painter = painterResource(LucideR.drawable.lucide_ic_palette),
                    )
                },
                onChange = {
                    state.categoryBadgeColors = it
                    ThemePrefs.notifyChanged()
                },
            )
        }
        if (PredictiveBackUtils.isSupported()) {
            switch(
                key = ShellSettingsController.KEY_PREDICTIVE_BACK,
                title = text.predictiveBack,
                summary = text.predictiveBackSummary,
                default = true,
                leading = {
                    PrefIcon(
                        icon = Icons.AutoMirrored.Rounded.MenuOpen,
                        painter = painterResource(LucideR.drawable.lucide_ic_panel_left_open),
                    )
                },
                onChange = { enabled ->
                    state.predictiveBack = enabled
                    // 即时生效：反射改写 ApplicationInfo 后重建 Activity（新窗口读取新 flag）
                    PredictiveBackUtils.applyImmediately(context, enabled)
                },
            )
        }
    }

    // 界面缩放（page_scale）：DSL 滑条只有 Int 档，这里是 Float 且存的就是 float，
    // 用 raw 自定义卡保留原控件与「拖动只更新页内状态、松手才提交」的语义
    custom(raw = true) {
        ThemeSliderCard(
            icon = Icons.Rounded.AspectRatio,
            painter = painterResource(LucideR.drawable.lucide_ic_scaling),
            title = text.pageScale,
            summary = text.pageScaleSummary,
            valueText = "${(state.pageScale * 100).roundToInt()}%",
            value = state.pageScale,
            valueRange = PageScaleMin..PageScaleMax,
            steps = 0,
            onValueChange = { state.pageScale = it },
            onValueChangeFinished = {
                context.putFloat(SettingsPrefs, ShellSettingsController.KEY_PAGE_SCALE, state.pageScale)
                ThemePrefs.notifyChanged()
            },
        )
    }
}

/**
 * 主题模式选择。
 *
 * 两条线统一用 Material 连体图标按钮组：四个模式固定一屏排开，
 * 选中态由按钮的 checked 表达；COUI 线复用同一控件，不再单独用分段条。
 */
@Composable
private fun ThemeModeSelector(state: ThemeScreenState) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val modes = listOf(
        ColorMode.SYSTEM to R.string.dark_theme_follow_system,
        ColorMode.LIGHT to R.string.dark_theme_off,
        ColorMode.DARK to R.string.dark_theme_on,
        ColorMode.DARK_AMOLED to R.string.dark_theme_on_amoled,
    )
    // 选中即写偏好并重算主题；两条线共用这一份落盘逻辑
    val select: (ColorMode) -> Unit = { mode ->
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        state.colorMode = mode
        context.putString(
            SettingsPrefs,
            KeyDarkTheme,
            mode.value.toString()
        )
        ThemePrefs.notifyChanged()
        // 同步平台 DayNight 主题：否则矢量图 ?attr/colorControlNormal
        // 仍按旧明暗解析，深色界面上的图标会变成黑色不可见
        ThemeUtils.initTheme(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pageInset(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        modes.chunked(4).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                rowModes.forEachIndexed { index, (mode, labelRes) ->
                    val label = stringResource(labelRes)
                    MaterialToggleButton(
                        checked = state.colorMode == mode,
                        onCheckedChange = { if (it) select(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            rowModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        if (LocalUiMode.current == UiMode.Coui) {
                            Icon(
                                painter = painterResource(
                                    when (mode) {
                                        ColorMode.SYSTEM -> LucideR.drawable.lucide_ic_sun_moon
                                        ColorMode.LIGHT -> LucideR.drawable.lucide_ic_sun
                                        ColorMode.DARK -> LucideR.drawable.lucide_ic_moon
                                        // AMOLED 是纯黑底，比普通深色更暗；太阳系图标只留给系统/亮色，这里用带星月亮区分
                                        else -> LucideR.drawable.lucide_ic_moon_star
                                    }
                                ),
                                contentDescription = label,
                            )
                        } else {
                            Icon(
                                imageVector = when (mode) {
                                    ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                    ColorMode.LIGHT -> Icons.Filled.Brightness7
                                    ColorMode.DARK -> Icons.Filled.Brightness3
                                    else -> Icons.Filled.Brightness1
                                },
                                contentDescription = label,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 滑杆卡片：图标 + 标题 / 说明 + 当前值，下面一条滑杆。
 *
 * 行首图标按外观线取：[painter] 给 COUI 线，[icon] 给 Material 线，前景色也随线走，
 * 与同页开关 / 下拉行的行首图标保持同一来源与同一颜色。
 *
 * 取值由调用方持有（页内状态），[AppSlider] 在两条外观线上写回的都是同一份；
 * material3 的 SliderState 是非受控语义，值必须由调用方写回，否则滑块不跟手。
 * 松手回调用于提交偏好，拖动过程中不落盘。
 */
@Composable
private fun ThemeSliderCard(
    icon: ImageVector,
    painter: Painter? = null,
    title: String,
    summary: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    // 行首图标走样式位：两条线各自优先自己的图标资源集（矢量图 / 绘画），
    // 着色与尺寸由样式位统一，和同页开关、下拉行保持一致
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .pageInset(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PrefIcon(icon = icon, painter = painter)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ThemeCardTitle(text = title)
                    ThemeCardSummary(text = summary)
                }
                ThemeCardSummary(text = valueText)
            }
            val sliderState = rememberSliderState(
                value = value,
                steps = steps,
                trackRange = valueRange,
            )
            AppSlider(
                state = sliderState,
                valueRange = valueRange,
                steps = steps,
                onValueChange = {
                    sliderState.value = it
                    onValueChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                onValueChangeFinished = onValueChangeFinished,
            )
        }
    }
}

/** 卡片标题：走行标题的语义字体与配色 */
@Composable
private fun ThemeCardTitle(text: String) = PrefRowTitle(text = text)

/** 卡片说明与当前值：走页内说明的语义字体与配色 */
@Composable
private fun ThemeCardSummary(text: String) = PrefNote(text = text)

/**
 * 主题预览卡片：以当前选择实时渲染一块迷你界面。
 *
 * 这里按外观线选的是「预览卡自己长什么样」（预览卡画的就是本线页面的样子），
 * 不是同一个控件的两种外形，所以两份预览各画各的，不套用样式位。
 */
@Composable
private fun ThemePreviewCard(
    appSettings: AppSettings,
    isDark: Boolean,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialPreviewCard(appSettings = appSettings, isDark = isDark)
        UiMode.Coui -> CouiPreviewCard(isDark = isDark)
    }
}

/** 主题预览卡片（Material 线）：色板按设置实时计算 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun MaterialPreviewCard(
    appSettings: AppSettings,
    isDark: Boolean,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val colorScheme = rememberAppColorScheme(
        seedColor = rememberSeedColor(appSettings = appSettings, isDark = isDark),
        isDark = isDark,
        isAmoled = appSettings.colorMode.isAmoled,
        style = appSettings.paletteStyle,
        specVersion = appSettings.colorSpec,
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio),
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, color = colorScheme.outlineVariant),
        ) {
            Column {
                // 顶栏
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface,
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val showInfoCard = maxHeight >= 72.dp
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppCard(
                            containerColor = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            content = {},
                        )
                        if (showInfoCard) {
                            AppCard(
                                containerColor = colorScheme.surfaceBright,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                content = {},
                            )
                        }
                    }
                }

                // 底栏
                Surface(
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.Home, null, tint = colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主题预览卡片（COUI 线）：色板取自当前主题（含壁纸动态取色），窗口底色取页面卡片色，
 * 内部两块取容器的两档填充，与真实的 COUI 页面层次一致。
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun CouiPreviewCard(isDark: Boolean) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight

    val colors = CouiTheme.colorScheme
    val pageColors = CouiPageDefaults.themeColors(isDark)
    val shape = CouiPageDefaults.shape

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio)
                .clip(shape)
                .background(pageColors.card)
                .border(1.dp, colors.outlineVariant, shape),
        ) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiText(
                    text = stringResource(id = R.string.app_name),
                    kind = CouiTextKind.Search,
                    textAlign = TextAlign.Start,
                    color = pageColors.title,
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val showInfoCard = maxHeight >= 72.dp
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.container),
                    )
                    if (showInfoCard) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface),
                        )
                    }
                }
            }

            // 底栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_house),
                    contentDescription = null,
                    tint = colors.accent,
                )
            }
        }
    }
}

/** 主题色按钮：上下双半圆展示该种子色生成的色板，选中时显示对勾并放大 */
@Composable
private fun ColorButtonMaterial(
    color: Color,
    isSelected: Boolean,
    appSettings: AppSettings,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = rememberAppColorScheme(
        seedColor = color,
        isDark = isDark,
        isAmoled = appSettings.colorMode.isAmoled,
        style = appSettings.paletteStyle,
        specVersion = appSettings.colorSpec,
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(48.dp)) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                )
            }

            val scale by animateFloatAsState(targetValue = if (isSelected) 1.1f else 1.0f)
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(16.dp),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}
