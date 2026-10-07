package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.miuix.BlurredBar
import com.luckyzyx.luckytool.ui.compose.components.miuix.ScaleDialog
import com.luckyzyx.luckytool.ui.compose.components.miuix.rememberBlurBackdrop
import com.luckyzyx.luckytool.ui.shell.ModuleLinesMax
import com.luckyzyx.luckytool.ui.shell.ModuleLinesMin
import com.luckyzyx.luckytool.ui.shell.PageScaleMax
import com.luckyzyx.luckytool.ui.shell.PageScaleMin
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.keyColorOptions
import com.luckyzyx.luckytool.ui.theme.rememberLuckyColorScheme
import com.luckyzyx.luckytool.ui.theme.resolveDarkTheme
import com.luckyzyx.luckytool.utils.SettingsPrefs
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
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

/** 偏好键：Miuix 主题预览卡片所用的底栏形态（对齐 KernelSU 同名键，LuckyTool 侧新增） */
private const val KeyEnableBlur = "enable_blur"
private const val KeyEnableFloatingBottomBar = "enable_floating_bottom_bar"
private const val KeyEnableFloatingBottomBarBlur = "enable_floating_bottom_bar_blur"
private const val KeyPaletteStyle = "palette_style"
private const val KeyColorSpec = "color_spec"
private const val KeyKeyColor = "key_color"
private const val KeyDarkTheme = "dark_theme"

/**
 * 主题与配色页（迁移 KernelSU `ui/screen/colorpalette/ColorPaletteScreenMiuix`，Miuix 实现）。
 *
 * 组成对齐 KernelSU ColorPaletteScreenMiuix：
 * 主题预览卡片 → 主题模式 TabRow（跟随系统 / 浅色 / 深色）→ Monet 取色卡片（含强调色 /
 * 色彩风格 / 色彩标准）→ Miuix 外观卡片（模糊 / 悬浮底栏 / 液态玻璃 / 导航角标）
 * → 外壳行为卡片（横移返回 + 界面缩放，含输入对话框）→ 模块描述最大行数 → 底部留白。
 *
 * 与 KernelSU 的差异：
 * 1. 不做 UiState / Actions 抽象，直接读写 SettingsPrefs；
 * 2. 不提供「预测式返回」开关与「翻页手势」下拉（本应用已隐藏，无对应功能）；
 * 3. 不调用 MonetColorsProvider.UpdateCss()（本应用无该实现）。
 *
 * 偏好键沿用 LuckyTool 既有约定（SettingsPrefs）：
 * dark_theme（0-3 普通 / 4-7 Monet）、miuix_monet、key_color、palette_style、color_spec、
 * enable_blur、enable_floating_bottom_bar、enable_floating_bottom_bar_blur、
 * enable_navigation_badge、enable_swipe_dismiss、page_scale、module_description_max_lines。
 * 写入后通过 [ThemePrefs.notifyChanged] 让应用主题即时重算，无需 recreate。
 */
