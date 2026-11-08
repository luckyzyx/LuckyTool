package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPreferenceGroup

/**
 * 设置卡片组的中性标签接口（渲染缝）。
 *
 * 解析 / 分段 / 索引逻辑只有一份：[item] 只把条目登记成中性三元组（key / visible / content），
 * 主题差异只出现在 [render] 一处。material 线委托 `MaterialGroup`；COUI 线在自己的实现里
 * 把条目逐条渲染成卡片切片。
 */
interface PrefCardScope {

    /**
     * 登记一个卡片条目。
     *
     * @param key 组合标识；缺省取当前条目数（与 material 侧 `MaterialGroupScope.item` 一致）。
     * @param visible 是否可见；material 线保留弹性显隐过渡，不入场时占位随过渡收起。
     * @param content 条目内容。
     */
    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit)

    /** 渲染已登记的条目。 */
    @Composable
    fun render(modifier: Modifier = Modifier)
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

/** 渲染缝：把中性条目交给 material 卡片组容器。 */
@Composable
internal fun PrefCardGroupRenderer(
    entries: List<PrefCardEntry>,
    modifier: Modifier,
    title: String,
) {
    MaterialPreferenceGroup(entries = entries, modifier = modifier, title = title)
}
