@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItemContainer
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCheckboxItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
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
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * P4 特殊页：隐藏应用 Intent（旧 HideAppIntentFragment 1:1 迁移）。
 * 使用 ScopeScreen 的 fullContent 全屏自定义渲染 + onRefresh 下拉刷新。
 *
 * 原「三文件制」已合并为单文件：状态/加载/写值逻辑唯一一份，渲染差异集中在
 * [HideAppIntentContent] 的 `if (miuix)` 分派上——开关与批量操作合并为一张卡（[PrefGroup] 内
 * [PrefSwitchRow]+[PrefRow]）、搜索框复用 [AppSearchField]、
 * 应用行 [IntentAppRow] 与多选对话框 [IntentInfoSelectDialog] 按线分派。
 */
object HideAppIntentPage {

    val spec = ScopePageSpec(
        pageKey = "hide_app_intent",
        prefsName = IntentPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { hideAppIntentRefresh?.invoke() },
        fullContent = { _ -> HideAppIntentContent() },
    ) { }
}

/** 多选对话框数据：packName + 类型组内全部条目 + 已启用预选 + 目标类型组 */
internal data class IntentDialogData(
    val packName: String,
    val appInfos: List<AppIntentInfo>,
    val enabled: List<AppIntentInfo>,
    val types: Array<IntentType>,
)

internal const val HIDE_APP_INTENT_ENABLE_KEY = "custom_config_app_intent_list"
internal const val HIDE_APP_INTENT_ENABLED_LIST_KEY = "enable_app_hide_list"

internal var hideAppIntentRefresh: (suspend () -> Unit)? = null

@Composable
internal fun intentTypeLabel(type: IntentType): String = stringResource(
    when (type) {
        IntentType.SINGLE_SHARE -> R.string.intent_single_share
        IntentType.MULTI_SHARE -> R.string.intent_multi_share
        IntentType.PROCESS_TEXT -> R.string.intent_long_press_text
        IntentType.CONTENT -> R.string.intent_open_content
        IntentType.FILE -> R.string.intent_open_file
        IntentType.HTTP_LINK -> R.string.intent_http_link
        IntentType.HTTPS_LINK -> R.string.intent_https_link
        IntentType.UNKNOWN -> R.string.default_
    }
)

/**
 * 页面渲染体：状态/加载逻辑唯一一份（两线旧实现逐字一致），渲染差异集中在此：
 * 顶部开关 + 批量操作合并为一张卡（[PrefGroup] 内 [PrefSwitchRow]+[PrefRow]）、搜索框 [AppSearchField]，
 * 列表容器与行、清空对话框、多选对话框、排序过滤弹层均按 `if (miuix)` 分派。
 */
