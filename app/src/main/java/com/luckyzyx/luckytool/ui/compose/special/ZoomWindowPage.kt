package com.luckyzyx.luckytool.ui.compose.special

import android.util.ArraySet
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveSwitch
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.sendPrefsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * P4 特殊页：自定义缩放窗口支持列表（旧 ZoomWindowFragment 1:1 迁移）。
 * 使用 ScopeScreen 的 fullContent 全屏自定义渲染 + onRefresh 下拉刷新。
 */
object ZoomWindowPage {

    private const val SUPPORT_KEY = "zoom_window_support_list"

    private var refresh: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "zoom_window",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
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
                context.putStringSet(ModulePrefs, SUPPORT_KEY, allEnabledInfos.toSet())
                context.sendPrefsKey("android", SUPPORT_KEY)
            }

            suspend fun reload() {
                loading = true
                query = ""
                withContext(Dispatchers.IO) {
                    val enableData =
                        context.getStringSet(ModulePrefs, SUPPORT_KEY, ArraySet())
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

            refresh = { reload() }

            LaunchedEffect(Unit) {
                if (allAppInfos.isEmpty()) reload()
            }

            Column(Modifier.fillParentMaxHeight()) {
                Column(Modifier.padding(horizontal = 16.dp)) {
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
                            SupportAppRow(
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
        },
    ) { }
}

@Composable
private fun SupportAppRow(
    info: AppInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
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
}
