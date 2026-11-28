@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.compose.components

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** AppInfo 排序模式：名称 / 包名 / 大小 / 安装时间 / 最近更新 / 目标 SDK 六个排序维度 */
enum class AppSortMode(@StringRes val labelRes: Int) {
    Name(R.string.appinfo_app_name),
    Package(R.string.appinfo_package_name),
    Size(R.string.appinfo_app_size),
    InstallTime(R.string.appinfo_install_time),
    LastInstallTime(R.string.appinfo_last_updated_time),
    Target(R.string.appinfo_target_sdk),
}

/** 对话框内应用列表的高度上限：竖屏按设计上限，窗口变矮时由 dialogListMaxHeight 再压 */
private val PickerListMaxHeight: Dp = 420.dp

/** 行内图标尺寸：与列表行首图标槽同边长，无图标时留同等占位保证行高一致 */
private val AppIconSize: Dp = 44.dp

/** Drawable → Painter（AppInfo.icon 为平台 Drawable，转位图渲染，避免引入图片库） */
@Composable
fun rememberAppIconPainter(icon: Drawable?): Painter? {
    val density = LocalDensity.current.density
    return remember(icon, density) {
        icon?.let { drawable ->
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
                // 自适应图标(AdaptiveIconDrawable)的 intrinsicWidth/Height 为 -1，
                // coerceAtLeast(1) 会生成 1×1 位图导致图标失效；回退到 48dp 尺寸渲染
                val fallback = (48f * density).roundToInt().coerceAtLeast(1)
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: fallback
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: fallback
                val bmp = createBitmap(width, height)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                bmp
            }
            BitmapPainter(bitmap.asImageBitmap())
        }
    }
}

/** 应用选择对话框内部状态（搜索 / 排序 / 系统应用开关 / 多选集合 / 排序筛选面板开合）。 */
private class AppPickerUiState(
    includeSystemApps: Boolean,
    enabledPackages: Set<String>,
) {
    var query by mutableStateOf("")
    var sortMode by mutableStateOf(AppSortMode.Name)
    var reverse by mutableStateOf(false)
    var includeSystem by mutableStateOf(includeSystemApps)
    var selected by mutableStateOf(enabledPackages)

    /** 排序/筛选面板开合：面板默认收起，避免常驻一排 chip 挤压列表 */
    var sortFilterOpen by mutableStateOf(false)

    fun toggle(packageName: String) {
        selected = if (packageName in selected) selected - packageName else selected + packageName
    }

    /** 全选/取消全选：只作用于当前可见（已过滤）的应用 */
    fun toggleAll(packages: List<String>) {
        val allSelected = packages.isNotEmpty() && packages.all { it in selected }
        selected = if (allSelected) selected - packages.toSet() else selected + packages
    }
}

/**
 * 应用选择对话框：可搜索、可排序的已安装应用列表选择器。
 *
 * 行为：加载 `PackageUtils(context.packageManager).getInstalledAppInfos(0)`，
 * 排除 overlay 应用，默认隐藏系统应用；搜索匹配名称/包名；6 种排序 + 倒序；
 * 多选模式底部确认返回，单选模式点击即返回；已选应用预勾选并置顶显示。
 *
 * 版式：顶部常驻搜索框（尾部漏斗入口展开内联「排序与筛选」面板）→
 * 「共 N 个应用 · 已选 M」状态行（多选模式带全选/取消全选）→ 分段卡片列表，
 * 底部动作栏由弹窗外壳给。外壳、正文与行都只写一份：外观差异走 [currentDialogStyle]、
 * [currentPrefStyle] 与 AppListItem 一族，本文件不判外观线。
 */
