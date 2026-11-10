@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.luckyzyx.luckytool.ui.compose.special

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.ArraySet
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.AppIntentInfo
import com.luckyzyx.luckytool.enums.IntentType
import com.luckyzyx.luckytool.ui.compose.components.AppSearchBar
import com.luckyzyx.luckytool.ui.compose.components.AppSearchPlaceholder
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.MultiChoiceDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilterChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiSuggestionChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextField
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageAction
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.DesignTokens
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

/**
 * P4 特殊页：隐藏应用 Intent。
 * 使用 ScopeScreen 的 fullContent 全屏自定义渲染 + onRefresh 下拉刷新。
 *
 * 单文件承载：状态/加载/写值逻辑只有一份，渲染集中在 [HideAppIntentContent]——开关独占一张卡
 * （[PrefGroup] 内 [PrefSwitchRow]）、搜索框复用 [AppSearchField]、
 * 应用行 [IntentAppRow] 与多选对话框 [IntentInfoSelectDialog]。
 * 批量操作（按 Intent 类型分别全选、清空全部）声明为 [ScopePageAction] 进顶栏溢出菜单：
 * 它们作用于整份数据而非某一项，放进正文选项行会与单个开关混排。
 */
object HideAppIntentPage {

    /** 顶栏溢出菜单动作 key，页面在 `actionHandler` 里据此分派整页批量操作。 */
    const val ACTION_SELECT_SHARE = "select_all_share_intent"
    const val ACTION_SELECT_TEXT = "select_all_text_intent"
    const val ACTION_SELECT_OPEN = "select_all_open_intent"
    const val ACTION_SELECT_BROWSER = "select_all_browser_intent"
    const val ACTION_CLEAR_ALL = "clear_all_data"

