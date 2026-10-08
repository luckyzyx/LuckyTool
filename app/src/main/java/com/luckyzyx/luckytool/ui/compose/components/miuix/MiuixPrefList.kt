package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 线设置行几何常量（对齐《Miuix 共享渲染层契约与差异化清单》§4）。
 *
 * 与 material 线的下列差异是登记过的允许差异，不算缺陷：内圆角 ±2dp（对 M3
 * `ListItemDefaults.segmentedShapes`）、卡片水平内缩 12dp（material 为整宽）、
 * 组间距 12dp（material 为 `Arrangement.spacedBy(2.dp)` + 8dp 分类标题）。
 */
object MiuixPrefDefaults {

    /** 卡片外圆角：单条卡片四角，卡片组首条上端、尾条下端。 */
    val OuterRadius = 16.dp

    /** 卡片组内部相邻条目的圆角。 */
    val InnerRadius = 4.dp

    /** 卡片组内相邻条目之间的垂直间距。 */
    val ItemGap = 2.dp

    /** 卡片组之间的垂直间距（由组内首条承载）。 */
    val GroupGap = 12.dp

    /**
     * 卡片组相对屏幕左右两侧的水平内缩。**由列表级单一拥有**，本文件不再对条目应用：
     * `ExpressivePage` 的列表级 padding 与 `PrefScope.kt` ScopeScreen 路径的
     * `LazyColumn(contentPadding = …)` 各自引用本常量（契约 §4.2 / §10.5）。
     */
    val CardHorizontalInset = 12.dp
}

/**
 * 卡片条目的容器色。
 *
 * @param highlighted 搜索跳转高亮态，取 `surfaceContainerHigh`；普通态取 `surfaceContainer`
 *   （与 miuix `CardDefaults` 一致）。
 */
@Composable
fun MiuixPrefItemColors(highlighted: Boolean = false): Color = if (highlighted) {
    MiuixTheme.colorScheme.surfaceContainerHigh
} else {
    MiuixTheme.colorScheme.surfaceContainer
}

/**
 * 按卡片组内索引计算分段圆角。
 *
 * count <= 1 → 四角 [MiuixPrefDefaults.OuterRadius]；首条上端外圆角 / 下端内圆角；
 * 尾条上端内圆角 / 下端外圆角；中间条四角 [MiuixPrefDefaults.InnerRadius]。
 */
@Composable
fun MiuixPrefItemShape(index: Int, count: Int): Shape = remember(index, count) {
    when {
        count <= 1 -> RoundedCornerShape(MiuixPrefDefaults.OuterRadius)
        index <= 0 -> RoundedCornerShape(
            topStart = MiuixPrefDefaults.OuterRadius,
            topEnd = MiuixPrefDefaults.OuterRadius,
            bottomStart = MiuixPrefDefaults.InnerRadius,
            bottomEnd = MiuixPrefDefaults.InnerRadius,
        )
        index >= count - 1 -> RoundedCornerShape(
            topStart = MiuixPrefDefaults.InnerRadius,
            topEnd = MiuixPrefDefaults.InnerRadius,
            bottomStart = MiuixPrefDefaults.OuterRadius,
            bottomEnd = MiuixPrefDefaults.OuterRadius,
        )
        else -> RoundedCornerShape(MiuixPrefDefaults.InnerRadius)
    }
}

/**
 * Miuix 设置卡片条目的容器：按索引分段圆角 + 卡片底色（**水平内缩不在此处**）。
 *
 * 12dp 水平内缩由**列表级单一拥有**：`ExpressivePage` 的列表级 padding（`:358`），以及
 * `PrefScope.kt` ScopeScreen 路径的 `LazyColumn(contentPadding = …)` start/end —— 条目自身
 * 通栏，避免与列表级叠加成 24dp（契约 §4.2 / §10.5 更正登记；基准 = 卡片距屏 12dp、行内容 28dp）。
 *
 * 用 `Surface` 而非 `Card`：`Card` 只有统一 cornerRadius 且走未解包的
 * `top.yukonga.miuix.kmp.squircle`（契约禁止引用）。垂直间距由调用方通过 [modifier] 传入。
 */
@Composable
fun MiuixPrefItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MiuixPrefItemShape(index, count),
        color = MiuixPrefItemColors(),
    ) {
        content()
    }
}

/**
 * Miuix 设置卡片组的分类标题。
 *
 * 水平内距显式取 16dp（`SmallTitleDefaults.InsideMargin` 默认为 28dp）：卡片组在列表级
 * 另有 12dp 内缩（`MiuixPrefDefaults.CardHorizontalInset`），16 + 12 = 28dp，
 * 与卡片行文字左对齐。
 */
@Composable
fun MiuixPrefCategoryHeader(title: String, modifier: Modifier = Modifier) {
    SmallTitle(
        text = title,
        modifier = modifier,
        textColor = MiuixTheme.colorScheme.onBackgroundVariant,
        insideMargin = PaddingValues(16.dp, 8.dp),
    )
}

/** Miuix 设置卡片组的 DSL 作用域标记。 */
@DslMarker
annotation class MiuixPrefScopeDsl

/**
 * Miuix 设置卡片组的 DSL 作用域：条目以中性三元组（key / visible / content）登记，
 * 渲染与解析分离，与 material 侧 `SegmentedColumnScope` 的 key 语义逐字对齐。
 */
@MiuixPrefScopeDsl
class MiuixPrefScope internal constructor() {

    internal data class Entry(
        val key: Any?,
        val visible: Boolean,
        val content: @Composable () -> Unit,
    )

    internal val entries = mutableListOf<Entry>()

    /** 登记一个条目；`key` 缺省取当前条目数，与 material 侧 `SegmentedColumnScope.item` 一致。 */
    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        entries.add(Entry(key ?: entries.size, visible, content))
    }
}

/**
 * 卡片组的唯一渲染点：可见条目顺序渲染，首条承载组间距，其余承载组内间距。
 *
 * 隐藏条目（`visible = false`）直接跳过（Miuix 线无弹性显隐过渡，属登记差异）；
 * material 侧由 `SegmentedColumn` 的动画路径自行处理，此处不复制解析逻辑。
 */
@Composable
internal fun MiuixPreferenceGroup(
    entries: List<MiuixPrefScope.Entry>,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    val visibleEntries = entries.filter { it.visible }
    if (visibleEntries.isEmpty()) return
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            MiuixPrefCategoryHeader(title)
        }
        visibleEntries.forEachIndexed { index, entry ->
            key(entry.key ?: index) {
                MiuixPrefItem(
                    index = index,
                    count = visibleEntries.size,
                    modifier = Modifier.padding(
                        top = if (index == 0) MiuixPrefDefaults.GroupGap else MiuixPrefDefaults.ItemGap,
                    ),
                ) {
                    entry.content()
                }
            }
        }
    }
}

/** Miuix 设置卡片组（DSL 形态）。 */
@Composable
fun MiuixPrefGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: MiuixPrefScope.() -> Unit,
) {
    MiuixPreferenceGroup(
        entries = MiuixPrefScope().apply(content).entries.toList(),
        modifier = modifier,
        title = title,
    )
}

/** Miuix 设置卡片组（静态条目列表形态）。 */
@Composable
fun MiuixPrefGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    items: List<@Composable () -> Unit>,
) {
    MiuixPreferenceGroup(
        entries = items.mapIndexed { index, item -> MiuixPrefScope.Entry(index, true, item) },
        modifier = modifier,
        title = title,
    )
}
