package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.collection.ArrayMap
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.DarkModeInfo
import com.luckyzyx.luckytool.ui.compose.components.material.expressiveBottomInset
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefDefaults
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.sendPrefsKey
import com.luckyzyx.luckytool.utils.sendPrefsValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * DarkModePage 的 Miuix 线呈现（`val spec` 的 `fullContent` 槽位）。
 *
 * 与 Material 线（[DarkModeMaterialContent]）差别只在呈现层，逐条等价关系：
 * - 「启用暗色模式列表」开关卡片 → `MiuixPrefItem(0, 1)` + `MiuixSwitchItem`（自带 VirtualKey 触感；
 *   `putBoolean` + `sendPrefsValue("android", …)` 写值语义与 Material 线一致）；
 * - 「打开暗色模式」卡片 → `MiuixListItem(onClick = { IntentUtils(context).jumpDarkMode() })`（同一跳转动作）；
 * - `OutlinedTextField` → miuix `TextField`（`useLabelAsPlaceholder` 保留原 placeholder 语义）；
 * - `SegmentedItem(index, count)` + `DarkModeAppRow`（`SegmentedItemContainer{Column{…}}`）
 *   → `MiuixPrefItem(index, count)` + [DarkModeAppRowMiuix]（`MiuixListItem` + 可选 miuix `Slider`，同一 `valueRange = 0f..4f`、`steps = 3`、
 *   拖动中只更新本地状态、松手才 `onTypeChange` 落盘）；
 * - `ListItemDefaults.SegmentedGap` → `MiuixPrefDefaults.ItemGap`；水平内缩 16dp → `CardHorizontalInset`（12dp，已登记像素差异）；
 * - LazyColumn 追加 `scrollEndHaptic().overScrollVertical()` 并关掉平台 overscroll 光晕（miuix 线统一口径）；
 * - 排序/筛选弹层复用共享件 [SortFilterSheet]（内部再按线分派），调用参数与 Material 线逐字相同。
 *
 * 状态与数据加载逻辑按 Material 线逐条重述（理由同 ZoomWindowPage：Material 线那份代码必须逐字不变），
 * prefs 写值语义（`putStringSet` + 双 `sendPrefsKey`（android / com.android.settings）、`DarkModeInfo` JSON 序列化）与 Material 线完全一致。
 */
@Composable
internal fun LazyItemScope.DarkModeMiuixContent(builder: PrefScopeBuilder) {
    val context = LocalContext.current
    val packageUtils = remember { PackageUtils(context.packageManager) }
    val scope = rememberCoroutineScope()

    var isReverse by remember { mutableStateOf(false) }
    var sortMode by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var allAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var filterAppInfos by remember { mutableStateOf(ArrayList<AppInfo>()) }
    var allEnabledInfos by remember { mutableStateOf(ArrayMap<String, DarkModeInfo>()) }

    var showSortSheet by remember { mutableStateOf(false) }

    fun applyQuery(q: String): ArrayList<AppInfo> =
        if (q.isBlank()) allAppInfos
        else ArrayList(
            allAppInfos.filter {
                it.name.contains(q) || it.packageName.lowercase().contains(q)
            }
        )

    fun saveEnableList() {
        val data = allEnabledInfos.mapNotNull { (_, v) ->
            safeOfNull { Json.encodeToString(v) }
        }
        context.putStringSet(ModulePrefs, DarkModePage.SUPPORT_KEY, data.toSet())
        context.sendPrefsKey("android", DarkModePage.SUPPORT_KEY)
        context.sendPrefsKey("com.android.settings", DarkModePage.SUPPORT_KEY)
    }

    suspend fun reload() {
        loading = true
        query = ""
        withContext(Dispatchers.IO) {
            val enableData =
                context.getStringSet(ModulePrefs, DarkModePage.SUPPORT_KEY, ArraySet())
            val appInfos = packageUtils.getInstalledAppInfos(0)
            appInfos.removeIf { it.isOverlay }
            val enabledInfos = ArrayMap<String, DarkModeInfo>()
            enableData.forEach { js ->
                val info = safeOfNull { Json.decodeFromString<DarkModeInfo>(js) }
                if (info != null &&
                    appInfos.find { it.packageName == info.packName } != null
                ) {
                    enabledInfos[info.packName] = info
                }
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
            }
            val sortDatas = ArrayList<AppInfo>()
            enabledInfos.keys.forEach { k ->
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
            allEnabledInfos = enabledInfos
            allAppInfos = appInfos
            filterAppInfos = appInfos
        }
        loading = false
    }

    DarkModePage.refresh = { reload() }

    LaunchedEffect(Unit) {
        if (allAppInfos.isEmpty()) reload()
    }

    Column(Modifier.fillParentMaxHeight()) {
        Column(
            modifier = Modifier.padding(horizontal = MiuixPrefDefaults.CardHorizontalInset),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiuixPrefItem(index = 0, count = 1) {
                MiuixSwitchItem(
                    title = stringResource(R.string.enable_dark_mode_list),
                    checked = context.getBoolean(ModulePrefs, DarkModePage.ENABLE_KEY, false),
                    onCheckedChange = { v ->
                        context.putBoolean(ModulePrefs, DarkModePage.ENABLE_KEY, v)
                        context.sendPrefsValue("android", DarkModePage.ENABLE_KEY, v)
                    },
                )
            }
            MiuixPrefItem(index = 0, count = 1) {
                MiuixListItem(
                    title = stringResource(R.string.open),
                    onClick = { IntentUtils(context).jumpDarkMode() },
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
                    DarkModeAppRowMiuix(
                        info = info,
                        enabled = allEnabledInfos.containsKey(info.packageName),
                        curType = allEnabledInfos[info.packageName]?.curType ?: 0,
                        onToggle = { v ->
                            allEnabledInfos.remove(info.packageName)
                            if (v) {
                                allEnabledInfos[info.packageName] = DarkModeInfo(info.packageName)
                            }
                            allEnabledInfos = ArrayMap(allEnabledInfos)
                            saveEnableList()
                        },
                        onTypeChange = { t ->
                            allEnabledInfos[info.packageName]?.curType = t
                            allEnabledInfos = ArrayMap(allEnabledInfos)
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

/**
 * Miuix 线的暗色模式应用行：`MiuixListItem` + 只读 `Switch`；启用后在本卡片内追加
 * `Slider(valueRange = 0f..4f, steps = 3)` 与数值文本（与 Material 线同卡片布局、同落盘时机）。
 */
@Composable
internal fun DarkModeAppRowMiuix(
    info: AppInfo,
    enabled: Boolean,
    curType: Int,
    onToggle: (Boolean) -> Unit,
    onTypeChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column {
        MiuixListItem(
            title = info.name,
            summary = info.packageName,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onToggle(!enabled)
            },
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
            endActions = {
                Switch(checked = enabled, onCheckedChange = null)
            },
        )
        if (enabled) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var sliderValue by remember(info) { mutableStateOf(curType.toFloat()) }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onTypeChange(sliderValue.toInt()) },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.weight(1f),
                )
                MiuixText(curType.toString(), style = MiuixTheme.textStyles.body2)
            }
        }
    }
}
