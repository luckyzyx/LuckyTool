package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.collection.ArrayMap
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.ITileServiceController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.AdbService
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.AdbDebugDialog
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.MultiChoiceDialog
import com.luckyzyx.luckytool.ui.compose.components.SingleChoiceDialog
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.MainPageActivationEffect
import com.luckyzyx.luckytool.ui.compose.components.OtherTopBarActions
import com.luckyzyx.luckytool.ui.compose.components.PrefIcon
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPageScaffold
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyTouchSamplingRateLevel
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ShortcutUtils
import com.luckyzyx.luckytool.utils.showToast
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Pin

/**
 * 其他页：磁贴控制器、远程 ADB、快捷方式、触摸采样率、快捷入口与 DEX 优化等工具入口。
 * 卡片顺序固定；每张卡能否操作取决于控制器是否连上（tileController / adbController），
 * 交互结果用对话框呈现（showTileDialog / showAdbDialog / showShortcutDialog / showTouchDialog）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPage(activity: MainActivity, isCurrentPage: Boolean = true) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val otherPrefs = remember { PrefState.of(context, OtherPrefs) }
    // 使用存活控制器作为恢复种子；分页保留本页状态，切换标签不重新加载。
    var tileController by remember { mutableStateOf(TilesService.getCachedController()) }
    var adbController by remember { mutableStateOf(AdbService.getCachedController()) }
    var showOptimizePicker by remember { mutableStateOf(false) }
    var optimizeScopes by remember { mutableStateOf<ArrayMap<String, CharSequence>?>(null) }
    var showTileDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showTouchDialog by remember { mutableStateOf(false) }
    var showAdbDialog by remember { mutableStateOf(false) }

    // 预组合不绑定服务，首次进入及当前页恢复前台时刷新控制器。
    fun loadControllers() {
        TilesService.get(activity) { tileController = it }
        AdbService.get(activity) { adbController = it }
    }
    MainPageActivationEffect(isCurrentPage, ::loadControllers, ::loadControllers)

    MaterialPageScaffold(
        title = stringResource(R.string.nav_other),
        actions = {
            OtherTopBarActions(onOptimize = { showOptimizePicker = true })
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val uiMode = LocalUiMode.current
        // 作用域内容列表自带 LazyColumn（不能再套 ExpressiveList）；Miuix 线只保留 start/end/bottom
        // 外置 padding（top 归 0，顶栏高度经 LocalScopeTopInset 交给列表做 contentPadding.top）。
        val scopeModifier = if (uiMode == UiMode.Miuix) {
            Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
        } else {
            Modifier
                .fillMaxSize()
                .padding(padding)
        }
        val scopeContent: @Composable () -> Unit = {
            // 控制器状态在 @Composable 作用域内先读取（原因见 HomePage.scopeContent 注释）：
            // 否则 AIDL 控制器连上后入口不出现，需要切页才显示
            val touchModeNow = tileController?.checkTouchMode() == true
            val hasAdbNow = adbController != null
            // 行参数在非 Composable 的构建 lambda 内求值（那里没有 Composer），字符串先在这里取一次
            val quickEntryTitle = stringResource(R.string.quick_entry)
            val quickEntrySummary = stringResource(R.string.quick_entry_summary)
            val tileListTitle = stringResource(R.string.tile_list)
            val tileListSummary = stringResource(R.string.tile_list_summary)
            val tileListClickTips = stringResource(R.string.tile_list_click_tips)
            val setModuleShortcutsTitle = stringResource(R.string.set_module_shortcuts)
            val setModuleShortcutsSummary = stringResource(R.string.set_module_shortcuts_summary)
            val fpsTitle = stringResource(R.string.fps_title)
            val fpsSummary = stringResource(R.string.fps_summary)
            val touchSamplingTitle = stringResource(R.string.set_touch_sampling_rate_tile_level)
            val touchSamplingSummary =
                stringResource(R.string.set_touch_sampling_rate_tile_level_summary)
            val remoteAdbTitle = stringResource(R.string.remote_adb_debug_title)
            val remoteAdbSummary = stringResource(R.string.remote_adb_debug_summary)
            ScopeScreen(
                state = settings,
                modifier = scopeModifier,
            ) {
                // 快捷入口、显示与触控、调试各自成组。
                group()
                click(
                    title = quickEntryTitle,
                    summary = quickEntrySummary,
                    leading = { PrefIcon(Icons.AutoMirrored.Rounded.OpenInNew) },
                    value = "",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    onClick = {
                        activity.requestFunctionNavigation("quick_entry", quickEntryTitle)
                    },
                )
                if (SDK >= A13) {
                    click(
                        title = tileListTitle,
                        summary = tileListSummary,
                        leading = { PrefIcon(Icons.Rounded.GridView) },
                        onClick = {
                            context.showToast(tileListClickTips)
                            showTileDialog = true
                        },
                    )
                }
                click(
                    title = setModuleShortcutsTitle,
                    summary = setModuleShortcutsSummary,
                    leading = {
                        PrefIcon(Icons.AutoMirrored.Rounded.AddToHomeScreen)
                    },
                    onClick = { showShortcutDialog = true },
                )
                group()
                click(
                    title = fpsTitle,
                    summary = fpsSummary,
                    leading = { PrefIcon(Icons.Rounded.Speed) },
                    value = "",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    onClick = {
                        activity.requestFunctionNavigation("force_fps", fpsTitle)
                    },
                )
                if (touchModeNow) {
                    click(
                        title = touchSamplingTitle,
                        summary = touchSamplingSummary,
                        leading = { PrefIcon(Icons.Rounded.TouchApp) },
                        onClick = { showTouchDialog = true },
                    )
                }
                if (hasAdbNow) {
                    group()
                    // ADB 服务是异步连上的，这一组会在页面已显示后才出现：稳定 itemKey + animate
                    // 让它淡入/淡出，而不是在列表末尾直接弹出来（默认槽位 key 会随上方条件行漂移，
                    // animateItem 认不出是同一行）。
                    click(
                        title = remoteAdbTitle,
                        summary = remoteAdbSummary,
                        leading = { PrefIcon(Icons.Rounded.Adb) },
                        itemKey = "other_adb",
                        animate = true,
                        onClick = { showAdbDialog = true },
                    )
                }
            }
        }
        if (uiMode == UiMode.Miuix) {
            CompositionLocalProvider(LocalScopeTopInset provides padding.calculateTopPadding()) {
                scopeContent()
            }
        } else {
            scopeContent()
        }
    }

    if (!isCurrentPage) return

    if (showOptimizePicker) {
        AppPickerDialog(
            title = "优化App",
            multiMode = true,
            showSystemApps = true,
            onDismiss = { showOptimizePicker = false },
            onConfirm = { list ->
                showOptimizePicker = false
                optimizeScopes = RestartMenuUtils.buildScopeMaps(
                    context, list.map { it.packageName }.toTypedArray()
                )
            },
        )
    }
    optimizeScopes?.let { scopes ->
        RestartMenuUtils.OptimizeDexDialog(context, scopes, confirmFirst = false) {
            optimizeScopes = null
        }
    }

    if (showTileDialog) {
        TileListDialog(context) { showTileDialog = false }
    }
    if (showShortcutDialog) {
        ShortcutDialog(context) { showShortcutDialog = false }
    }
    if (showTouchDialog && tileController?.checkTouchMode() == true) {
        TouchSamplingRateDialog(settings, tileController) { showTouchDialog = false }
    }
    if (showAdbDialog && adbController != null) {
        AdbDebugDialog(otherPrefs, adbController) { showAdbDialog = false }
    }
}

/** 模块内置磁贴列表：枚举带 BIND_QUICK_SETTINGS_TILE 权限的服务，选中项请求添加到控制中心 */
@SuppressLint("NewApi")
@Composable
private fun TileListDialog(context: Context, onDismiss: () -> Unit) {
    val info = remember(context) {
        PackageUtils(context.packageManager).getPackageInfo(
            context.packageName, PackageManager.GET_SERVICES
        )
    } ?: return
    val tileInfos = remember(info) {
        info.services?.filter {
            it.permission == "android.permission.BIND_QUICK_SETTINGS_TILE"
        }?.toList() ?: emptyList()
    }
    val statusBarManager = remember { context.getSystemService(classOf<StatusBarManager>()) }
    // 添加结果回调内没有 Composer，字符串先在这里取一次
    val addFailText = stringResource(R.string.add_fail)
    val addRepeatText = stringResource(R.string.add_repeat)
    val addSuccessText = stringResource(R.string.add_success)
    val items = remember(tileInfos) {
        tileInfos.map { serviceInfo ->
            ChoiceItem(
                key = serviceInfo.name,
                title = serviceInfo.loadLabel(context.packageManager).toString(),
            )
        }
    }
    ActionListDialog(
        title = stringResource(R.string.tile_list),
        items = items,
        onDismiss = onDismiss,
        onSelect = { item ->
            val serviceInfo = tileInfos.firstOrNull { it.name == item.key }
            if (serviceInfo != null) {
                val icon = serviceInfo.loadIcon(context.packageManager)
                statusBarManager.requestAddTileService(
                    ComponentName(context.packageName, serviceInfo.name),
                    item.title,
                    android.graphics.drawable.Icon.createWithBitmap(icon.toBitmap()),
                    context.mainExecutor
                ) { resultCode ->
                    when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                            context.showToast("${item.title} $addFailText")

                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            context.showToast("${item.title} $addRepeatText")

                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            context.showToast("${item.title} $addSuccessText")
                    }
                }
            }
            onDismiss()
        },
    )
}

