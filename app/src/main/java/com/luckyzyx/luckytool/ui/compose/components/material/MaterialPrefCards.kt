package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luckyzyx.luckytool.ui.compose.components.PrefCardEntries
import com.luckyzyx.luckytool.ui.compose.components.PrefCardEntry
import com.luckyzyx.luckytool.ui.compose.components.PrefCardGroupRenderer
import com.luckyzyx.luckytool.ui.compose.components.PrefCardScope

/**
 * material 线的卡片组作用域实现（默认渲染 material 分段卡片组）。
 *
 * 行为与今天 `PrefCards.PrefGroup` 直接调用 `MaterialGroup` 完全一致：条目顺序、key、
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

/**
 * material 线渲染：原样重放中性条目到现有 `MaterialGroup`（动态重载，`MaterialList.kt`）。
 *
 * 重放走 `MaterialGroupScope.item(key, visible, content)`，与今天 `PrefGroup` 的
 * `MaterialGroup(modifier, content = content)` 逐条目等价（key 非空，组合标识不变）。
 */
@Composable
internal fun MaterialPreferenceGroup(
    entries: List<PrefCardEntry>,
    modifier: Modifier,
    title: String,
) {
    MaterialGroup(modifier = modifier, title = title) {
        val scope = this
        entries.forEach { entry ->
            scope.item(key = entry.key, visible = entry.visible, content = entry.content)
        }
    }
}
