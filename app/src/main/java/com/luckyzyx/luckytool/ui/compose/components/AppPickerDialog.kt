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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.itos.coui.checkbox.Checkbox as CouiCheckbox
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiCircularProgressIndicator
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilterChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextButton
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
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

/** 对话框内应用列表的高度上限：两条线的弹层都不给内容加高度上限，由内容自己收敛 */
private val PickerListMaxHeight: Dp = 420.dp

/** 行内图标尺寸：分段卡片行的紧凑图标用 34dp，44dp 供无图标时的校高占位 */
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
 * 底部动作栏。两条线与 [AppPickerUiState] 共用过滤/排序，只在渲染体里分派到
 * Material / Coui 原语；对外签名与调用点零改动。
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

        UiMode.Coui -> CouiAppPickerDialog(
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
                AppSearchBar(
                    query = state.query,
                    onQueryChange = { state.query = it },
                    placeholder = stringResource(R.string.appinfo_search_hint),
                    enabled = !loading,
                    filterActive = state.sortFilterOpen,
                    onFilterClick = { state.sortFilterOpen = !state.sortFilterOpen },
                )
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
                            AppItem(index = index, count = filtered.size) {
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

/**
 * Coui 线弹窗：COUI 弹窗的按钮位与 M3 不同——确认位必填、取消只在取消位。
 * 多选时「确定」是主操作；单选时点条目即返回，按钮位只留「取消」，
 * 因此把取消放进必填的确认位并按次级样式渲染，而不是塞进取消位后空着主位。
 */
@Composable
private fun CouiAppPickerDialog(
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
    CouiAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmAction = if (multiMode) {
            CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onConfirmSelected,
                enabled = !loading,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            )
        } else {
            CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                enabled = !loading,
            )
        },
        dismissAction = if (multiMode) {
            CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
                enabled = !loading,
            )
        } else null,
    ) {
        CouiAppPickerBody(
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
 * Coui 线正文：搜索头 → 可开合的排序筛选面板 → 状态行 → 分隔 → 列表。
 * 正文槽自带滚动且按钮区在它之外，因此列表只约束自身高度上限，
 * 不取权重（父级可滚动时高度无界，权重拿不到有效空间）。
 */
@Composable
private fun CouiAppPickerBody(
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
            // 弹窗正文槽本身不带留白，左右与上下边距由调用方补齐
            .padding(
                start = CouiDialogDefaults.contentPadding,
                end = CouiDialogDefaults.contentPadding,
                top = 6.dp,
                bottom = 16.dp,
            ),
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
            CouiPickerSortFilterPanel(state = state, loading = loading)
        }
        CouiPickerStatusRow(
            state = state,
            multiMode = multiMode,
            visibleCount = filtered.size,
            loading = loading,
            allVisibleSelected = allVisibleSelected,
            onSelectAll = onSelectAll,
        )
        CouiDivider(
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            startIndent = 0.dp,
            endIndent = 0.dp,
        )
        when {
            loading -> PickerPlaceholder {
                // 不传尺寸：控件内部已固定 32dp，外部再套 size 会互相打架
                CouiCircularProgressIndicator()
            }

            filtered.isEmpty() -> PickerPlaceholder {
                CouiText(
                    text = stringResource(R.string.appinfo_no_match),
                    kind = CouiTextKind.Search,
                    textAlign = TextAlign.Start,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PickerListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                    AppItem(index = index, count = filtered.size) {
                        CouiAppRow(
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

/** Coui 线状态行：共 N 个应用 · 已选 M（多选），右侧全选/取消全选 */
@Composable
private fun CouiPickerStatusRow(
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
        CouiText(
            text = stringResource(R.string.appinfo_app_count, visibleCount) +
                if (multiMode) " · " + stringResource(R.string.appinfo_selected_count, state.selected.size) else "",
            kind = CouiTextKind.ItemSummary,
            color = CouiTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (multiMode) {
            CouiTextButton(
                text = stringResource(
                    if (allVisibleSelected) R.string.appinfo_deselect_all else R.string.appinfo_select_all
                ),
                onClick = onSelectAll,
                enabled = !loading && visibleCount > 0,
            )
        }
    }
}

/** Coui 线内联排序/筛选面板（由搜索头漏斗按钮开合） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouiPickerSortFilterPanel(state: AppPickerUiState, loading: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        CouiPanelLabel(stringResource(R.string.appinfo_sort_by))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSortMode.entries.forEach { mode ->
                CouiFilterChip(
                    label = stringResource(mode.labelRes),
                    selected = state.sortMode == mode,
                    onClick = { state.sortMode = mode },
                    enabled = !loading,
                )
            }
            CouiFilterChip(
                label = stringResource(R.string.appinfo_reverse),
                selected = state.reverse,
                onClick = { state.reverse = !state.reverse },
                enabled = !loading,
            )
        }
        CouiPanelLabel(
            text = stringResource(R.string.appinfo_filter),
            modifier = Modifier.padding(top = 10.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CouiFilterChip(
                label = stringResource(R.string.appinfo_system_app),
                selected = state.includeSystem,
                onClick = { state.includeSystem = !state.includeSystem },
                enabled = !loading,
            )
        }
    }
}

/** 面板分组标题：字号取 COUI 的分组标题语义，颜色压到次要文字 */
@Composable
private fun CouiPanelLabel(text: String, modifier: Modifier = Modifier) {
    CouiText(
        text = text,
        kind = CouiTextKind.GroupTitle,
        color = CouiTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Coui 线条目：图标 + 名称/包名 + 勾选框；勾选由整行点击驱动，勾选框自身不装点击 */
@Composable
private fun CouiAppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    val colors = CouiTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            CouiText(
                text = app.name,
                kind = CouiTextKind.ItemTitle,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            CouiText(
                text = app.packageName,
                kind = CouiTextKind.ItemSummary,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (multiMode) {
            Spacer(Modifier.width(8.dp))
            CouiCheckbox(checked = checked)
        }
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
