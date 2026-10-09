package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.ArraySet
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.GlobalKeyValue
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal var memcRefresh: (suspend () -> Unit)? = null
private const val CONFIG_PACKAGE_LIST = GlobalKeyValue.memcConfigPackageList
private const val CONFIG_ACTIVITY_LIST = GlobalKeyValue.memcConfigActivityList

/**
 * P4 特殊页：Memc 帧插入配置（旧 MemcConfigFragment 1:1 迁移）。
 * 双 Tab（Packages/Activitys）+ 搜索 + 下拉刷新 + Import Xml / Reset。
 *
 * 原「三文件制」已合并为单文件：状态/写值逻辑唯一一份 [MemcConfigState]，
 * 渲染差异集中在 [MemcConfigContent] 的 `if (miuix)` 分派上——顶部 Import/Reset
 * 卡片组复用 prefCard 的 [PrefGroup]+[PrefRow]、双面板与四个弹层按线分派。
 */
object MemcConfigPage {

    val spec = ScopePageSpec(
        pageKey = "memc_config",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { memcRefresh?.invoke() },
        fullContent = { _ -> MemcConfigContent() },
    ) { }
}

/** Memc 双 Tab 共享状态（存于内容层，Tab 切换不丢失）。 */
internal class MemcConfigState(private val context: Context) {

    var pkgAll by mutableStateOf(ArrayList<MemcConfigPackage>())
    var pkgFilter by mutableStateOf(ArrayList<MemcConfigPackage>())
    var pkgQuery by mutableStateOf("")
    var pkgLoading by mutableStateOf(false)

    var actAll by mutableStateOf(ArrayList<MemcConfigActivity>())
    var actFilter by mutableStateOf(ArrayList<MemcConfigActivity>())
    var actQuery by mutableStateOf("")
    var actLoading by mutableStateOf(false)

    fun applyPkgQuery(q: String) {
        pkgQuery = q
        pkgFilter = if (q.isBlank()) pkgAll
        else ArrayList(pkgAll.filter { it.packName.lowercase().contains(q.lowercase()) })
    }

    fun applyActQuery(q: String) {
        actQuery = q
        actFilter = if (q.isBlank()) actAll
        else ArrayList(
            actAll.filter {
                it.packName.lowercase().contains(q.lowercase()) ||
                        it.activity.lowercase().contains(q.lowercase())
            }
        )
    }

    suspend fun reload() {
        pkgLoading = true
        actLoading = true
        pkgQuery = ""
        actQuery = ""
        withContext(Dispatchers.IO) {
            var packages = decodePackages(
                context.getStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, ArraySet())
            )
            var activities = decodeActivities(
                context.getStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, ArraySet())
            )
            if (packages.isEmpty() || activities.isEmpty()) {
                resetFromStream(null, "")
                packages = decodePackages(
                    context.getStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, ArraySet())
                )
                activities = decodeActivities(
                    context.getStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, ArraySet())
                )
            }
            pkgAll = packages
            actAll = activities
            applyPkgQuery("")
            applyActQuery("")
        }
        pkgLoading = false
        actLoading = false
    }

    suspend fun reset(inputStream: InputStream?, version: String) {
        withContext(Dispatchers.IO) {
            resetFromStream(inputStream, version)
        }
        reload()
    }

    fun savePackage(old: MemcConfigPackage?, new: MemcConfigPackage) {
        val index = old?.let { pkgAll.indexOf(it) } ?: -1
        if (index != -1) pkgAll[index] = new else pkgAll.add(new)
        persistPackages()
    }

    fun deletePackage(info: MemcConfigPackage) {
        pkgAll.remove(info)
        persistPackages()
    }

    fun saveActivity(old: MemcConfigActivity?, new: MemcConfigActivity) {
        val index = old?.let { actAll.indexOf(it) } ?: -1
        if (index != -1) actAll[index] = new else actAll.add(new)
        persistActivities()
    }

    fun deleteActivity(info: MemcConfigActivity) {
        actAll.remove(info)
        persistActivities()
    }

    private fun decodePackages(raw: Set<String>): ArrayList<MemcConfigPackage> =
        ArrayList(raw.mapNotNull { safeOfNull { Json.decodeFromString<MemcConfigPackage>(it) } })

    private fun decodeActivities(raw: Set<String>): ArrayList<MemcConfigActivity> =
        ArrayList(raw.mapNotNull { safeOfNull { Json.decodeFromString<MemcConfigActivity>(it) } })

    private fun persistPackages() {
        val set = pkgAll.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        context.putStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, set)
        applyPkgQuery(pkgQuery)
    }

    private fun persistActivities() {
        val set = actAll.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        context.putStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, set)
        applyActQuery(actQuery)
    }

    private fun resetFromStream(inputStream: InputStream?, version: String) {
        val packages = ArrayList<MemcConfigPackage>()
        val activities = ArrayList<MemcConfigActivity>()
        val stream = inputStream ?: safeOfNull {
            context.resources.openRawResource(R.raw.multimedia_pixelworks_apps_x7)
        } ?: return
        FileUtils.parseMemcXml(stream, packages, activities)
        if (version == "x7p") {
            activities.forEachIndexed { index, config ->
                activities[index] =
                    MemcConfigActivity(config.packName, config.activity, "258-10-0-0")
            }
        }
        val packageSet =
            packages.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        val activitySet =
            activities.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        if (packageSet.isNotEmpty() && activitySet.isNotEmpty()) {
            context.putStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, packageSet)
            context.putStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, activitySet)
        }
    }
}

