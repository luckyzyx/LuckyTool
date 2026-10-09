package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 线设置行几何常量（对齐《Miuix 共享渲染层契约与差异化清单》§4）。
 *
 * 与 material 线的下列差异是登记过的允许差异，不算缺陷：卡片水平内缩 12dp（material 为整宽）、
 * 组间距 12dp（material 为 8dp 分类标题间距）。组内条目不设内圆角与间距
 * （见 [DesignTokens.ItemInnerRadius] / [DesignTokens.ItemGap]），同一分组渲染为一张连续卡片。
 */
object MiuixPrefDefaults {

    /** 卡片外圆角：单条卡片四角，卡片组首条上端、尾条下端。 */
    val OuterRadius = DesignTokens.CardRadius

    /** 卡片组内部相邻条目的圆角。 */
    val InnerRadius = DesignTokens.ItemInnerRadius

    /** 卡片组内相邻条目之间的垂直间距。 */
    val ItemGap = DesignTokens.ItemGap

    /** 卡片组之间的垂直间距（由组内首条承载）。 */
    val GroupGap = DesignTokens.GroupGap

    /**
     * 卡片组相对屏幕左右两侧的水平内缩。**由列表级单一拥有**，本文件不再对条目应用：
     * `ExpressivePage` 的列表级 padding 与 `PrefScope.kt` ScopeScreen 路径的
     * `LazyColumn(contentPadding = …)` 各自引用本常量（契约 §4.2 / §10.5）。
     */
    val CardHorizontalInset = DesignTokens.CardHorizontalInset
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
 * count <= 1 → 四角 [MiuixPrefDefaults.OuterRadius]；分组首条上端外圆角、尾条下端外圆角（外角取
 * [MiuixPrefDefaults.OuterRadius]）；其余相邻边取 [MiuixPrefDefaults.InnerRadius]（0dp）——
 * 同一分组的条目因此拼成一张连续卡片，而不会各自成卡。
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
 *
 * 垂直方向与 material 侧 `titleSmall` 对齐：`subtitle`（14sp Bold）本无显式行高，
 * 字形贴齐行盒顶部；material 的 `titleSmall` 行高 20sp 且字形在行盒内居中，导致
 * Miuix 侧文字「偏上」。此处显式 `lineHeight = 20.sp` 使字形同样居中，消除偏移。
 *
 * [summary] 对应旧 PreferenceCategory 的 summary：紧随标题之下以 `footnote1` 渲染，
 * 与标题同属一个分类头，不再是单独悬挂的文字条目。
 */
@Composable
fun MiuixPrefCategoryHeader(title: String, summary: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp, 8.dp),
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            style = MiuixTheme.textStyles.subtitle,
            lineHeight = 20.sp,
        )
        if (summary != null) {
            Text(
                text = summary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
    }
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
 * 条目不再被「直接跳过」，而是始终留在组合树里用 [AnimatedVisibility] 折叠/展开，
 * 使 `visible = false → true` 出现时带淡入 + 纵向展开过渡（与 material 侧
 * `SegmentedColumn` 的弹性显隐对齐），避免 AIDL 服务连上后条目突然弹出。
 * 分段圆角仍按「可见索引 / 可见总数」计算，隐藏条目不计入。
 */
@Composable
internal fun MiuixPreferenceGroup(
    entries: List<MiuixPrefScope.Entry>,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    if (entries.none { it.visible }) return
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            MiuixPrefCategoryHeader(title)
        }
        val visibleCount = entries.count { it.visible }
        var visibleIndex = 0
        entries.forEachIndexed { rawIndex, entry ->
            val index = visibleIndex
            if (entry.visible) visibleIndex++
            key(entry.key ?: rawIndex) {
                AnimatedVisibility(
                    visible = entry.visible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    MiuixPrefItem(
                        index = index,
                        count = visibleCount,
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
