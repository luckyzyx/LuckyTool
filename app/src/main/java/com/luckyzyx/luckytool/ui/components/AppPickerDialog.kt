@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.components

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference

/** AppInfo 排序模式（与旧 AppInfoSelectDialog 的 sortMode 对齐） */
enum class AppSortMode(@StringRes val labelRes: Int) {
    Name(R.string.appinfo_app_name),
    Package(R.string.appinfo_package_name),
    Size(R.string.appinfo_app_size),
    InstallTime(R.string.appinfo_install_time),
    LastInstallTime(R.string.appinfo_last_updated_time),
    Target(R.string.appinfo_target_sdk),
}

/** Drawable → Painter（AppInfo.icon 为平台 Drawable，转位图渲染，避免引入图片库） */
@Composable
fun rememberAppIconPainter(icon: Drawable?): Painter? = remember(icon) {
    icon?.let { drawable ->
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = createBitmap(width, height)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bmp
        }
        BitmapPainter(bitmap.asImageBitmap())
    }
}

/** 应用选择对话框内部状态（搜索 / 排序 / 系统应用开关 / 多选集合），两条外观线共用。 */
private class AppPickerUiState(
    includeSystemApps: Boolean,
    enabledPackages: Set<String>,
) {
    var query by mutableStateOf("")
    var sortMode by mutableStateOf(AppSortMode.Name)
    var reverse by mutableStateOf(false)
    var includeSystem by mutableStateOf(includeSystemApps)
    var selected by mutableStateOf(enabledPackages)

    fun toggle(packageName: String) {
        selected = if (packageName in selected) selected - packageName else selected + packageName
    }
}

/**
 * 应用选择对话框 —— 旧 AppInfoSelectDialog 的 Compose 等价物。
 *
 * 行为对齐旧实现：加载 `PackageUtils(context.packageManager).getInstalledAppInfos(0)`，
 * 排除 overlay 应用，默认隐藏系统应用；搜索匹配名称/包名；6 种排序 + 倒序；
 * 多选模式底部确认返回，单选模式点击即返回；
 * 已选应用（旧 setEnabledList 逻辑）：预勾选并置顶显示。
 *
 * 外观按 [LocalUiMode] 分派：Miuix 线走 Miuix 弹层（OverlayDialog + Miuix 行件），
 * Material 线行为不变；对外签名与调用点零改动。Miuix 弹层统一由根部 Miuix Scaffold
 * 的默认 popup host 承载，本组件不自装 host、不传 `renderInRootScaffold = false`。
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
    val apps by produceState<List<AppInfo>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            PackageUtils(context.packageManager).getInstalledAppInfos(0)
        }
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

    // 单选模式点击即返回；多选模式切换勾选
    val onSelect: (AppInfo) -> Unit = { app ->
        if (multiMode) state.toggle(app.packageName) else onConfirm(listOf(app))
    }
    val onConfirmSelected: () -> Unit = {
        onConfirm(filtered.filter { it.packageName in state.selected })
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixAppPickerDialog(
            title = title,
            multiMode = multiMode,
            state = state,
            filtered = filtered,
            onDismiss = onDismiss,
            onSelect = onSelect,
            onConfirmSelected = onConfirmSelected,
        )

        UiMode.Material -> MaterialAppPickerDialog(
            title = title,
            multiMode = multiMode,
            state = state,
            filtered = filtered,
            onDismiss = onDismiss,
            onSelect = onSelect,
            onConfirmSelected = onConfirmSelected,
        )
    }
}

/** Material 线：M3 AlertDialog（本任务不改行为，只把状态提到 [AppPickerUiState]） */
@Composable
private fun MaterialAppPickerDialog(
    title: String,
    multiMode: Boolean,
    state: AppPickerUiState,
    filtered: List<AppInfo>,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    onConfirmSelected: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { state.query = it },
                    placeholder = { Text(stringResource(R.string.appinfo_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.includeSystem,
                        onClick = { state.includeSystem = !state.includeSystem },
                        label = { Text(stringResource(R.string.appinfo_system_app)) },
                    )
                    FilterChip(
                        selected = state.reverse,
                        onClick = { state.reverse = !state.reverse },
                        label = { Text("倒序") },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.sortMode == mode,
                            onClick = { state.sortMode = mode },
                            label = { Text(stringResource(mode.labelRes)) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            multiMode = multiMode,
                            checked = app.packageName in state.selected,
                            onSelect = { onSelect(app) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (multiMode) {
                TextButton(
                    enabled = state.selected.isNotEmpty(),
                    onClick = onConfirmSelected,
                ) { Text(stringResource(android.R.string.ok)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
        dismissButton = {
            if (multiMode) {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}

/**
 * Miuix 线：OverlayDialog（根部 popup host 承载）+ Miuix 行件。
 *
 * 内容列高度上限 520.dp：OverlayDialog 在小屏为底部贴合展示且不给内容加高度上限，
 * 因此由内容自己收敛高度，列表用 `weight(1f, fill = false)` 吃掉剩余空间。
 */
@Composable
private fun MiuixAppPickerDialog(
    title: String,
    multiMode: Boolean,
    state: AppPickerUiState,
    filtered: List<AppInfo>,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    onConfirmSelected: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
        ) {
            MiuixTextField(
                value = state.query,
                onValueChange = { state.query = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.appinfo_search_hint),
                useLabelAsPlaceholder = true,
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixToggleChip(
                    selected = state.includeSystem,
                    onClick = { state.includeSystem = !state.includeSystem },
                    label = stringResource(R.string.appinfo_system_app),
                )
                MiuixToggleChip(
                    selected = state.reverse,
                    onClick = { state.reverse = !state.reverse },
                    label = "倒序",
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSortMode.entries.forEach { mode ->
                    MiuixToggleChip(
                        selected = state.sortMode == mode,
                        onClick = { state.sortMode = mode },
                        label = stringResource(mode.labelRes),
                    )
                }
            }
            MiuixHorizontalDivider(Modifier.padding(top = 12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .weight(1f, fill = false),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        MiuixAppRow(
                            app = app,
                            multiMode = multiMode,
                            checked = app.packageName in state.selected,
                            onSelect = { onSelect(app) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (multiMode) {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    MiuixTextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = onConfirmSelected,
                        enabled = state.selected.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                } else {
                    MiuixTextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Miuix 筛选开关（等价 M3 FilterChip，选中态用 primary 实心按钮） */
@Composable
private fun MiuixToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    Button(
        onClick = onClick,
        minWidth = 0.dp,
        minHeight = 32.dp,
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = if (selected) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
    ) {
        MiuixText(text = label)
    }
}

/** Material 线条目：与全局 Expressive 分段卡片一致的条目外观 */
@Composable
private fun AppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    SegmentedListItem(
        onClick = { onSelect() },
        headlineContent = { Text(app.name) },
        supportingContent = { Text(app.packageName) },
        leadingContent = {
            val painter = rememberAppIconPainter(app.icon)
            if (painter != null) {
                Image(painter, contentDescription = null, modifier = Modifier.size(40.dp))
            } else {
                Box(Modifier.size(40.dp))
            }
        },
        trailingContent = {
            if (multiMode) {
                Checkbox(checked = checked, onCheckedChange = { onSelect() })
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Miuix 线条目：多选用 CheckboxPreference（勾选在尾部），单选用 BasicComponent，整行可点 */
@Composable
private fun MiuixAppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    val icon: @Composable () -> Unit = {
        val painter = rememberAppIconPainter(app.icon)
        if (painter != null) {
            Image(painter, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Box(Modifier.size(40.dp))
        }
    }
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
