package com.luckyzyx.luckytool.ui.compose.pages

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.ArraySet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.luckyzyx.luckytool.ui.application.MyApplication
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchCard
import com.luckyzyx.luckytool.ui.compose.components.PrefValueCard
import com.luckyzyx.luckytool.ui.theme.keyColorOptions
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.BiometricUtils
import com.luckyzyx.luckytool.utils.DonateUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PrefState
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
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putInt
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.showToast
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess

/**
 * 设置页（旧 SettingsFragment 的 Compose 等价实现）。
 * 主题 / 其他 / 备份恢复 / 关于四组设置，含备份恢复 JSON 逻辑与生物识别门控。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(activity: MainActivity) {
    val context = LocalContext.current
    val settings = remember { PrefState.of(context, SettingsPrefs) }
    val zh = remember(context) { isZh(context) }
    val reload = { (activity.application as MyApplication).reloadAllActivities() }
    var pendingRestoreJson by remember { mutableStateOf<JSONObject?>(null) }

    // 备份/恢复（旧 registerForActivityResult）
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) writeBackupData(activity, uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val entryData = FileUtils.readFromUri(activity, uri)
            val json = JSONObject(base64Decode(entryData))
            val osCode = json.optInt("osCode")
            if (osCode > 0 && osCode != getOSVersionCode) {
                // 旧 checkRestoreData：跨版本备份先提示，再恢复
                pendingRestoreJson = json
            } else {
                writeRestoreData(activity, json)
            }
        }
    }

    val deviceSecure = remember(context) {
        context.getSystemService(KeyguardManager::class.java).isDeviceSecure
    }

    var showDonateList by remember { mutableStateOf(false) }
    var showQrType by remember { mutableStateOf(-1) }
    var showDarkThemeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var darkThemeSelected by remember {
        mutableStateOf(settings.getString("dark_theme", "0") ?: "0")
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_setting)) }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------------- 主题 ----------------
            item(key = "theme_header") {
                PrefCategoryHeader(stringResource(R.string.theme_title))
            }
            item(key = "use_dynamic_color") {
                SettingsSwitch(
                    settings = settings,
                    key = "use_dynamic_color",
                    title = stringResource(R.string.use_dynamic_color),
                    summary = stringResource(R.string.use_dynamic_color_summary),
                    default = true,
                    visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    onChanged = { reload() },
                )
            }
            item(key = "dark_theme") {
                val darkThemeEntries = context.resources.getStringArray(R.array.dark_theme)
                val darkThemeValues = arrayOf("0", "1", "2", "3")
                PrefValueCard(
                    title = stringResource(R.string.dark_theme),
                    value = darkThemeEntries.getOrNull(darkThemeValues.indexOf(darkThemeSelected))
                        ?: darkThemeSelected,
                    onClick = { showDarkThemeDialog = true },
                )
            }
            item(key = "theme_palette") {
                PrefValueCard(
                    title = stringResource(R.string.theme_palette),
                    value = settings.getString("palette_style", "TonalSpot") ?: "TonalSpot",
                    summary = stringResource(R.string.theme_palette_summary),
                    onClick = { showPaletteDialog = true },
                )
            }

            // ---------------- 其他 ----------------
            item(key = "other_header") {
                PrefCategoryHeader(stringResource(R.string.other_settings))
            }
            item(key = "auto_check_update") {
                SettingsSwitch(
                    settings = settings,
                    key = "auto_check_update",
                    title = stringResource(R.string.auto_check_update),
                    summary = stringResource(R.string.auto_check_update_summary),
                    default = true,
                )
            }
            if (deviceSecure) {
                item(key = "enable_biometric_unlock_verification") {
                    var checked by remember {
                        mutableStateOf(
                            settings.getBoolean("enable_biometric_unlock_verification", false)
                        )
                    }
                    PrefSwitchCard(
                        title = stringResource(R.string.enable_biometric_unlock_verification),
                        checked = checked,
                        onCheckedChange = { enable ->
                            if (enable) {
                                // 旧行为：验证通过才真正写入
                                BiometricUtils.showBiometricPrompt(activity, onSucceed = {
                                    settings.set("enable_biometric_unlock_verification", true)
                                    checked = true
                                })
                            } else {
                                settings.set("enable_biometric_unlock_verification", false)
                                checked = false
                            }
                        },
                    )
                }
            }
            item(key = "tile_auto_start") {
                SettingsSwitch(
                    settings = settings,
                    key = "tile_auto_start",
                    title = stringResource(R.string.tile_auto_start),
                    summary = stringResource(R.string.tile_auto_start_summary),
                    default = true,
                )
            }
            item(key = "hide_function_page_icon") {
                SettingsSwitch(
                    settings = settings,
                    key = "hide_function_page_icon",
                    title = stringResource(R.string.hide_function_page_icon),
                    onChanged = { activity.restart() },
                )
            }
            item(key = "hide_desktop_module_icon") {
                SettingsSwitch(
                    settings = settings,
                    key = "hide_desktop_module_icon",
                    title = stringResource(R.string.hide_desktop_module_icon),
                    summary = stringResource(R.string.hide_desktop_module_icon_summary),
                    onChanged = { value ->
                        AppUtils(context).setComponentDisabled(
                            ComponentName(context.packageName, "${context.packageName}.Hide"),
                            value,
                        )
                    },
                )
            }

            // ---------------- 备份/恢复/清除 ----------------
            item(key = "backup_header") {
                PrefCategoryHeader(stringResource(R.string.backup_restore_clear))
            }
            item(key = "backup_data") {
                PrefCard(
                    title = stringResource(R.string.backup_data),
                    onClick = {
                        FileUtils.checkDownloadDir(context, "LuckyTool").apply {
                            if (isFile) delete()
                            if (!exists()) mkdirs()
                        }
                        val fileName =
                            "LuckyTool_" + formatDate("yyyyMMdd_HHmmss") + "_backup.json"
                        if (IntentUtils(activity).checkCreateDocument()) {
                            backupLauncher.launch(fileName)
                        } else {
                            context.showToast("Intent Create Document Error!")
                        }
                    },
                )
            }
            item(key = "restore_data") {
                PrefCard(
                    title = stringResource(R.string.restore_data),
                    onClick = {
                        FileUtils.checkDownloadDir(context, "LuckyTool").apply {
                            if (isFile) delete()
                            if (!exists()) mkdirs()
                        }
                        restoreLauncher.launch("application/json")
                    },
                )
            }
            item(key = "clear_all_data") {
                PrefCard(
                    title = stringResource(R.string.clear_all_data),
                    summary = stringResource(R.string.clear_all_data_summary),
                    onClick = { showClearDialog = true },
                )
            }

            // ---------------- 关于 ----------------
            item(key = "about_header") {
                PrefCategoryHeader(stringResource(R.string.about_title))
            }
            item(key = "donate") {
                PrefCard(
                    title = stringResource(R.string.donate),
                    summary = stringResource(R.string.donate_summary),
                    onClick = {
                        showDonateList = true
                    },
                )
            }
            item(key = "feedback_download") {
                PrefCard(
                    title = stringResource(R.string.feedback_download),
                    summary = stringResource(R.string.feedback_download_summary),
                    onClick = { showFeedbackDialog = true },
                )
            }
            item(key = "participate_translation") {
                PrefCard(
                    title = stringResource(R.string.participate_translation),
                    summary = stringResource(R.string.participate_translation_summary),
                    onClick = {
                        context.openUrl("https://github.com/luckyzyx/LuckyTool-Localization")
                    },
                )
            }
        }
    }

    if (showDonateList) {
        val donateList = arrayListOf(
            context.getString(R.string.qq),
            context.getString(R.string.wechat),
            context.getString(R.string.alipay),
            context.getString(R.string.donation_list),
        )
        if (!zh) {
            donateList.add(3, context.getString(R.string.patreon))
            donateList.add(4, context.getString(R.string.paypal))
        }
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
                                    3 -> if (zh) {
                                        activity.requestFunctionNavigation(
                                            "donate",
                                            context.getString(R.string.donation_list),
                                        )
                                    } else {
                                        context.openUrl("https://www.patreon.com/LuckyTool")
                                    }
                                    4 -> context.openUrl("https://paypal.me/luckyzyx")
                                    5 -> activity.requestFunctionNavigation(
                                        "donate",
                                        context.getString(R.string.donation_list),
                                    )
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

    if (showDarkThemeDialog) {
        val darkThemeEntries = context.resources.getStringArray(R.array.dark_theme)
        val darkThemeValues = arrayOf("0", "1", "2", "3")
        AlertDialog(
            onDismissRequest = { showDarkThemeDialog = false },
            title = { Text(stringResource(R.string.dark_theme)) },
            text = {
                Column {
                    darkThemeEntries.forEachIndexed { index, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = darkThemeSelected == darkThemeValues[index],
                                    role = Role.RadioButton,
                                    onClick = {
                                        darkThemeSelected = darkThemeValues[index]
                                        settings.set("dark_theme", darkThemeSelected)
                                        showDarkThemeDialog = false
                                        reload()
                                    },
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = darkThemeSelected == darkThemeValues[index],
                                onClick = null,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showPaletteDialog) {
        ThemePaletteDialog(
            settings = settings,
            onDismiss = { showPaletteDialog = false },
            onChanged = {
                showPaletteDialog = false
                reload()
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
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

    if (showFeedbackDialog) {
        val items = arrayOf(
            context.getString(R.string.coolmarket),
            context.getString(R.string.module_doc),
            context.getString(R.string.qq_chat_group),
            context.getString(R.string.qq_channel),
            context.getString(R.string.telegram_channel),
            context.getString(R.string.lsposed_repo),
        )
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            text = {
                Column {
                    items.forEachIndexed { index, label ->
                        Text(
                            label,
                            Modifier.fillMaxWidth().clickable {
                                showFeedbackDialog = false
                                when (index) {
                                    0 -> context.openUrl("coolmarket://u/1930284")
                                    1 -> context.openUrl("https://luckyzyx.gitlab.io/LuckyTool_Doc")
                                    2 -> context.openUrl(
                                        "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=3fYu6lT8IHrBPKAfFTNSHbd8wcWX0oGs&authKey=dyIpjTWH8KWHMU3v6gI05T0bAzr6XigJKasMiCwmco1%2F8BRtPCN%2B1zOGgXyK7IUB&noverify=0&group_code=663884734"
                                    )

                                    3 -> context.openUrl("https://pd.qq.com/s/ahjm4zyxb")
                                    4 -> context.openUrl("https://t.me/LuckyTool")
                                    5 -> context.openUrl(
                                        "https://modules.lsposed.org/module/com.luckyzyx.luckytool"
                                    )
                                }
                            }.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    pendingRestoreJson?.let { json ->
        val osCode = json.optInt("osCode")
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            text = {
                Text(
                    """
                    ${context.getString(R.string.data_backup_data_version)}: ${getOSVersionName(osCode)}
                    ${context.getString(R.string.data_current_system_version)}: $getOSVersionName
                    
                    ${context.getString(R.string.data_restore_version_tips)}
                    """.trimIndent()
                )
            },
            confirmButton = {
                TextButton(onClick = { pendingRestoreJson = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingRestoreJson = null
                    writeRestoreData(activity, json)
                }) { Text(stringResource(R.string.ignore)) }
            },
        )
    }
}

/** 设置开关（写入 PrefState，可选 onChanged 副作用，如重启 Activity / 系统组件） */
@Composable
private fun SettingsSwitch(
    settings: PrefState,
    key: String,
    title: String,
    summary: String? = null,
    default: Boolean = false,
    visible: Boolean = true,
    onChanged: ((Boolean) -> Unit)? = null,
) {
    if (!visible) return
    var checked by remember { mutableStateOf(settings.getBoolean(key, default)) }
    PrefSwitchCard(
        title = title,
        summary = summary,
        checked = checked,
        onCheckedChange = { value ->
            checked = value
            settings.set(key, value)
            onChanged?.invoke(value)
        },
    )
}

