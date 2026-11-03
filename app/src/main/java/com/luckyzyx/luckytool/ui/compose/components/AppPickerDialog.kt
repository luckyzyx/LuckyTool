@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.compose.components

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItem
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixDialogScrollBody
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField

/** AppInfo 排序模式：名称 / 包名 / 大小 / 安装时间 / 最近更新 / 目标 SDK 六个排序维度 */
enum class AppSortMode(@StringRes val labelRes: Int) {
    Name(R.string.appinfo_app_name),
    Package(R.string.appinfo_package_name),
    Size(R.string.appinfo_app_size),
    InstallTime(R.string.appinfo_install_time),
    LastInstallTime(R.string.appinfo_last_updated_time),
    Target(R.string.appinfo_target_sdk),
}

/** 对话框内应用列表的高度上限：两条线的弹层都不给内容加高度上限，由内容自己收敛 */
private val PickerListMaxHeight: Dp = 420.dp

/** 行内图标尺寸：Miuix 线 44dp，Material 线的紧凑行用 34dp */
private val AppIconSize: Dp = 44.dp
private val MaterialAppIconSize: Dp = 34.dp

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

/** 应用选择对话框内部状态（搜索 / 排序 / 系统应用开关 / 多选集合 / 排序筛选面板开合），两条外观线共用。 */
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
 * 版式（重新设计）：顶部常驻搜索框（尾部漏斗入口展开内联「排序与筛选」面板）→
 * 「共 N 个应用 · 已选 M」状态行（多选模式带全选/取消全选）→ 分段卡片列表 →
 * 底部动作栏。两条线共用 [AppPickerUiState] 与过滤/排序，只在渲染体里分派到
 * Miuix / Material 原语；对外签名与调用点零改动。Miuix 弹层统一由根部 Miuix
 * Scaffold 的默认 popup host 承载，本组件不自装 host、不传 `renderInRootScaffold = false`。
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

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixAppPickerDialog(
            title = title,
            multiMode = multiMode,
            state = state,
            filtered = filtered,
            loading = loading,
            allVisibleSelected = allVisibleSelected,
            onDismiss = onDismiss,
            onSelect = onSelect,
            onSelectAll = onSelectAll,
            onConfirmSelected = onConfirmSelected,
        )

        UiMode.Material -> MaterialAppPickerDialog(
            title = title,
            multiMode = multiMode,
            state = state,
            filtered = filtered,
            loading = loading,
            allVisibleSelected = allVisibleSelected,
            onDismiss = onDismiss,
            onSelect = onSelect,
            onSelectAll = onSelectAll,
            onConfirmSelected = onConfirmSelected,
        )
    }
}

