package com.luckyzyx.luckytool.ui.compose.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.GlobalFuncService
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.MainPageActivationEffect
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.shell.ShellBadgeState
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.DeviceInfoEntry
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
import com.luckyzyx.luckytool.utils.getDeviceInfoEntries
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getVersionCode
import com.luckyzyx.luckytool.utils.getVersionName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.openUrl
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import java.io.File
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/**
 * 主页数据与操作入口，展示层按当前主题使用对应的原生组件。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomePage(activity: MainActivity, isCurrentPage: Boolean = true) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val zh = remember(context) { isZh(context) }
    // 隐藏功能可在运行期被「关于作者」长按反复切换：必须走可观察 Flow，
    // 不能用 remember 快照（否则开关关掉后 isDev 仍停留在组合时的旧值）
    val isDev by settings.booleanFlow("hidden_function", false).collectAsStateWithLifecycle()

    // 激活状态直接跟随框架服务绑定/断开（观察式），不再靠进入页面时问一次
    val moduleActive by XposedServiceBridge.isModuleActive.collectAsStateWithLifecycle()
    // 用缓存做种子：切回主页时避免 systemInfo 从 null 起步（闪现「加载中」再淡入）
    var systemInfo by remember { mutableStateOf(DeviceInfoCache.info) }
    var rootVersion by remember { mutableStateOf<String?>(null) }
    var frameworkVersion by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var runtimeInfoJob by remember { mutableStateOf<Job?>(null) }
    var deviceInfoJob by remember { mutableStateOf<Job?>(null) }
    var updateInfo by remember { mutableStateOf<String?>(null) }
    var updateClick by remember { mutableStateOf<(() -> Unit)?>(null) }
    var latestUpdate by remember { mutableStateOf<UpdateUtils.UpdateInfo?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var aboutText by remember { mutableStateOf("") }
    var dexDialogVisible by remember { mutableStateOf(false) }
    var pendingOsVersion by remember { mutableStateOf("") }
    var showRestartMenu by remember { mutableStateOf(false) }
    var showOptimizeDex by remember { mutableStateOf(false) }
    var showDonateList by remember { mutableStateOf(false) }
    var showQrType by remember { mutableIntStateOf(-1) }
    var updateStage by remember { mutableStateOf<UpdateStage?>(null) }

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

    fun checkForUpdates() {
        if (!settings.getBoolean("auto_check_update", true)) return
        UpdateUtils(activity, isDev).checkUpdate { info ->
            // 只留存发布信息，长按状态卡是否可用在展示侧按当前 isDev 现场判定
            latestUpdate = info
            // 导航角标：有新版本时在「主页」tab 上提示（受主题页的导航角标开关控制）
            ShellBadgeState.updateAvailable = getVersionCode < info.code
            if (getVersionCode < info.code) {
                updateInfo = "${info.name} (${info.code})"
                updateClick = { updateStage = UpdateStage.Changelog(info) }
            }
        }
    }

    // 仅当前页首次激活或恢复前台时读取；预组合其他标签不会发起服务请求。
    fun loadHomeData() {
        runtimeInfoJob?.cancel()
        runtimeInfoJob = scope.launch {
            val versions = withContext(Dispatchers.IO) {
                DeviceUtils.getRootVersion(context) to DeviceUtils.getFrameWorkVersion(context)
            }
            rootVersion = versions.first
            frameworkVersion = versions.second.takeUnless { "null" in it }
                ?: "${context.getString(R.string.framework_version)} —"
        }
        GlobalFuncService.get(activity) { controller ->
            deviceInfoJob?.cancel()
            deviceInfoJob = scope.launch {
                val (ota, entries) = withContext(Dispatchers.IO) {
                    val ota = controller?.otaVersion
                    ota to (DeviceInfoCache.get(ota) ?: getDeviceInfoEntries(controller))
                }
                DeviceInfoCache.put(ota, entries)
                systemInfo = entries
                checkDexOptimize(controller)
            }
        }
    }
    MainPageActivationEffect(
        isCurrentPage = isCurrentPage,
        onFirstActive = {
            checkForUpdates()
            loadHomeData()
        },
        onResume = ::loadHomeData,
    )

    // 长按状态卡查看更新日志属于隐藏功能：关闭后立刻失效，不残留上一次绑定的回调
    val statusCardLongClick: (() -> Unit)? = if (!isDev) null else latestUpdate?.let { info ->
        { updateStage = UpdateStage.Changelog(info) }
    }

    HomeDashboard(
        state = HomeDashboardState(
            moduleActive = moduleActive,
            moduleVersion = "$getVersionName ($getVersionCode)",
            rootVersion = rootVersion,
            frameworkVersion = frameworkVersion,
            deviceInfo = systemInfo,
            updateVersion = updateInfo,
            showNotice = zh,
        ),
        onRestart = { showRestartMenu = true },
        onAbout = {
            aboutText = if (settings.getBoolean("hidden_function", false)) "忆清鸣、luckyzyx T"
                else "忆清鸣、luckyzyx"
            showAbout = true
        },
        onUpdate = { updateClick?.invoke() },
        onStatusLongClick = statusCardLongClick,
        onCopyDeviceInfo = {
            context.copyStr(DeviceUtils.getOTACOnfigs())
            context.showToast(context.getString(R.string.home_device_info_copied))
        },
        onDonate = {
            context.openUrl(
                if (zh) "https://docs.qq.com/doc/DS2ZDZlNIeUlpdlV1"
                else "https://luckyzyx.github.io/LuckyTool_Doc/en/donate"
            )
        },
        onDonateMethods = { showDonateList = true },
        onOpenNotice = { context.openUrl("https://luckyzyx.github.io/LuckyTool_Doc/use/download_link") },
    )

    // 异步结果可以保留，但只在主页真正可交互时展示弹层。
    if (!isCurrentPage) return

    if (showAbout) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> MiuixAboutDialog(
                aboutText = aboutText,
                onLongPress = {
                    val newValue = !settings.getBoolean("hidden_function", false)
                    settings.set("hidden_function", newValue)
                    aboutText = if (newValue) {
                        "忆清鸣、luckyzyx T"
                    } else {
                        "忆清鸣、luckyzyx"
                    }
                },
                onDismiss = { showAbout = false },
            )

            UiMode.Material -> AlertDialog(
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
    }

    if (showRestartMenu) {
        RestartMenuUtils.RestartMenuDialog(context) { showRestartMenu = false }
    }

    if (dexDialogVisible) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> OverlayDialog(
                show = true,
                onDismissRequest = { dexDialogVisible = false },
            ) {
                MiuixText(
                    text = stringResource(R.string.optimize_dex_after_system_update),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    MiuixTextButton(
                        text = stringResource(R.string.ignore),
                        onClick = { dexDialogVisible = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            dexDialogVisible = false
                            settings.set("current_os_version", pendingOsVersion)
                            showOptimizeDex = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            UiMode.Material -> AlertDialog(
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
        // 原来 Miuix / Material 两条线各手写一遍同一个动作列表，差异只在容器组件；
        // 统一交给 ActionListDialog 后只剩一份数据，且改用 key 而不是下标路由：
        // patreon 在非中文环境插在末尾，用下标判断就得依赖列表长度，key 不随插入位置漂移。
        val donateItems = arrayListOf(
            ChoiceItem(key = "qq", title = stringResource(R.string.qq)),
            ChoiceItem(key = "wechat", title = stringResource(R.string.wechat)),
            ChoiceItem(key = "alipay", title = stringResource(R.string.alipay)),
        )
        // 非中文环境追加 patreon（原实现在下标 3 处 add，等价于追加到末尾）
        if (!zh) {
            donateItems.add(ChoiceItem(key = "patreon", title = stringResource(R.string.patreon)))
        }
        ActionListDialog(
            // 原来两条线都没有标题，ActionListDialog 的 title 必填，用捐赠入口自己的文案补上
            title = stringResource(R.string.donate),
            items = donateItems,
            onDismiss = { showDonateList = false },
            onSelect = { item ->
                // 先关弹层再执行动作，保持原实现里 showDonateList = false 在路由之前的顺序
                showDonateList = false
                when (item.key) {
                    "qq" -> showQrType = 0
                    "wechat" -> showQrType = 1
                    "alipay" -> showQrType = 2
                    "patreon" -> context.openUrl("https://www.patreon.com/LuckyTool")
                }
            },
            // 原两条线都没有可点的按钮（Miuix 无按钮行、Material 是空 confirmButton），
            // 所以这里不造「取消」：showCancel = false 让整行动作区都不画
            showCancel = false,
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

/**
 * 关于作者对话框（Miuix 线）。
 *
 * 弹层由根部 Miuix Scaffold 的 popup host 承载（`renderInRootScaffold` 保持默认 true），
 * 页面内不自装 host。
 */