/** 主题配色对话框：主题色（跟随系统 / 15 预设）+ 调色风格 + 色彩规格，对齐 KernelSU 颜色屏幕（简化为对话框） */
@Composable
private fun ThemePaletteDialog(
    settings: PrefState,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val dynamicEnabled = settings.getBoolean("use_dynamic_color", true)
    val keyColor = settings.getInt("key_color", 0)
    val currentStyle = settings.getString("palette_style", "TonalSpot") ?: "TonalSpot"
    val currentSpec = settings.getString("color_spec", "SPEC_2025") ?: "SPEC_2025"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_palette)) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.theme_color_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ThemeColorButton(
                            color = null,
                            selected = dynamicEnabled,
                            onClick = {
                                settings.set("use_dynamic_color", true)
                                onChanged()
                            },
                        )
                    }
                    keyColorOptions.forEach { argb ->
                        ThemeColorButton(
                            color = Color(argb),
                            selected = !dynamicEnabled && keyColor == argb,
                            onClick = {
                                settings.set("use_dynamic_color", false)
                                settings.set("key_color", argb)
                                onChanged()
                            },
                        )
                    }
                }
                Text(
                    stringResource(R.string.palette_style_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                PaletteStyle.entries.forEach { style ->
                    PaletteRadioRow(
                        label = style.name,
                        selected = style.name == currentStyle,
                        onClick = {
                            settings.set("palette_style", style.name)
                            onChanged()
                        },
                    )
                }
                Text(
                    stringResource(R.string.color_spec_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                ColorSpec.SpecVersion.entries.forEach { spec ->
                    PaletteRadioRow(
                        label = spec.name,
                        selected = spec.name == currentSpec,
                        onClick = {
                            settings.set("color_spec", spec.name)
                            onChanged()
                        },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

/** 主题色圆形按钮：color 为 null 时渲染"跟随系统"双半圆（primaryContainer / tertiaryContainer，对齐 KernelSU） */
@Composable
private fun ThemeColorButton(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color ?: Color.Transparent)
            .drawBehind {
                if (color == null) {
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
            }
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) colorScheme.primary else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            val checkColor = when {
                color == null -> colorScheme.primary
                color.luminance() > 0.5f -> Color.Black
                else -> Color.White
            }
            Text("✓", color = checkColor, fontWeight = FontWeight.Bold)
        }
    }
}

/** 调色风格 / 色彩规格单选行（写入偏好后由 onChanged 触发 Activity recreate 生效） */
@Composable
private fun PaletteRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** 旧 writeBackupData：JSON(osCode + 四个 prefs 文件) → base64 → 写入 uri */
private fun writeBackupData(context: Context, uri: Uri) {
    val json = JSONObject().apply { put("osCode", getOSVersionCode) }
    val dataMapList = context.backupAllPrefs(ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs)
    dataMapList.keys.forEach { prefs ->
        val jsons = JSONObject()
        dataMapList[prefs]?.keys?.forEach { key ->
            val value = dataMapList[prefs]?.get(key)
            if (value?.javaClass?.simpleName == "HashSet") {
                val arr = JSONArray()
                (value as HashSet<*>).toTypedArray().forEach { arr.put(it) }
                jsons.put(key, arr)
            } else {
                jsons.put(key, value)
            }
        }
        json.put(prefs, jsons)
    }
    val str = base64Encode(json.toString())
    try {
        context.contentResolver.openFileDescriptor(uri, "w")?.use { its ->
            FileOutputStream(its.fileDescriptor).use { it.write(str.toByteArray()) }
        }
        context.showToast(context.getString(R.string.data_backup_complete))
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        context.showToast(context.getString(R.string.data_backup_error))
    } catch (e: IOException) {
        e.printStackTrace()
        context.showToast(context.getString(R.string.data_backup_error))
    }
}

/** 旧 writeRestoreData：逐 prefs 逐键写回，完成后重启 Activity */
private fun writeRestoreData(context: Context, json: JSONObject) {
    if (json.length() <= 0) return
    json.remove("osCode")
    json.keys().forEach { prefs ->
        val prefsDatas = json.getJSONObject(prefs)
        if (prefsDatas.length() > 0) {
            prefsDatas.keys().forEach { key ->
                val value = prefsDatas.get(key)
                when (value.javaClass.simpleName) {
                    "Boolean" -> context.putBoolean(prefs, key, value as Boolean)
                    "Integer" -> context.putInt(prefs, key, value as Int)
                    "JSONArray" -> {
                        val set = ArraySet<String>()
                        val list = value as JSONArray
                        for (i in 0 until list.length()) set.add(list[i] as String)
                        context.putStringSet(prefs, key, set)
                    }

                    "String" -> context.putString(prefs, key, value as String)
                    else -> context.showToast("Error: $key")
                }
            }
        }
    }
    context.showToast(context.getString(R.string.data_restore_complete))
    (context as? MainActivity)?.restart()
}