    val spec = ScopePageSpec(
        pageKey = "hide_app_intent",
        prefsName = IntentPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        // 特殊页：整页由 fullContent 自渲染，入口可见性只看 isVisible + 作用域
        selfRendered = true,
        onRefresh = { hideAppIntentRefresh?.invoke() },
        fullContent = { _ -> HideAppIntentContent() },
        // 这 5 项都不设图标、不显示为顶栏按钮，只出现在溢出菜单里
        topActions = listOf(
            ScopePageAction(ACTION_SELECT_SHARE, R.string.select_all_share_intent),
            ScopePageAction(ACTION_SELECT_TEXT, R.string.select_all_text_intent),
            ScopePageAction(ACTION_SELECT_OPEN, R.string.select_all_open_intent),
            ScopePageAction(ACTION_SELECT_BROWSER, R.string.select_all_browser_intent),
            ScopePageAction(ACTION_CLEAR_ALL, R.string.clear_all_data),
        ),
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
 * 页面渲染体：状态/加载逻辑唯一一份，渲染集中在此：
 * 顶部开关独占一张卡（[PrefGroup] 内 [PrefSwitchRow]，批量操作已移回顶栏溢出菜单）、
 * 搜索框 [AppSearchField]，列表容器与行、清空对话框、多选对话框、排序过滤弹层。
 */
@Composable
internal fun LazyItemScope.HideAppIntentContent() {
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
        // all* 已在写入点整体换成新集合（见 [saveAppIntentList]），这里只需按搜索词重算列表，
        // 不能再靠「赋一个内容相等的副本」来触发重组：MutableState 的结构相等策略会丢弃该写入。
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

        // 先拷贝再改、最后整体赋值：就地改状态当前持有的集合后赋一个内容相等的副本，
        // MutableState 会判定「值没变」而丢弃写入，表现为点了没反应、重开页面才刷新。
        val nextEnabled = ArrayList(allEnabledInfos)
        nextEnabled.removeIf { it.packName == packName }
        nextEnabled.addAll(appIntents)
        var nextIntents = allIntentInfos
        var nextAppInfos = allAppInfos
        if (appIntents.isNotEmpty()) {
            nextIntents = ArrayList(allIntentInfos).apply {
                removeIf { !intentFilter.contains(it.type) }
            }
            nextEnabled.removeIf { !intentFilter.contains(it.type) }
            nextAppInfos = ArrayList(allAppInfos).apply { removeIf { info ->
                !nextIntents.map { it.packName }.contains(info.packageName)
            } }
        }
        allEnabledInfos = nextEnabled
        allIntentInfos = nextIntents
        allAppInfos = nextAppInfos

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

            if (intentFilter.isEmpty()) intentFilter = ArraySet(allIntentFilter.map { it.key })

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

    /**
     * 切换类型筛选。与页面其余状态写入一致：先拷贝再整体赋值，
     * 就地改 `intentFilter` 会被 MutableState 的结构相等策略判为「值没变」，筛选片点了不刷新。
     */
    fun setFilterTypes(types: Array<IntentType>, enabled: Boolean) {
        val next = ArraySet(intentFilter)
        if (enabled) next.addAll(types.toList()) else next.removeAll(types.toList())
        intentFilter = next
        scope.launch { reload() }
    }

    hideAppIntentRefresh = { reload() }

    // 批量操作在顶栏溢出菜单里触发（见 [HideAppIntentPage.topActions]），事件期回调到页面状态
    HideAppIntentPage.spec.actionHandler = { key ->
        when (key) {
            HideAppIntentPage.ACTION_SELECT_SHARE -> {
                selectAllInfos(IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE)
            }

            HideAppIntentPage.ACTION_SELECT_TEXT -> selectAllInfos(IntentType.PROCESS_TEXT)
            HideAppIntentPage.ACTION_SELECT_OPEN -> {
                selectAllInfos(IntentType.CONTENT, IntentType.FILE)
            }

            HideAppIntentPage.ACTION_SELECT_BROWSER -> {
                selectAllInfos(IntentType.HTTP_LINK, IntentType.HTTPS_LINK)
            }

            HideAppIntentPage.ACTION_CLEAR_ALL -> showClearDialog = true
        }
    }

    LaunchedEffect(Unit) {
        if (allAppInfos.isEmpty() || allIntentInfos.isEmpty()) reload()
    }

    // 两条线共用的搜索回调：搜索词与列表同源，渲染分支里各写一份会分叉
    val onQueryChange: (String) -> Unit = { q ->
        query = q
        filterAppInfos = applyQuery(q)
    }

    AppListLoadingContainer(
        visible = loading,
        modifier = Modifier.fillParentMaxHeight(),
    ) {
        Column(Modifier.fillParentMaxHeight()) {
            // COUI 线的分组卡片自带 16dp 水平内缩（CouiScopeCard），页级 padding 只补给 Material 线，
            // 否则开关卡左右各多缩 16dp，与搜索框 / 列表对不齐。
            PrefGroup(
                modifier = if (LocalUiMode.current == UiMode.Coui) Modifier else Modifier.padding(horizontal = 16.dp),
            ) {
                item {
                    PrefSwitchRow(
                        title = stringResource(R.string.custom_config_app_intent_list),
                        checked = context.getBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, false),
                        onCheckedChange = { v ->
                            context.putBoolean(IntentPrefs, HIDE_APP_INTENT_ENABLE_KEY, v)
                            context.sendPrefsValue("system", HIDE_APP_INTENT_ENABLE_KEY, v)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = AppSearchPlaceholder,
                    enabled = !loading,
                    filterActive = showSortSheet,
                    onFilterClick = { showSortSheet = true },
                )
            }
            Spacer(Modifier.height(AppListContentGap))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.ItemGap),
            ) {
                itemsIndexed(
                    filterAppInfos,
                    key = { _, info -> info.packageName },
                ) { index, info ->
                    AppItem(index = index, count = filterAppInfos.size) {
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
        // 清空是整页批量操作，两条线的确认弹窗都只放按钮、不带标题正文
        val onConfirm: () -> Unit = {
            showClearDialog = false
            context.clearPrefs(IntentPrefs)
            scope.launch { reload() }
        }
        when (LocalUiMode.current) {
            UiMode.Material -> AlertDialog(
                onDismissRequest = { showClearDialog = false },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
            )

            UiMode.Coui -> CouiIntentClearDialog(
                onDismiss = { showClearDialog = false },
                onConfirm = onConfirm,
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
                // 弹层本身已分线，但筛选项的标签由调用方提供，必须跟着渲染线换控件：
                // 两份标签算一遍（状态与回调只建一次），再各按线渲染。
                val chips = listOf(
                    FilterChipSpec(
                        label = stringResource(R.string.appinfo_system_app),
                        selected = showSystemApps,
                        onClick = {
                            showSystemApps = !showSystemApps
                            scope.launch { reload() }
                        },
                    ),
                    FilterChipSpec(
                        label = stringResource(R.string.intent_share),
                        selected = intentFilter.contains(IntentType.SINGLE_SHARE),
                        onClick = {
                            setFilterTypes(
                                arrayOf(IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE),
                                !intentFilter.contains(IntentType.SINGLE_SHARE),
                            )
                        },
                    ),
                    FilterChipSpec(
                        label = stringResource(R.string.intent_text),
                        selected = intentFilter.contains(IntentType.PROCESS_TEXT),
                        onClick = {
                            setFilterTypes(
                                arrayOf(IntentType.PROCESS_TEXT),
                                !intentFilter.contains(IntentType.PROCESS_TEXT),
                            )
                        },
                    ),
                    FilterChipSpec(
                        label = stringResource(R.string.intent_open),
                        selected = intentFilter.contains(IntentType.CONTENT),
                        onClick = {
                            setFilterTypes(
                                arrayOf(IntentType.CONTENT, IntentType.FILE),
                                !intentFilter.contains(IntentType.CONTENT),
                            )
                        },
                    ),
                    FilterChipSpec(
                        label = stringResource(R.string.intent_browser),
                        selected = intentFilter.contains(IntentType.HTTP_LINK),
                        onClick = {
                            setFilterTypes(
                                arrayOf(IntentType.HTTP_LINK, IntentType.HTTPS_LINK),
                                !intentFilter.contains(IntentType.HTTP_LINK),
                            )
                        },
                    ),
                )
                when (LocalUiMode.current) {
                    UiMode.Material -> FlowRow {
                        chips.forEach { chip ->
                            FilterChip(
                                selected = chip.selected,
                                onClick = chip.onClick,
                                label = { Text(chip.label) },
                            )
                        }
                    }

                    UiMode.Coui -> FlowRow {
                        chips.forEach { chip ->
                            CouiFilterChip(
                                label = chip.label,
                                selected = chip.selected,
                                onClick = chip.onClick,
                            )
                        }
                    }
                }
            },
            onDismiss = { showSortSheet = false },
        )
    }
}

/**
 * 应用行：图标 + 应用名 + 包名 + 四组类型计数按钮。
 * 每组类型 (share/text/open/browser) 各自回调
 * `(packName, 全部条目, 已启用条目, 类型组)`；外层卡片容器由调用点提供（`MaterialItem`）。
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

    when (LocalUiMode.current) {
        UiMode.Material -> IntentAppRowMaterial(info, intentInfo, enabledInfo, onOpenDialog)
        UiMode.Coui -> IntentAppRowCoui(info, intentInfo, enabledInfo, onOpenDialog)
    }
}

/** Material 线：整行自己套卡片容器，行标题走 M3 文本 */
@Composable
private fun IntentAppRowMaterial(
    info: AppInfo,
    intentInfo: List<AppIntentInfo>,
    enabledInfo: List<AppIntentInfo>,
    onOpenDialog: (String, List<AppIntentInfo>, List<AppIntentInfo>, Array<IntentType>) -> Unit,
) {
    MaterialItemContainer {
        Column {
            AppListItem(
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

/**
 * COUI 线：卡片本体由外层 [AppItem] 的 COUI 分支绘制，这里再套一层容器会变成双层圆角，
 * 所以只排版「图标行 + 四组计数行」。
 */
@Composable
private fun IntentAppRowCoui(
    info: AppInfo,
    intentInfo: List<AppIntentInfo>,
    enabledInfo: List<AppIntentInfo>,
    onOpenDialog: (String, List<AppIntentInfo>, List<AppIntentInfo>, Array<IntentType>) -> Unit,
) {
    Column {
        AppListItem(
            headlineContent = {
                CouiText(
                    text = info.name,
                    kind = CouiTextKind.Search,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                CouiText(
                    text = info.packageName,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
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

/** 行内四组类型计数按钮（share/text/open/browser） */
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

/** 行内计数按钮：text = 已启用/全部，无条目时不可点 */
@Composable
private fun RowScope.IntentCountChip(
    allIntent: List<AppIntentInfo>,
    enabled: List<AppIntentInfo>,
    onClick: () -> Unit,
) {
    // 两条线的计数文案一致，只有承载控件不同
    val countText = "${enabled.size}/${allIntent.size}"
    when (LocalUiMode.current) {
        UiMode.Material -> SuggestionChip(
            onClick = onClick,
            enabled = allIntent.isNotEmpty(),
            label = {
                Text(
                    countText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            },
            modifier = Modifier.weight(1f),
        )

        UiMode.Coui -> CouiSuggestionChip(
            label = countText,
            onClick = onClick,
            enabled = allIntent.isNotEmpty(),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 多选 Intent 对话框：已启用项置顶预选、按 label/activity 过滤、全选/清空，
 * confirm 回传 ArrayList(selected)（对话框内临时态，取消不落盘）。
 *
 * 行渲染、全选/计数行与按钮行统一交给 MultiChoiceDialog，
 * 行首不单独渲染应用图标，信息由 title/summary 承担。
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
    // 已启用项置顶；过滤只在它上面收缩，所以已启用项之间的相对顺序保持不变
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
    val items = filtered.map { info ->
        ChoiceItem(
            key = info.choiceKey(),
            title = "${resolveMap[info]?.loadLabel(pm) ?: info.name} " + intentTypeLabel(info.type),
            summary = info.activity,
        )
    }
    MultiChoiceDialog(
        title = stringResource(R.string.custom_config_app_intent_list),
        items = items,
        selectedKeys = remember(data) { data.enabled.map { it.choiceKey() }.toSet() },
        onDismiss = onDismiss,
        onConfirm = { keys ->
            // 回传顺序沿用已启用项置顶的稳定顺序，不让 set 的迭代顺序决定落盘内容
            onConfirm(ArrayList(ordered.filter { it.choiceKey() in keys }))
        },
        header = { IntentQueryField(query = query, onQueryChange = { query = it }) },
        showSelectAll = true,
    )
}

/** 行 key（也是 confirm 回传时还原对象的索引）：沿用旧的 action|type|activity 组合 */
private fun AppIntentInfo.choiceKey(): String = "$action|$type|$activity"

/** 对话框内搜索框：调用点的 header 里只保留这一次调用。 */
@Composable
private fun IntentQueryField(query: String, onQueryChange: (String) -> Unit) {
    when (LocalUiMode.current) {
        UiMode.Material -> OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.activity_search_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        UiMode.Coui -> CouiTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = stringResource(R.string.activity_search_hint),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 排序过滤弹层里的一个筛选项：文案在这里算好，选中态与回调由调用方持有，
 * 两条线只按渲染线换承载控件（M3 FilterChip / COUI 胶囊标签）。
 */
private class FilterChipSpec(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * COUI 线的清空确认弹窗：确认走主色位、取消走次级位。
 * 原对话框没有标题与正文，这里同样只画按钮，避免凭空多出一句文案。
 */
@Composable
private fun CouiIntentClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CouiAlertDialog(
        onDismissRequest = onDismiss,
        confirmAction = CouiDialogAction(
            text = stringResource(android.R.string.ok),
            onClick = onConfirm,
            style = CouiDialogActionStyle.Primary,
            // 主色动作必须显式给配色：CouiDialogAction 的 init 会校验，缺了直接抛异常
            colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(android.R.string.cancel),
            onClick = onDismiss,
        ),
    )
}