/** 页面渲染体：状态/加载逻辑唯一一份（两线旧实现逐字一致），渲染差异集中在此。 */
@Composable
internal fun MemcConfigContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { MemcConfigState(context) }
    memcRefresh = { state.reload() }
    val miuix = LocalUiMode.current == UiMode.Miuix

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
                horizontal = if (miuix) MiuixPrefDefaults.CardHorizontalInset else 16.dp,
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
        if (miuix) {
            MiuixTabRow(
                tabs = listOf("Packages", "Activitys"),
                selectedTabIndex = tabIndex,
                onTabSelected = { tabIndex = it },
                modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
            )
        } else {
            SecondaryTabRow(
                tabIndex,
                Modifier,
                TabRowDefaults.primaryContainerColor,
                TabRowDefaults.primaryContentColor,
                @Composable {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabIndex)
                    )
                },
                @Composable { HorizontalDivider() },
                {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Packages") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Activitys") },
                    )
                })
        }
        when (tabIndex) {
            0 -> MemcPackagePanel(
                state = state,
                modifier = Modifier.weight(1f),
                onEdit = { target ->
                    pkgTarget = target
                    showPkgDialog = true
                },
            )

            1 -> MemcActivityPanel(
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
        MemcPackageDialog(
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
        MemcActivityDialog(
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
        if (miuix) {
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
        } else {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                text = { Text(stringResource(R.string.restore_frame_insertion_configuration_data)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            showVersionSheet = true
                        },
                    ) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }

    if (showVersionSheet) {
        if (miuix) {
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
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showVersionSheet = false },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = { showVersionSheet = false },
                text = {
                    Column {
                        listOf("x7", "x7p").forEach { v ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    showVersionSheet = false
                                    scope.launch { state.reset(null, v) }
                                },
                                content = { Text(v) },
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVersionSheet = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
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
        MiuixTextButton(
            text = dismissText,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
        MiuixTextButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 面板搜索框：仅搜索（无排序按钮），Miuix=TextField / Material=OutlinedTextField。 */
@Composable
private fun MemcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextField(
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
                MiuixIcon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
            },
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
internal fun MemcPackagePanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigPackage?) -> Unit,
) {
    val miuix = LocalUiMode.current == UiMode.Miuix
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
        if (miuix) {
            MiuixPrefItem(
                index = 0,
                count = 1,
                modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
            ) {
                MiuixListItem(title = "＋ Add", onClick = { onEdit(null) })
            }
        } else {
            PrefCard(
                title = "＋ Add",
                onClick = { onEdit(null) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.pkgFilter.isEmpty() && !state.pkgLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (miuix) MiuixText(stringResource(R.string.no_memc_data))
                else Text(stringResource(R.string.no_memc_data))
            }
        } else {
            if (miuix) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = MiuixPrefDefaults.CardHorizontalInset,
                        end = MiuixPrefDefaults.CardHorizontalInset,
                        bottom = expressiveBottomInset(),
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
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
                ) {
                    itemsIndexed(
                        state.pkgFilter,
                        key = { _, info -> "${info.packName}|${info.rate}|${info.type}" },
                    ) { index, info ->
                        SegmentedItem(index = index, count = state.pkgFilter.size) {
                            SegmentedListItem(
                                onClick = { onEdit(info) },
                                headlineContent = { Text(info.packName) },
                                supportingContent = {
                                    Column {
                                        Text("Rate: ${info.rate}")
                                        Text("Type: ${info.type}")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MemcActivityPanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigActivity?) -> Unit,
) {
    val miuix = LocalUiMode.current == UiMode.Miuix
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
        if (miuix) {
            MiuixPrefItem(
                index = 0,
                count = 1,
                modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
            ) {
                MiuixListItem(title = "＋ Add", onClick = { onEdit(null) })
            }
        } else {
            PrefCard(
                title = "＋ Add",
                onClick = { onEdit(null) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (state.actFilter.isEmpty() && !state.actLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (miuix) MiuixText(stringResource(R.string.no_memc_data))
                else Text(stringResource(R.string.no_memc_data))
            }
        } else {
            if (miuix) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = MiuixPrefDefaults.CardHorizontalInset,
                        end = MiuixPrefDefaults.CardHorizontalInset,
                        bottom = expressiveBottomInset(),
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
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
                ) {
                    itemsIndexed(
                        state.actFilter,
                        key = { _, info -> "${info.packName}|${info.activity}|${info.type}" },
                    ) { index, info ->
                        SegmentedItem(index = index, count = state.actFilter.size) {
                            SegmentedListItem(
                                onClick = { onEdit(info) },
                                headlineContent = { Text(info.packName) },
                                supportingContent = {
                                    Column {
                                        Text(
                                            info.activity,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text("Type: ${info.type}")
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MemcPackageDialog(
    context: Context,
    initial: MemcConfigPackage?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigPackage) -> Unit,
    onDelete: (MemcConfigPackage) -> Unit,
) {
    val miuix = LocalUiMode.current == UiMode.Miuix
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var rate by remember(initial) { mutableStateOf(initial?.rate ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (miuix) {
        OverlayDialog(
            show = true,
            onDismissRequest = onDismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixTextField(
                    value = packName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = "PackageName",
                    trailingIcon = {
                        MiuixIcon(
                            painterResource(R.drawable.ic_baseline_extension_24),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppPicker = true },
                )
                MiuixTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    singleLine = true,
                    label = "ScreenRate",
                    modifier = Modifier.fillMaxWidth(),
                )
                MiuixTextField(
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
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column {
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { packName = it },
                        readOnly = true,
                        singleLine = true,
                        label = { Text("PackageName") },
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
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it },
                        singleLine = true,
                        label = { Text("ScreenRate") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        singleLine = true,
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(
                            R.string.edit_memc_configuration_tips,
                            CommandUtils.memcHdrConfigHelp,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (packName.isBlank() || rate.isBlank() || type.isBlank()) {
                            context.showToast("Data is incomplete!")
                        } else {
                            onSave(MemcConfigPackage(packName, rate, type))
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                if (initial != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(stringResource(R.string.remove))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
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
        if (miuix) {
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
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                text = {
                    Text(
                        stringResource(
                            R.string.confirm_to_delete_this_configuration,
                            initial.packName,
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(initial)
                        },
                    ) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
internal fun MemcActivityDialog(
    context: Context,
    initial: MemcConfigActivity?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigActivity) -> Unit,
    onDelete: (MemcConfigActivity) -> Unit,
) {
    val miuix = LocalUiMode.current == UiMode.Miuix
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var activity by remember(initial) { mutableStateOf(initial?.activity ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }
    var activityInfos by remember { mutableStateOf(emptyList<ActivityInfo>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (miuix) {
        OverlayDialog(
            show = true,
            onDismissRequest = onDismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MiuixTextField(
                    value = packName,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = "PackageName",
                    trailingIcon = {
                        MiuixIcon(
                            painterResource(R.drawable.ic_baseline_extension_24),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppPicker = true },
                )
                MiuixTextField(
                    value = activity,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = "ActivityName",
                    trailingIcon = {
                        MiuixIcon(
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
                MiuixTextField(
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
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = {
                Column {
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { packName = it },
                        readOnly = true,
                        singleLine = true,
                        label = { Text("PackageName") },
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
                    OutlinedTextField(
                        value = activity,
                        onValueChange = { activity = it },
                        readOnly = true,
                        singleLine = true,
                        label = { Text("ActivityName") },
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
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        singleLine = true,
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(
                            R.string.edit_memc_configuration_tips,
                            CommandUtils.memcConfigHelp,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (packName.isBlank() || activity.isBlank() || type.isBlank()) {
                            context.showToast("Data is incomplete!")
                        } else {
                            onSave(MemcConfigActivity(packName, activity, type))
                        }
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                if (initial != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(stringResource(R.string.remove))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            },
        )
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
        if (miuix) {
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
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showActivityPicker = false },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = { showActivityPicker = false },
                title = { Text("ActivityName") },
                text = {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(activityInfos, key = { it.name }) { ai ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    activity = ai.name
                                    showActivityPicker = false
                                },
                                content = {
                                    Text(
                                        ai.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActivityPicker = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }

    if (showDeleteConfirm && initial != null) {
        if (miuix) {
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
        } else {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                text = {
                    Text(
                        stringResource(
                            R.string.confirm_to_delete_this_configuration,
                            initial.activity,
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(initial)
                        },
                    ) { Text(stringResource(android.R.string.ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )
        }
    }
}
