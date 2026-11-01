package com.luckyzyx.luckytool.ui.compose.pages

import android.app.Activity
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.ArraySet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.LocalScopeTopInset
import com.luckyzyx.luckytool.ui.compose.components.ScopeScreen
import com.luckyzyx.luckytool.ui.compose.components.PrefIcon
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPageScaffold
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.ThemeController
import com.luckyzyx.luckytool.ui.theme.ThemePrefs
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.BiometricUtils
import com.luckyzyx.luckytool.utils.DonateUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.PrefsTarget
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.backupAllPrefs
import com.luckyzyx.luckytool.utils.base64Decode
import com.luckyzyx.luckytool.utils.base64Encode
import com.luckyzyx.luckytool.utils.clearAllPrefs
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getOSVersionName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.openUrl
import com.luckyzyx.luckytool.utils.prefsData
import com.luckyzyx.luckytool.utils.prefsForTarget
import com.luckyzyx.luckytool.utils.prefsTarget
import com.luckyzyx.luckytool.utils.recordedTarget
import com.luckyzyx.luckytool.utils.showToast
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import kotlin.system.exitProcess
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/**
 * 设置页。
 * 主题 / 其他 / 备份恢复 / 关于四组设置，含备份恢复 JSON 逻辑与生物识别门控。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(activity: MainActivity, onOpenTheme: () -> Unit = {}) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    // 以 Configuration 作 key：语言切换后 isZh 需要重新求值
    val zh = remember(LocalConfiguration.current) { isZh(context) }
    var pendingRestoreJson by remember { mutableStateOf<JSONObject?>(null) }

    // 备份仍走系统文件选择器（落点由用户确认），只是把选择器的起始目录预设成 Download/LuckyTool；
    // 备份前先展示四个 prefs 各自的落点，回调链里没有 Composer，这段收进 lambda
    val backupLauncher = rememberLauncherForActivityResult(
        SaveBackupDocument
    ) { uri ->
        if (uri != null) writeBackupData(activity, uri)
    }
    val startBackup: () -> Unit = {
        backupLauncher.launch("LuckyTool_" + formatDate("yyyyMMdd_HHmmss") + "_backup.json")
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        OpenBackupDocument
    ) { uri ->
        if (uri != null) {
            val entryData = FileUtils.readFromUri(activity, uri)
            val json = JSONObject(base64Decode(entryData))
            // 总是先弹对话框展示落点与当前状态，跨版本提示改在对话框里按数据版本比较决定
            pendingRestoreJson = json
        }
    }

    val deviceSecure = remember(context) {
        context.getSystemService(KeyguardManager::class.java).isDeviceSecure
    }

    var showDonateList by remember { mutableStateOf(false) }
    var showQrType by remember { mutableStateOf(-1) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    MaterialPageScaffold(
        title = stringResource(R.string.nav_setting),
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
            // 行参数在非 Composable 的构建 lambda 内求值（那里没有 Composer），字符串先在这里取一次
            val settingsUiModeTitle = stringResource(R.string.settings_ui_mode)
            val settingsUiModeSummary = stringResource(R.string.settings_ui_mode_summary)
            val themePaletteTitle = stringResource(R.string.theme_palette)
            val themePaletteSummary = stringResource(R.string.theme_palette_summary)
            val autoCheckUpdateTitle = stringResource(R.string.auto_check_update)
            val autoCheckUpdateSummary = stringResource(R.string.auto_check_update_summary)
            val biometricUnlockTitle = stringResource(R.string.enable_biometric_unlock_verification)
            val tileAutoStartTitle = stringResource(R.string.tile_auto_start)
            val tileAutoStartSummary = stringResource(R.string.tile_auto_start_summary)
            val hideModuleIconTitle = stringResource(R.string.hide_desktop_module_icon)
            val hideModuleIconSummary = stringResource(R.string.hide_desktop_module_icon_summary)
            val backupDataTitle = stringResource(R.string.backup_data)
            val restoreDataTitle = stringResource(R.string.restore_data)
            val clearAllDataTitle = stringResource(R.string.clear_all_data)
            val clearAllDataSummary = stringResource(R.string.clear_all_data_summary)
            val donateTitle = stringResource(R.string.donate)
            val donateSummary = stringResource(R.string.donate_summary)
            val feedbackDownloadTitle = stringResource(R.string.feedback_download)
            val feedbackDownloadSummary = stringResource(R.string.feedback_download_summary)
            val participateTranslationTitle = stringResource(R.string.participate_translation)
            val participateTranslationSummary =
                stringResource(R.string.participate_translation_summary)
            val appPermissionTitle = stringResource(R.string.app_permission)
            val appPermissionSummary = stringResource(R.string.app_permission_summary)
            ScopeScreen(
                state = settings,
                modifier = scopeModifier,
            ) {
                // ---------------- 主题 ----------------
                // 主题模式 / 主题色 / 调色风格 / 色彩规格 / 动态取色 全部迁至独立主题页
                group()
                // 界面风格切换（Material / Miuix）：放在设置页而不是主题页，
                // 保证切到 Miuix 后仍能在这里切回 Material。界面风格在上，主题入口在下。
                list(
                    key = ThemeController.KEY_UI_MODE,
                    title = settingsUiModeTitle,
                    entries = UiMode.entries.map { it.name }.toTypedArray(),
                    entryValues = UiMode.entries.map { it.value }.toTypedArray(),
                    default = UiMode.DEFAULT_VALUE,
                    summary = settingsUiModeSummary,
                    leading = { PrefIcon(Icons.Rounded.DisplaySettings) },
                    onChange = { ThemePrefs.notifyChanged() },
                )
                // 导航入口用箭头，不暴露内部色板名称。
                click(
                    title = themePaletteTitle,
                    summary = themePaletteSummary,
                    leading = { PrefIcon(Icons.Rounded.Palette) },
                    value = "",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    onClick = onOpenTheme,
                )

                // ---------------- 其他 ----------------
                group()
                switch(
                    key = "auto_check_update",
                    title = autoCheckUpdateTitle,
                    summary = autoCheckUpdateSummary,
                    default = true,
                    leading = { PrefIcon(Icons.Rounded.SystemUpdate) },
                )
                if (deviceSecure) {
                    switch(
                        key = "enable_biometric_unlock_verification",
                        title = biometricUnlockTitle,
                        leading = { PrefIcon(Icons.Rounded.Fingerprint) },
                        beforeApply = { enable, apply ->
                            if (enable) {
                                // 验证通过才真正写入（取消或失败则不写）
                                BiometricUtils.showBiometricPrompt(
                                    activity,
                                    onSucceed = { apply(true) })
                            } else {
                                apply(false)
                            }
                        },
                    )
                }
                switch(
                    key = "tile_auto_start",
                    title = tileAutoStartTitle,
                    summary = tileAutoStartSummary,
                    default = true,
                    leading = { PrefIcon(Icons.Rounded.Widgets) },
                )
                switch(
                    key = "hide_desktop_module_icon",
                    title = hideModuleIconTitle,
                    summary = hideModuleIconSummary,
                    leading = { PrefIcon(Icons.Rounded.VisibilityOff) },
                    onChange = { value ->
                        AppUtils(context).setComponentDisabled(
                            ComponentName(
                                context.packageName,
                                "${context.packageName}.Hide",
                            ),
                            value,
                        )
                    },
                )

                // 应用权限：与冷启动权限阻塞门是同一个页面内容（pageKey = app_permission）
                click(
                    title = appPermissionTitle,
                    summary = appPermissionSummary,
                    leading = { PrefIcon(Icons.Rounded.AdminPanelSettings) },
                    value = "",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
                    onClick = {
                        activity.requestFunctionNavigation("app_permission", appPermissionTitle)
                    },
                )

                // ---------------- 备份/恢复/清除 ----------------
                group()
                click(
                    title = backupDataTitle,
                    leading = { PrefIcon(Icons.Rounded.SaveAlt) },
                    onClick = { showBackupDialog = true },
                )
                click(
                    title = restoreDataTitle,
                    leading = { PrefIcon(Icons.Rounded.Restore) },
                    onClick = {
                        FileUtils.checkDownloadDir(context, "LuckyTool").apply {
                            if (isFile) delete()
                            if (!exists()) mkdirs()
                        }
                        restoreLauncher.launch("application/json")
                    },
                )
                click(
                    title = clearAllDataTitle,
                    summary = clearAllDataSummary,
                    leading = { PrefIcon(Icons.Rounded.Delete) },
                    onClick = { showClearDialog = true },
                )

                // ---------------- 关于 ----------------
                group()
                click(
                    title = donateTitle,
                    summary = donateSummary,
                    leading = { PrefIcon(Icons.Rounded.FavoriteBorder) },
                    onClick = { showDonateList = true },
                )
                click(
                    title = feedbackDownloadTitle,
                    summary = feedbackDownloadSummary,
                    leading = { PrefIcon(Icons.Rounded.Feedback) },
                    onClick = { showFeedbackDialog = true },
                )
                click(
                    title = participateTranslationTitle,
                    summary = participateTranslationSummary,
                    leading = { PrefIcon(Icons.Rounded.Translate) },
                    onClick = {
                        context.openUrl("https://github.com/luckyzyx/LuckyTool-Localization")
                    },
                )
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

    if (showDonateList) {
        // 回调 lambda 内没有 Composer，取一次给 lambda 捕获
        val donationListTitle = stringResource(R.string.donation_list)
        // key 用渠道标识而不是下标：中文环境没有 patreon / paypal，「捐赠列表」的下标会跟着变
        val donateItems = arrayListOf(
            ChoiceItem(key = "qq", title = stringResource(R.string.qq)),
            ChoiceItem(key = "wechat", title = stringResource(R.string.wechat)),
            ChoiceItem(key = "alipay", title = stringResource(R.string.alipay)),
        )
        if (!zh) {
            donateItems.add(ChoiceItem(key = "patreon", title = stringResource(R.string.patreon)))
            donateItems.add(ChoiceItem(key = "paypal", title = stringResource(R.string.paypal)))
        }
        donateItems.add(ChoiceItem(key = "donation_list", title = donationListTitle))
        ActionListDialog(
            // 原来没有标题，ActionListDialog 的 title 必填，用这个入口自己的文案补上
            title = stringResource(R.string.donate),
            items = donateItems,
            onDismiss = { showDonateList = false },
            onSelect = { item ->
                // 先关再执行动作，保持原实现里 showDonateList = false 在路由之前的顺序
                showDonateList = false
                when (item.key) {
                    "qq" -> showQrType = 0
                    "wechat" -> showQrType = 1
                    "alipay" -> showQrType = 2
                    "patreon" -> context.openUrl("https://www.patreon.com/LuckyTool")
                    "paypal" -> context.openUrl("https://paypal.me/luckyzyx")
                    else -> activity.requestFunctionNavigation("donate", donationListTitle)
                }
            },
        )
    }

    if (showQrType >= 0) {
        DonateUtils.DonateQRDialog(showQrType) { showQrType = -1 }
    }

    if (showClearDialog) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> OverlayDialog(
                show = true,
                onDismissRequest = { showClearDialog = false },
            ) {
                MiuixText(
                    text = stringResource(R.string.clear_all_data_message),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showClearDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            showClearDialog = false
                            context.clearAllPrefs(
                                ModulePrefs,
                                IntentPrefs,
                                SettingsPrefs,
                                OtherPrefs
                            )
                            exitProcess(0)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            UiMode.Material -> AlertDialog(
                onDismissRequest = { showClearDialog = false },
                text = { Text(stringResource(R.string.clear_all_data_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                        context.clearAllPrefs(ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs)
                        exitProcess(0)
                    }) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }

    if (showFeedbackDialog) {
        val feedbackItems = listOf(
            ChoiceItem(key = "coolmarket", title = stringResource(R.string.coolmarket)),
            ChoiceItem(key = "module_doc", title = stringResource(R.string.module_doc)),
            ChoiceItem(key = "qq_chat_group", title = stringResource(R.string.qq_chat_group)),
            ChoiceItem(key = "qq_channel", title = stringResource(R.string.qq_channel)),
            ChoiceItem(key = "telegram_channel", title = stringResource(R.string.telegram_channel)),
            ChoiceItem(key = "lsposed_repo", title = stringResource(R.string.lsposed_repo)),
        )
        ActionListDialog(
            // 原来没有标题，ActionListDialog 的 title 必填，用这个入口自己的文案补上
            title = stringResource(R.string.feedback_download),
            items = feedbackItems,
            onDismiss = { showFeedbackDialog = false },
            onSelect = { item ->
                // 先关再打开链接，保持原实现里 showFeedbackDialog = false 在路由之前的顺序
                showFeedbackDialog = false
                when (item.key) {
                    "coolmarket" -> context.openUrl("coolmarket://u/1930284")
                    "module_doc" -> context.openUrl("https://luckyzyx.gitlab.io/LuckyTool_Doc")
                    "qq_chat_group" -> context.openUrl(
                        "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=3fYu6lT8IHrBPKAfFTNSHbd8wcWX0oGs&authKey=dyIpjTWH8KWHMU3v6gI05T0bAzr6XigJKasMiCwmco1%2F8BRtPCN%2B1zOGgXyK7IUB&noverify=0&group_code=663884734"
                    )

                    "qq_channel" -> context.openUrl("https://pd.qq.com/s/ahjm4zyxb")
                    "telegram_channel" -> context.openUrl("https://t.me/LuckyTool")
                    "lsposed_repo" -> context.openUrl(
                        "https://modules.lsposed.org/module/com.luckyzyx.luckytool"
                    )
                }
            },
        )
    }

    // 备份前确认：展示当前四个 prefs 各自的落点（remote 依赖框架 service 可用）
    if (showBackupDialog) {
        val localLabel = stringResource(R.string.data_prefs_target_local)
        val remoteLabel = stringResource(R.string.data_prefs_target_remote)
        // 每次打开现算：service 可用性会变，remember 会把上一次的状态带过来
        val statusLines = BackupPrefs.map { name ->
            val label =
                if (context.prefsTarget(name) == PrefsTarget.REMOTE) remoteLabel else localLabel
            "$name: $label"
        }
        // 三段式（信息 / 落点 / 说明）与恢复对话框保持同一排版：段间空行，段内逐行
        val message = listOf(
            stringResource(R.string.data_backup_path, DOWNLOAD_TOOL_DIR),
            stringResource(R.string.data_prefs_targets_title) + "\n" + statusLines.joinToString("\n"),
            stringResource(R.string.data_backup_status_tips),
        ).joinToString("\n\n")
        when (LocalUiMode.current) {
            UiMode.Miuix -> OverlayDialog(
                show = true,
                onDismissRequest = { showBackupDialog = false },
            ) {
                MiuixText(text = message, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showBackupDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(R.string.backup_data),
                        onClick = {
                            showBackupDialog = false
                            startBackup()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            UiMode.Material -> AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = {
                        showBackupDialog = false
                        startBackup()
                    }) { Text(stringResource(R.string.backup_data)) }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }

    pendingRestoreJson?.let { json ->
        val osCode = json.optInt("osCode")
        // 只用来决定要不要显示跨版本提示行：备份数据版本与当前系统版本不一致才提示
        val crossVersion = osCode > 0 && osCode != getOSVersionCode
        val versionLines = buildList {
            if (osCode > 0) {
                add("${stringResource(R.string.data_backup_data_version)}: ${getOSVersionName(osCode)}")
            } else {
                add("${stringResource(R.string.data_backup_data_version)}: -")
            }
            add("${stringResource(R.string.data_current_system_version)}: $getOSVersionName")
            if (crossVersion) add(stringResource(R.string.data_restore_version_tips))
        }
        val localLabel = stringResource(R.string.data_prefs_target_local)
        val remoteLabel = stringResource(R.string.data_prefs_target_remote)
        // 落点按备份记录的 target 展示；旧备份没有 target 时说明按当前落点恢复
        val legacyTargets = BackupPrefs.all { json.recordedTarget(it) == null }
        // 与备份对话框同形：标题行 + 逐行落点，旧备份没有 target 时换成一句说明
        val targetsSummary = if (legacyTargets) {
            stringResource(R.string.data_restore_targets_legacy)
        } else {
            stringResource(R.string.data_prefs_targets_title) + "\n" + BackupPrefs.joinToString("\n") { name ->
                val label = when (json.recordedTarget(name)) {
                    PrefsTarget.REMOTE -> remoteLabel
                    else -> localLabel
                }
                "$name: $label"
            }
        }
        // 备份记录的是远程落点、此刻 service 不可用：这些键计入失败，不静默写本地
        val remoteUnwritable = BackupPrefs.any { name ->
            json.recordedTarget(name) == PrefsTarget.REMOTE &&
                    context.prefsTarget(name) != PrefsTarget.REMOTE
        }
        // 同三段式：版本信息 / 落点 / 提示
        val restoreMessage = buildList {
            add(versionLines.joinToString("\n"))
            add(targetsSummary)
            if (remoteUnwritable) add(stringResource(R.string.data_remote_unavailable_tips))
        }.joinToString("\n\n")
        when (LocalUiMode.current) {
            UiMode.Miuix -> OverlayDialog(
                show = true,
                onDismissRequest = { pendingRestoreJson = null },
            ) {
                MiuixText(
                    text = restoreMessage,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { pendingRestoreJson = null },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(R.string.restore_data),
                        onClick = {
                            pendingRestoreJson = null
                            writeRestoreData(activity, json)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }

            UiMode.Material -> AlertDialog(
                onDismissRequest = { pendingRestoreJson = null },
                text = { Text(restoreMessage) },
                confirmButton = {
                    TextButton(onClick = {
                        pendingRestoreJson = null
                        writeRestoreData(activity, json)
                    }) { Text(stringResource(R.string.restore_data)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestoreJson = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

/**
 * Download/LuckyTool 在「外部存储」文档 provider 里的 document uri。
 *
 * EXTRA_INITIAL_URI 只吃 document uri（buildDocumentUri 的 documentId 形如 primary:Download/LuckyTool），
 * 拿不到时选择器会退回默认目录，所以这只是「打开在哪」的预设，不影响实际读取的 uri。
 */
