package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItem
import com.luckyzyx.luckytool.ui.compose.components.material.materialBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageAction
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.sendPrefsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * P4 特殊页：应用分身自定义列表。
 *
 * 单文件双线：状态与数据加载逻辑只有一份，只在渲染缝按 [LocalUiMode] 分派
 * （搜索框 [AppSearchField]、LazyColumn 骨架、条目容器、应用行 [AppToggleRow]）。
 * 与 prefCard 同一分派哲学：分派只发生在渲染体里，不为 Material / Miuix 各维护一份页面逻辑。
 *
 * 「打开应用分身」是对整页数据的一次性动作，故声明为 [ScopePageAction] 并设
 * `showAsAction = true`，由页面宿主渲染进顶栏图标位，不占页面正文的卡片。
 */
object MultiAppPage {

    internal const val SUPPORT_KEY = "multi_app_custom_list"

    /** 顶栏动作 key：打开应用分身（旧菜单 id 1） */
    internal const val ACTION_OPEN = "open"

    internal var refresh: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "multi_app",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        // 特殊页：整页由 fullContent 自渲染，入口可见性只看 isVisible + 作用域
        selfRendered = true,
        onRefresh = { refresh?.invoke() },
        fullContent = { builder -> MultiAppContent(builder) },
        topActions = listOf(
            ScopePageAction(
                key = ACTION_OPEN,
                labelRes = R.string.open,
                icon = R.drawable.baseline_open_in_new_24,
                showAsAction = true,
            ),
        ),
    ) { }
}

@Composable
internal fun LazyItemScope.MultiAppContent(builder: PrefScopeBuilder) {
    val miuix = LocalUiMode.current == UiMode.Miuix
    val context = LocalContext.current
    val packageUtils = remember { PackageUtils(context.packageManager) }
    val scope = rememberCoroutineScope()

    var isReverse by remember { mutableStateOf(false) }
    var sortMode by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var allAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var filterAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var allEnabledInfos by remember { mutableStateOf(ArraySet<String>()) }

    var showSortSheet by remember { mutableStateOf(false) }

    fun applyQuery(q: String): ArrayList<AppInfo> =
        if (q.isBlank()) allAppInfos
        else ArrayList(
            allAppInfos.filter {
                it.name.lowercase().contains(q) ||
                        it.packageName.lowercase().contains(q)
            }
        )

    fun saveEnableList() {
        context.putStringSet(ModulePrefs, MultiAppPage.SUPPORT_KEY, allEnabledInfos.toSet())
        context.sendPrefsKey("android", MultiAppPage.SUPPORT_KEY)
    }

    fun toggle(info: AppInfo, enable: Boolean) {
        // 先拷贝再改、再整体赋值：就地改 state 当前持有的集合后赋一个内容相等的副本，
        // MutableState 的结构相等策略会认定"值没变"而丢弃这次写入，界面不重组
        //（表现为点了没反应、重开页面才刷新）。
        val next = ArraySet(allEnabledInfos)
        if (enable) next.add(info.packageName) else next.remove(info.packageName)
        allEnabledInfos = next
        saveEnableList()
    }

    suspend fun reload() {
        loading = true
        query = ""
        withContext(Dispatchers.IO) {
            val enableData =
                context.getStringSet(ModulePrefs, MultiAppPage.SUPPORT_KEY, ArraySet())
            val appInfos = packageUtils.getInstalledAppInfos(0)
            appInfos.removeIf { it.isOverlay }
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
            }
            val sortDatas = ArrayList<AppInfo>()
            enableData.forEach { k ->
                val find = appInfos.find { it.packageName == k } ?: return@forEach
                sortDatas.add(find)
            }
            sortDatas.apply {
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
            appInfos.removeAll(sortDatas.toSet())
            appInfos.addAll(0, sortDatas)
            allEnabledInfos = ArraySet<String>().apply { addAll(enableData) }
            allAppInfos = appInfos
            filterAppInfos = appInfos
        }
        loading = false
    }

    MultiAppPage.refresh = { reload() }
    MultiAppPage.spec.actionHandler = { key ->
        if (key == MultiAppPage.ACTION_OPEN) IntentUtils(context).jumpMultiApp()
    }

    LaunchedEffect(Unit) {
        if (allAppInfos.isEmpty()) reload()
    }

    // 条目内容两线共享，只在容器上分派（同 ScopeScreen 的 listItems 范式）。
    val listItems: LazyListScope.() -> Unit = {
        itemsIndexed(filterAppInfos, key = { _, info -> info.packageName }) { index, info ->
            val enabled = allEnabledInfos.contains(info.packageName)
            if (miuix) {
                MiuixPrefItem(index = index, count = filterAppInfos.size) {
                    AppToggleRow(info = info, enabled = enabled) { v -> toggle(info, v) }
                }
            } else {
                MaterialItem(index = index, count = filterAppInfos.size) {
                    AppToggleRow(info = info, enabled = enabled) { v -> toggle(info, v) }
                }
            }
        }
    }

    Column(Modifier.fillParentMaxHeight()) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (miuix) MiuixPrefDefaults.CardHorizontalInset else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
        Spacer(Modifier.height(AppListContentGap))
        if (miuix) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .scrollEndHaptic()
                    .overScrollVertical(),
                contentPadding = PaddingValues(
                    start = MiuixPrefDefaults.CardHorizontalInset,
                    end = MiuixPrefDefaults.CardHorizontalInset,
                    bottom = materialBottomInset(),
                ),
                verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
                overscrollEffect = null,
            ) { listItems() }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(MiuixPrefDefaults.ItemGap),
            ) { listItems() }
        }
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
            filterContent = null,
            onDismiss = { showSortSheet = false },
        )
    }
}
