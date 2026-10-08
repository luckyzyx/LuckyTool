package com.luckyzyx.luckytool.ui.compose.special

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.ArraySet
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.AppIntentInfo
import com.luckyzyx.luckytool.enums.IntentType
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.clearPrefs
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.removeKey
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.sendPrefsValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyItemScope
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchCard
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCheckboxItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
/* t21 三文件制：本文件 = HideAppIntentPage 的 Miuix 线渲染（0 处 material3 / MaterialTheme 引用）。
 * 状态与逻辑块自原文件 107-331 行逐字迁移（仅 `refresh` -> `hideAppIntentRefresh`），布局按 Miuix 件重写。 */
@Composable
internal fun LazyItemScope.HideAppIntentContentMiuix() {
            val context = LocalContext.current
            val pm = context.packageManager
            val packageUtils = remember { PackageUtils(pm) }
            val scope = rememberCoroutineScope()

            var isReverse by remember { mutableStateOf(false) }
            var sortMode by remember { mutableIntStateOf(0) }
            var showSystemApps by remember { mutableStateOf(true) }
            var intentFilter by remember { mutableStateOf(ArraySet<IntentType>()) }
            var query by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }

            var allAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
            var filterAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
            var allResolveInfoMap by remember { mutableStateOf(ArrayMap<AppIntentInfo, ResolveInfo>()) }
            var allIntentInfos by remember { mutableStateOf(ArrayList<AppIntentInfo>()) }
            var allEnabledInfos by remember { mutableStateOf(ArrayList<AppIntentInfo>()) }

            var showSortSheet by remember { mutableStateOf(false) }
            var showClearDialog by remember { mutableStateOf(false) }
            var selectDialog by remember { mutableStateOf<IntentDialogData?>(null) }

            fun applyQuery(q: String): ArrayList<AppInfo> =
                if (q.isBlank()) allAppInfos
                else ArrayList(
                    allAppInfos.filter {
                        it.name.contains(q) || it.packageName.lowercase().contains(q)
                    }
                )

            fun bump() {
                allIntentInfos = ArrayList(allIntentInfos)
                allEnabledInfos = ArrayList(allEnabledInfos)
                allAppInfos = ArrayList(allAppInfos)
                filterAppInfos = applyQuery(query)
            }

            fun saveAppIntentList(
                packName: String, list: ArrayList<AppIntentInfo>, vararg types: IntentType
            ) {
                val filte = IntentUtils.getIntentFilter(*types)
                val appIntents = ArrayList<AppIntentInfo>().apply {
                    context.getStringSet(IntentPrefs, packName, ArraySet()).forEach { js ->
                        val info = safeOfNull { Json.decodeFromString<AppIntentInfo>(js) }
                            ?: return@forEach
                        add(info)
                    }
                }
                appIntents.removeIf(filte)
                if (list.isNotEmpty()) appIntents.addAll(list)

                allEnabledInfos.removeIf { it.packName == packName }
                allEnabledInfos.addAll(appIntents)
                if (appIntents.isNotEmpty()) {
                    allIntentInfos.removeIf { !intentFilter.contains(it.type) }
                    allEnabledInfos.removeIf { !intentFilter.contains(it.type) }
                    allAppInfos.removeIf { info ->
                        !allIntentInfos.map { it.packName }.contains(info.packageName)
                    }
                }

                if (appIntents.isEmpty()) {
                    context.removeKey(IntentPrefs, packName)
                } else {
                    val list = appIntents.mapNotNull { safeOfNull { Json.encodeToString(it) } }
                    context.putStringSet(IntentPrefs, packName, list.toSet())
                }

                context.sendPrefsValue(
                    "android", "custom_config_app_intent_list_update_app_config", packName
                )
            }

            fun saveEnabledAppList(packName: String, list: ArrayList<AppIntentInfo>) {
                val enabledApps = context.getStringSet(IntentPrefs, HIDE_APP_INTENT_ENABLED_LIST_KEY, ArraySet())
                val intents = context.getStringSet(IntentPrefs, packName, ArraySet())
                val isAdd = list.isNotEmpty() || intents.isNotEmpty()
                val newList = ArraySet(enabledApps).apply {
                    remove(packName)
                    if (isAdd) add(packName)
                }
                context.putStringSet(IntentPrefs, HIDE_APP_INTENT_ENABLED_LIST_KEY, newList.toSet())
                context.sendPrefsValue(
                    "android", "custom_config_app_intent_list_update_apps",
                    Pair(packName, isAdd)
                )
            }

            fun selectAllInfos(vararg type: IntentType) {
                val filte = IntentUtils.getIntentFilter(*type)
                val allIntents = allIntentInfos.filter(filte)
                val enabledIntents = allEnabledInfos.filter(filte)
                val isAll = allIntents.size == enabledIntents.size
                allIntents.map { it.packName }.forEach { packName ->
                    if (isAll) {
                        saveAppIntentList(packName, arrayListOf(), *type)
                        saveEnabledAppList(packName, arrayListOf())
                    } else {
                        val intents = allIntents.filter { it.packName == packName }
                        saveAppIntentList(packName, ArrayList(intents), *type)
                        saveEnabledAppList(packName, ArrayList(intents))
                    }
                }
                bump()
            }

            fun openSelectDialog(
                packName: String,
                allIntent: List<AppIntentInfo>,
                enabled: List<AppIntentInfo>,
                types: Array<IntentType>,
            ) {
                selectDialog = IntentDialogData(packName, allIntent, enabled, types)
            }

            suspend fun reload() {
                loading = true
                query = ""
                withContext(Dispatchers.IO) {
                    val allIntentFilter = arrayMapOf(
                        IntentType.SINGLE_SHARE to Intent(Intent.ACTION_SEND),
                        IntentType.MULTI_SHARE to Intent(Intent.ACTION_SEND_MULTIPLE),
                        IntentType.PROCESS_TEXT to Intent(Intent.ACTION_PROCESS_TEXT),
                        IntentType.CONTENT to Intent().setDataAndType("content://".toUri(), "*/*"),
                        IntentType.FILE to Intent().setDataAndType("file://".toUri(), "*/*"),
                        IntentType.HTTP_LINK to Intent().setDataAndType("http://".toUri(), "*/*"),
                        IntentType.HTTPS_LINK to Intent().setDataAndType("https://".toUri(), "*/*"),
                    ).onEach {
                        if (it.value.action == null) it.value.setAction(Intent.ACTION_VIEW)
                        if (it.value.data == null) it.value.setType("*/*")
                        it.value.putExtra("result_origin_data", true)
                    }

                    if (intentFilter.isEmpty()) intentFilter.addAll(allIntentFilter.map { it.key })

                    val existIntentApps = ArraySet<String>()
                    val intentInfos = ArrayList<AppIntentInfo>()
                    val resolveMap = ArrayMap<AppIntentInfo, ResolveInfo>()
                    allIntentFilter.forEach { (type, intent) ->
                        packageUtils.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                            .onEach {
                                existIntentApps.add(it.activityInfo.packageName)
                                val info = AppIntentInfo(
                                    it.loadLabel(pm).toString(), it.activityInfo.packageName,
                                    intent.action!!, it.activityInfo.name, type
                                )
                                intentInfos.add(info)
                                resolveMap[info] = it
                            }
                    }

                    val enabledApps =
                        context.getStringSet(IntentPrefs, HIDE_APP_INTENT_ENABLED_LIST_KEY, ArraySet())

                    val sortList = ArrayList<AppInfo>()
                    val appInfos = ArrayList<AppInfo>()
                    val enabledInfos = ArrayList<AppIntentInfo>()
                    existIntentApps.forEach { packName ->
                        val info = packageUtils.getInstalledAppInfo(packName, 0)
                            ?: return@forEach

                        if (!showSystemApps && info.isSystem) {
                            intentInfos.removeIf { it.packName == packName }
                            return@forEach
                        }

                        if (enabledApps.contains(packName)) sortList.add(info)
                        else appInfos.add(info)

                        context.getStringSet(IntentPrefs, packName, ArraySet()).forEach { js ->
                            val i = safeOfNull { Json.decodeFromString<AppIntentInfo>(js) }
                                ?: return@forEach
                            val find = intentInfos.find {
                                it.action == i.action && it.type == i.type &&
                                    it.packName == packName && it.activity == i.activity
                            }
                            if (find != null) enabledInfos.add(find)
                        }
                    }
                    sortList.apply {
                        when (sortMode) {
                            0 -> sortBy { it.name }
                            1 -> sortBy { it.packageName }
                            2 -> sortBy { it.size }
                            3 -> sortBy { it.installTime }
                            4 -> sortBy { it.lastInstallTime }
                            5 -> sortBy { it.target }
                        }
                        if (isReverse) reverse()
                    }
                    appInfos.apply {
                        when (sortMode) {
                            0 -> sortBy { it.name }
                            1 -> sortBy { it.packageName }
                            2 -> sortBy { it.size }
                            3 -> sortBy { it.installTime }
                            4 -> sortBy { it.lastInstallTime }
                            5 -> sortBy { it.target }
                        }
                        if (isReverse) reverse()
                        addAll(0, sortList)
                    }

                    if (intentFilter.isNotEmpty()) {
                        intentInfos.removeIf { !intentFilter.contains(it.type) }
                        enabledInfos.removeIf { !intentFilter.contains(it.type) }
                        appInfos.removeIf { info ->
                            !intentInfos.map { it.packName }.contains(info.packageName)
                        }
                    }

                    allResolveInfoMap = resolveMap
                    allIntentInfos = intentInfos
                    allEnabledInfos = enabledInfos
                    allAppInfos = appInfos
                    filterAppInfos = appInfos
                }
                loading = false
            }

            hideAppIntentRefresh = { reload() }

            LaunchedEffect(Unit) {
                if (allAppInfos.isEmpty() || allIntentInfos.isEmpty()) reload()
            }
    Column(Modifier.fillParentMaxHeight()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrefSwitchCard(
                title = stringResource(R.string.custom_config_app_intent_list),
                checked = context.getBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, false),
                onCheckedChange = { v ->
                    context.putBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, v)
                    context.sendPrefsValue("android", HIDE_APP_INTENT_ENABLE_KEY, v)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    filterAppInfos = applyQuery(q)
                },
                enabled = !loading,
                singleLine = true,
                label = "Name / PackageName",
                useLabelAsPlaceholder = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_search_24),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_filter_list_24),
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            PrefGroup {
                item {
                    PrefRow(
                        title = stringResource(R.string.select_all_share_intent),
                        onClick = {
                            selectAllInfos(
                                IntentType.SINGLE_SHARE,
                                IntentType.MULTI_SHARE,
                            )
                        },
                    )
                }
                item {
                    PrefRow(
                        title = stringResource(R.string.select_all_text_intent),
                        onClick = { selectAllInfos(IntentType.PROCESS_TEXT) },
                    )
                }
                item {
                    PrefRow(
                        title = stringResource(R.string.select_all_open_intent),
                        onClick = {
                            selectAllInfos(IntentType.CONTENT, IntentType.FILE)
                        },
                    )
                }
                item {
                    PrefRow(
                        title = stringResource(R.string.select_all_browser_intent),
                        onClick = {
                            selectAllInfos(
                                IntentType.HTTP_LINK,
                                IntentType.HTTPS_LINK,
                            )
                        },
                    )
                }
            }
            PrefGroup {
                item {
                    PrefRow(
                        title = stringResource(R.string.clear_all_data),
                        onClick = { showClearDialog = true },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .scrollEndHaptic()
                .overScrollVertical(),
            contentPadding = PaddingValues(
                start = MiuixPrefDefaults.CardHorizontalInset,
                end = MiuixPrefDefaults.CardHorizontalInset,
                bottom = expressiveBottomInset(),
            ),
            verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
            overscrollEffect = null,
        ) {
            itemsIndexed(
                filterAppInfos,
                key = { _, info -> info.packageName },
            ) { index, info ->
                MiuixPrefItem(index = index, count = filterAppInfos.size) {
                    IntentAppRowMiuix(
                        info = info,
                        allIntentInfos = allIntentInfos,
                        allEnabledInfos = allEnabledInfos,
                        onOpenDialog = ::openSelectDialog,
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        OverlayDialog(
            show = true,
            onDismissRequest = { showClearDialog = false },
        ) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = { showClearDialog = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MiuixPrefDefaults.ItemGap))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = {
                        showClearDialog = false
                        context.clearPrefs(IntentPrefs)
                        scope.launch { reload() }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    selectDialog?.let { data ->
        IntentInfoSelectDialogMiuix(
            data = data,
            resolveMap = allResolveInfoMap.filterKeys { it.packName == data.packName },
            onDismiss = { selectDialog = null },
            onConfirm = { list ->
                selectDialog = null
                saveAppIntentList(data.packName, list, *data.types)
                saveEnabledAppList(data.packName, list)
                bump()
            },
        )
    }

    if (showSortSheet) {
        SortFilterSheet(
            reverse = isReverse,
            sortMode = sortMode,
            sortLabels = context.resources.getStringArray(R.array.sort_selector_chips).toList(),
            onReverseChange = {
                isReverse = !isReverse
                scope.launch { reload() }
            },
            onSortChange = { mode ->
                sortMode = mode
                scope.launch { reload() }
            },
            filterContent = {
                Column {
                    MiuixSwitchItem(
                        title = stringResource(R.string.appinfo_system_app),
                        checked = showSystemApps,
                        onCheckedChange = {
                            showSystemApps = !showSystemApps
                            scope.launch { reload() }
                        },
                    )
                    MiuixCheckboxItem(
                        title = stringResource(R.string.intent_share),
                        checked = intentFilter.contains(IntentType.SINGLE_SHARE),
                        onCheckedChange = {
                            val types = arrayOf(
                                IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE
                            )
                            if (intentFilter.contains(IntentType.SINGLE_SHARE)) {
                                intentFilter.removeAll(types)
                            } else {
                                intentFilter.addAll(types.toList())
                            }
                            scope.launch { reload() }
                        },
                    )
                    MiuixCheckboxItem(
                        title = stringResource(R.string.intent_text),
                        checked = intentFilter.contains(IntentType.PROCESS_TEXT),
                        onCheckedChange = {
                            val types = arrayOf(IntentType.PROCESS_TEXT)
                            if (intentFilter.contains(IntentType.PROCESS_TEXT)) {
                                intentFilter.removeAll(types)
                            } else {
                                intentFilter.addAll(types.toList())
                            }
                            scope.launch { reload() }
                        },
                    )
                    MiuixCheckboxItem(
                        title = stringResource(R.string.intent_open),
                        checked = intentFilter.contains(IntentType.CONTENT),
                        onCheckedChange = {
                            val types = arrayOf(IntentType.CONTENT, IntentType.FILE)
                            if (intentFilter.contains(IntentType.CONTENT)) {
                                intentFilter.removeAll(types)
                            } else {
                                intentFilter.addAll(types.toList())
                            }
                            scope.launch { reload() }
                        },
                    )
                    MiuixCheckboxItem(
                        title = stringResource(R.string.intent_browser),
                        checked = intentFilter.contains(IntentType.HTTP_LINK),
                        onCheckedChange = {
                            val types = arrayOf(
                                IntentType.HTTP_LINK, IntentType.HTTPS_LINK
                            )
                            if (intentFilter.contains(IntentType.HTTP_LINK)) {
                                intentFilter.removeAll(types)
                            } else {
                                intentFilter.addAll(types.toList())
                            }
                            scope.launch { reload() }
                        },
                    )
                }
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

/**
 * 应用行（Miuix 线）：`MiuixListItem` + 四个计数按钮。
 * 与 material 线 `IntentAppRow` 行为等价：4 组类型各自 (packName, 全部条目, 已启用条目, 类型组) 回调。
 */
@Composable
private fun IntentAppRowMiuix(
    info: AppInfo,
    allIntentInfos: List<AppIntentInfo>,
    allEnabledInfos: List<AppIntentInfo>,
    onOpenDialog: (String, List<AppIntentInfo>, List<AppIntentInfo>, Array<IntentType>) -> Unit,
) {
    val intentInfo = allIntentInfos.filter { it.packName == info.packageName }
    val enabledInfo = allEnabledInfos.filter { it.packName == info.packageName }

    Column {
        MiuixListItem(
            title = info.name,
            summary = info.packageName,
            startAction = {
                info.icon?.let { d ->
                    Image(
                        remember(info) { d.toBitmap().asImageBitmap() },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp),
                    )
                }
            },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            val shareTypes = arrayOf(IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE)
            val shareFilter = IntentUtils.getIntentFilter(*shareTypes)
            val shareAll = intentInfo.filter(shareFilter)
            val shareEnabled = enabledInfo.filter(shareFilter)
            IntentCountChipMiuix(shareAll, shareEnabled) {
                onOpenDialog(info.packageName, shareAll, shareEnabled, shareTypes)
            }
            val textTypes = arrayOf(IntentType.PROCESS_TEXT)
            val textFilter = IntentUtils.getIntentFilter(*textTypes)
            val textAll = intentInfo.filter(textFilter)
            val textEnabled = enabledInfo.filter(textFilter)
            IntentCountChipMiuix(textAll, textEnabled) {
                onOpenDialog(info.packageName, textAll, textEnabled, textTypes)
            }
            val openTypes = arrayOf(IntentType.CONTENT, IntentType.FILE)
            val openFilter = IntentUtils.getIntentFilter(*openTypes)
            val openAll = intentInfo.filter(openFilter)
            val openEnabled = enabledInfo.filter(openFilter)
            IntentCountChipMiuix(openAll, openEnabled) {
                onOpenDialog(info.packageName, openAll, openEnabled, openTypes)
            }
            val browserTypes = arrayOf(IntentType.HTTP_LINK, IntentType.HTTPS_LINK)
            val browserFilter = IntentUtils.getIntentFilter(*browserTypes)
            val browserAll = intentInfo.filter(browserFilter)
            val browserEnabled = enabledInfo.filter(browserFilter)
            IntentCountChipMiuix(browserAll, browserEnabled) {
                onOpenDialog(info.packageName, browserAll, browserEnabled, browserTypes)
            }
        }
    }
}

/** 行内计数按钮：text = 已启用/全部，无条目时不可点（对应 material 线的 SuggestionChip） */
@Composable
private fun RowScope.IntentCountChipMiuix(
    allIntent: List<AppIntentInfo>,
    enabled: List<AppIntentInfo>,
    onClick: () -> Unit,
) {
    TextButton(
        text = "${enabled.size}/${allIntent.size}",
        onClick = onClick,
        enabled = allIntent.isNotEmpty(),
        modifier = Modifier.weight(1f),
    )
}

/**
 * 多选 Intent 对话框（Miuix 线）：`OverlayDialog`（根 host）+ `MiuixCheckboxItem` 多选列表。
 * 与 material 线 `IntentInfoSelectDialog` 行为等价：已启用项置顶预选、按 label/activity 过滤、全选/清空、
 * confirm 回传 `ArrayList(selected)`（对话框内状态，取消不落盘）。
 * 允许差异：库行件无前置图标槽位，不显示 32dp 图标；无 maxLines/ellipsis（库行件按单行摘要渲染）。
 */
@Composable
private fun IntentInfoSelectDialogMiuix(
    data: IntentDialogData,
    resolveMap: Map<AppIntentInfo, ResolveInfo>,
    onDismiss: () -> Unit,
    onConfirm: (ArrayList<AppIntentInfo>) -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var query by remember(data) { mutableStateOf("") }
    val selected = remember(data) {
        mutableStateListOf<AppIntentInfo>().apply { addAll(data.enabled) }
    }
    val ordered = remember(data) {
        ArrayList<AppIntentInfo>().apply {
            addAll(data.appInfos.filter { it !in data.enabled })
            addAll(0, data.appInfos.filter { it in data.enabled })
        }
    }
    val filtered = if (query.isBlank()) {
        ordered
    } else {
        ordered.filter { info ->
            val ri = resolveMap[info]
            ri != null && (
                ri.loadLabel(pm).contains(query) ||
                    ri.activityInfo.name.lowercase().contains(query)
                )
        }
    }

    OverlayDialog(
        show = true,
        title = stringResource(R.string.custom_config_app_intent_list),
        onDismissRequest = onDismiss,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    text = stringResource(android.R.string.selectAll),
                    onClick = {
                        if (selected.size == ordered.size) {
                            selected.clear()
                        } else {
                            selected.clear()
                            selected.addAll(ordered)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                MiuixText("${selected.size}/${ordered.size}", style = MiuixTheme.textStyles.body2)
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = "ActivityName",
                useLabelAsPlaceholder = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
                overscrollEffect = null,
            ) {
                items(filtered, key = { "${it.action}|${it.type}|${it.activity}" }) { info ->
                    MiuixCheckboxItem(
                        title = "${resolveMap[info]?.loadLabel(pm) ?: info.name} " +
                            intentTypeLabel(info.type),
                        checked = selected.contains(info),
                        onCheckedChange = { v ->
                            if (v) {
                                if (!selected.contains(info)) selected.add(info)
                            } else {
                                selected.remove(info)
                            }
                        },
                        summary = info.activity,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                TextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MiuixPrefDefaults.ItemGap))
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = { onConfirm(ArrayList(selected)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