@Composable
internal fun LazyItemScope.HideAppIntentContent() {
    val context = LocalContext.current
    val pm = context.packageManager
    val packageUtils = remember { PackageUtils(pm) }
    val scope = rememberCoroutineScope()
    val miuix = LocalUiMode.current == UiMode.Miuix

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
            PrefGroup {
                item {
                    PrefSwitchRow(
                        title = stringResource(R.string.custom_config_app_intent_list),
                        checked = context.getBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, false),
                        onCheckedChange = { v ->
                            context.putBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, v)
                            context.sendPrefsValue("android", HIDE_APP_INTENT_ENABLE_KEY, v)
                        },
                    )
                }
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
                item {
                    PrefRow(
                        title = stringResource(R.string.clear_all_data),
                        onClick = { showClearDialog = true },
                    )
                }
            }
            AppSearchField(
                query = query,
                enabled = !loading,
                onQueryChange = { q ->
                    query = q
                    filterAppInfos = applyQuery(q)
                },
                onSortClick = { showSortSheet = true },
            )
        }
        if (miuix) {
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
                        IntentAppRow(
                            info = info,
                            allIntentInfos = allIntentInfos,
                            allEnabledInfos = allEnabledInfos,
                            onOpenDialog = ::openSelectDialog,
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
                    filterAppInfos,
                    key = { _, info -> info.packageName },
                ) { index, info ->
                    SegmentedItem(index = index, count = filterAppInfos.size) {
                        IntentAppRow(
                            info = info,
                            allIntentInfos = allIntentInfos,
                            allEnabledInfos = allEnabledInfos,
                            onOpenDialog = ::openSelectDialog,
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        if (miuix) {
            OverlayDialog(
                show = true,
                onDismissRequest = { showClearDialog = false },
            ) {
                Row(Modifier.fillMaxWidth()) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showClearDialog = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    MiuixTextButton(
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
        } else {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                        context.clearPrefs(IntentPrefs)
                        scope.launch { reload() }
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

    selectDialog?.let { data ->
        IntentInfoSelectDialog(
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
            sortLabels =
                context.resources.getStringArray(R.array.sort_selector_chips).toList(),
            onReverseChange = {
                isReverse = !isReverse
                scope.launch { reload() }
            },
            onSortChange = { mode ->
                sortMode = mode
                scope.launch { reload() }
            },
            filterContent = {
                if (miuix) {
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
                } else {
                    FlowRow {
                        FilterChip(
                            selected = showSystemApps,
                            onClick = {
                                showSystemApps = !showSystemApps
                                scope.launch { reload() }
                            },
                            label = { Text(stringResource(R.string.appinfo_system_app)) },
                        )
                        FilterChip(
                            selected = intentFilter.contains(IntentType.SINGLE_SHARE),
                            onClick = {
                                val types = arrayOf(
                                    IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE
                                )
                                if (intentFilter.contains(IntentType.SINGLE_SHARE)) {
                                    intentFilter.removeAll(types)
                                } else {
                                    intentFilter.addAll(types)
                                }
                                scope.launch { reload() }
                            },
                            label = { Text(stringResource(R.string.intent_share)) },
                        )
                        FilterChip(
                            selected = intentFilter.contains(IntentType.PROCESS_TEXT),
                            onClick = {
                                val types = arrayOf(IntentType.PROCESS_TEXT)
                                if (intentFilter.contains(IntentType.PROCESS_TEXT)) {
                                    intentFilter.removeAll(types)
                                } else {
                                    intentFilter.addAll(types)
                                }
                                scope.launch { reload() }
                            },
                            label = { Text(stringResource(R.string.intent_text)) },
                        )
                        FilterChip(
                            selected = intentFilter.contains(IntentType.CONTENT),
                            onClick = {
                                val types = arrayOf(IntentType.CONTENT, IntentType.FILE)
                                if (intentFilter.contains(IntentType.CONTENT)) {
                                    intentFilter.removeAll(types)
                                } else {
                                    intentFilter.addAll(types)
                                }
                                scope.launch { reload() }
                            },
                            label = { Text(stringResource(R.string.intent_open)) },
                        )
                        FilterChip(
                            selected = intentFilter.contains(IntentType.HTTP_LINK),
                            onClick = {
                                val types = arrayOf(
                                    IntentType.HTTP_LINK, IntentType.HTTPS_LINK
                                )
                                if (intentFilter.contains(IntentType.HTTP_LINK)) {
                                    intentFilter.removeAll(types)
                                } else {
                                    intentFilter.addAll(types)
                                }
                                scope.launch { reload() }
                            },
                            label = { Text(stringResource(R.string.intent_browser)) },
                        )
                    }
                }
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

/**
 * 应用行：图标 + 应用名 + 包名 + 四组类型计数按钮。
 * 与 material/miuix 两线旧实现行为等价：每组类型 (share/text/open/browser) 各自回调
 * `(packName, 全部条目, 已启用条目, 类型组)`；容器分派在调用点（`MiuixPrefItem`/`SegmentedItem`），
 * 行内 ListItem 与计数按钮在此按线分派。
 */
@Composable
internal fun IntentAppRow(
    info: AppInfo,
    allIntentInfos: List<AppIntentInfo>,
    allEnabledInfos: List<AppIntentInfo>,
    onOpenDialog: (String, List<AppIntentInfo>, List<AppIntentInfo>, Array<IntentType>) -> Unit,
) {
    val intentInfo = allIntentInfos.filter { it.packName == info.packageName }
    val enabledInfo = allEnabledInfos.filter { it.packName == info.packageName }

    if (LocalUiMode.current == UiMode.Miuix) {
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
                IntentTypeChips(intentInfo, enabledInfo, onOpenDialog, info.packageName)
            }
        }
    } else {
        SegmentedItemContainer {
            Column {
                SegmentedListItem(
                    headlineContent = {
                        Text(
                            info.name,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            info.packageName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
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
                    IntentTypeChips(intentInfo, enabledInfo, onOpenDialog, info.packageName)
                }
            }
        }
    }
}

/** 行内四组类型计数按钮（share/text/open/browser），逻辑两线一致 */
@Composable
private fun RowScope.IntentTypeChips(
    intentInfo: List<AppIntentInfo>,
    enabledInfo: List<AppIntentInfo>,
    onOpenDialog: (String, List<AppIntentInfo>, List<AppIntentInfo>, Array<IntentType>) -> Unit,
    packName: String,
) {
    val shareTypes = arrayOf(IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE)
    val shareFilter = IntentUtils.getIntentFilter(*shareTypes)
    val shareAll = intentInfo.filter(shareFilter)
    val shareEnabled = enabledInfo.filter(shareFilter)
    IntentCountChip(shareAll, shareEnabled) {
        onOpenDialog(packName, shareAll, shareEnabled, shareTypes)
    }
    val textTypes = arrayOf(IntentType.PROCESS_TEXT)
    val textFilter = IntentUtils.getIntentFilter(*textTypes)
    val textAll = intentInfo.filter(textFilter)
    val textEnabled = enabledInfo.filter(textFilter)
    IntentCountChip(textAll, textEnabled) {
        onOpenDialog(packName, textAll, textEnabled, textTypes)
    }
    val openTypes = arrayOf(IntentType.CONTENT, IntentType.FILE)
    val openFilter = IntentUtils.getIntentFilter(*openTypes)
    val openAll = intentInfo.filter(openFilter)
    val openEnabled = enabledInfo.filter(openFilter)
    IntentCountChip(openAll, openEnabled) {
        onOpenDialog(packName, openAll, openEnabled, openTypes)
    }
    val browserTypes = arrayOf(IntentType.HTTP_LINK, IntentType.HTTPS_LINK)
    val browserFilter = IntentUtils.getIntentFilter(*browserTypes)
    val browserAll = intentInfo.filter(browserFilter)
    val browserEnabled = enabledInfo.filter(browserFilter)
    IntentCountChip(browserAll, browserEnabled) {
        onOpenDialog(packName, browserAll, browserEnabled, browserTypes)
    }
}

/** 行内计数按钮：text = 已启用/全部，无条目时不可点（Material=SuggestionChip / Miuix=TextButton） */
@Composable
private fun RowScope.IntentCountChip(
    allIntent: List<AppIntentInfo>,
    enabled: List<AppIntentInfo>,
    onClick: () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextButton(
            text = "${enabled.size}/${allIntent.size}",
            onClick = onClick,
            enabled = allIntent.isNotEmpty(),
            modifier = Modifier.weight(1f),
        )
    } else {
        SuggestionChip(
            onClick = onClick,
            enabled = allIntent.isNotEmpty(),
            label = {
                Text(
                    "${enabled.size}/${allIntent.size}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 多选 Intent 对话框：已启用项置顶预选、按 label/activity 过滤、全选/清空、
 * confirm 回传 `ArrayList(selected)`（对话框内状态，取消不落盘）。
 * Material 线 `AlertDialog` 列表行带 32dp 图标 + Checkbox；Miuix 线 `OverlayDialog` 用
 * `MiuixCheckboxItem` 多选（库行件无前置图标槽位、无 maxLines/ellipsis，属允许差异）。
 */
@Composable
internal fun IntentInfoSelectDialog(
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

    if (LocalUiMode.current == UiMode.Miuix) {
        OverlayDialog(
            show = true,
            title = stringResource(R.string.custom_config_app_intent_list),
            onDismissRequest = onDismiss,
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiuixTextButton(
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
                    MiuixText(
                        "${selected.size}/${ordered.size}",
                        style = MiuixTheme.textStyles.body2,
                    )
                }
                MiuixTextField(
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
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    MiuixTextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = { onConfirm(ArrayList(selected)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { onConfirm(ArrayList(selected)) }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                if (selected.size == ordered.size) {
                                    selected.clear()
                                } else {
                                    selected.clear()
                                    selected.addAll(ordered)
                                }
                            },
                        ) { Text(stringResource(android.R.string.selectAll)) }
                        Spacer(Modifier.weight(1f))
                        Text("${selected.size}/${ordered.size}")
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("ActivityName") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                        items(filtered, key = { "${it.action}|${it.type}|${it.activity}" }) { info ->
                            val ri = resolveMap[info]
                            val checked = selected.contains(info)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selected.remove(info)
                                        else selected.add(info)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(40.dp)) {
                                    ri?.loadIcon(pm)?.let { d ->
                                        Image(
                                            remember(info) { d.toBitmap().asImageBitmap() },
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${ri?.loadLabel(pm) ?: info.name} ${intentTypeLabel(info.type)}",
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        info.activity,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { v ->
                                        if (v) {
                                            if (!selected.contains(info)) selected.add(info)
                                        } else {
                                            selected.remove(info)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
