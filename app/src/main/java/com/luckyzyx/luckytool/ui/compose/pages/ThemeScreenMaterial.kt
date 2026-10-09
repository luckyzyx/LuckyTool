package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveScaffold
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveToggleButton
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedColumn
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedDropdownItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedSwitchItem
import com.luckyzyx.luckytool.ui.compose.components.material.TonalCard
import com.luckyzyx.luckytool.ui.compose.components.material.TopBarBackButton
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveTopAppBarColors
import com.luckyzyx.luckytool.ui.shell.ModuleLinesMax
import com.luckyzyx.luckytool.ui.shell.ModuleLinesMin
import com.luckyzyx.luckytool.ui.shell.PageScaleMax
import com.luckyzyx.luckytool.ui.shell.PageScaleMin
import com.luckyzyx.luckytool.ui.shell.ShellSettingsController
import com.luckyzyx.luckytool.ui.theme.AppSettings
import com.luckyzyx.luckytool.ui.theme.ColorMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.keyColorOptions
import com.luckyzyx.luckytool.ui.theme.rememberLuckyColorScheme
import com.luckyzyx.luckytool.ui.theme.rememberSeedColor
import com.luckyzyx.luckytool.ui.theme.resolveDarkTheme
import com.luckyzyx.luckytool.utils.PredictiveBackUtils
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
import kotlin.math.roundToInt

/**
 * 主题与配色页（迁移 KernelSU `ui/screen/colorpalette`，Material 实现）。
 *
 * 组成对齐 KernelSU ColorPaletteScreenMaterial：
 * 主题预览卡片 → 主题色选择（跟随系统 + 15 预设）→ 主题模式分段按钮
 * → 调色风格 / 色彩规格下拉 → 动态取色开关 → 预测式返回开关。
 *
 * 偏好键沿用 LuckyTool 既有约定（SettingsPrefs）：
 * dark_theme（0 跟随系统 / 1 浅色 / 2 深色 / 3 深色 AMOLED）、key_color、
 * palette_style、color_spec、use_dynamic_color、enable_predictive_back。
 * 写入后通过 [ThemePrefs.notifyChanged] 让应用主题即时重算，无需 recreate。
 */
