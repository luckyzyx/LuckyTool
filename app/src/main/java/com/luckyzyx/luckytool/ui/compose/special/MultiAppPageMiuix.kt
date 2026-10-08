package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.sendPrefsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * MultiAppPage 的 Miuix 线呈现（`val spec` 的 `fullContent` 槽位）。
 *
 * 与 Material 线（[MultiAppMaterialContent]）差别只在呈现层，逐条等价关系：
 * - 「打开应用分身」单行卡片 → `MiuixPrefItem(index = 0, count = 1)` + `MiuixListItem(onClick = { IntentUtils(context).jumpMultiApp() })`（同一跳转动作）；
 * - `OutlinedTextField` → miuix `TextField`（String 受控重载，`useLabelAsPlaceholder` 保留原 placeholder 语义）；
 * - `SegmentedItem(index, count)` → `MiuixPrefItem(index, count)`；应用行复用共享件 [AppToggleRowMiuix]
 *   （与 ZoomWindowPage 同一份行声明；整行点击 + VirtualKey 触感 + `onToggle` 写值语义与 Material 线一致）；
 * - `ListItemDefaults.SegmentedGap` → `MiuixPrefDefaults.ItemGap`，水平内缩 16dp → `CardHorizontalInset`（12dp，已登记像素差异）；
 * - LazyColumn 追加 `scrollEndHaptic().overScrollVertical()` 并关掉平台 overscroll 光晕（miuix 线统一口径）；
 * - 排序/筛选弹层复用共享件 [SortFilterSheet]（内部再按线分派），调用参数与 Material 线逐字相同。
 *
 * 状态与数据加载逻辑按 Material 线逐条重述（理由同 ZoomWindowPage：Material 线那份代码必须逐字不变），
 * prefs 写值语义（`putStringSet` + `sendPrefsKey("android", …)`）与排序/过滤口径与 Material 线完全一致。
 */
@Composable
internal fun LazyItemScope.MultiAppMiuixContent(builder: PrefScopeBuilder) {
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

    LaunchedEffect(Unit) {
        if (allAppInfos.isEmpty()) reload()
    }

    Column(Modifier.fillParentMaxHeight()) {
        Column(
            modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiuixPrefItem(index = 0, count = 1) {
                MiuixListItem(
                    title = stringResource(R.string.open),
                    onClick = { IntentUtils(context).jumpMultiApp() },
                )
            }
            TextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    filterAppInfos = applyQuery(q)
                },
                label = "Name / PackageName",
                useLabelAsPlaceholder = true,
                enabled = !loading,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_baseline_search_24),
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            painterResource(R.drawable.baseline_filter_list_24),
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
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
            itemsIndexed(filterAppInfos, key = { _, info -> info.packageName }) { index, info ->
                MiuixPrefItem(index = index, count = filterAppInfos.size) {
                    AppToggleRowMiuix(
                        info = info,
                        enabled = allEnabledInfos.contains(info.packageName),
                        onToggle = { v ->
                            if (v) allEnabledInfos.add(info.packageName)
                            else allEnabledInfos.remove(info.packageName)
                            allEnabledInfos = ArraySet(allEnabledInfos)
                            saveEnableList()
                        },
                    )
                }
            }
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