private fun backupDirDocumentUri(): Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents",
    "primary:$DOWNLOAD_TOOL_DIR",
)

/**
 * 恢复用的文件选择器：ACTION_OPEN_DOCUMENT + EXTRA_INITIAL_URI 预设到备份目录，
 * 用户不必每次从 Download 根目录一层层点进 LuckyTool（部分文件管理器会忽略该预设，回退到默认目录）。
 */
private object OpenBackupDocument : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, backupDirDocumentUri())
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/**
 * 备份用的文件选择器：ACTION_CREATE_DOCUMENT + EXTRA_INITIAL_URI 把起始目录预设到 Download/LuckyTool，
 * EXTRA_TITLE 给一个带时间戳的建议文件名。落点仍由用户在系统选择器里确认（可以改到别处），
 * 预设只是省掉「从 Download 根目录一层层点进去」。
 */
private object SaveBackupDocument : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, input)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, backupDirDocumentUri())
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

/**
 * 备份：JSON(osCode + 四个 prefs 文件，各带 target 记录落点) → base64 → 写进用户在文件选择器里确认的 uri。
 *
 * 落点仍由用户在系统选择器里确认（起始目录已预设到 Download/LuckyTool），写入失败只提示错误。
 */
private fun writeBackupData(context: Context, uri: Uri) {
    val json = JSONObject().apply { put("osCode", getOSVersionCode) }
    val dataMapList = context.backupAllPrefs(*BackupPrefs.toTypedArray())
    dataMapList.keys.forEach { prefs ->
        val jsons = JSONObject()
        dataMapList[prefs]?.keys?.forEach { key ->
            val value = dataMapList[prefs]?.get(key)
            // 集合值一律写 JSONArray（不能只认 HashSet）：JSONObject.put 会把其它集合实现
            // （SharedPreferences 复制出的 android.util.ArraySet 等）兜底成 toString() 字符串。
            // 不认识的类型同样不能落到那个兜底分支：记录日志后跳过该键，绝不写成字符串。
            try {
                when (value) {
                    null -> Unit
                    is Set<*> -> {
                        val arr = JSONArray()
                        value.forEach { arr.put(it) }
                        jsons.put(key, arr)
                    }

                    is String, is Boolean, is Int, is Long, is Float, is Double -> jsons.put(
                        key,
                        value
                    )

                    else -> error("${value.javaClass.name} is not JSON-safe")
                }
            } catch (t: Throwable) {
                LogUtils.e("SettingPage", "backup skip $key", "$t", false)
            }
        }
        json.put(prefs, JSONObject().apply {
            put("target", context.prefsTarget(prefs).key)
            put("data", jsons)
        })
    }
    val str = base64Encode(json.toString())
    val saved = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { it.write(str.toByteArray()) }
            ?: error("openOutputStream null")
    }.onFailure {
        LogUtils.e("SettingPage", "backup write", "$it", false)
    }.isSuccess
    if (saved) {
        context.showToast(context.getString(R.string.data_backup_saved, backupSavedPath(context, uri)))
    } else {
        context.showToast(context.getString(R.string.data_backup_error))
    }
}