/** Material 线：M3 AlertDialog + 搜索头 + 内联排序筛选面板 + 分段卡片列表 */
@Composable
private fun MaterialAppPickerDialog(
    title: String,
    multiMode: Boolean,
    state: AppPickerUiState,
    filtered: List<AppInfo>,
    loading: Boolean,
    allVisibleSelected: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    onSelectAll: () -> Unit,
    onConfirmSelected: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                MaterialPickerSearchField(state = state, loading = loading)
                AnimatedVisibility(visible = state.sortFilterOpen) {
                    MaterialPickerSortFilterPanel(state = state, loading = loading)
                }
                MaterialPickerStatusRow(
                    state = state,
                    multiMode = multiMode,
                    visibleCount = filtered.size,
                    loading = loading,
                    allVisibleSelected = allVisibleSelected,
                    onSelectAll = onSelectAll,
                )
                HorizontalDivider(Modifier.padding(top = 6.dp, bottom = 6.dp))
                when {
                    loading -> PickerPlaceholder {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    }

                    filtered.isEmpty() -> PickerPlaceholder {
                        Text(
                            text = stringResource(R.string.appinfo_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = PickerListMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                            MaterialItem(index = index, count = filtered.size) {
                                MaterialAppRow(
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
        },
        confirmButton = {
            if (multiMode) {
                TextButton(enabled = !loading, onClick = onConfirmSelected) {
                    Text(stringResource(android.R.string.ok))
                }
            } else {
                TextButton(enabled = !loading, onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
        dismissButton = {
            if (multiMode) {
                TextButton(enabled = !loading, onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        },
    )
}

/**
 * Miuix 线：OverlayDialog（根部 popup host 承载）+ Miuix 行件。
 *
 * 内容列整体上限 520.dp：OverlayDialog 在小屏为底部贴合展示且不给内容加高度上限，
 * 由内容自己收敛高度；按钮行交给 [MiuixDialogScrollBody] 的 actions 槽，避免被
 * 可收缩的列表体挤到 0 高。
 */
@Composable
private fun MiuixAppPickerDialog(
    title: String,
    multiMode: Boolean,
    state: AppPickerUiState,
    filtered: List<AppInfo>,
    loading: Boolean,
    allVisibleSelected: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    onSelectAll: () -> Unit,
    onConfirmSelected: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        MiuixDialogScrollBody(
            modifier = Modifier.heightIn(max = 520.dp),
            // 列表本身可滚动（LazyColumn 不接受无界高度），正文只做有界收缩
            scrollable = false,
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                    )
                    if (multiMode) {
                        Spacer(Modifier.width(20.dp))
                        MiuixTextButton(
                            text = stringResource(android.R.string.ok),
                            onClick = onConfirmSelected,
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        ) {
            MiuixPickerSearchField(state = state, loading = loading)
            AnimatedVisibility(visible = state.sortFilterOpen) {
                MiuixPickerSortFilterPanel(state = state, loading = loading)
            }
            MiuixPickerStatusRow(
                state = state,
                multiMode = multiMode,
                visibleCount = filtered.size,
                loading = loading,
                allVisibleSelected = allVisibleSelected,
                onSelectAll = onSelectAll,
            )
            MiuixHorizontalDivider(Modifier.padding(top = 6.dp, bottom = 6.dp))
            when {
                loading -> PickerPlaceholder(minHeight = 240.dp) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }

                filtered.isEmpty() -> PickerPlaceholder {
                    MiuixText(
                        text = stringResource(R.string.appinfo_no_match),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                        MiuixPrefItem(index = index, count = filtered.size) {
                            MiuixAppRow(
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
}

/** 两条线共用的空态/加载态占位（有界高度，避免弹层塌成一条） */
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

/** Material 线搜索头：搜索图标 + 清除 + 排序/筛选开关 */
@Composable
private fun MaterialPickerSearchField(state: AppPickerUiState, loading: Boolean) {
    OutlinedTextField(
        value = state.query,
        onValueChange = { state.query = it },
        placeholder = { Text(stringResource(R.string.appinfo_search_hint)) },
        singleLine = true,
        enabled = !loading,
        leadingIcon = {
            Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { state.query = "" }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.appinfo_clear_search),
                        )
                    }
                }
                IconButton(onClick = { state.sortFilterOpen = !state.sortFilterOpen }) {
                    Icon(
                        painterResource(R.drawable.baseline_filter_list_24),
                        contentDescription = stringResource(R.string.appinfo_sort_and_filter),
                        tint = if (state.sortFilterOpen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Miuix 线搜索头：与 Material 线同版式，图标与行件换成 Miuix 原语 */
@Composable
private fun MiuixPickerSearchField(state: AppPickerUiState, loading: Boolean) {
    MiuixTextField(
        value = state.query,
        onValueChange = { state.query = it },
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.appinfo_search_hint),
        useLabelAsPlaceholder = true,
        singleLine = true,
        enabled = !loading,
        leadingIcon = {
            // miuix TextField 把 leading/trailing 图标直接贴边（无内边距），手动补内边距
            MiuixIcon(
                painterResource(R.drawable.ic_baseline_search_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.query.isNotEmpty()) {
                    MiuixIconButton(onClick = { state.query = "" }) {
                        MiuixIcon(
                            painterResource(R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(R.string.appinfo_clear_search),
                        )
                    }
                }
                MiuixIconButton(
                    onClick = { state.sortFilterOpen = !state.sortFilterOpen },
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    MiuixIcon(
                        painterResource(R.drawable.baseline_filter_list_24),
                        contentDescription = stringResource(R.string.appinfo_sort_and_filter),
                    )
                }
            }
        },
    )
}

/** Material 线状态行：共 N 个应用 · 已选 M（多选），右侧全选/取消全选 */
@Composable
private fun MaterialPickerStatusRow(
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
        Text(
            text = stringResource(R.string.appinfo_app_count, visibleCount) +
                if (multiMode) " · " + stringResource(R.string.appinfo_selected_count, state.selected.size) else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (multiMode) {
            TextButton(
                enabled = !loading && visibleCount > 0,
                onClick = onSelectAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(
                    stringResource(
                        if (allVisibleSelected) R.string.appinfo_deselect_all else R.string.appinfo_select_all
                    )
                )
            }
        }
    }
}

/** Miuix 线状态行：与 Material 线同版式 */
@Composable
private fun MiuixPickerStatusRow(
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
        MiuixText(
            text = stringResource(R.string.appinfo_app_count, visibleCount) +
                if (multiMode) " · " + stringResource(R.string.appinfo_selected_count, state.selected.size) else "",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (multiMode) {
            MiuixTextButton(
                text = stringResource(
                    if (allVisibleSelected) R.string.appinfo_deselect_all else R.string.appinfo_select_all
                ),
                onClick = onSelectAll,
                enabled = !loading && visibleCount > 0,
            )
        }
    }
}

/** Material 线内联排序/筛选面板（由搜索头漏斗按钮开合） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialPickerSortFilterPanel(state: AppPickerUiState, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        MaterialPanelLabel(stringResource(R.string.appinfo_sort_by))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSortMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.sortMode == mode,
                    onClick = { state.sortMode = mode },
                    label = { Text(stringResource(mode.labelRes)) },
                    enabled = !loading,
                )
            }
            FilterChip(
                selected = state.reverse,
                onClick = { state.reverse = !state.reverse },
                label = { Text(stringResource(R.string.appinfo_reverse)) },
                enabled = !loading,
            )
        }
        MaterialPanelLabel(stringResource(R.string.appinfo_filter), Modifier.padding(top = 10.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.includeSystem,
                onClick = { state.includeSystem = !state.includeSystem },
                label = { Text(stringResource(R.string.appinfo_system_app)) },
                enabled = !loading,
            )
        }
    }
}

@Composable
private fun MaterialPanelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Miuix 线内联排序/筛选面板：与 Material 线同结构，chip 用同包 [MiuixToggleChip] */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiuixPickerSortFilterPanel(state: AppPickerUiState, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        MiuixPanelLabel(stringResource(R.string.appinfo_sort_by))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSortMode.entries.forEach { mode ->
                MiuixToggleChip(
                    selected = state.sortMode == mode,
                    onClick = { state.sortMode = mode },
                    label = stringResource(mode.labelRes),
                    enabled = !loading,
                )
            }
            MiuixToggleChip(
                selected = state.reverse,
                onClick = { state.reverse = !state.reverse },
                label = stringResource(R.string.appinfo_reverse),
                enabled = !loading,
            )
        }
        MiuixPanelLabel(stringResource(R.string.appinfo_filter), Modifier.padding(top = 10.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiuixToggleChip(
                selected = state.includeSystem,
                onClick = { state.includeSystem = !state.includeSystem },
                label = stringResource(R.string.appinfo_system_app),
                enabled = !loading,
            )
        }
    }
}

@Composable
private fun MiuixPanelLabel(text: String, modifier: Modifier = Modifier) {
    MiuixText(
        text = text,
        modifier = modifier,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

/**
 * Miuix 筛选开关（等价 M3 FilterChip，选中态用 primary 实心按钮）。
 *
 * `internal` 以便同包的 [com.luckyzyx.luckytool.ui.compose.components.ColorPickerDialog]
 * 复用同一实现（取色模式切换行）。
 */
@Composable
internal fun MiuixToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        minWidth = 0.dp,
        minHeight = 32.dp,
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = if (selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        MiuixText(text = label)
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

/** Material 线条目：分段卡片外壳 + 紧凑两行（14sp 应用名 / 12sp 包名），尾部 18dp 圆形勾选 */
@Composable
private fun MaterialAppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    MaterialItemContainer {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect() }
                .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.icon, size = MaterialAppIconSize)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (multiMode) {
                Spacer(Modifier.width(8.dp))
                MaterialCheckIndicator(checked)
            }
        }
    }
}

/** 多选指示：M3 Checkbox 自带 48dp 触摸区，塞进对话框会挤掉包名宽度，这里用 18dp 圆形指示 */
@Composable
private fun MaterialCheckIndicator(checked: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (checked) colors.primary else Color.Transparent)
            .border(1.5.dp, if (checked) colors.primary else colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Miuix 线条目：多选用 CheckboxPreference（勾选在尾部），单选用 BasicComponent，整行可点 */
@Composable
private fun MiuixAppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    val icon: @Composable () -> Unit = { AppIcon(app.icon) }
    if (multiMode) {
        CheckboxPreference(
            title = app.name,
            summary = app.packageName,
            checked = checked,
            onCheckedChange = { onSelect() },
            startAction = icon,
            checkboxLocation = CheckboxLocation.End,
        )
    } else {
        BasicComponent(
            title = app.name,
            summary = app.packageName,
            startAction = icon,
            onClick = onSelect,
        )
    }
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
