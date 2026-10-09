package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPreferenceGroup
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPreferenceGroup

/**
 * 设置卡片组的中性标签接口（渲染缝）。
 *
 * 解析 / 分段 / 索引逻辑只有一份：[item] 只把条目登记成中性三元组（key / visible / content），
 * 主题差异只出现在 [render] 一处。material 线继续委托现有 `MaterialGroup`，
 * Miuix 线走 [MiuixPrefGroup] 的卡片组渲染器。
 */
interface PrefCardScope {

    /**
     * 登记一个卡片条目。
     *
     * @param key 组合标识；缺省取当前条目数（与 material 侧 `MaterialGroupScope.item` 一致）。
     * @param visible 是否可见；Miuix 线直接跳过不可见条目，material 线保留弹性显隐过渡。
     * @param content 条目内容。
     */
    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit)

    /**
     * 渲染已登记的条目。
     *
     * @param preferMiuix 是否按 Miuix 外观渲染；由调用方从 `LocalUiMode` 求得。
     */
    @Composable
    fun render(preferMiuix: Boolean, modifier: Modifier = Modifier)
}

/** 卡片组中性条目：key / visible / content 三元组（两线共用的登记单元）。 */
internal data class PrefCardEntry(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

/** 中性条目收集器：两个实现共用，保证 key 语义与条目顺序完全一致。 */
internal class PrefCardEntries {

    val entries = mutableListOf<PrefCardEntry>()

    fun add(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        entries.add(PrefCardEntry(key ?: entries.size, visible, content))
    }
}

/** 渲染缝：唯一按主题分派的位置。 */
@Composable
internal fun PrefCardGroupRenderer(
    entries: List<PrefCardEntry>,
    preferMiuix: Boolean,
    modifier: Modifier,
    title: String,
) {
    if (preferMiuix) {
        MiuixPreferenceGroup(entries = entries, modifier = modifier, title = title)
    } else {
        MaterialPreferenceGroup(entries = entries, modifier = modifier, title = title)
    }
}
