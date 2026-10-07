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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.luckyzyx.luckytool.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/**
 * M3 应用选择对话框 —— 旧 AppInfoSelectDialog 的 Compose 等价物。
 *
 * 行为对齐旧实现：加载 `PackageUtils(context.packageManager).getInstalledAppInfos(0)`，
 * 排除 overlay 应用，默认隐藏系统应用；搜索匹配名称/包名；6 种排序 + 倒序；
 * 多选模式底部确认返回，单选模式点击即返回；
 * 已选应用（旧 setEnabledList 逻辑）：预勾选并置顶显示。
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

    var query by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(AppSortMode.Name) }
    var reverse by remember { mutableStateOf(false) }
    var includeSystem by remember { mutableStateOf(showSystemApps) }
    var selected by remember { mutableStateOf(enabledList) }

    val filtered = remember(apps, query, sortMode, reverse, includeSystem, enabledList) {
        val q = query.trim().lowercase()
        apps.asSequence()
            .filter { !it.isOverlay }
            .filter { includeSystem || !it.isSystem }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
            .sortedWith(
                compareByDescending<AppInfo> { it.packageName in enabledList }
                    .then(comparatorOf(sortMode, reverse))
            )
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
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
                        selected = includeSystem,
                        onClick = { includeSystem = !includeSystem },
                        label = { Text(stringResource(R.string.appinfo_system_app)) },
                    )
                    FilterChip(
                        selected = reverse,
                        onClick = { reverse = !reverse },
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
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
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
                            checked = app.packageName in selected,
                            onSelect = {
                                if (multiMode) {
                                    selected = if (app.packageName in selected) {
                                        selected - app.packageName
                                    } else {
                                        selected + app.packageName
                                    }
                                } else {
                                    onConfirm(listOf(app))
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (multiMode) {
                TextButton(
                    enabled = selected.isNotEmpty(),
                    onClick = { onConfirm(filtered.filter { it.packageName in selected }) },
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

@Composable
private fun AppRow(
    app: AppInfo,
    multiMode: Boolean,
    checked: Boolean,
    onSelect: () -> Unit,
) {
    // 与全局 Expressive 分段卡片一致的条目外观
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