@Composable
fun AppPickerDialog(
    title: String,
    multiMode: Boolean = false,
    showSystemApps: Boolean = false,
    enabledList: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (List<AppInfo>) -> Unit,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    val apps by produceState<List<AppInfo>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            PackageUtils(context.packageManager).getInstalledAppInfos(0)
        }
        loading = false
    }

    val state = remember { AppPickerUiState(showSystemApps, enabledList) }

    val filtered = remember(apps, state.query, state.sortMode, state.reverse, state.includeSystem, enabledList) {
        val q = state.query.trim().lowercase()
        apps.asSequence()
            .filter { !it.isOverlay }
            .filter { state.includeSystem || !it.isSystem }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            .sortedWith(
                compareByDescending<AppInfo> { it.packageName in enabledList }
                    .then(comparatorOf(state.sortMode, state.reverse))
            )
            .toList()
    }

    val allVisibleSelected = filtered.isNotEmpty() && filtered.all { it.packageName in state.selected }

    // 单选模式点击即返回；多选模式切换勾选
    val onSelect: (AppInfo) -> Unit = { app ->
        if (multiMode) state.toggle(app.packageName) else onConfirm(listOf(app))
    }
    // 确认返回「全量已选」并按当前排序排列：不能只取 filtered，
    // 否则先搜索勾选、再清空搜索时这些勾选会在确认时被丢掉
    val onConfirmSelected: () -> Unit = {
        onConfirm(apps.filter { it.packageName in state.selected }.sortedWith(comparatorOf(state.sortMode, state.reverse)))
        onDismiss()
    }
    val onSelectAll: () -> Unit = { state.toggleAll(filtered.map { it.packageName }) }

    val cancelLabel = stringResource(android.R.string.cancel)
    // 单选模式点条目即返回，关闭动作落在按钮位，因此取消占确认位
    currentDialogStyle().Alert(
        onDismissRequest = onDismiss,
        confirmLabel = if (multiMode) stringResource(android.R.string.ok) else cancelLabel,
        onConfirm = if (multiMode) onConfirmSelected else onDismiss,
        title = title,
        confirmEnabled = !loading,
        secondaryLabel = if (multiMode) cancelLabel else null,
        onSecondary = onDismiss,
        // 原文两条线在加载中都不让按取消：列表还没就绪时关掉弹窗会丢掉选择
        secondaryEnabled = !loading,
        // 正文自带惰性列表滚动，外壳不再套一层滚动
        bodyScrollable = false,
    ) {
        AppPickerBody(
            state = state,
            multiMode = multiMode,
            filtered = filtered,
            loading = loading,
            allVisibleSelected = allVisibleSelected,
            onSelect = onSelect,
            onSelectAll = onSelectAll,
        )
    }
}

/**
 * 对话框正文：搜索头 → 可开合的排序筛选面板 → 状态行 → 分隔 → 列表。
 *
 * 左右留白由弹窗外壳补（正文槽已带 24dp），这里只管纵向间距；列表只约束自身高度上限，
 * 不取权重（外壳正文的高度无界，权重拿不到有效空间）。
 */
@Composable
private fun AppPickerBody(
    state: AppPickerUiState,
    multiMode: Boolean,
    filtered: List<AppInfo>,
    loading: Boolean,
    allVisibleSelected: Boolean,
    onSelect: (AppInfo) -> Unit,
    onSelectAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 16.dp),
    ) {
        AppSearchBar(
            query = state.query,
            onQueryChange = { state.query = it },
            placeholder = stringResource(R.string.appinfo_search_hint),
            enabled = !loading,
            filterActive = state.sortFilterOpen,
            onFilterClick = { state.sortFilterOpen = !state.sortFilterOpen },
        )
        AnimatedVisibility(visible = state.sortFilterOpen) {
            PickerSortFilterPanel(state = state, loading = loading)
        }
        PickerStatusRow(
            state = state,
            multiMode = multiMode,
            visibleCount = filtered.size,
            loading = loading,
            allVisibleSelected = allVisibleSelected,
            onSelectAll = onSelectAll,
        )
        currentPrefStyle().Divider(Modifier.padding(top = 6.dp, bottom = 6.dp))
        when {
            loading -> PickerPlaceholder {
                // 不传尺寸：控件内部已固定自身直径，外部再套 size 会互相打架
                currentPrefStyle().Progress(Modifier)
            }

            filtered.isEmpty() -> PickerPlaceholder {
                PrefNote(stringResource(R.string.appinfo_no_match))
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogListMaxHeight(PickerListMaxHeight, chrome = 280.dp)),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                    AppItem(index = index, count = filtered.size) {
                        AppPickerRow(
                            app = app,
                            multiMode = multiMode,
                            checked = app.packageName in state.selected,
                            onSelect = { onSelect(app) },
                        )
                    }
                }
            }
        }
    }
}