/** 模块快捷方式：多选启用 + Pin 到桌面 */
@Composable
private fun ShortcutDialog(context: Context, onDismiss: () -> Unit) {
    val shortcutUtils = remember { ShortcutUtils(context) }
    val beans = remember { shortcutUtils.getDefaultShortcutBean() }
    val enabledKeys = remember(beans) {
        shortcutUtils.getEnabledShortcutList().map { it.id }.toSet()
    }
    val items = remember(beans) {
        beans.map { bean -> ChoiceItem(key = bean.key, title = bean.label) }
    }
    val pinContentDescription = stringResource(R.string.pin_shortcut)
    // 固定的图标两条 UI 线共用同一个图钉，只有按钮外壳按模式区分，
    // 否则同一行在 Miuix / Material 下会显示成两个不同含义的图标
    val pinIcon = MiuixIcons.Pin
    // Pin 改为行尾自定义动作：固定被点的那一项，不再用当前勾选反推
    val pinAction: (@Composable (ChoiceItem) -> Unit)? = if (
        shortcutUtils.shortcutManager.isRequestPinShortcutSupported
    ) {
        { item ->
            val onClick: () -> Unit = {
                beans.firstOrNull { it.key == item.key }?.let { bean ->
                    shortcutUtils.requestPinShortcut(bean.toShortcutInfo(context))
                }
            }
            when (LocalUiMode.current) {
                UiMode.Miuix -> MiuixIconButton(onClick = onClick) {
                    MiuixIcon(imageVector = pinIcon, contentDescription = pinContentDescription)
                }

                UiMode.Material -> IconButton(onClick = onClick) {
                    Icon(imageVector = pinIcon, contentDescription = pinContentDescription)
                }
            }
        }
    } else null
    MultiChoiceDialog(
        title = stringResource(R.string.set_module_shortcuts),
        items = items,
        selectedKeys = enabledKeys,
        onDismiss = onDismiss,
        onConfirm = { keys ->
            beans.forEachIndexed { _, bean ->
                shortcutUtils.setShortcutStatus(beans, bean, bean.key in keys)
            }
            onDismiss()
        },
        itemActions = pinAction,
    )
}

/** 触摸采样率档位：120~720 单选，确认后写 prefs 并同步给磁贴 */
@Composable
private fun TouchSamplingRateDialog(
    settings: PrefState,
    controller: ITileServiceController?,
    onDismiss: () -> Unit,
) {
    val touchs = remember { arrayOf("120", "180", "240", "360", "480", "600", "720") }
    val initialIndex = remember {
        touchs.indexOf(settings.getString(keyTouchSamplingRateLevel, "240"))
    }
    val items = remember(touchs) {
        touchs.map { level -> ChoiceItem(key = level, title = level) }
    }
    SingleChoiceDialog(
        title = stringResource(R.string.set_touch_sampling_rate_tile_level),
        items = items,
        selectedKey = touchs.getOrNull(initialIndex),
        onDismiss = onDismiss,
        onConfirm = { level ->
            // 历史写值算法保持等价：索引 0 落库为 "0"（而不是 "120"）
            val index = touchs.indexOf(level)
            val value = if (index > 0) touchs[index] else index.toString()
            settings.set(keyTouchSamplingRateLevel, value)
            controller?.touchMode = value.toInt()
            onDismiss()
        },
    )
}
