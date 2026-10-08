package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedColumn

/**
 * 设置卡片组的中性标签接口（渲染缝）。
 *
 * 解析 / 分段 / 索引逻辑只有一份：[item] 只把条目登记成中性三元组（key / visible / content），
 * 主题差异只出现在 [render] 一处。material 线继续委托现有 `SegmentedColumn`，
 * Miuix 线走 [MiuixPrefGroup] 的卡片组渲染器。
 */
interface PrefCardScope {

    /**
     * 登记一个卡片条目。
     *
     * @param key 组合标识；缺省取当前条目数（与 material 侧 `SegmentedColumnScope.item` 一致）。
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

/** 中性条目收集器：两个实现共用，保证 key 语义与条目顺序完全一致。 */
internal class PrefCardEntries {

    val entries = mutableListOf<MiuixPrefScope.Entry>()

    fun add(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        entries.add(MiuixPrefScope.Entry(key ?: entries.size, visible, content))
    }
}

/**
 * material 线的卡片组作用域实现（默认渲染 material 分段卡片组）。
 *
 * 行为与今天 `PrefCards.PrefGroup` 直接调用 `SegmentedColumn` 完全一致：条目顺序、key、
 * 可见性、分组间距与弹性显隐动画都不变。
 */
class MaterialPrefCardScope internal constructor(private val title: String = "") : PrefCardScope {

    private val collected = PrefCardEntries()

    override fun item(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        collected.add(key, visible, content)
    }

    @Composable
    override fun render(preferMiuix: Boolean, modifier: Modifier) {
        PrefCardGroupRenderer(collected.entries, preferMiuix, modifier, title)
    }
}

/** Miuix 线的卡片组作用域实现（默认渲染 Miuix 卡片组）。 */
class MiuixPrefCardScope internal constructor(private val title: String = "") : PrefCardScope {

    private val collected = PrefCardEntries()

    override fun item(key: Any?, visible: Boolean, content: @Composable () -> Unit) {
        collected.add(key, visible, content)
    }

    @Composable
    override fun render(preferMiuix: Boolean, modifier: Modifier) {
        PrefCardGroupRenderer(collected.entries, preferMiuix, modifier, title)
    }
}

/** 渲染缝：唯一按主题分派的位置。 */
@Composable
private fun PrefCardGroupRenderer(
    entries: List<MiuixPrefScope.Entry>,
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

/**
 * material 线渲染：原样重放中性条目到现有 `SegmentedColumn`（动态重载，`SegmentedList.kt:163`）。
 *
 * 重放走 `SegmentedColumnScope.item(key, visible, content)`，与今天 `PrefGroup` 的
 * `SegmentedColumn(modifier, content = content)` 逐条目等价（key 非空，组合标识不变）。
 */
@Composable
private fun MaterialPreferenceGroup(
    entries: List<MiuixPrefScope.Entry>,
    modifier: Modifier,
    title: String,
) {
    SegmentedColumn(modifier = modifier, title = title) {
        val scope = this
        entries.forEach { entry ->
            scope.item(key = entry.key, visible = entry.visible, content = entry.content)
        }
    }
}
