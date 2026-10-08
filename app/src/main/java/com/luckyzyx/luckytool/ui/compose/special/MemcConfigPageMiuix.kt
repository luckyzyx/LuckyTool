package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Memc 帧插入配置 Miuix 线内容。
 *
 * 与 Material 线同结构：Import Xml / Reset 卡片组 + Packages/Activitys 双 Tab + 两个面板 + 四个弹层。
 * 行为等价：状态与 IPC 写值全部复用 [MemcConfigState]（唯一一份，见 `MemcConfigPage.kt`），
 * 弹层走根部 host 的 `overlay.OverlayDialog`（默认 `renderInRootScaffold = true`）。
 * 允许差异：卡片水平内缩用 `MiuixPrefDefaults.CardHorizontalInset`（12dp，契约 §4.2），
 * 行摘要用单字符串（`Rate: …\nType: …`）替代 Material 的双 `Text` 列。
 */
@Composable
internal fun MemcConfigContentMiuix() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { MemcConfigState(context) }
    memcRefresh = { state.reload() }

    var tabIndex by remember { mutableIntStateOf(0) }

    var showPkgDialog by remember { mutableStateOf(false) }
    var pkgTarget by remember { mutableStateOf<MemcConfigPackage?>(null) }
    var showActDialog by remember { mutableStateOf(false) }
    var actTarget by remember { mutableStateOf<MemcConfigActivity?>(null) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showVersionSheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val stream = safeOfNull { context.contentResolver.openInputStream(uri) }
                if (stream != null) scope.launch { state.reset(stream, "") }
            }
        }

    LaunchedEffect(Unit) {
        state.reload()
    }

    Column(Modifier.fillMaxSize()) {
        PrefGroup(
            modifier = Modifier.padding(
                horizontal = MiuixPrefDefaults.CardHorizontalInset,
                vertical = 8.dp,
            )
        ) {
            item {
                PrefRow(
                    title = stringResource(R.string.import_) + " Xml",
                    onClick = {
                        FileUtils.checkDownloadDir(context, "LuckyTool")
                        importLauncher.launch("text/xml")
                    },
                )
            }
            item {
                PrefRow(
                    title = stringResource(R.string.reset),
                    onClick = { showResetConfirm = true },
                )
            }
        }
        TabRow(
            tabs = listOf("Packages", "Activitys"),
            selectedTabIndex = tabIndex,
            onTabSelected = { tabIndex = it },
            modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
        )
        when (tabIndex) {
            0 -> MemcPackagePanelMiuix(
                state = state,
                modifier = Modifier.weight(1f),
                onEdit = { target ->
                    pkgTarget = target
                    showPkgDialog = true
                },
            )

            1 -> MemcActivityPanelMiuix(
                state = state,
                modifier = Modifier.weight(1f),
                onEdit = { target ->
                    actTarget = target
                    showActDialog = true
                },
            )
        }
    }

    if (showPkgDialog) {
        MemcPackageDialogMiuix(
            context = context,
            initial = pkgTarget,
            onDismiss = { showPkgDialog = false },
            onSave = { new ->
                state.savePackage(pkgTarget, new)
                showPkgDialog = false
            },
            onDelete = { info ->
                state.deletePackage(info)
                showPkgDialog = false
            },
        )
    }

    if (showActDialog) {
        MemcActivityDialogMiuix(
            context = context,
            initial = actTarget,
            onDismiss = { showActDialog = false },
            onSave = { new ->
                state.saveActivity(actTarget, new)
                showActDialog = false
            },
            onDelete = { info ->
                state.deleteActivity(info)
                showActDialog = false
            },
        )
    }

    if (showResetConfirm) {
        OverlayDialog(
            show = true,
            summary = stringResource(R.string.restore_frame_insertion_configuration_data),
            onDismissRequest = { showResetConfirm = false },
        ) {
            DialogButtons(
                dismissText = stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onDismiss = { showResetConfirm = false },
                onConfirm = {
                    showResetConfirm = false
                    showVersionSheet = true
                },
            )
        }
    }

    if (showVersionSheet) {
        OverlayDialog(
            show = true,
            onDismissRequest = { showVersionSheet = false },
        ) {
            Column {
                listOf("x7", "x7p").forEach { v ->
                    MiuixListItem(
                        title = v,
                        onClick = {
                            showVersionSheet = false
                            scope.launch { state.reset(null, v) }
                        },
                    )
                }
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showVersionSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Miuix 线弹层按钮行：与 Material `AlertDialog` 同序（左 dismiss / 右 confirm）。 */
@Composable
private fun DialogButtons(
    dismissText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(
            text = dismissText,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Miuix 线搜索框：占位符由 `label` + `useLabelAsPlaceholder` 承担，前置放大镜图标。 */
@Composable
private fun MemcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
        label = placeholder,
        useLabelAsPlaceholder = true,
        singleLine = true,
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
        },
    )
}

@Composable
internal fun MemcPackagePanelMiuix(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigPackage?) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemcSearchField(
            value = state.pkgQuery,
            onValueChange = { state.applyPkgQuery(it) },
            enabled = !state.pkgLoading,
            placeholder = "PackageName",
        )
        MiuixPrefItem(
            index = 0,
            count = 1,
            modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
        ) {
            MiuixListItem(title = "＋ Add", onClick = { onEdit(null) })
        }
        if (state.pkgFilter.isEmpty() && !state.pkgLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                MiuixText(stringResource(R.string.no_memc_data))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = MiuixPrefDefaults.CardHorizontalInset,
                    end = MiuixPrefDefaults.CardHorizontalInset,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
            ) {
                itemsIndexed(
                    state.pkgFilter,
                    key = { _, info -> "${info.packName}|${info.rate}|${info.type}" },
                ) { index, info ->
                    MiuixPrefItem(index = index, count = state.pkgFilter.size) {
                        MiuixListItem(
                            title = info.packName,
                            summary = "Rate: ${info.rate}\nType: ${info.type}",
                            onClick = { onEdit(info) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MemcActivityPanelMiuix(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigActivity?) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemcSearchField(
            value = state.actQuery,
            onValueChange = { state.applyActQuery(it) },
            enabled = !state.actLoading,
            placeholder = "PackageName / ActivityName",
        )
        MiuixPrefItem(
            index = 0,
            count = 1,
            modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
        ) {
            MiuixListItem(title = "＋ Add", onClick = { onEdit(null) })
        }
        if (state.actFilter.isEmpty() && !state.actLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                MiuixText(stringResource(R.string.no_memc_data))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = MiuixPrefDefaults.CardHorizontalInset,
                    end = MiuixPrefDefaults.CardHorizontalInset,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
            ) {
                itemsIndexed(
                    state.actFilter,
                    key = { _, info -> "${info.packName}|${info.activity}|${info.type}" },
                ) { index, info ->
                    MiuixPrefItem(index = index, count = state.actFilter.size) {
                        MiuixListItem(
                            title = info.packName,
                            summary = "${info.activity}\nType: ${info.type}",
                            onClick = { onEdit(info) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MemcPackageDialogMiuix(
    context: Context,
    initial: MemcConfigPackage?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigPackage) -> Unit,
    onDelete: (MemcConfigPackage) -> Unit,
) {
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var rate by remember(initial) { mutableStateOf(initial?.rate ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    OverlayDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = packName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = "PackageName",
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_baseline_extension_24),
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppPicker = true },
            )
            TextField(
                value = rate,
                onValueChange = { rate = it },
                singleLine = true,
                label = "ScreenRate",
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = type,
                onValueChange = { type = it },
                singleLine = true,
                label = "Type",
                modifier = Modifier.fillMaxWidth(),
            )
            MiuixText(
                stringResource(
                    R.string.edit_memc_configuration_tips,
                    CommandUtils.memcHdrConfigHelp,
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            DialogButtons(
                dismissText = if (initial != null) stringResource(R.string.remove)
                else stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onDismiss = { if (initial != null) showDeleteConfirm = true else onDismiss() },
                onConfirm = {
                    if (packName.isBlank() || rate.isBlank() || type.isBlank()) {
                        context.showToast("Data is incomplete!")
                    } else {
                        onSave(MemcConfigPackage(packName, rate, type))
                    }
                },
            )
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            title = "PackageName",
            multiMode = false,
            onDismiss = { showAppPicker = false },
            onConfirm = { list ->
                if (list.isNotEmpty()) packName = list.first().packageName
                showAppPicker = false
            },
        )
    }

    if (showDeleteConfirm && initial != null) {
        OverlayDialog(
            show = true,
            summary = stringResource(
                R.string.confirm_to_delete_this_configuration,
                initial.packName,
            ),
            onDismissRequest = { showDeleteConfirm = false },
        ) {
            DialogButtons(
                dismissText = stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete(initial)
                },
            )
        }
    }
}

@Composable
internal fun MemcActivityDialogMiuix(
    context: Context,
    initial: MemcConfigActivity?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigActivity) -> Unit,
    onDelete: (MemcConfigActivity) -> Unit,
) {
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var activity by remember(initial) { mutableStateOf(initial?.activity ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }
    var activityInfos by remember { mutableStateOf(emptyList<ActivityInfo>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OverlayDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = packName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = "PackageName",
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_baseline_extension_24),
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppPicker = true },
            )
            TextField(
                value = activity,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = "ActivityName",
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_baseline_extension_24),
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (packName.isBlank()) {
                            context.showToast("PackageName is null!")
                        } else {
                            scope.launch(Dispatchers.IO) {
                                val packInfo = PackageUtils(context.packageManager)
                                    .getPackageInfo(packName, PackageManager.GET_ACTIVITIES)
                                withContext(Dispatchers.Main) {
                                    if (packInfo == null) {
                                        context.showToast("App data is null!")
                                    } else {
                                        activityInfos =
                                            packInfo.activities?.toList() ?: emptyList()
                                        showActivityPicker = true
                                    }
                                }
                            }
                        }
                    },
            )
            TextField(
                value = type,
                onValueChange = { type = it },
                singleLine = true,
                label = "Type",
                modifier = Modifier.fillMaxWidth(),
            )
            MiuixText(
                stringResource(
                    R.string.edit_memc_configuration_tips,
                    CommandUtils.memcConfigHelp,
                ),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            DialogButtons(
                dismissText = if (initial != null) stringResource(R.string.remove)
                else stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onDismiss = { if (initial != null) showDeleteConfirm = true else onDismiss() },
                onConfirm = {
                    if (packName.isBlank() || activity.isBlank() || type.isBlank()) {
                        context.showToast("Data is incomplete!")
                    } else {
                        onSave(MemcConfigActivity(packName, activity, type))
                    }
                },
            )
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            title = "PackageName",
            multiMode = false,
            onDismiss = { showAppPicker = false },
            onConfirm = { list ->
                if (list.isNotEmpty()) packName = list.first().packageName
                showAppPicker = false
            },
        )
    }

    if (showActivityPicker) {
        OverlayDialog(
            show = true,
            title = "ActivityName",
            onDismissRequest = { showActivityPicker = false },
        ) {
            Column {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(activityInfos, key = { it.name }) { ai ->
                        MiuixListItem(
                            title = ai.name,
                            onClick = {
                                activity = ai.name
                                showActivityPicker = false
                            },
                        )
                    }
                }
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showActivityPicker = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showDeleteConfirm && initial != null) {
        OverlayDialog(
            show = true,
            summary = stringResource(
                R.string.confirm_to_delete_this_configuration,
                initial.activity,
            ),
            onDismissRequest = { showDeleteConfirm = false },
        ) {
            DialogButtons(
                dismissText = stringResource(android.R.string.cancel),
                confirmText = stringResource(android.R.string.ok),
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete(initial)
                },
            )
        }
    }
}
