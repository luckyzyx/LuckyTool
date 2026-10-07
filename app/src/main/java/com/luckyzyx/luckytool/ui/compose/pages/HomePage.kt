package com.luckyzyx.luckytool.ui.compose.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.GlobalFuncService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.DonateUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.UpdateChangelogDialog
import com.luckyzyx.luckytool.utils.UpdateDownloadProgressDialog
import com.luckyzyx.luckytool.utils.UpdateDownloadSourceDialog
import com.luckyzyx.luckytool.utils.UpdateDownloadedDialog
import com.luckyzyx.luckytool.utils.UpdateUtils
import com.luckyzyx.luckytool.utils.copyStr
import com.luckyzyx.luckytool.utils.getDeviceInfo
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getVersionCode
import com.luckyzyx.luckytool.utils.getVersionName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.openUrl
import com.luckyzyx.luckytool.utils.showToast
import java.io.File

/**
 * 主页（旧 HomeFragment 的 Compose 等价实现）。
 * 状态卡 / 更新卡 / 系统信息卡 / 捐赠卡 / 授权提示与旧实现逐项对齐。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomePage(activity: MainActivity) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val zh = remember(context) { isZh(context) }
    val isDev = remember { settings.getBoolean("hidden_function", false) }

    var moduleActive by remember { mutableStateOf(XposedServiceBridge.isModuleActive) }
    var systemInfo by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<String?>(null) }
    var updateClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var statusCardClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var aboutText by remember { mutableStateOf("") }
    var dexDialogVisible by remember { mutableStateOf(false) }
    var pendingOsVersion by remember { mutableStateOf("") }
    var showRestartMenu by remember { mutableStateOf(false) }
    var showOptimizeDex by remember { mutableStateOf(false) }
    var showDonateList by remember { mutableStateOf(false) }
    var showQrType by remember { mutableStateOf(-1) }
    var updateStage by remember { mutableStateOf<UpdateStage?>(null) }

    fun refreshModuleStatus() {
        moduleActive = XposedServiceBridge.isModuleActive
    }

    fun checkDexOptimize(controller: com.luckyzyx.luckytool.IGlobalFuncController?) {
        if (getOSVersionCode < 34) return
        val getOs = settings.getString("current_os_version", "")
        val curOs = controller?.otaVersion
            ?: DeviceUtils.getOtaVersion().takeIf { it != "null" }
            ?: ""
        if (getOs != curOs && !dexDialogVisible) {
            pendingOsVersion = curOs
            dexDialogVisible = true
        }
    }

    // 旧 onViewCreated：自动检查更新（每次进入页面组合执行一次）
    LaunchedEffect(Unit) {
        if (!settings.getBoolean("auto_check_update", true)) return@LaunchedEffect
        UpdateUtils(activity, isDev).checkUpdate { info ->
            if (getVersionCode < info.code) {
                updateInfo = context.getString(R.string.check_update_hint) +
                        "  -->  ${info.name}(${info.code})"
                updateClick = { updateStage = UpdateStage.Changelog(info) }
            }
            if (isDev) statusCardClick = { updateStage = UpdateStage.Changelog(info) }
        }
    }

    // 旧 onResume：系统信息 + 模块状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshModuleStatus()
        GlobalFuncService.get(activity) { controller ->
            val info = context.getDeviceInfo(controller)
            if (!info.isNullOrBlank()) {
                systemInfo = info
                checkDexOptimize(controller)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                actions = {
                    IconButton(onClick = { showRestartMenu = true }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_refresh_24),
                            contentDescription = stringResource(R.string.menu_reboot),
                        )
                    }
                    IconButton(onClick = {
                        aboutText = if (settings.getBoolean("hidden_function", false)) {
                            "忆清鸣、luckyzyx T"
                        } else {
                            "忆清鸣、luckyzyx"
                        }
                        showAbout = true
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_info_24),
                            contentDescription = stringResource(R.string.menu_settings),
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
            item(key = "status_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { statusCardClick?.invoke() },
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (moduleActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Gray
                        },
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(
                                if (moduleActive) {
                                    R.drawable.ic_round_check_24
                                } else {
                                    R.drawable.ic_round_warning_24
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White,
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = stringResource(
                                    if (moduleActive) {
                                        R.string.module_isactivated
                                    } else {
                                        R.string.module_is_disabled
                                    }
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "${stringResource(R.string.module_version)} " +
                                        "$getVersionName ($getVersionCode) " +
                                        BuildConfig.BUILD_TYPE.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                            Text(
                                text = DeviceUtils.getRootVersion(context),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                            Text(
                                text = DeviceUtils.getFrameWorkVersion(context),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 12.sp,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
            updateInfo?.let { info ->
                item(key = "update_card") {
                    PrefCard(
                        title = info,
                        onClick = { updateClick?.invoke() },
                    )
                }
            }
            item(key = "system_info_card") {
                PrefCard(
                    title = systemInfo ?: stringResource(R.string.loading),
                    onLongClick = {
                        context.copyStr(DeviceUtils.getOTACOnfigs())
                        context.showToast("Copy Device OTA Data Success!")
                    },
                )
            }
            item(key = "donate_card") {
                PrefCard(
                    title = stringResource(R.string.donate_tv_title) + " by: 忆清鸣、luckyzyx",
                    summary = stringResource(R.string.donate_tv__summary),
                    onClick = {
                        context.openUrl(
                            if (zh) "https://docs.qq.com/doc/DS2ZDZlNIeUlpdlV1"
                            else "https://luckyzyx.github.io/LuckyTool_Doc/en/donate"
                        )
                    },
                    onLongClick = { showDonateList = true },
                )
            }
            if (zh) {
                item(key = "authorized") {
                    Text(
                        text = stringResource(R.string.authorized),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.openUrl("https://luckyzyx.github.io/LuckyTool_Doc/use/download_link")
                            },
                        fontSize = 16.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about_author)) },
            text = {
                Text(
                    text = aboutText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                val newValue = !settings.getBoolean("hidden_function", false)
                                settings.set("hidden_function", newValue)
                                aboutText = if (newValue) {
                                    "忆清鸣、luckyzyx T"
                                } else {
                                    "忆清鸣、luckyzyx"
                                }
                            },
                        ),
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (showRestartMenu) {
        RestartMenuUtils.RestartMenuDialog(context) { showRestartMenu = false }
    }

    if (dexDialogVisible) {
        AlertDialog(
            onDismissRequest = { dexDialogVisible = false },
            text = { Text(stringResource(R.string.optimize_dex_after_system_update)) },
            confirmButton = {
                TextButton(onClick = {
                    dexDialogVisible = false
                    settings.set("current_os_version", pendingOsVersion)
                    showOptimizeDex = true
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { dexDialogVisible = false }) {
                    Text(stringResource(R.string.ignore))
                }
            },
        )
    }

    if (showOptimizeDex) {
        RestartMenuUtils.OptimizeDexDialog(
            context,
            RestartMenuUtils.buildScopeMaps(
                context, context.resources.getStringArray(R.array.xposed_scope)
            ),
            confirmFirst = true,
        ) { showOptimizeDex = false }
    }

    if (showDonateList) {
        val donateList = arrayListOf(
            context.getString(R.string.qq),
            context.getString(R.string.wechat),
            context.getString(R.string.alipay),
        )
        if (!zh) donateList.add(3, context.getString(R.string.patreon))
        AlertDialog(
            onDismissRequest = { showDonateList = false },
            text = {
                Column {
                    donateList.forEachIndexed { index, label ->
                        Text(
                            label,
                            Modifier.fillMaxWidth().clickable {
                                showDonateList = false
                                when (index) {
                                    0, 1, 2 -> showQrType = index
                                    else -> context.openUrl("https://www.patreon.com/LuckyTool")
                                }
                            }.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showQrType >= 0) {
        DonateUtils.DonateQRDialog(showQrType) { showQrType = -1 }
    }

    when (val stage = updateStage) {
        null -> Unit
        is UpdateStage.Changelog -> UpdateChangelogDialog(
            context, stage.info, isDev,
            onDismiss = { updateStage = null },
            onDownload = {
                val apkFile = UpdateUtils(context).prepareApkFile(stage.info.fileName)
                updateStage = if (apkFile.exists()) {
                    UpdateStage.Downloaded(stage.info, apkFile)
                } else {
                    UpdateStage.Source(stage.info, apkFile)
                }
            },
        )
        is UpdateStage.Source -> UpdateDownloadSourceDialog(
            context, stage.info.downloadUrl, isDev,
            onDismiss = { updateStage = null },
            onSelect = { url -> updateStage = UpdateStage.Downloading(stage.apkFile, url) },
        )
        is UpdateStage.Downloaded -> UpdateDownloadedDialog(
            context, stage.apkFile, isDev,
            onDismiss = { updateStage = null },
            onDownloadAgain = {
                stage.apkFile.delete()
                updateStage = UpdateStage.Source(stage.info, stage.apkFile)
            },
            onInstall = { UpdateUtils(context).installApk(stage.apkFile) },
        )
        is UpdateStage.Downloading -> UpdateDownloadProgressDialog(
            context, stage.apkFile, stage.url,
            onDismiss = { updateStage = null },
        )
    }
}

/** 更新对话框状态机：更新日志 → 下载源 → 下载中 → 已下载 */
private sealed class UpdateStage {
    data class Changelog(val info: UpdateUtils.UpdateInfo) : UpdateStage()
    data class Source(val info: UpdateUtils.UpdateInfo, val apkFile: File) : UpdateStage()
    data class Downloaded(val info: UpdateUtils.UpdateInfo, val apkFile: File) : UpdateStage()
    data class Downloading(val apkFile: File, val url: String) : UpdateStage()
}