@Composable
fun ThemeScreenMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()

    // 外观与配色
    var colorMode by remember {
        mutableStateOf(
            ColorMode.fromValue(
                context.getString(SettingsPrefs, KeyDarkTheme, ColorMode.SYSTEM.value.toString())
                    .toIntOrNull() ?: ColorMode.SYSTEM.value
            )
        )
    }
    var miuixMonet by remember { mutableStateOf(context.getBoolean(SettingsPrefs, ThemeController.KEY_MIUIX_MONET, false)) }
    var keyColor by remember { mutableIntStateOf(context.getInt(SettingsPrefs, KeyKeyColor, 0)) }
    var paletteStyle by remember {
        mutableStateOf(
            PaletteStyle.entries.firstOrNull {
                it.name == context.getString(SettingsPrefs, KeyPaletteStyle, PaletteStyle.TonalSpot.name)
            } ?: PaletteStyle.TonalSpot
        )
    }
    var colorSpec by remember {
        mutableStateOf(
            ColorSpec.SpecVersion.entries.firstOrNull {
                it.name == context.getString(SettingsPrefs, KeyColorSpec, ColorSpec.SpecVersion.SPEC_2025.name)
            } ?: ColorSpec.SpecVersion.SPEC_2025
        )
    }

    // Miuix 外观项
    var enableBlur by remember { mutableStateOf(context.getBoolean(SettingsPrefs, KeyEnableBlur, false)) }
    var enableFloatingBottomBar by remember {
        mutableStateOf(context.getBoolean(SettingsPrefs, KeyEnableFloatingBottomBar, false))
    }
    var enableFloatingBottomBarBlur by remember {
        mutableStateOf(context.getBoolean(SettingsPrefs, KeyEnableFloatingBottomBarBlur, false))
    }

    // 外壳行为项
    var navigationBadge by remember {
        mutableStateOf(context.getBoolean(SettingsPrefs, ShellSettingsController.KEY_NAVIGATION_BADGE, true))
    }
    var swipeDismiss by remember {
        mutableStateOf(context.getBoolean(SettingsPrefs, ShellSettingsController.KEY_SWIPE_DISMISS, true))
    }
    var pageScale by remember {
        mutableFloatStateOf(
            context.getFloat(SettingsPrefs, ShellSettingsController.KEY_PAGE_SCALE, 1.0f)
                .coerceIn(PageScaleMin, PageScaleMax)
        )
    }
    var moduleLines by remember {
        mutableIntStateOf(
            context.getInt(SettingsPrefs, ShellSettingsController.KEY_MODULE_LINES, 4)
                .coerceIn(ModuleLinesMin, ModuleLinesMax)
        )
    }

    // 写入偏好：统一走「先落盘、再通知主题重算」
    val setColorMode: (ColorMode) -> Unit = { mode ->
        colorMode = mode
        context.putString(SettingsPrefs, KeyDarkTheme, mode.value.toString())
        ThemePrefs.notifyChanged()
    }
    val setMiuixMonet: (Boolean) -> Unit = { enabled ->
        miuixMonet = enabled
        context.putBoolean(SettingsPrefs, ThemeController.KEY_MIUIX_MONET, enabled)
        // 与 KernelSU 一致：开关 Monet 时同步归一化主题模式（Monet_* 与普通模式互转）
        val normalized = if (enabled) colorMode.toMonetMode() else colorMode.toNonMonetMode()
        if (normalized != colorMode) {
            setColorMode(normalized)
        } else {
            ThemePrefs.notifyChanged()
        }
    }
    val setKeyColor: (Int) -> Unit = { color ->
        keyColor = color
        context.putInt(SettingsPrefs, KeyKeyColor, color)
        ThemePrefs.notifyChanged()
    }
    val setPaletteStyle: (PaletteStyle) -> Unit = { style ->
        paletteStyle = style
        context.putString(SettingsPrefs, KeyPaletteStyle, style.name)
        ThemePrefs.notifyChanged()
    }
    val setColorSpec: (ColorSpec.SpecVersion) -> Unit = { spec ->
        colorSpec = spec
        context.putString(SettingsPrefs, KeyColorSpec, spec.name)
        ThemePrefs.notifyChanged()
    }
    val setEnableBlur: (Boolean) -> Unit = { enabled ->
        enableBlur = enabled
        context.putBoolean(SettingsPrefs, KeyEnableBlur, enabled)
        ThemePrefs.notifyChanged()
    }
    val setEnableFloatingBottomBar: (Boolean) -> Unit = { enabled ->
        enableFloatingBottomBar = enabled
        context.putBoolean(SettingsPrefs, KeyEnableFloatingBottomBar, enabled)
        ThemePrefs.notifyChanged()
    }
    val setEnableFloatingBottomBarBlur: (Boolean) -> Unit = { enabled ->
        enableFloatingBottomBarBlur = enabled
        context.putBoolean(SettingsPrefs, KeyEnableFloatingBottomBarBlur, enabled)
        ThemePrefs.notifyChanged()
    }
    val setNavigationBadge: (Boolean) -> Unit = { enabled ->
        navigationBadge = enabled
        context.putBoolean(SettingsPrefs, ShellSettingsController.KEY_NAVIGATION_BADGE, enabled)
        ThemePrefs.notifyChanged()
    }
    val setSwipeDismiss: (Boolean) -> Unit = { enabled ->
        swipeDismiss = enabled
        context.putBoolean(SettingsPrefs, ShellSettingsController.KEY_SWIPE_DISMISS, enabled)
        ThemePrefs.notifyChanged()
    }
    val setPageScale: (Float) -> Unit = { scale ->
        val clamped = scale.coerceIn(PageScaleMin, PageScaleMax)
        pageScale = clamped
        context.putFloat(SettingsPrefs, ShellSettingsController.KEY_PAGE_SCALE, clamped)
        ThemePrefs.notifyChanged()
    }
    val setModuleLines: (Int) -> Unit = { lines ->
        val clamped = lines.coerceIn(ModuleLinesMin, ModuleLinesMax)
        moduleLines = clamped
        context.putInt(SettingsPrefs, ShellSettingsController.KEY_MODULE_LINES, clamped)
        ThemePrefs.notifyChanged()
    }

    // 模糊开关仅对预览与顶栏生效，与 KernelSU 一样由偏好直接驱动
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val isDark = colorMode.resolveDarkTheme()

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings_theme),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            Icon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onBackground
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val showScaleDialog = rememberSaveable { mutableStateOf(false) }

        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    ThemePreviewCardMiuix(
                        keyColor = keyColor,
                        isDark = isDark,
                        miuixMonet = miuixMonet,
                        enableFloatingBottomBar = enableFloatingBottomBar,
                        enableFloatingBottomBarBlur = enableFloatingBottomBarBlur,
                        paletteStyle = paletteStyle,
                        colorSpec = colorSpec,
                    )
                    Spacer(modifier = Modifier.height(72.dp))

                    val themeItems = listOf(
                        stringResource(id = R.string.settings_theme_mode_system),
                        stringResource(id = R.string.settings_theme_mode_light),
                        stringResource(id = R.string.settings_theme_mode_dark),
                    )
                    TabRow(
                        tabs = themeItems,
                        selectedTabIndex = (if (colorMode.value >= 3) colorMode.value - 3 else colorMode.value)
                            .coerceIn(0, 2),
                        onTabSelected = { index ->
                            // 保持 Monet 组合：Monet 模式下切 Tab 落在对应的 Monet 变体上
                            setColorMode(colorMode.toNonMonetMode().withTabIndex(index).let {
                                if (miuixMonet) it.toMonetMode() else it
                            })
                        },
                    )

                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_monet),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Wallpaper,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_monet),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = miuixMonet,
                            onCheckedChange = { setMiuixMonet(it) }
                        )

                        AnimatedVisibility(visible = miuixMonet) {
                            Column {
                                val colorItems = listOf(
                                    stringResource(id = R.string.settings_key_color_default),
                                    stringResource(id = R.string.color_red),
                                    stringResource(id = R.string.color_pink),
                                    stringResource(id = R.string.color_purple),
                                    stringResource(id = R.string.color_deep_purple),
                                    stringResource(id = R.string.color_indigo),
                                    stringResource(id = R.string.color_blue),
                                    stringResource(id = R.string.color_cyan),
                                    stringResource(id = R.string.color_teal),
                                    stringResource(id = R.string.color_green),
                                    stringResource(id = R.string.color_yellow),
                                    stringResource(id = R.string.color_amber),
                                    stringResource(id = R.string.color_orange),
                                    stringResource(id = R.string.color_brown),
                                    stringResource(id = R.string.color_blue_grey),
                                    stringResource(id = R.string.color_sakura),
                                )
                                val colorValues = listOf(0) + keyColorOptions
                                OverlayDropdownPreference(
                                    title = stringResource(id = R.string.settings_key_color),
                                    items = colorItems,
                                    startAction = {
                                        Icon(
                                            Icons.Rounded.Colorize,
                                            modifier = Modifier.padding(end = 6.dp),
                                            contentDescription = stringResource(id = R.string.settings_key_color),
                                            tint = colorScheme.onBackground
                                        )
                                    },
                                    selectedIndex = colorValues.indexOf(keyColor).takeIf { it >= 0 } ?: 0,
                                    onSelectedIndexChange = { index ->
                                        setKeyColor(colorValues[index])
                                    }
                                )

                                AnimatedVisibility(visible = keyColor != 0) {
                                    Column {
                                        val styles = PaletteStyle.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(id = R.string.settings_color_style),
                                            items = styles.map { it.name },
                                            startAction = {
                                                Icon(
                                                    Icons.Rounded.Style,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    contentDescription = stringResource(id = R.string.settings_color_style),
                                                    tint = colorScheme.onBackground
                                                )
                                            },
                                            selectedIndex = styles.indexOf(paletteStyle).coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                setPaletteStyle(styles[index])
                                            }
                                        )

                                        val specs = ColorSpec.SpecVersion.entries
                                        OverlayDropdownPreference(
                                            title = stringResource(id = R.string.settings_color_spec),
                                            items = specs.map { it.name },
                                            startAction = {
                                                Icon(
                                                    Icons.Rounded.DesignServices,
                                                    modifier = Modifier.padding(end = 6.dp),
                                                    contentDescription = stringResource(id = R.string.settings_color_spec),
                                                    tint = colorScheme.onBackground
                                                )
                                            },
                                            selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                            onSelectedIndexChange = { index ->
                                                setColorSpec(specs[index])
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Miuix 外观：模糊 / 悬浮底栏 / 液态玻璃 / 导航角标
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_blur),
                                summary = stringResource(id = R.string.settings_enable_blur_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.BlurOn,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_blur),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = enableBlur,
                                onCheckedChange = { setEnableBlur(it) }
                            )
                        }

                        SwitchPreference(
                            title = stringResource(id = R.string.settings_floating_bottom_bar),
                            summary = stringResource(id = R.string.settings_floating_bottom_bar_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.CallToAction,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_floating_bottom_bar),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = enableFloatingBottomBar,
                            onCheckedChange = { setEnableFloatingBottomBar(it) }
                        )

                        AnimatedVisibility(
                            visible = enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ) {
                            SwitchPreference(
                                title = stringResource(id = R.string.settings_enable_glass),
                                summary = stringResource(id = R.string.settings_enable_glass_summary),
                                startAction = {
                                    Icon(
                                        Icons.Rounded.WaterDrop,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = stringResource(id = R.string.settings_enable_glass),
                                        tint = colorScheme.onBackground
                                    )
                                },
                                checked = enableFloatingBottomBarBlur,
                                onCheckedChange = { setEnableFloatingBottomBarBlur(it) }
                            )
                        }

                        SwitchPreference(
                            title = stringResource(id = R.string.settings_navigation_badge),
                            summary = stringResource(id = R.string.settings_navigation_badge_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Pin,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_navigation_badge),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = navigationBadge,
                            onCheckedChange = { setNavigationBadge(it) }
                        )
                    }

                    // 外壳行为：横移返回 + 界面缩放
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_enable_swipe_dismiss),
                            summary = stringResource(id = R.string.settings_enable_swipe_dismiss_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Swipe,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_enable_swipe_dismiss),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = swipeDismiss,
                            onCheckedChange = { setSwipeDismiss(it) }
                        )

                        // 拖动只更新页内状态，松手才提交偏好，避免重建全应用密度
                        var sliderValue by remember(pageScale) { mutableFloatStateOf(pageScale) }
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_page_scale),
                            summary = stringResource(id = R.string.settings_page_scale_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.AspectRatio,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_page_scale),
                                    tint = colorScheme.onBackground
                                )
                            },
                            endActions = {
                                Text(
                                    text = "${(sliderValue * 100).toInt()}%",
                                    color = colorScheme.onSurfaceVariantActions
                                )
                            },
                            onClick = {
                                showScaleDialog.value = !showScaleDialog.value
                            },
                            holdDownState = showScaleDialog.value,
                            bottomAction = {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = { setPageScale(sliderValue) },
                                    valueRange = PageScaleMin..PageScaleMax,
                                    showKeyPoints = true,
                                    keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step
                                )
                            }
                        )
                    }

                    // 模块描述最大行数
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        var linesValue by remember(moduleLines) { mutableIntStateOf(moduleLines) }
                        BasicComponent(
                            title = stringResource(id = R.string.settings_module_description_max_lines),
                            summary = stringResource(id = R.string.settings_module_description_max_lines_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Description,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_module_description_max_lines),
                                    tint = colorScheme.onBackground
                                )
                            },
                            endActions = {
                                Text(
                                    text = "$linesValue " + stringResource(R.string.unit_lines),
                                    color = colorScheme.onSurfaceVariantActions
                                )
                            },
                            bottomAction = {
                                Slider(
                                    value = linesValue.toFloat(),
                                    onValueChange = { linesValue = it.roundToInt() },
                                    onValueChangeFinished = { setModuleLines(linesValue) },
                                    valueRange = ModuleLinesMin.toFloat()..ModuleLinesMax.toFloat(),
                                    showKeyPoints = true,
                                    keyPoints = listOf(1f, 2f, 3f, 4f, 5f),
                                    magnetThreshold = 0.25f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step
                                )
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(
                            12.dp +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                WindowInsets.captionBar.asPaddingValues().calculateBottomPadding()
                        )
                    )
                }
            }
        }

        ScaleDialog(
            show = showScaleDialog.value,
            onDismissRequest = { showScaleDialog.value = false },
            volumeState = { pageScale },
            onVolumeChange = { setPageScale(it) },
        )
    }
}

