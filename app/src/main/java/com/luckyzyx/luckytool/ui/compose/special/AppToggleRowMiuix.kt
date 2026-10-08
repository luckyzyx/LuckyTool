package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import top.yukonga.miuix.kmp.basic.Switch

/**
 * Miuix 线的「应用行」（图标 + 应用名 + 包名 + 只读开关，整行点击切换）。
 *
 * P4 特殊页的 Miuix 分支共享这一份行声明（ZoomWindowPage / MultiAppPage 等），
 * 写值语义与各自 Material 线的 `SupportAppRow` 完全一致：
 * `HapticFeedbackType.VirtualKey` 触感 → `onToggle(!enabled)` → 调用方写 prefs 并 `sendPrefsKey`。
 */
@Composable
internal fun AppToggleRowMiuix(
    info: AppInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
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
}
