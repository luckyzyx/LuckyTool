package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.collection.ArrayMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.ITileServiceController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.AdbService
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.prefGroup
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveList
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressivePageScaffold
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyTouchSamplingRateLevel
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ShortcutUtils
import com.luckyzyx.luckytool.utils.copyStr
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 其他页（旧 OtherFragment 的 Compose 等价实现）。
 * 卡片顺序、可见性条件、对话框行为与旧实现逐项对齐。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPage(activity: MainActivity) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val otherPrefs = remember { PrefState.of(context, OtherPrefs) }
    val scope = rememberCoroutineScope()
    var tileController by remember { mutableStateOf<ITileServiceController?>(null) }
    var adbController by remember { mutableStateOf<IAdbDebugController?>(null) }
    var showOptimizePicker by remember { mutableStateOf(false) }
    var optimizeScopes by remember { mutableStateOf<ArrayMap<String, CharSequence>?>(null) }
    var showTileDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showTouchDialog by remember { mutableStateOf(false) }
    var showAdbDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 旧 onResume：刷新 tiles / adb 控制器
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        TilesService.get(activity) { tileController = it }
        AdbService.get(activity) { adbController = it }
    }

    ExpressivePageScaffold(
        title = stringResource(R.string.nav_other),
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = { showOptimizePicker = true }) {
                Icon(
                    painterResource(R.drawable.ic_baseline_extension_24),
                    contentDescription = "优化App",
                )
            }
        },
    ) { padding ->
        ExpressiveList(
            scrollBehavior = scrollBehavior,
            modifier = Modifier.padding(padding),
        ) {
            // 全部入口合并为一张分段卡片（KernelSU 主题页外观）
            prefGroup(key = "other_entries") {
                item(key = "quick_entry") {
                    PrefRow(
                        title = stringResource(R.string.quick_entry),
                        summary = stringResource(R.string.quick_entry_summary),
                        onClick = {
                            activity.requestFunctionNavigation(
                                "quick_entry", context.getString(R.string.quick_entry)
                            )
                        },
                    )
                }
                @SuppressLint("NewApi")
                if (SDK >= A13) {
                    item(key = "tile_list") {
                        PrefRow(
                            title = stringResource(R.string.tile_list),
                            summary = stringResource(R.string.tile_list_summary),
                            onClick = {
                                context.showToast(context.getString(R.string.tile_list_click_tips))
                                showTileDialog = true
                            },
                        )
                    }
                }
                item(key = "shortcut") {
                    PrefRow(
                        title = stringResource(R.string.set_module_shortcuts),
                        summary = stringResource(R.string.set_module_shortcuts_summary),
                        onClick = { showShortcutDialog = true },
                    )
                }
                item(key = "fps") {
                    PrefRow(
                        title = stringResource(R.string.fps_title),
                        summary = stringResource(R.string.fps_summary),
                        onClick = {
                            activity.requestFunctionNavigation(
                                "force_fps", context.getString(R.string.fps_title)
                            )
                        },
                    )
                }
                if (tileController?.checkTouchMode() == true) {
                    item(key = "touch_panel") {
                        PrefRow(
                            title = stringResource(R.string.set_touch_sampling_rate_tile_level),
                            summary = stringResource(R.string.set_touch_sampling_rate_tile_level_summary),
                            onClick = { showTouchDialog = true },
                        )
                    }
                }
                if (adbController != null) {
                    item(key = "remote_adb_debug") {
                        PrefRow(
                            title = stringResource(R.string.remote_adb_debug_title),
                            summary = stringResource(R.string.remote_adb_debug_summary),
                            onClick = { showAdbDialog = true },
                        )
                    }
                }
            }
        }
    }

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
        TouchSamplingRateDialog(context, settings, tileController) { showTouchDialog = false }
    }
    if (showAdbDialog && adbController != null) {
        AdbDebugDialog(context, otherPrefs, adbController, scope) { showAdbDialog = false }
    }
}

/** 模块内置磁贴列表：请求添加到控制中心（旧 initQuickTile） */
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
    val statusBarManager = remember { context.getSystemService(StatusBarManager::class.java) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                tileInfos.forEach { serviceInfo ->
                    val label = serviceInfo.loadLabel(context.packageManager).toString()
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val icon = serviceInfo.loadIcon(context.packageManager)
                                statusBarManager.requestAddTileService(
                                    ComponentName(context.packageName, serviceInfo.name),
                                    label,
                                    android.graphics.drawable.Icon.createWithBitmap(icon.toBitmap()),
                                    context.mainExecutor
                                ) { resultCode ->
                                    when (resultCode) {
                                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                            context.showToast("$label ${context.getString(R.string.add_fail)}")

                                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                            context.showToast("$label ${context.getString(R.string.add_repeat)}")

                                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                            context.showToast("$label ${context.getString(R.string.add_success)}")
                                    }
                                }
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
    )
}