/** 空态/加载态占位（有界高度，避免弹层塌成一条） */
@Composable
private fun PickerPlaceholder(
    minHeight: Dp = 180.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 状态行：共 N 个应用 · 已选 M（多选），右侧全选/取消全选 */
@Composable
private fun PickerStatusRow(
    state: AppPickerUiState,
    multiMode: Boolean,
    visibleCount: Int,
    loading: Boolean,
    allVisibleSelected: Boolean,
    onSelectAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 文字占满剩余宽度，全选按钮才贴到行尾
        Box(Modifier.weight(1f)) {
            PrefRowSummary(
                text = stringResource(R.string.appinfo_app_count, visibleCount) +
                    if (multiMode) " · " + stringResource(R.string.appinfo_selected_count, state.selected.size) else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (multiMode) {
            currentPrefStyle().TextButton(
                text = stringResource(
                    if (allVisibleSelected) R.string.appinfo_deselect_all else R.string.appinfo_select_all
                ),
                onClick = onSelectAll,
                modifier = Modifier,
                enabled = !loading && visibleCount > 0,
            )
        }
    }
}

/** 内联排序/筛选面板（由搜索头漏斗按钮开合） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickerSortFilterPanel(state: AppPickerUiState, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        PrefRowTitle(stringResource(R.string.appinfo_sort_by))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSortMode.entries.forEach { mode ->
                currentPrefStyle().FilterChip(
                    label = stringResource(mode.labelRes),
                    selected = state.sortMode == mode,
                    onClick = { state.sortMode = mode },
                    modifier = Modifier,
                    enabled = !loading,
                )
            }
            currentPrefStyle().FilterChip(
                label = stringResource(R.string.appinfo_reverse),
                selected = state.reverse,
                onClick = { state.reverse = !state.reverse },
                modifier = Modifier,
                enabled = !loading,
            )
        }
        Box(Modifier.padding(top = 10.dp)) {
            PrefRowTitle(stringResource(R.string.appinfo_filter))
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            currentPrefStyle().FilterChip(
                label = stringResource(R.string.appinfo_system_app),
                selected = state.includeSystem,
                onClick = { state.includeSystem = !state.includeSystem },
                modifier = Modifier,
                enabled = !loading,
            )
        }
    }
}

/** 应用图标：无图标时留同等占位，保证行高一致 */
@Composable
private fun AppIcon(icon: Drawable?, modifier: Modifier = Modifier, size: Dp = AppIconSize) {
    val painter = rememberAppIconPainter(icon)
    val box = modifier.size(size)
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = box.clip(RoundedCornerShape(size * 0.27f)),
        )
    } else {
        Box(box)
    }
}

/**
 * 列表行：图标 + 名称/包名，多选模式行尾加勾选指示；整行点击由行容器接管。
 *
 * 单选模式没有指示器——点条目就当选中返回，行尾只为多选露出勾选态。
 */
@Composable
private fun AppPickerRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    // 勾选指示只画图形，点击由整行接管；与标题之间留出 8dp，避免贴着包名
    val indicator: (@Composable () -> Unit)? = if (multiMode) {
        {
            Row(
                modifier = Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                currentPrefStyle().ChoiceIndicator(radio = false, checked = checked, enabled = true)
            }
        }
    } else {
        null
    }
    AppListItem(
        onClick = onSelect,
        enabled = true,
        iconSize = AppIconSize,
        headlineContent = { PrefRowTitle(app.name, maxLines = 1) },
        supportingContent = { PrefRowSummary(app.packageName, maxLines = 1) },
        leadingContent = { AppIcon(app.icon) },
        trailingContent = indicator,
    )
}

private fun comparatorOf(mode: AppSortMode, reverse: Boolean): Comparator<AppInfo> {
    val base: Comparator<AppInfo> = when (mode) {
        AppSortMode.Name -> compareBy { it.name }
        AppSortMode.Package -> compareBy { it.packageName }
        AppSortMode.Size -> compareBy { it.size }
        AppSortMode.InstallTime -> compareBy { it.installTime }
        AppSortMode.LastInstallTime -> compareBy { it.lastInstallTime }
        AppSortMode.Target -> compareBy { it.target }
    }
    return if (reverse) base.reversed() else base
}
