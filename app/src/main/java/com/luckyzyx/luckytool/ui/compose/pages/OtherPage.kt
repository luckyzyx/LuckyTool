package com.luckyzyx.luckytool.ui.compose.pages

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.ITileServiceController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.databinding.DialogAdbLayoutBinding
import com.luckyzyx.luckytool.listener.OnSelectAppInfoListener
import com.luckyzyx.luckytool.selector.AppInfoSelectDialog
import com.luckyzyx.luckytool.service.AdbService
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
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
import com.luckyzyx.luckytool.utils.dialogCentered
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

    // 旧 onResume：刷新 tiles / adb 控制器
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        TilesService.get(activity) { tileController = it }
        AdbService.get(activity) { adbController = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_other)) },
                actions = {
                    IconButton(onClick = {
                        AppInfoSelectDialog(activity, true).apply {
                            setDefaultShowSystem(true)
                            setOnSelectAppListener(object : OnSelectAppInfoListener {
                                override fun resultSelectAppInfos(list: ArrayList<AppInfo>) {
                                    RestartMenuUtils.optimizeScope(
                                        context, list.map { it.packageName }.toTypedArray()
                                    )
                                }
                            })
                            show()
                        }
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_extension_24),
                            contentDescription = "优化App",
                        )
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "quick_entry") {
                PrefCard(
                    title = stringResource(R.string.quick_entry),
                    summary = stringResource(R.string.quick_entry_summary),
                    onClick = {
                        activity.requestFunctionNavigation(
                            R.id.systemQuickEntry, context.getString(R.string.quick_entry)
                        )
                    },
                )
            }
            @SuppressLint("NewApi")
            if (SDK >= A13) {
                item(key = "tile_list") {
                    PrefCard(
                        title = stringResource(R.string.tile_list),
                        summary = stringResource(R.string.tile_list_summary),
                        onClick = { showTileListDialog(context) },
                    )
                }
            }
            item(key = "shortcut") {
                PrefCard(
                    title = stringResource(R.string.set_module_shortcuts),
                    summary = stringResource(R.string.set_module_shortcuts_summary),
                    onClick = { showShortcutDialog(context) },
                )
            }
            item(key = "fps") {
                PrefCard(
                    title = stringResource(R.string.fps_title),
                    summary = stringResource(R.string.fps_summary),
                    onClick = {
                        activity.requestFunctionNavigation(
                            R.id.forceFpsFragment, context.getString(R.string.fps_title)
                        )
                    },
                )
            }
            if (tileController?.checkTouchMode() == true) {
                item(key = "touch_panel") {
                    PrefCard(
                        title = stringResource(R.string.set_touch_sampling_rate_tile_level),
                        summary = stringResource(R.string.set_touch_sampling_rate_tile_level_summary),
                        onClick = {
                            showTouchSamplingRateDialog(context, settings, tileController)
                        },
                    )
                }
            }
            if (adbController != null) {
                item(key = "remote_adb_debug") {
                    PrefCard(
                        title = stringResource(R.string.remote_adb_debug_title),
                        summary = stringResource(R.string.remote_adb_debug_summary),
                        onClick = {
                            showAdbDialog(context, otherPrefs, adbController, scope)
                        },
                    )
                }
            }
        }
    }
}