@Composable
private fun MiuixAboutDialog(
    aboutText: String,
    onLongPress: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.about_author),
        onDismissRequest = onDismiss,
    ) {
        MiuixText(
            text = aboutText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
        )
        Row(modifier = Modifier.padding(top = 12.dp)) {
            MiuixTextButton(
                text = stringResource(android.R.string.ok),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/**
 * 主页系统信息缓存：以 OTA 版本为键，OTA 未变化时复用上一次读取的字段，
 * 避免每次进入主页都重新走一遍 AIDL 调用。
 */
private object DeviceInfoCache {
    private data class Snapshot(val ota: String?, val entries: List<DeviceInfoEntry>)

    @Volatile
    private var snapshot: Snapshot? = null

    val info: List<DeviceInfoEntry>? get() = snapshot?.entries

    fun get(otaVersion: String?): List<DeviceInfoEntry>? =
        snapshot?.takeIf { it.ota == otaVersion }?.entries

    fun put(otaVersion: String?, value: List<DeviceInfoEntry>) {
        snapshot = Snapshot(otaVersion, value)
    }
}

/** 更新对话框状态机：更新日志 → 下载源 → 下载中 → 已下载 */
private sealed class UpdateStage {
    data class Changelog(val info: UpdateUtils.UpdateInfo) : UpdateStage()
    data class Source(val info: UpdateUtils.UpdateInfo, val apkFile: File) : UpdateStage()
    data class Downloaded(val info: UpdateUtils.UpdateInfo, val apkFile: File) : UpdateStage()
    data class Downloading(val apkFile: File, val url: String) : UpdateStage()
}
