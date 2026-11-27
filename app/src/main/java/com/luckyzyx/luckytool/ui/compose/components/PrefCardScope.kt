package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable

/**
 * 设置卡片组的条目收集缝：只登记，不画。
 *
 * 调用方（[PrefGroup]）把条目收进 [PrefCardEntries] 后按外观线统一渲染，
 * 因此 key / 可见性语义在两套容器里完全一致。
 */
interface PrefCardScope {

    /**
     * 登记一个卡片条目。
     *
     * @param key 组合标识；缺省取当前条目数 —— 因此**显隐不定的行必须显式给 key**：
     *   缺省序号会在前面的行插入 / 移除时整体后移，行状态（显隐动画、输入框等）会跟着换到别的行上。
     * @param visible 是否可见；收起时随过渡让出高度。
     * @param content 条目内容。
     */
    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit)
}

/** 卡片组中性条目：key / visible / content 三元组（两线共用的登记单元）。 */
internal data class PrefCardEntry(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

/** 中性条目收集器：保证 key 语义与条目顺序只有一份实现。 */
internal class PrefCardEntries {

    val entries = mutableListOf<PrefCardEntry>()

    fun add(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        entries.add(PrefCardEntry(key ?: entries.size, visible, content))
    }
}