/** 模块内置磁贴列表：请求添加到控制中心（旧 initQuickTile） */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun showTileListDialog(context: Context) {
    context.showToast(context.getString(R.string.tile_list_click_tips))
    val info = PackageUtils(context.packageManager).getPackageInfo(
        context.packageName, PackageManager.GET_SERVICES
    ) ?: return
    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    val tileInfos = info.services?.filter {
        it.permission == "android.permission.BIND_QUICK_SETTINGS_TILE"
    }?.toList() ?: arrayListOf()
    val items = Array(tileInfos.size) { i -> tileInfos[i].loadLabel(context.packageManager) }
    MaterialAlertDialogBuilder(context, dialogCentered).apply {
        setItems(items) { _, which ->
            val clazz = tileInfos[which].name
            val label = tileInfos[which].loadLabel(context.packageManager)
            val icon = tileInfos[which].loadIcon(context.packageManager)
            statusBarManager.requestAddTileService(
                ComponentName(context.packageName, clazz), label,
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
        }
    }.show()
}

/** 模块快捷方式：多选启用 + Pin 到桌面（旧 shortcut 卡点击） */
private fun showShortcutDialog(context: Context) {
    val shortcutUtils = ShortcutUtils(context)
    val beans = shortcutUtils.getDefaultShortcutBean()
    val titles = Array(beans.size) { i -> beans[i].label }
    val values = Array(beans.size) { i ->
        shortcutUtils.getEnabledShortcutList().find { it.id == beans[i].key } != null
    }
    MaterialAlertDialogBuilder(context, dialogCentered).apply {
        setTitle(context.getString(R.string.set_module_shortcuts))
        setMultiChoiceItems(titles, values.toBooleanArray(), null)
        setPositiveButton(android.R.string.ok) { dialog, _ ->
            val positions = (dialog as androidx.appcompat.app.AlertDialog).listView.checkedItemPositions
            for (i in 0 until positions.size()) {
                shortcutUtils.setShortcutStatus(
                    beans, beans[positions.keyAt(i)], positions.valueAt(i)
                )
            }
        }
        if (shortcutUtils.shortcutManager.isRequestPinShortcutSupported) {
            setNeutralButton("Pin") { dialog, _ ->
                val positions = (dialog as androidx.appcompat.app.AlertDialog).listView.checkedItemPositions
                val checked = (0 until positions.size()).filter { positions.valueAt(it) }
                if (checked.size > 1) {
                    context.showToast("Only select one item")
                    return@setNeutralButton
                }
                val indexValue = checked.firstOrNull() ?: return@setNeutralButton
                val key = positions.keyAt(indexValue)
                shortcutUtils.requestPinShortcut(beans[key].toShortcutInfo(context))
            }
        }
    }.show()
}

/** 触摸采样率档位（旧 initTouchPanelView） */
private fun showTouchSamplingRateDialog(
    context: Context,
    settings: PrefState,
    controller: ITileServiceController?,
) {
    val touchs = arrayOf("120", "180", "240", "360", "480", "600", "720")
    val curLevel = settings.getString(keyTouchSamplingRateLevel, "240")
    MaterialAlertDialogBuilder(context, dialogCentered).apply {
        setTitle(context.getString(R.string.set_touch_sampling_rate_tile_level))
        setSingleChoiceItems(touchs, touchs.indexOf(curLevel), null)
        setPositiveButton(android.R.string.ok) { dialog, _ ->
            val position =
                (dialog as androidx.appcompat.app.AlertDialog).listView.checkedItemPosition
            val value = if (position > 0) touchs[position] else position.toString()
            settings.set(keyTouchSamplingRateLevel, value)
            controller?.touchMode = value.toInt()
        }
        setNeutralButton(android.R.string.cancel, null)
    }.show()
}

/** 远程 ADB 调试（旧 initAdbDebugView，布局与状态机保持原样） */
@SuppressLint("SetTextI18n")
private fun showAdbDialog(
    context: Context,
    otherPrefs: PrefState,
    controller: IAdbDebugController?,
    scope: CoroutineScope,
) {
    val adb = controller ?: return
    val getPort = adb.adbPort
    var getIP = adb.wifiIP ?: "IP"

    val dialogBinding = DialogAdbLayoutBinding.inflate(LayoutInflater.from(context))
    MaterialAlertDialogBuilder(context).apply {
        setCancelable(true)
        setView(dialogBinding.root)
    }.show()

    val adbPortLayout = dialogBinding.adbPortLayout
    val adbPort = dialogBinding.adbPort.apply {
        setText(
            if (getPort == 0 || getPort == -1) otherPrefs.getString("adb_port", "6666")
            else getPort.toString()
        )
    }
    val adbTv = dialogBinding.adbTv.apply {
        if (getPort != 0 && getPort != -1) text = "adb connect $getIP:$getPort"
        setOnLongClickListener { context.copyStr(text.toString()); true }
    }
    val adbTvTip = dialogBinding.adbTvTip.apply {
        isVisible = !adbTv.text.isNullOrBlank()
        setOnLongClickListener { context.copyStr(adbTv.text.toString()); true }
    }
    dialogBinding.adbSwitch.apply {
        isChecked = isEnabled && getPort != 0 && getPort != -1
        adbPortLayout.isEnabled = !isChecked
        setOnCheckedChangeListener { buttonView, checked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            if (checked) {
                val portStr = adbPort.text?.toString()
                if (portStr.isNullOrBlank()) {
                    isChecked = false
                    adbTv.text = context.getString(R.string.adb_debug_port_cannot_null)
                    return@setOnCheckedChangeListener
                }
                scope.launch {
                    val port = portStr.toInt()
                    isEnabled = false
                    runCatching {
                        withContext(Dispatchers.IO) {
                            adb.adbPort = port
                            adb.restartAdb()
                        }
                        getIP = adb.wifiIP ?: "IP"
                        otherPrefs.set("adb_port", port.toString())
                    }
                    adbPortLayout.isEnabled = false
                    adbTv.text = "adb connect $getIP:$portStr"
                    adbTvTip.isVisible = true
                    isEnabled = true
                }
            } else scope.launch {
                isEnabled = false
                runCatching {
                    withContext(Dispatchers.IO) {
                        adb.adbPort = -1
                        adb.restartAdb()
                        adb.adbPort = 0
                    }
                }
                adbPortLayout.isEnabled = true
                adbTv.text = ""
                adbTvTip.isVisible = false
                isEnabled = true
            }
        }
    }
}
