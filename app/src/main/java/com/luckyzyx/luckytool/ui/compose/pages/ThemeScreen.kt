package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.ChromeInsets
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPageScaffold
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiSegmentedTabs
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialToggleButton
import com.luckyzyx.luckytool.ui.compose.components.AppGroup
import com.luckyzyx.luckytool.ui.compose.components.AppDropdownItem
import com.luckyzyx.luckytool.ui.compose.components.AppSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.AppCard
import com.luckyzyx.luckytool.ui.compose.components.AppSlider
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTopBarBackButton
import com.luckyzyx.luckytool.ui.compose.components.material.materialTopAppBarColors
import com.luckyzyx.luckytool.ui.compose.MainTab
import com.luckyzyx.luckytool.ui.shell.FloatingBottomBarStyle
import com.luckyzyx.luckytool.ui.shell.PageScaleMax
import com.luckyzyx.luckytool.ui.shell.PageScaleMin
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.compose.isRenderEffectSupported
import com.luckyzyx.luckytool.ui.theme.LocalLuckyColorScheme
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.ui.theme.keyColorOptions
import com.luckyzyx.luckytool.ui.theme.rememberLuckyColorScheme
import com.luckyzyx.luckytool.ui.theme.rememberSeedColor
import com.luckyzyx.luckytool.ui.theme.resolveDarkTheme
import com.luckyzyx.luckytool.utils.PredictiveBackUtils
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

/**
 * 主题与配色页入口。
 *
 * COUI 与 Material 两种外观共用本页：配色、取色、外壳手势与悬浮底栏都在这里配置。
 * 状态在两条线之外只建一份，外观切换时页内读数与滑条中间态都不回退。
 */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val state = rememberThemeScreenState()
    when (LocalUiMode.current) {
        UiMode.Material -> ThemeScreenMaterial(state = state, onBack = onBack)
        UiMode.Coui -> ThemeScreenCoui(state = state, onBack = onBack)
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

    /** COUI 线是否跟随壁纸取色；默认关，界面用 COUI 自带预设配色 */
    var couiDynamicColor by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            ThemeController.KEY_COUI_DYNAMIC_COLOR,
            false
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

    var swipeDismiss by mutableStateOf(
        context.getBoolean(
            SettingsPrefs,
            ShellSettingsController.KEY_SWIPE_DISMISS,
            true
        )
    )

    var pagerMode by mutableIntStateOf(ShellSettingsController.get(context).pagerInterceptionMode)

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
 * 主题与配色页的 Material 实现。
 *
 * 页面组成：
 * 主题预览卡片 → 主题色选择（跟随系统 + 15 预设）→ 主题模式分段按钮
 * → 调色风格 / 色彩规格下拉 → 动态取色开关 → 返回与切页手势。
 *
 * COUI 线收起固定主题色与调色风格 / 色彩规格：那几项只进 Material 线的取色管线。
 * 那条线只留一个自己的动态取色开关，关闭（默认）时用 COUI 预设配色。
 *
 * 偏好键沿用 LuckyTool 既有约定（SettingsPrefs）：
 * dark_theme（0 跟随系统 / 1 深色 / 2 浅色 / 3 深色 AMOLED）、key_color、
 * palette_style、color_spec、use_dynamic_color、coui_dynamic_color（COUI 线专用）、enable_predictive_back、
 * enable_swipe_dismiss、pager_interception_mode。
 * 写入后通过 [ThemePrefs.notifyChanged] 让应用主题即时重算，无需 recreate。
 */
@Composable
private fun ThemeScreenMaterial(state: ThemeScreenState, onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    MaterialScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { MaterialTopBarBackButton(onClick = onBack) },
                title = { Text(stringResource(R.string.settings_theme)) },
                colors = materialTopAppBarColors(),
                scrollBehavior = scrollBehavior,
                windowInsets = ChromeInsets.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
            )
        },
        contentWindowInsets = ChromeInsets.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()
        ThemeScreenList(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            bottomSpace = 16.dp +
                navBars.calculateBottomPadding() +
                captionBar.calculateBottomPadding(),
        )
    }
}