/** 模块快捷方式：多选启用 + Pin 到桌面（旧 shortcut 卡点击） */
@Composable
private fun ShortcutDialog(context: Context, onDismiss: () -> Unit) {
    val shortcutUtils = remember { ShortcutUtils(context) }
    val beans = remember { shortcutUtils.getDefaultShortcutBean() }
    val checked = remember(beans) {
        val enabledIds = shortcutUtils.getEnabledShortcutList().map { it.id }.toSet()
        beans.map { bean -> enabledIds.contains(bean.key) }.toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_module_shortcuts)) },
        text = {
            Column {
                beans.forEachIndexed { i, bean ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked[i],
                                role = Role.Checkbox,
                                onValueChange = { checked[i] = it },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                        Spacer(Modifier.width(8.dp))
                        Text(bean.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    beans.forEachIndexed { i, bean ->
                        if (checked[i]) shortcutUtils.setShortcutStatus(beans, bean, true)
                    }
                    onDismiss()
                },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (shortcutUtils.shortcutManager.isRequestPinShortcutSupported) {
                    TextButton(
                        onClick = {
                            val selected = beans.indices.filter { checked[it] }
                            if (selected.size > 1) {
                                context.showToast("Only select one item")
                            } else {
                                selected.firstOrNull()?.let { index ->
                                    shortcutUtils.requestPinShortcut(beans[index].toShortcutInfo(context))
                                }
                            }
                        },
                    ) { Text("Pin") }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}

/** 触摸采样率档位（旧 initTouchPanelView） */
@Composable
private fun TouchSamplingRateDialog(
    context: Context,
    settings: PrefState,
    controller: ITileServiceController?,
    onDismiss: () -> Unit,
) {
    val touchs = remember { arrayOf("120", "180", "240", "360", "480", "600", "720") }
    var tempSelection by remember {
        mutableStateOf(touchs.indexOf(settings.getString(keyTouchSamplingRateLevel, "240")))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_touch_sampling_rate_tile_level)) },
        text = {
            Column {
                touchs.forEachIndexed { position, level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = tempSelection == position,
                                role = Role.RadioButton,
                                onClick = { tempSelection = position },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = tempSelection == position,
                            onClick = { tempSelection = position },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(level)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = if (tempSelection > 0) touchs[tempSelection] else tempSelection.toString()
                    settings.set(keyTouchSamplingRateLevel, value)
                    controller?.touchMode = value.toInt()
                    onDismiss()
                },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** 远程 ADB 调试（旧 initAdbDebugView，布局与状态机保持原样） */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetTextI18n")
@Composable
private fun AdbDebugDialog(
    context: Context,
    otherPrefs: PrefState,
    controller: IAdbDebugController?,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val adb = controller ?: return
    val getPort = adb.adbPort
    var getIP by remember { mutableStateOf(adb.wifiIP ?: "IP") }

    var portText by remember {
        mutableStateOf(
            (if (getPort == 0 || getPort == -1) otherPrefs.getString("adb_port", "6666")
            else getPort.toString()) ?: "6666"
        )
    }
    var adbTvText by remember {
        mutableStateOf(if (getPort != 0 && getPort != -1) "adb connect $getIP:$getPort" else "")
    }
    var busy by remember { mutableStateOf(false) }
    var adbEnabled by remember { mutableStateOf(getPort != 0 && getPort != -1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.enable_remote_adb_debugging),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = adbEnabled,
                        enabled = !busy,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val portStr = portText
                                if (portStr.isBlank()) {
                                    adbTvText = context.getString(R.string.adb_debug_port_cannot_null)
                                } else {
                                    scope.launch {
                                        val port = portStr.toIntOrNull()
                                        if (port == null) {
                                            adbTvText = context.getString(R.string.adb_debug_port_cannot_null)
                                            return@launch
                                        }
                                        busy = true
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                adb.adbPort = port
                                                adb.restartAdb()
                                            }
                                            getIP = adb.wifiIP ?: "IP"
                                            otherPrefs.set("adb_port", port.toString())
                                        }
                                        adbEnabled = true
                                        adbTvText = "adb connect $getIP:$portStr"
                                        busy = false
                                    }
                                }
                            } else {
                                scope.launch {
                                    busy = true
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            adb.adbPort = -1
                                            adb.restartAdb()
                                            adb.adbPort = 0
                                        }
                                    }
                                    adbEnabled = false
                                    adbTvText = ""
                                    busy = false
                                }
                            }
                        },
                    )
                }
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    enabled = !adbEnabled && !busy,
                    label = { Text(stringResource(R.string.adb_port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                if (adbTvText.isNotBlank()) {
                    Text(
                        text = adbTvText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = { context.copyStr(adbTvText) },
                                onClick = {},
                            )
                            .padding(vertical = 12.dp),
                    )
                    Text(
                        text = stringResource(R.string.adb_tv_tip),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = { context.copyStr(adbTvText) },
                                onClick = {},
                            )
                            .padding(bottom = 20.dp),
                    )
                }
            }
        },
    )
}
