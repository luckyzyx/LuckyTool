package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.collection.ArrayMap
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.DarkModeInfo
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveSwitch
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItemContainer
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedSwitchItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
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

/**
 * P4 特殊页：暗色模式应用列表（旧 DarkModeFragment 1:1 迁移）。
 * 使用 ScopeScreen 的 fullContent 全屏自定义渲染 + onRefresh 下拉刷新。
 */
object DarkModePage {

    private const val ENABLE_KEY = "dark_mode_list_enable"
    private const val SUPPORT_KEY = "dark_mode_support_list"

    private var refresh: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "dark_mode",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf("com.android.settings"),
        restartEnabled = true,
        onRefresh = { refresh?.invoke() },
        fullContent = { _ ->
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
                context.putStringSet(ModulePrefs, SUPPORT_KEY, data.toSet())
                context.sendPrefsKey("android", SUPPORT_KEY)
                context.sendPrefsKey("com.android.settings", SUPPORT_KEY)
            }

            suspend fun reload() {
                loading = true
                query = ""
                withContext(Dispatchers.IO) {
                    val enableData =
                        context.getStringSet(ModulePrefs, SUPPORT_KEY, ArraySet())
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

            refresh = { reload() }

            LaunchedEffect(Unit) {
                if (allAppInfos.isEmpty()) reload()
            }

            Column(Modifier.fillParentMaxHeight()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SegmentedItem(index = 0, count = 1) {
                        SegmentedSwitchItem(
                            title = stringResource(R.string.enable_dark_mode_list),
                            checked = context.getBoolean(ModulePrefs, ENABLE_KEY, false),
                            onCheckedChange = { v ->
                                context.putBoolean(ModulePrefs, ENABLE_KEY, v)
                                context.sendPrefsValue("android", ENABLE_KEY, v)
                            },
                        )
                    }
                    SegmentedItem(index = 0, count = 1) {
                        SegmentedListItem(
                            onClick = { IntentUtils(context).jumpDarkMode() },
                            headlineContent = { Text(stringResource(R.string.open)) },
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { q ->
                            query = q
                            filterAppInfos = applyQuery(q)
                        },
                        enabled = !loading,
                        singleLine = true,
                        placeholder = { Text("Name / PackageName") },
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
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(filterAppInfos, key = { _, info -> info.packageName }) { index, info ->
                        SegmentedItem(index = index, count = filterAppInfos.size) {
                            DarkModeAppRow(
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
        },
    ) { }
}

@Composable
private fun DarkModeAppRow(
    info: AppInfo,
    enabled: Boolean,
    curType: Int,
    onToggle: (Boolean) -> Unit,
    onTypeChange: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    SegmentedItemContainer {
        Column {
            SegmentedListItem(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onToggle(!enabled)
                },
                interactionSource = interactionSource,
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
                trailingContent = {
                    ExpressiveSwitch(
                        checked = enabled,
                        onCheckedChange = null,
                        interactionSource = interactionSource,
                    )
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
                    Text(
                        curType.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