/**
 * toast 里展示的落点：外部存储 provider 的 documentId 就是相对路径（primary:Download/LuckyTool/x.json），
 * 取冒号后半段即可；别的 provider（如 Downloads）退回文件名。
 */
private fun backupSavedPath(context: Context, uri: Uri): String {
    val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
    docId?.substringAfter(':', docId)?.takeIf { it.contains('/') }?.let { return it }
    val name = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
    return name ?: uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
}

/**
 * 恢复：按备份记录的 target 逐 prefs 逐键写回，完成后重启 Activity。
 *
 * target 是落点而不是建议：备份记 remote 但此刻框架 service 不可用时，这些键按失败计数并提示，
 * 不静默回落到本地 —— 否则界面显示恢复成功，hook 侧却读不到。
 */
private fun writeRestoreData(context: Context, json: JSONObject) {
    if (json.length() <= 0) return
    json.remove("osCode")
    var success = 0
    var failed = 0
    var remoteUnwritable = 0
    val failedKeys = ArrayList<String>()
    json.keys().forEach { prefsName ->
        val prefsDatas = json.prefsData(prefsName) ?: return@forEach
        if (prefsDatas.length() <= 0) return@forEach
        // 旧备份没记 target：退回当前落点（[prefsTarget] 解析），保证这些键仍写到该去的那份 prefs
        val target = json.recordedTarget(prefsName) ?: context.prefsTarget(prefsName)
        val prefs = context.prefsForTarget(prefsName, target)
        if (prefs == null) {
            remoteUnwritable += prefsDatas.length()
            failed += prefsDatas.length()
            failedKeys.add("$prefsName/*(${target.key} unavailable)")
            LogUtils.e(
                "SettingPage",
                "restore skip $prefsName",
                "${target.key} unavailable, ${prefsDatas.length()} keys",
                false,
            )
            return@forEach
        }
        prefsDatas.keys().forEach { key ->
            val value = prefsDatas.get(key)
            try {
                val editor = prefs.edit()
                when (value.javaClass.simpleName) {
                    "Boolean" -> editor.putBoolean(key, value as Boolean)
                    "Integer" -> editor.putInt(key, value as Int)
                    "JSONArray" -> {
                        // 集合一律写成 java.util.HashSet：remote 侧按 Serializable 反序列化，
                        // Kotlin 单例集合与 androidx 集合解析不到会让整次更新失败
                        val set = HashSet<String>()
                        val list = value as JSONArray
                        for (i in 0 until list.length()) set.add(list[i] as String)
                        editor.putStringSet(key, set)
                    }

                    "String" -> editor.putString(key, value as String)
                    else -> error("${value.javaClass.simpleName} is not restorable")
                }
                if (!editor.commit()) error("write failed")
                success++
            } catch (t: Throwable) {
                // 恢复不了的值：记录日志、跳过，并把该键从目标 prefs 移除，不让坏数据留在设备上
                failed++
                failedKeys.add("$prefsName/$key")
                LogUtils.e("SettingPage", "restore skip $prefsName/$key", "$t", false)
                runCatching { prefs.edit().remove(key).commit() }
            }
        }
    }
    if (failed > 0) {
        LogUtils.e(
            "SettingPage",
            "restore result",
            "success=$success failed=$failed remoteUnwritable=$remoteUnwritable " +
                    failedKeys.joinToString(),
            false,
        )
    }
    context.showToast(context.getString(R.string.data_restore_result, success, failed))
    if (remoteUnwritable > 0) {
        context.showToast(
            context.getString(R.string.data_restore_remote_unavailable, remoteUnwritable)
        )
    }
    (context as? MainActivity)?.restart()
}

/** 备份/恢复涉及的四个 prefs 文件，顺序即 JSON 里的顺序 */
private val BackupPrefs = listOf(ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs)

/** 备份目录（外部存储相对路径）：只用来预设文件选择器打开的位置，真实落点由用户在选择器里确认 */
private const val DOWNLOAD_TOOL_DIR = "Download/LuckyTool"
