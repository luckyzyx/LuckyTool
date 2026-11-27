package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luckyzyx.luckytool.ui.compose.components.PrefCardEntry

/**
 * material 线渲染：原样重放中性条目到现有 [MaterialGroup]（动态重载，`MaterialList.kt`）。
 *
 * 内缩与分组间距不在这里给：[PrefGroup] 负责 16dp 横向内缩，[MaterialGroup] 只负责
 * 分段卡片与条目不丢 key 的重放。
 */
@Composable
internal fun MaterialPreferenceGroup(
    entries: List<PrefCardEntry>,
    modifier: Modifier = Modifier,
) {
    MaterialGroup(modifier = modifier) {
        entries.forEach { entry ->
            item(key = entry.key, visible = entry.visible, content = entry.content)
        }
    }
}