/**
 * 主题与配色页的 COUI 实现。
 *
 * 顶栏是大标题折叠栏，正文内边距由折叠状态给出（滚动时随顶栏收起而收缩），
 * 折叠连接已由外壳挂在正文外层，列表里不要再挂一次。
 */
@Composable
private fun ThemeScreenCoui(state: ThemeScreenState, onBack: () -> Unit) {
    val behavior = CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    CouiPageScaffold(
        title = stringResource(R.string.settings_theme),
        scrollBehavior = behavior,
        onBack = onBack,
    ) { paddingValues ->
        ThemeScreenList(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // 外壳的系统栏内边距已含在 paddingValues 里，末尾只留视觉留白
            bottomSpace = 16.dp,
        )
    }
}

/**
 * 主题页正文：两条外观线的外壳不同，列表内容与状态共用这一份。
 *
 * @param modifier 已带上外壳内边距（Material 线还带折叠滚动连接）的列表修饰符
 * @param bottomSpace 列表末尾留白；外壳已消费的底部内边距不要在列表里重复叠加
 */
@Composable
private fun ThemeScreenList(
    state: ThemeScreenState,
    modifier: Modifier,
    bottomSpace: Dp,
) {
    val context = LocalContext.current
    val isDark = state.colorMode.resolveDarkTheme()
    // LazyColumn 的 item 作用域在 lambda 里，外观线的读取在这里取好再传进去。
    val isCouiLine = LocalUiMode.current == UiMode.Coui
    val appSettings = AppSettings(
        colorMode = state.colorMode,
        keyColor = state.keyColor,
        paletteStyle = state.paletteStyle,
        colorSpec = state.colorSpec,
        dynamicColor = state.dynamicColor,
        couiDynamicColor = state.couiDynamicColor,
    )

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            ThemePreviewCard(
                appSettings = appSettings,
                isDark = isDark,
            )
        }

        // 首个按钮是「跟随壁纸取色」，其余为固定主题色；这些只作用于 Material 线，
        // COUI 线的配色由本线自己的动态取色开关决定（见下方开关项）。
        if (!isCouiLine) item {
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

        item {
            ThemeModeSelector(state = state)
        }

        // 调色风格 / 色彩规格只进 Material 线的取色管线，COUI 线不显示。
        if (!isCouiLine) item {
            AppGroup(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = listOf(
                    {
                        val styles = PaletteStyle.entries
                        AppDropdownItem(
                            icon = Icons.Rounded.Style,
                            title = stringResource(R.string.palette_style_title),
                            items = styles.map { it.name },
                            selectedIndex = styles.indexOf(state.paletteStyle),
                            onItemSelected = { index ->
                                state.paletteStyle = styles[index]
                                context.putString(
                                    SettingsPrefs,
                                    KeyPaletteStyle,
                                    styles[index].name
                                )
                                ThemePrefs.notifyChanged()
                            },
                        )
                    },
                    {
                        val specs = ColorSpec.SpecVersion.entries
                        AppDropdownItem(
                            icon = Icons.Rounded.DesignServices,
                            title = stringResource(R.string.color_spec_title),
                            items = specs.map { it.name },
                            selectedIndex = specs.indexOf(state.colorSpec).coerceAtLeast(0),
                            onItemSelected = { index ->
                                state.colorSpec = specs[index]
                                context.putString(
                                    SettingsPrefs,
                                    KeyColorSpec,
                                    specs[index].name
                                )
                                ThemePrefs.notifyChanged()
                            },
                        )
                    },
                ),
            )
        }

        // 系统没有取色能力（API 31 以下）时整项不出现。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                AppGroup(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            AppSwitchItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.use_dynamic_color),
                                summary = stringResource(R.string.use_dynamic_color_summary),
                                // COUI 线有独立的开关：关（默认）取 COUI 预设配色，开才跟随壁纸取色
                                checked = if (isCouiLine) {
                                    state.couiDynamicColor
                                } else {
                                    state.dynamicColor
                                },
                                onCheckedChange = {
                                    if (isCouiLine) {
                                        state.couiDynamicColor = it
                                        context.putBoolean(
                                            SettingsPrefs,
                                            ThemeController.KEY_COUI_DYNAMIC_COLOR,
                                            it,
                                        )
                                    } else {
                                        state.dynamicColor = it
                                        context.putBoolean(SettingsPrefs, "use_dynamic_color", it)
                                        // 开启动态取色时把自定义主题色归零，保证"跟随系统"色板与开关状态一致
                                        if (it) {
                                            state.keyColor = 0
                                            context.putInt(SettingsPrefs, "key_color", 0)
                                        }
                                    }
                                    ThemePrefs.notifyChanged()
                                },
                            )
                        },
                    ),
                )
            }
        }

        // 分类页图标配色 + 应用内手势；系统不支持预测性返回时，另外两项仍可配置。
        item {
            AppGroup(
                modifier = Modifier.padding(horizontal = 16.dp),
                content = buildList<@Composable () -> Unit> {
                    // 低于 API 31 没有 RenderEffect，模糊开关无从生效，整项不出现
                    if (isRenderEffectSupported()) add {
                        AppSwitchItem(
                            icon = Icons.Rounded.BlurOn,
                            title = stringResource(R.string.settings_enable_blur),
                            summary = stringResource(R.string.settings_enable_blur_summary),
                            checked = state.enableBlur,
                            onCheckedChange = {
                                state.enableBlur = it
                                context.putBoolean(SettingsPrefs, KeyEnableBlur, it)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                    // COUI 底栏本身就是浮起胶囊，悬浮底栏开关只对 Material 线有意义
                    if (!isCouiLine) add {
                        AppSwitchItem(
                            icon = Icons.Rounded.CallToAction,
                            title = stringResource(R.string.settings_floating_bottom_bar),
                            summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                            checked = state.enableFloatingBottomBar,
                            onCheckedChange = {
                                state.enableFloatingBottomBar = it
                                context.putBoolean(SettingsPrefs, KeyEnableFloatingBottomBar, it)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                    // 悬浮形态只在开启悬浮底栏时才有意义
                    if (!isCouiLine && state.enableFloatingBottomBar) {
                        add {
                            AppDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.settings_floating_bottom_bar_style),
                                items = listOf(
                                    stringResource(R.string.settings_floating_style_classic),
                                    stringResource(R.string.settings_floating_style_ios),
                                ),
                                selectedIndex = state.floatingBottomBarStyle.ordinal,
                                onItemSelected = { index ->
                                    val style = FloatingBottomBarStyle.entries[index]
                                    state.floatingBottomBarStyle = style
                                    context.putString(
                                        SettingsPrefs,
                                        ShellSettingsController.KEY_FLOATING_BOTTOM_BAR_STYLE,
                                        style.value,
                                    )
                                    ThemePrefs.notifyChanged()
                                },
                            )
                        }
                    }
                    add {
                        AppSwitchItem(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.settings_category_badge_colors),
                            summary = stringResource(R.string.settings_category_badge_colors_summary),
                            checked = state.categoryBadgeColors,
                            onCheckedChange = {
                                state.categoryBadgeColors = it
                                context.putBoolean(
                                    SettingsPrefs,
                                    ShellSettingsController.KEY_CATEGORY_BADGE_COLORS,
                                    it
                                )
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                    if (PredictiveBackUtils.isSupported()) {
                        add {
                            AppSwitchItem(
                                icon = Icons.AutoMirrored.Rounded.MenuOpen,
                                title = stringResource(R.string.settings_enable_predictive_back),
                                summary = stringResource(R.string.settings_enable_predictive_back_summary),
                                checked = state.predictiveBack,
                                onCheckedChange = {
                                    state.predictiveBack = it
                                    context.putBoolean(
                                        SettingsPrefs,
                                        ShellSettingsController.KEY_PREDICTIVE_BACK,
                                        it
                                    )
                                    // 即时生效：反射改写 ApplicationInfo 后重建 Activity（新窗口读取新 flag）
                                    PredictiveBackUtils.applyImmediately(context, it)
                                },
                            )
                        }
                    }
                    add {
                        AppSwitchItem(
                            icon = Icons.Rounded.Swipe,
                            title = stringResource(R.string.settings_enable_swipe_dismiss),
                            summary = stringResource(R.string.settings_enable_swipe_dismiss_summary),
                            checked = state.swipeDismiss,
                            onCheckedChange = {
                                state.swipeDismiss = it
                                context.putBoolean(SettingsPrefs, ShellSettingsController.KEY_SWIPE_DISMISS, it)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                    add {
                        AppDropdownItem(
                            icon = Icons.Rounded.ViewCarousel,
                            title = stringResource(R.string.settings_pager_gesture_mode),
                            items = listOf(
                                stringResource(R.string.settings_pager_gesture_native),
                                stringResource(R.string.settings_pager_gesture_cross_axis),
                                stringResource(R.string.settings_pager_gesture_ios_like),
                            ),
                            selectedIndex = state.pagerMode,
                            onItemSelected = {
                                state.pagerMode = it
                                context.putInt(SettingsPrefs, ShellSettingsController.KEY_PAGER_MODE, it)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                },
            )
        }

        // 界面缩放（page_scale）：拖动只更新页内状态，松手才提交偏好，避免重建全应用密度
        item {
            ThemeSliderCard(
                icon = Icons.Rounded.AspectRatio,
                title = stringResource(R.string.settings_page_scale),
                summary = stringResource(R.string.settings_page_scale_summary),
                valueText = "${(state.pageScale * 100).roundToInt()}%",
                value = state.pageScale,
                valueRange = PageScaleMin..PageScaleMax,
                steps = 0,
                onValueChange = { state.pageScale = it },
                onValueChangeFinished = {
                    context.putFloat(
                        SettingsPrefs,
                        ShellSettingsController.KEY_PAGE_SCALE,
                        state.pageScale
                    )
                    ThemePrefs.notifyChanged()
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(bottomSpace))
        }
    }
}

/**
 * 主题模式选择。
 *
 * Material 线是连体按钮组（选中态由按钮的 checked 表达），COUI 线没有 ButtonGroup，
 * 用等宽分段条取而代之：四个模式固定一屏排开，选中下标由同一份状态换算。
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
    // 选中即写偏好并重算主题；两条线的触发方式不同（按钮 / 分段条），落盘逻辑共用这一份
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

    when (LocalUiMode.current) {
        UiMode.Material -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        UiMode.Coui -> CouiSegmentedTabs(
            labels = modes.map { stringResource(it.second) },
            // 动态取色模式下 colorMode 是 MONET_* 系列，四个模式里没有对应项，
            // 这里按「无选中项」呈现，与 Material 线连体按钮的观感一致
            selectedIndex = modes.indexOfFirst { it.first == state.colorMode },
            onSelect = { index -> modes.getOrNull(index)?.let { select(it.first) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

/**
 * 滑杆卡片：图标 + 标题 / 说明 + 当前值，下面一条滑杆。
 *
 * 取值由调用方持有（页内状态），[AppSlider] 在两条外观线上写回的都是同一份；
 * material3 的 SliderState 是非受控语义，值必须由调用方写回，否则滑块不跟手。
 * 松手回调用于提交偏好，拖动过程中不落盘。
 */
@Composable
private fun ThemeSliderCard(
    icon: ImageVector,
    title: String,
    summary: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val iconTint = when (LocalUiMode.current) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
        UiMode.Coui -> CouiTheme.colorScheme.onSurfaceVariant
    }
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                )
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

/** 卡片标题：Material 取 titleMedium，COUI 取偏好标题的语义字体 */
@Composable
private fun ThemeCardTitle(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        UiMode.Coui -> CouiText(
            text = text,
            kind = CouiTextKind.PreferenceTitle,
            color = CouiPageDefaults.themeColors().title,
        )
    }
}

/** 卡片说明与当前值：Material 取 bodyMedium，COUI 取偏好说明的语义字体 */
@Composable
private fun ThemeCardSummary(text: String) {
    when (LocalUiMode.current) {
        UiMode.Material -> Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        UiMode.Coui -> CouiText(
            text = text,
            kind = CouiTextKind.PreferenceSummary,
            color = CouiPageDefaults.themeColors().summary,
        )
    }
}

/** 主题预览卡片：以当前选择实时渲染一块迷你界面 */
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

    val colorScheme = rememberLuckyColorScheme(
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
                    kind = CouiTextKind.Body,
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
                Icon(Icons.Filled.Home, null, tint = colors.accent)
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
    val colorScheme = rememberLuckyColorScheme(
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