@Composable
fun ThemeScreenMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var colorMode by remember {
        mutableStateOf(
            ColorMode.fromValue(
                context.getString(
                    SettingsPrefs,
                    "dark_theme",
                    "0"
                ).toIntOrNull() ?: 0
            )
        )
    }
    var keyColor by remember { mutableIntStateOf(context.getInt(SettingsPrefs, "key_color", 0)) }
    var dynamicColor by remember {
        mutableStateOf(
            context.getBoolean(
                SettingsPrefs,
                "use_dynamic_color",
                true
            )
        )
    }
    var paletteStyle by remember {
        mutableStateOf(
            PaletteStyle.entries.firstOrNull {
                it.name == context.getString(
                    SettingsPrefs,
                    "palette_style",
                    PaletteStyle.TonalSpot.name
                )
            } ?: PaletteStyle.TonalSpot
        )
    }
    var colorSpec by remember {
        mutableStateOf(
            ColorSpec.SpecVersion.entries.firstOrNull {
                it.name == context.getString(
                    SettingsPrefs,
                    "color_spec",
                    ColorSpec.SpecVersion.SPEC_2025.name
                )
            } ?: ColorSpec.SpecVersion.SPEC_2025
        )
    }

    // KernelSU 主题页中与配色无关的外壳行为项（键名与默认值对齐 KernelSU）
    var navigationBadge by remember {
        mutableStateOf(
            context.getBoolean(
                SettingsPrefs,
                ShellSettingsController.KEY_NAVIGATION_BADGE,
                true
            )
        )
    }
    var swipeDismiss by remember {
        mutableStateOf(
            context.getBoolean(
                SettingsPrefs,
                ShellSettingsController.KEY_SWIPE_DISMISS,
                true
            )
        )
    }
    // 预测式返回手势（需 Android 14+，反射 ApplicationInfo.setEnableOnBackInvokedCallback）
    var predictiveBack by remember {
        mutableStateOf(context.getBoolean(SettingsPrefs, ShellSettingsController.KEY_PREDICTIVE_BACK, false))
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
    var uiMode by remember { mutableStateOf(ThemeController.getUiMode(context)) }

    val appSettings = AppSettings(
        colorMode = colorMode,
        keyColor = keyColor,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
        dynamicColor = dynamicColor,
    )

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                title = { Text(stringResource(R.string.theme_title)) },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { paddingValues ->
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val captionBar = WindowInsets.captionBar.asPaddingValues()
        val isDark = colorMode.resolveDarkTheme()
        val isAmoled = colorMode.isAmoled

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item {
                ThemePreviewCard(
                    appSettings = appSettings,
                    isDark = isDark,
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        ColorButtonMaterial(
                            color = Color.Unspecified,
                            isSelected = dynamicColor,
                            appSettings = appSettings,
                            isDark = isDark,
                            onClick = {
                                keyColor = 0
                                context.putInt(SettingsPrefs, "key_color", 0)
                                dynamicColor = true
                                context.putBoolean(SettingsPrefs, "use_dynamic_color", true)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                    items(keyColorOptions) { color ->
                        ColorButtonMaterial(
                            color = Color(color),
                            isSelected = !dynamicColor && keyColor == color,
                            appSettings = appSettings,
                            isDark = isDark,
                            onClick = {
                                keyColor = color
                                context.putInt(SettingsPrefs, "key_color", color)
                                dynamicColor = false
                                context.putBoolean(SettingsPrefs, "use_dynamic_color", false)
                                ThemePrefs.notifyChanged()
                            },
                        )
                    }
                }
            }

            item {
                val modes = listOf(
                    ColorMode.SYSTEM to R.string.dark_theme_follow_system,
                    ColorMode.LIGHT to R.string.dark_theme_off,
                    ColorMode.DARK to R.string.dark_theme_on,
                    ColorMode.DARK_AMOLED to R.string.dark_theme_on_amoled,
                )
                val haptic = LocalHapticFeedback.current

                Column(
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
                                ExpressiveToggleButton(
                                    checked = colorMode == mode,
                                    onCheckedChange = {
                                        if (it) {
                                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            colorMode = mode
                                            context.putString(
                                                SettingsPrefs,
                                                "dark_theme",
                                                mode.value.toString()
                                            )
                                            ThemePrefs.notifyChanged()
                                        }
                                    },
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
            }

            item {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = listOf(
                        {
                            val styles = PaletteStyle.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.Style,
                                title = stringResource(R.string.palette_style_title),
                                items = styles.map { it.name },
                                selectedIndex = styles.indexOf(paletteStyle),
                                onItemSelected = { index ->
                                    paletteStyle = styles[index]
                                    context.putString(
                                        SettingsPrefs,
                                        "palette_style",
                                        styles[index].name
                                    )
                                    ThemePrefs.notifyChanged()
                                },
                            )
                        },
                        {
                            val specs = ColorSpec.SpecVersion.entries
                            SegmentedDropdownItem(
                                icon = Icons.Rounded.DesignServices,
                                title = stringResource(R.string.color_spec_title),
                                items = specs.map { it.name },
                                selectedIndex = specs.indexOf(colorSpec).coerceAtLeast(0),
                                onItemSelected = { index ->
                                    colorSpec = specs[index]
                                    context.putString(
                                        SettingsPrefs,
                                        "color_spec",
                                        specs[index].name
                                    )
                                    ThemePrefs.notifyChanged()
                                },
                            )
                        },
                    ),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SegmentedColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.Rounded.Style,
                                    title = stringResource(R.string.use_dynamic_color),
                                    summary = stringResource(R.string.use_dynamic_color_summary),
                                    checked = dynamicColor,
                                    onCheckedChange = {
                                        dynamicColor = it
                                        context.putBoolean(SettingsPrefs, "use_dynamic_color", it)
                                        // 开启动态取色时把自定义主题色归零，保证"跟随系统"色板与开关状态一致
                                        if (it) {
                                            keyColor = 0
                                            context.putInt(SettingsPrefs, "key_color", 0)
                                        }
                                        ThemePrefs.notifyChanged()
                                    },
                                )
                            },
                        ),
                    )
                }
            }

            // 预测式返回手势（需 Android 14+，反射 ApplicationInfo.setEnableOnBackInvokedCallback）
            if (PredictiveBackUtils.isSupported()) {
                item {
                    SegmentedColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        content = listOf(
                            {
                                SegmentedSwitchItem(
                                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                    title = stringResource(R.string.settings_enable_predictive_back),
                                    summary = stringResource(R.string.settings_enable_predictive_back_summary),
                                    checked = predictiveBack,
                                    onCheckedChange = {
                                        predictiveBack = it
                                        context.putBoolean(
                                            SettingsPrefs,
                                            ShellSettingsController.KEY_PREDICTIVE_BACK,
                                            it
                                        )
                                        // 生效于下次启动：ApplicationInfo 于进程启动时同步
                                    },
                                )
                            },
                        ),
                    )
                }
            }

            // 界面缩放（page_scale）：拖动只更新页内状态，松手才提交偏好，避免重建全应用密度
            item {
                TonalCard(
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
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = stringResource(R.string.settings_page_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_page_scale),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_page_scale_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${(pageScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            state = rememberSliderState(
                                value = pageScale,
                                trackRange = PageScaleMin..PageScaleMax
                            ),
                            onValueChange = { pageScale = it },
                            modifier = Modifier.fillMaxWidth(),
                            onValueChangeFinished = {
                                context.putFloat(
                                    SettingsPrefs,
                                    ShellSettingsController.KEY_PAGE_SCALE,
                                    pageScale
                                )
                                ThemePrefs.notifyChanged()
                            })
                    }
                }
            }

            // 模块描述最大行数（module_description_max_lines）
            item {
                TonalCard(
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
                                imageVector = Icons.Rounded.Description,
                                contentDescription = stringResource(R.string.settings_module_description_max_lines),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_module_description_max_lines),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_module_description_max_lines_summary),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "$moduleLines ${stringResource(R.string.unit_lines)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            state = rememberSliderState(
                                value = moduleLines.toFloat(),
                                steps = 3,
                                trackRange = ModuleLinesMin.toFloat()..ModuleLinesMax.toFloat()
                            ),
                            onValueChange = { moduleLines = it.roundToInt() },
                            modifier = Modifier.fillMaxWidth(),
                            onValueChangeFinished = {
                                context.putInt(
                                    SettingsPrefs,
                                    ShellSettingsController.KEY_MODULE_LINES,
                                    moduleLines
                                )
                                ThemePrefs.notifyChanged()
                            })
                    }
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(
                        16.dp + navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
                    )
                )
            }
        }
    }
}

/** 主题预览卡片：以当前选择实时渲染一块迷你界面（对齐 KernelSU ThemePreviewCard） */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
private fun ThemePreviewCard(
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
                        TonalCard(
                            containerColor = colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            content = {},
                        )
                        if (showInfoCard) {
                            TonalCard(
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
