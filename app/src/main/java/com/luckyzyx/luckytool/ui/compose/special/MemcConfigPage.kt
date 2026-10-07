package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.ArraySet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
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

private var memcRefresh: (suspend () -> Unit)? = null

private const val CONFIG_PACKAGE_LIST = GlobalKeyValue.memcConfigPackageList
private const val CONFIG_ACTIVITY_LIST = GlobalKeyValue.memcConfigActivityList

/**
 * P4 特殊页：Memc 帧插入配置（旧 MemcConfigFragment 1:1 迁移）。
 * 双 Tab（Packages/Activitys）+ 搜索 + 下拉刷新 + Import Xml / Reset。
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

@Composable
private fun MemcConfigContent() {
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
        Column(Modifier.padding(horizontal = 16.dp)) {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.import_) + " Xml")
                },
                modifier = Modifier.clickable {
                    FileUtils.checkDownloadDir(context, "LuckyTool")
                    importLauncher.launch("text/xml")
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.reset)) },
                modifier = Modifier.clickable { showResetConfirm = true },
            )
        }
        TabRow(selectedTabIndex = tabIndex) {
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

    if (showVersionSheet) {
        AlertDialog(
            onDismissRequest = { showVersionSheet = false },
            text = {
                Column {
                    listOf("x7", "x7p").forEach { v ->
                        ListItem(
                            headlineContent = { Text(v) },
                            modifier = Modifier.clickable {
                                showVersionSheet = false
                                scope.launch { state.reset(null, v) }
                            },
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

/** Memc 双 Tab 共享状态（存于内容层，Tab 切换不丢失）。 */
private class MemcConfigState(private val context: Context) {

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

@Composable
private fun MemcPackagePanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigPackage?) -> Unit,
) {
    Column(modifier) {
        OutlinedTextField(
            value = state.pkgQuery,
            onValueChange = { state.applyPkgQuery(it) },
            enabled = !state.pkgLoading,
            singleLine = true,
            placeholder = { Text("PackageName") },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        ListItem(
            headlineContent = { Text("＋ Add") },
            modifier = Modifier.clickable { onEdit(null) },
        )
        if (state.pkgFilter.isEmpty() && !state.pkgLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_memc_data))
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.pkgFilter, key = { "${it.packName}|${it.rate}|${it.type}" }) { info ->
                    ListItem(
                        headlineContent = { Text(info.packName) },
                        supportingContent = {
                            Column {
                                Text("Rate: ${info.rate}")
                                Text("Type: ${info.type}")
                            }
                        },
                        modifier = Modifier.clickable { onEdit(info) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MemcActivityPanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigActivity?) -> Unit,
) {
    Column(modifier) {
        OutlinedTextField(
            value = state.actQuery,
            onValueChange = { state.applyActQuery(it) },
            enabled = !state.actLoading,
            singleLine = true,
            placeholder = { Text("PackageName / ActivityName") },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        ListItem(
            headlineContent = { Text("＋ Add") },
            modifier = Modifier.clickable { onEdit(null) },
        )
        if (state.actFilter.isEmpty() && !state.actLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_memc_data))
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(
                    state.actFilter,
                    key = { "${it.packName}|${it.activity}|${it.type}" },
                ) { info ->
                    ListItem(
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
                        modifier = Modifier.clickable { onEdit(info) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MemcPackageDialog(
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

@Composable
private fun MemcActivityDialog(
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
        AlertDialog(
            onDismissRequest = { showActivityPicker = false },
            title = { Text("ActivityName") },
            text = {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(activityInfos, key = { it.name }) { ai ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    ai.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.clickable {
                                activity = ai.name
                                showActivityPicker = false
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

    if (showDeleteConfirm && initial != null) {
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