/** 主题模式 Tab 下标（0 跟随系统 / 1 浅色 / 2 深色）→ ColorMode（普通变体） */
private fun ColorMode.withTabIndex(index: Int): ColorMode = when (index) {
    1 -> ColorMode.LIGHT
    2 -> ColorMode.DARK
    else -> ColorMode.SYSTEM
}

/** 主题预览卡片：以当前选择实时渲染一块迷你界面（对齐 KernelSU ThemePreviewCardMiuix） */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCardMiuix(
    keyColor: Int,
    isDark: Boolean,
    miuixMonet: Boolean,
    enableFloatingBottomBar: Boolean = false,
    enableFloatingBottomBarBlur: Boolean = false,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
    val useRail = useNavigationRail(enableFloatingBottomBar)

    val seedColor = if (keyColor == 0) colorScheme.primary else Color(keyColor)
    val effectiveStyle = if (keyColor == 0) PaletteStyle.TonalSpot else paletteStyle
    val effectiveSpec = if (keyColor == 0) ColorSpec.SpecVersion.Default else colorSpec
    // 预览色板：0 跟随应用主题，非 0 用所选强调色实时生成
    val dynamicCs = rememberLuckyColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = false,
        style = effectiveStyle,
        specVersion = effectiveSpec,
    )

    val bgColor = if (miuixMonet) dynamicCs.background else colorScheme.surface
    val textColor = if (miuixMonet) dynamicCs.onSurface else colorScheme.onBackground
    val accentCardColor = when {
        miuixMonet -> dynamicCs.secondaryContainer
        isDark -> Color(0xFF1A3825)
        else -> Color(0xFFDFFAE4)
    }
    val cardColor = if (miuixMonet) dynamicCs.surfaceContainerHighest else colorScheme.surfaceVariant
    val navBarColor = if (miuixMonet) dynamicCs.surfaceContainer else colorScheme.surface
    val iconColor = if (miuixMonet) dynamicCs.primary else colorScheme.primary
    val navSelectedColor = colorScheme.onSurfaceContainer
    val navUnselectedColor = colorScheme.onSurfaceContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(screenRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            val content = @Composable {
                Column {
                    Row(
                        modifier = Modifier
                            .height(if (useRail) 36.dp else 48.dp)
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = if (useRail) 12.dp else 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentCardColor)
                    )

                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val smallCardHeight = 12.dp
                        val smallCardCount = when {
                            maxHeight >= 96.dp -> 2
                            maxHeight >= 72.dp -> 1
                            else -> 0
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cardColor)
                            )
                            repeat(smallCardCount) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(smallCardHeight)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cardColor)
                                )
                            }
                        }
                    }
                }
            }

            if (useRail) {
                Row {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(navBarColor),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Box(modifier = Modifier.weight(1f)) { content() }
                }
            } else {
                content()
            }

            if (!useRail && enableFloatingBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enableFloatingBottomBarBlur) navBarColor.copy(alpha = 0.5f)
                                else navBarColor
                            )
                            .border(0.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (it == 0) iconColor else textColor)
                            )
                        }
                    }
                }
            } else if (!useRail) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(textColor.copy(alpha = 0.1f))
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .background(navBarColor)
                            .padding(top = 2.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (it == 0) navSelectedColor else navUnselectedColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 是否使用导航栏（宽屏分栏）布局（对齐 KernelSU `ui/component/bottombar/BottomBar.kt` + `ui/util/WindowSize.kt`）。
 *
 * 宽屏时启用；但 Miuix 外观下开启了悬浮底栏则不启用（悬浮底栏与分栏导航互斥）。
 * LuckyTool 目前只提供 Miuix / Material 两种外观，故此处按「Miuix 外观」判断。
 */
@Composable
private fun useNavigationRail(enableFloatingBottomBar: Boolean): Boolean =
    shouldShowSplitPane() && !enableFloatingBottomBar

/** 窗口是否达到分栏阈值（对齐 KernelSU `shouldShowSplitPane`） */
@Composable
private fun shouldShowSplitPane(): Boolean {
    // 只使用稳定 API：LocalConfiguration 的 screenWidthDp / screenHeightDp（KernelSU 用的是窗口容器尺寸，
    // 这里用屏幕配置近似，避免依赖 LocalWindowInfo 的 @ExperimentalComposeUiApi）
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()
    return widthDp >= 840f || (widthDp >= 600f && heightDp / widthDp < 1.2f)
}
