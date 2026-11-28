package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable

/** 分组渲染的中性条目：key / visible / content 三元组，两条外观线的 Section 实现共用的入参单元。 */
internal data class PrefCardEntry(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)
