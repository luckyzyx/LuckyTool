@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.compose.components

import android.content.Context

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.InputDialog as CouiInputDialog
import com.itos.coui.layout.CouiPageColors
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppDropdownItem
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPullToRefreshBox
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.components.material.materialGroupColors
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PrefState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * 页面顶栏 inset 注入点（唯一机制）：自带 LazyColumn 的页面用它作为 `contentPadding` 的 top，
 * 让内容能滚到顶栏之下（scroll-under）而不被首行遮挡。
 *
 * 默认 `0.dp` → 未提供者零影响（material 分支不做任何条件判断）。
 * **provide 方是页面骨架**（`ui/compose/FunctionPage.kt` 把顶栏高度 provide 下来、
 * 并把外置 padding 的 top 置 0）；本文件只声明，不 provide。
 * 除本机制外不要再补任何 padding 去解决首行遮挡（会与它叠加）。
 */
internal val LocalScopeTopInset = compositionLocalOf { 0.dp }

/**
 * 页面列表的 [LazyListState]（[ScopeScreen] 里那个 LazyColumn 的状态）。
 *
 * 由 [ScopeScreen] provide；页面内容可用它实现需要主动滚动的交互（例如拖拽排序时
 * 「拖到列表上下边缘自动滚动」）。不在页面列表内时为 null，调用点必须做空值回落。
 */
internal val LocalScopeListState = compositionLocalOf<LazyListState?> { null }

/** 搜索跳转目标：position 为 LazyColumn 槽位（与 [PrefIndexItem.slot] 对应） */
data class ScrollTarget(val key: String, val position: Int)

/**
 * 单个偏好项的搜索索引条目（Function 页全局搜索由 [PrefScopeBuilder.snapshotIndex] 收集，
 * key/title/summary 参与过滤，pageTarget 非空则命中即导航）。
 */
data class PrefIndexItem(
    val key: String,
    val title: String?,
    val summary: String?,
    val visible: () -> Boolean = { true },
    val slot: Int,
    val pageTarget: String? = null,
)

/**
 * 渲染单元：一个 LazyColumn item 槽位。
 *
 * [segIndex]/[segCount] 用于同一分组的条目之间保持组内间距（每条各自一张 16dp 圆角卡片，
 * 组与组之间用 [com.luckyzyx.luckytool.ui.theme.DesignTokens.GroupGap] 拉开层级）；
 * [groupable] = false 的条目独占一张卡片（[PrefScopeBuilder.group] 的起止标记、`custom(raw = true)` 的页面级 body）。
 * [raw] = true 的条目完全裸渲染，不套任何卡片容器。
 * [groupStart]/[groupBreak] 是 [PrefScopeBuilder.group] 发出的分组起止标记：只参与分块计算，
 * 两个线上都不占高度（material 线借结束标记的 [sectionFooter] 画卡片下方那行说明）。
 */
internal class PrefEntry(
    val slot: Int,
    val itemKey: String,
    val indexKey: String?,
    val indexTitle: String?,
    val indexSummary: String?,
    val pageTarget: String?,
    val groupable: Boolean,
    /** 自身永不进卡片：页面级整页内容、组内不参与卡片体的 body，按顺序裸渲染 */
    val raw: Boolean,
    /** [PrefScopeBuilder.custom] 的任意内容槽：组内当普通行，组外裸渲染 */
    val custom: Boolean = false,
    val groupBreak: Boolean = false,
    /** 分组起始标记：先切块，再把自己当成块的标题（标题与说明都为空时不作标题） */
    val groupStart: Boolean = false,
    /** 分组标题：画在卡片上方，两条线都走 [PrefCategoryHeader] */
    val sectionTitle: String? = null,
    /** 分组说明：与标题同组画在卡片上方（标题在上、说明在下） */
    val sectionSummary: String? = null,
    /** 分组底部说明：COUI 线画在卡片下方，material 线由结束标记自己画 */
    val sectionFooter: String? = null,
    /**
     * 该行参与列表增删动画（需要 [itemKey] 稳定，默认关闭）。
     * COUI 线的行并进同一个分组块后不再是独立 item，动画只在整块粒度生效。
     */
    val animate: Boolean,
    /**
     * 该行是否显示（默认显示）。false 时行原地收起、true 时原地展开，两条线都是行级动画。
     *
     * 条件行用 [PrefScopeBuilder.click] 的 `visible` 表达，不要再用 `if (条件) { click(…) }` 包起来：
     * 那样条目会整块增删，COUI 线的行并在一张卡片里，只有整块卡片能跟着动。
     */
    val visible: Boolean,
    val render: @Composable (slot: Int) -> Unit,
) {
    var segIndex: Int = 0
    var segCount: Int = 1
}

/**
 * COUI 线的分组块：一个 LazyColumn item 承载 [CouiPrefGroup] 画出的整张卡片。
 *
 * [head] 是块首的分组起始标记或裸条目（[PrefScopeBuilder.group]、`custom(raw = true)`、组外的 [PrefScopeBuilder.custom]）：
 * 有 [rows] 时它是卡片的标题，没有 [rows] 时它自己裸渲染在卡片之外。
 * [rows] 是块内成组的选项行，行间由 [CouiDivider] 切分。
 * [footer] 是卡片下方那行说明（[PrefScopeBuilder.group] 的 footer）。
 */
internal class PrefBlock(
    val itemKey: String,
    val head: PrefEntry?,
    val rows: List<PrefEntry>,
    val footer: String? = null,
)

/**
 * 搜索跳转命中的行底蒙层色：与手动长按同一按压蒙层色（浅色 0x1F000000 / 深色 0x33FFFFFF），
 * 叠在卡片底色上保持不透明，避免透出滚动内容。
 */
@Composable
internal fun couiHighlightOverlay(card: Color): Color {
    val dark = CouiTheme.isDark
    return Color(if (dark) 0x33FFFFFF else 0x1F000000).compositeOver(card)
}

/**
 * COUI 整卡容器：一段内容自成一张卡片。
 *
 * 只给「不成组」的内容用（[PrefCard] / [PrefSwitchCard]）：卡片前后留白由调用方给出，
 * 这里只提供 16dp 横向内缩、24dp 平滑圆角与卡片底色。成组内容用 [CouiPrefGroup]
 * ——它自带标题、说明、底部说明与分组前后留白，不要再用本容器拼分组卡片。
 */
@Composable
internal fun CouiCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CouiPageDefaults.themeColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CouiPageDefaults.horizontalMargin)
            .clip(CouiPageDefaults.shape)
            .background(colors.card),
        content = content,
    )
}

/**
 * COUI 卡片里的一行：key / 可见性 / 可选的整行底色（搜索跳转高亮）/ 行内容。
 *
 * 只是行的描述，画法统一交给 [CouiRowsColumn]。
 */
internal class PrefCardRow(
    val key: Any? = null,
    val visible: Boolean = true,
    val background: Color? = null,
    val content: @Composable () -> Unit,
)

/**
 * COUI 卡片内的行序列：按可见顺序画分隔线，行级显隐原地展开/收起。
 *
 * **COUI 线唯一的分行实现**：DSL 分组块（[ScopeScreen]）与页面自绘分组（[PrefGroup]）
 * 都交给它，分隔线位置、显隐动画与跳转高亮因此不会各写一份。
 * 卡片本身（内缩、圆角、底色）由外层 [CouiPrefGroup] / [CouiCard] 提供。
 */
@Composable
internal fun CouiRowsColumn(rows: List<PrefCardRow>) {
    var visibleRows = 0
    rows.forEachIndexed { rawIndex, row ->
        val hasDivider = visibleRows > 0
        if (row.visible) visibleRows++
        key(row.key ?: rawIndex) {
            AnimatedVisibility(
                visible = row.visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (hasDivider) CouiDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (row.background != null) {
                                    Modifier.background(row.background)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        // 卡片由分组画：行内容据此不再自绘卡片（见 AppKit 的 CouiRowFrame）
                        CompositionLocalProvider(LocalCouiRowFramed provides true) {
                            row.content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * COUI 分组卡片：[PrefScopeBuilder.group] 的容器实现。
 *
 * 卡片内外三段文字各归其位：标题与说明画在卡片上方（标题一行、说明一行，与库 Group 的标题行同档：
 * 12dp 分组上沿、32dp 水平内缩、[CouiTextKind.GroupTitle]，说明用 GroupFooter 样式），
 * [footer] 画在卡片下方（与标题下方那行说明同版式：32dp 水平内缩 + 上下 8dp）。
 *
 * 分组上沿留白也在这里给：有标题走 [CouiPageDefaults.categoryTopSpacing]，无标题走
 * [CouiPageDefaults.groupSpacing] —— 分组之间的垂直间距只由分组自己给，页面不再自己排（见 [PrefGroup]）。
 */
@Composable
internal fun CouiPrefGroup(
    title: String?,
    summary: String?,
    footer: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasHeader = !title.isNullOrEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (hasHeader) CouiPageDefaults.categoryTopSpacing else CouiPageDefaults.groupSpacing,
            ),
    ) {
        if (title.isNullOrEmpty()) {
            // 说明没有标题可挂时单独成行，与标题下方的说明同一版式
            if (!summary.isNullOrEmpty()) PrefGroupFooter(summary)
        } else {
            PrefCategoryHeader(title = title, summary = summary)
        }
        CouiCard(content = content)
        if (!footer.isNullOrEmpty()) PrefGroupFooter(footer)
    }
}

/**
 * 分类标题 + 可选说明：**全应用唯一的分组标题版式**，[CouiPrefGroup] 与页面自绘内容
 * （如应用权限页）都走这里。版式本身在 [PrefStyle.CategoryHeader] 的两个实现里，本函数只做转发，
 * 所以新增外观样式时不需要改这里。
 *
 * [materialInset] 只影响 material 线：宿主的列表/行本身没有水平 contentPadding，所以默认自带 16dp；
 * 宿主已经提供过 16dp（例如 `custom()` 里的 [PrefRowInset]）就传 0dp，避免叠成 32dp。
 * COUI 线的 32dp 是卡片内缩 + 行内缩的固定和，不受它影响。
 */
@Composable
internal fun PrefCategoryHeader(
    title: String,
    summary: String? = null,
    materialInset: Dp = 16.dp,
) = currentPrefStyle().CategoryHeader(title = title, summary = summary, materialInset = materialInset)

/** 分组底部说明：画在卡片下方，与标题下方那行说明同一档字号与内缩，版式在 [PrefStyle.GroupFooter] */
@Composable
internal fun PrefGroupFooter(text: String) = currentPrefStyle().GroupFooter(text)

/** COUI 行的标题文字（16 号 ItemTitle）：禁用态改用 colors.disabled；[maxLines] 用来限制长标题占用的行数 */
@Composable
internal fun CouiRowTitle(
    text: String,
    enabled: Boolean,
    colors: CouiPageColors,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    CouiText(
        text = text,
        kind = CouiTextKind.ItemTitle,
        color = if (enabled) colors.title else colors.disabled,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** COUI 行的说明文字（13 号 ItemSummary）：禁用态改用 colors.disabled；[maxLines] 用来限制长说明占用的行数 */
@Composable
internal fun CouiRowSummary(
    text: String,
    enabled: Boolean,
    colors: CouiPageColors,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    CouiText(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = if (enabled) colors.summary else colors.disabled,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * COUI 行尾当前值（14 号 ItemValue，右对齐）：
 * 右缘与行尾对齐，宽度上限 162dp 以免长文本把标题挤没。
 */
@Composable
internal fun CouiRowValue(text: String, enabled: Boolean, colors: CouiPageColors) {
    CouiText(
        modifier = Modifier.padding(start = 16.dp).widthIn(max = 162.dp),
        text = text,
        kind = CouiTextKind.ItemValue,
        color = if (enabled) colors.summary else colors.disabled,
        textAlign = TextAlign.End,
    )
}

/** COUI 行尾次级图标色（导航箭头、下拉箭头共用，弱于正文前景） */
internal fun couiTrailingIconTint(dark: Boolean): Color =
    if (dark) Color(0x4DFFFFFF) else Color(0x42000000)

/**
 * 偏好页声明式 DSL：用普通 lambda 声明分组、开关、列表、滑条、输入与页面入口。
 *
 * 用法：
 * ```
 * ScopeScreen(state, sendValue = { k, v -> context.sendPrefsValue(pack, k, v) }) {
 *     group(title = "基础", summary = "整组说明", footer = "整组脚注") {
 *         switch("key1", "标题", "摘要")
 *         if (state.getBoolean("master", false)) {   // 条件可见性：任意偏好变更自动重求值
 *             slider("key2", "滑条", 1..100)
 *         }
 *     }
 *     page("其他页面", "statusbar_clock")   // 组外条目自成一块
 * }
 * ```
 *
 * 构建 lambda 是普通（非 Composable）函数：每次重组重跑（相当于“重进页面刷新”，
 * 但自动触发），因此条件判断用 PrefState 同步 getter 即天然响应式。搜索索引同样来自构建结果：
 * Function 页可不在 UI 中渲染，直接用同一 lambda 收集 [PrefIndexItem]。
 *
 * 写值语义：`state.set(key, value)` 先落 SharedPreferences（host 进程 hook 经
 * remote prefs 同源可见），再经 [sendValue] 触发 `Context.sendPrefsValue(packName, key, value)`。
 */
class PrefScopeBuilder internal constructor(
    internal val state: PrefState,
) {
    internal var sendValue: (key: String, value: Any) -> Unit = { _, _ -> }
    internal var navigate: ((target: String, title: String?) -> Unit)? = null
    internal var context: Context? = null
    internal val highlightSlot = mutableStateOf<Int?>(null)

    internal val entries = ArrayList<PrefEntry>()

    /** COUI 线的分组块（[computeSegments] 里随条目一起算好），material 线不使用 */
    internal var blocks: List<PrefBlock> = emptyList()

    /** 每次重建前清空（ScopeScreen 每次重组前调用；外部索引收集同样先调用） */
    fun beginBuild() {
        entries.clear()
    }

    /** 当前构建轮次的搜索索引快照（收起的行不进索引：搜到也跳不到一个没有高度的槽位） */
    fun snapshotIndex(): List<PrefIndexItem> = entries.mapNotNull { entry ->
        if (!entry.visible) return@mapNotNull null
        entry.indexKey?.let { key ->
            PrefIndexItem(
                key,
                entry.indexTitle,
                entry.indexSummary,
                { true },
                entry.slot,
                entry.pageTarget
            )
        }
    }

    /**
     * 当前高亮行的容器底色：命中的槽位由 [PrefStyle.highlightContainer] 给出，
     * COUI 线的高亮由分组块里的行蒙层绘制（见 couiHighlightOverlay），实现对象返回透明。
     */
    @Composable
    internal fun highlightColor(slot: Int): Color =
        currentPrefStyle().highlightContainer(active = highlightSlot.value == slot)

    /** 高亮（搜索跳转命中）时把卡片容器换成 primaryContainer */
    @Composable
    internal fun itemColors(slot: Int): ListItemColors {
        val bg = highlightColor(slot)
        return if (bg == Color.Transparent) {
            materialGroupColors()
        } else {
            materialGroupColors(containerColor = bg)
        }
    }

    private fun emit(
        itemKey: String?,
        indexKey: String?,
        indexTitle: String?,
        indexSummary: String?,
        pageTarget: String? = null,
        groupable: Boolean = true,
        raw: Boolean = false,
        custom: Boolean = false,
        groupBreak: Boolean = false,
        groupStart: Boolean = false,
        sectionTitle: String? = null,
        sectionSummary: String? = null,
        sectionFooter: String? = null,
        animate: Boolean = false,
        visible: Boolean = true,
        render: @Composable (slot: Int) -> Unit,
    ) {
        val slot = entries.size
        entries += PrefEntry(
            slot = slot,
            itemKey = itemKey ?: "slot-$slot",
            indexKey = indexKey,
            indexTitle = indexTitle,
            indexSummary = indexSummary,
            pageTarget = pageTarget,
            groupable = groupable,
            raw = raw,
            custom = custom,
            groupBreak = groupBreak,
            groupStart = groupStart,
            sectionTitle = sectionTitle,
            sectionSummary = sectionSummary,
            sectionFooter = sectionFooter,
            animate = animate,
            visible = visible,
            render = render,
        )
    }

    /**
     * 计算分组：连续且 [PrefEntry.groupable] 的条目合并为一组。
     *
     * 分组起止标记与页面级裸 body（`custom(raw = true)`）都是 groupable = false，各自把相邻条目切开；
     * 滑条、自定义控件等选项行一律并入所在分组卡片。
     */
    internal fun computeSegments() {
        var i = 0
        while (i < entries.size) {
            // 收起的行不算分段成员：它不占高度，相邻两行要各自拿到卡片端点（圆角/组间距）
            if (!entries[i].groupable || !entries[i].visible) {
                entries[i].segIndex = 0
                entries[i].segCount = 1
                i++
                continue
            }
            var j = i
            while (j + 1 < entries.size && entries[j + 1].groupable && entries[j + 1].visible) j++
            val count = j - i + 1
            for (k in i..j) {
                entries[k].segIndex = k - i
                entries[k].segCount = count
            }
            i = j + 1
        }
        computeBlocks()
    }

    /**
     * 按块切开 COUI 线的条目：[group] 的起始标记开一块、结束标记收尾，标记之间的行就是这块的卡片内容。
     *
     * [PrefScopeBuilder.custom] 在组内是普通行，在组外裸渲染（不与相邻条目拼卡，也不套卡片）；
     * 声明了 [PrefEntry.raw] 的条目与没有行的分组标题各自独占一块。
     */
    private fun computeBlocks() {
        val result = ArrayList<PrefBlock>()
        var head: PrefEntry? = null
        var rows = ArrayList<PrefEntry>()
        var inGroup = false

        fun flush() {
            if (head == null && rows.isEmpty()) return
            result += PrefBlock(head?.itemKey ?: rows.first().itemKey, head, rows)
            head = null
            rows = ArrayList()
        }

        entries.forEach { entry ->
            when {
                entry.groupStart -> {
                    flush()
                    inGroup = true
                    if (entry.sectionTitle != null || entry.sectionSummary != null) head = entry
                }
                // 结束标记：footer 归它之前那一块，块本身空着就整块丢弃
                entry.groupBreak -> {
                    if (head != null || rows.isNotEmpty()) {
                        result += PrefBlock(head?.itemKey ?: rows.first().itemKey, head, rows, entry.sectionFooter)
                    }
                    head = null
                    rows = ArrayList()
                    inGroup = false
                }
                // 不进卡片的条目：页面级整页内容、没有行的分组标题
                entry.raw -> {
                    flush()
                    result += PrefBlock(entry.itemKey, entry, emptyList())
                }
                // 组外的 custom 不是行：裸渲染，不和相邻条目拼成无标题卡片
                entry.custom && !inGroup -> {
                    flush()
                    result += PrefBlock(entry.itemKey, entry, emptyList())
                }
                !entry.groupable -> {
                    flush()
                    result += PrefBlock(entry.itemKey, null, listOf(entry))
                }
                else -> rows += entry
            }
        }
        flush()
        blocks = result
    }

    /** 条目槽位 → COUI 线列表 item 下标（滚动定位用；找不到返回 -1） */
    internal fun blockIndexOf(slot: Int): Int = blocks.indexOfFirst { block ->
        block.head?.slot == slot || block.rows.any { it.slot == slot }
    }

    // ---------------- DSL 项 ----------------

    /**
     * 一张分组卡片：卡片里的行由 [content] 直接声明，行间分隔线由渲染层自动插，不用手写 divider。
     *
     * [title]/[summary] 画在卡片上方（标题一行、说明一行，见 [PrefCategoryHeader]），
     * [footer] 画在卡片下方；三个都可以省略，全省略就是一张无标题卡片。
     * 卡片之间的留白由渲染层统一给，不要再自己加 padding。
     */
    fun group(
        title: String? = null,
        summary: String? = null,
        footer: String? = null,
        content: PrefScopeBuilder.() -> Unit,
    ) {
        emit(
            null, null, null, null,
            groupable = false,
            raw = true,
            groupStart = true,
            sectionTitle = title,
            sectionSummary = summary,
        ) { _ ->
            // 仅 material 线会走到这里（COUI 线由 CouiPrefGroup 画标题）；没标题就没有可画的内容
            if (!title.isNullOrEmpty()) PrefCategoryHeader(title = title, summary = summary)
        }
        content()
        emit(
            null, null, null, null,
            groupable = false,
            raw = true,
            groupBreak = true,
            sectionFooter = footer?.takeIf { it.isNotEmpty() },
        ) { _ ->
            // 同理：只有 material 线需要自己画卡片下方这行说明
            if (!footer.isNullOrEmpty()) PrefGroupFooter(footer)
        }
    }

    /** 开关（对应 SwitchPreference + setOnPreferenceChangeListener）：整行点击 + Expressive 开关 */
    fun switch(
        key: String,
        title: String,
        summary: String? = null,
        default: Boolean = false,
        enabled: Boolean = true,
        notify: Boolean = false,
        leading: (@Composable () -> Unit)? = null,
        beforeApply: ((Boolean, (Boolean) -> Unit) -> Unit)? = null,
        onChange: ((Boolean) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val checked by state.booleanFlow(key, default).collectAsStateWithLifecycle()
        fun apply(newValue: Boolean) {
            state.set(key, newValue)
            if (notify) sendValue(key, newValue)
            onChange?.invoke(newValue)
        }
        // beforeApply 允许在落盘前拦截（如生物识别验证通过才写入）；不提供时直接落盘
        val onCheckedChange: (Boolean) -> Unit = if (beforeApply != null) {
            { newValue -> beforeApply(newValue, ::apply) }
        } else {
            ::apply
        }
        PrefRowInset {
            PrefSwitchRow(
                title = title,
                checked = checked,
                summary = summary,
                enabled = enabled,
                leading = leading,
                onCheckedChange = onCheckedChange,
                colors = itemColors(slot),
            )
        }
    }

    /**
     * 状态驱动的开关：值和可用性都从页面自己的状态取，不读写 prefs。
     *
     * 用于真相不在 prefs 的页面（系统 Settings 键、IPC 查询结果等）：[checked]/[enabled]
     * 在渲染时求值，页面状态一变行就跟着重画；[onChange] 只负责把请求交给页面。
     * 键值版的开关用上面那个 [switch] 重载。
     */
    fun switch(
        title: String,
        checked: () -> Boolean,
        summary: String? = null,
        enabled: () -> Boolean = { true },
        leading: (@Composable () -> Unit)? = null,
        itemKey: String? = null,
        visible: Boolean = true,
        onChange: (Boolean) -> Unit,
    ) = emit(itemKey, null, title, summary, visible = visible) { slot ->
        PrefRowInset {
            PrefSwitchRow(
                title = title,
                checked = checked(),
                summary = summary,
                enabled = enabled(),
                leading = leading,
                onCheckedChange = onChange,
                colors = itemColors(slot),
            )
        }
    }

    /** 复选框（多选项）：整行切换，行尾为复选框；互斥的单个值用 [select]，条目多到该折叠时用 [list] */
    fun check(
        key: String,
        title: String,
        summary: String? = null,
        default: Boolean = false,
        enabled: Boolean = true,
        notify: Boolean = false,
        leading: (@Composable () -> Unit)? = null,
        onChange: ((Boolean) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val checked by state.booleanFlow(key, default).collectAsStateWithLifecycle()
        PrefRowInset {
            PrefCheckRow(
                title = title,
                checked = checked,
                summary = summary,
                enabled = enabled,
                leading = leading,
                onCheckedChange = { newValue ->
                    state.set(key, newValue)
                    if (notify) sendValue(key, newValue)
                    onChange?.invoke(newValue)
                },
                colors = itemColors(slot),
            )
        }
    }

    /**
     * 单选行组（几个互斥选项平铺成行，选中的那行行首为单选按钮）。
     *
     * 存储与 [list] 同一个键、同一套 [entries] / [entryValues]：只有两三个选项、
     * 直接把候选摆出来比「点开菜单再选」更快时用本函数，条目多就让 [list] 收进菜单。
     * 每行一个条目，行间分隔线与卡片由所在分组给。
     */
    fun select(
        key: String,
        entries: Array<String>,
        entryValues: Array<String>,
        default: String = "",
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((String) -> Unit)? = null,
    ) {
        entries.forEachIndexed { index, label ->
            val entryValue = entryValues.getOrNull(index) ?: return@forEachIndexed
            emit("${key}#$entryValue", null, label, null) { slot ->
                val current by state.stringFlow(key, default).collectAsStateWithLifecycle()
                PrefRowInset {
                    PrefSelectRow(
                        title = label,
                        selected = current == entryValue,
                        enabled = enabled,
                        onClick = {
                            state.set(key, entryValue)
                            if (notify) sendValue(key, entryValue)
                            onChange?.invoke(entryValue)
                        },
                        colors = itemColors(slot),
                    )
                }
            }
        }
    }

    /** 单选列表（对应 DropDownPreference：条目文字 + entryValues 存储值），尾部显示当前值 */
    fun list(
        key: String,
        title: String,
        entries: Array<String>,
        entryValues: Array<String>,
        default: String = "",
        summary: String? = null,
        leading: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((String) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val current by state.stringFlow(key, default).collectAsStateWithLifecycle()
        val currentLabel = entries.getOrNull(entryValues.indexOf(current)) ?: current
        val shownSummary = summary?.replace("%s", currentLabel)
        PrefRowInset {
            AppDropdownItem(
                title = title,
                summary = shownSummary,
                items = entries.toList(),
                colors = itemColors(slot),
                enabled = enabled,
                leading = leading,
                selectedIndex = entryValues.indexOf(current).coerceAtLeast(0),
                onItemSelected = { index ->
                    val newValue = entryValues.getOrNull(index) ?: return@AppDropdownItem
                    state.set(key, newValue)
                    if (notify) sendValue(key, newValue)
                    onChange?.invoke(newValue)
                },
            )
        }
    }

    /**
     * 整数滑条（对应 SeekBarPreference；拖动结束才落盘，避免 commit() 拖拽风暴）。
     *
     * 滑条属于分组内的「选项行」，与相邻开关/滑条合并成同一张卡片。
     */
    fun slider(
        key: String,
        title: String,
        valueRange: IntRange,
        step: Int = 1,
        default: Int? = null,
        summary: String? = null,
        valueLabel: ((Int) -> String)? = null,
        enabled: Boolean = true,
        notify: Boolean = false,
        onChange: ((Int) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val stored by state.intFlow(key, default ?: valueRange.first).collectAsStateWithLifecycle()
        PrefRowInset {
            PrefSliderRow(
                title = title,
                value = stored,
                valueRange = valueRange,
                summary = summary,
                step = step,
                valueLabel = valueLabel,
                enabled = enabled,
            ) { stepped ->
                state.set(key, stepped)
                if (notify) sendValue(key, stepped)
                onChange?.invoke(stepped)
            }
        }
    }

    /**
     * 状态驱动的整数滑条：值从页面状态取，拖动结束经 [onChange] 交回页面。
     *
     * 与 [slider] 的区别只在数据源：键值版自己读写 prefs，本重载不做任何落盘，
     * 页码、IPC 读回来的档位这类「真相在别处」的滑条用它。
     */
    fun slider(
        title: String,
        value: () -> Int,
        valueRange: IntRange,
        step: Int = 1,
        summary: String? = null,
        valueLabel: ((Int) -> String)? = null,
        enabled: () -> Boolean = { true },
        itemKey: String? = null,
        visible: Boolean = true,
        onChange: (Int) -> Unit,
    ) = emit(itemKey, null, title, summary, visible = visible) { slot ->
        PrefRowInset {
            PrefSliderRow(
                title = title,
                value = value(),
                valueRange = valueRange,
                summary = summary,
                step = step,
                valueLabel = valueLabel,
                enabled = enabled(),
                onCommit = onChange,
            )
        }
    }

    /**
     * 卡片之外的说明文字：不属于任何分组，因此不参与行间分隔线，也不进卡片。
     *
     * 用它代替页面里手写的 `if (外观线) CouiText else Text`：字号与配色由 [PrefNote] 统一，
     * 左右内缩与卡片边界对齐（[PageInset]），上下只留 4dp，紧邻的分组卡片自带间距。
     */
    fun note(
        text: String,
        visible: Boolean = true,
        itemKey: String? = null,
    ) = emit(itemKey, null, null, null, groupable = false, visible = visible) {
        PrefNote(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = PageInset, vertical = 4.dp),
        )
    }

    /** 文本输入（对应 EditTextPreference，M3 对话框 + OutlinedTextField） */
    fun editText(
        key: String,
        title: String,
        summary: String? = null,
        hint: String? = null,
        dialogMessage: String? = null,
        default: String = "",
        keyboardType: KeyboardType = KeyboardType.Text,
        enabled: Boolean = true,
        notify: Boolean = false,
        maxLines: Int = 1,
        onChange: ((String) -> Unit)? = null,
    ) = emit(key, key, title, summary) { slot ->
        val current by state.stringFlow(key, default).collectAsStateWithLifecycle()
        var showDialog by remember { mutableStateOf(false) }
        PrefRowInset {
            PrefRow(
                title = title,
                summary = current.ifBlank { summary ?: "" }.takeIf { it.isNotEmpty() },
                onClick = { showDialog = true },
                enabled = enabled,
                trailing = { PrefRowChevron() },
                colors = itemColors(slot),
            )
        }
        if (showDialog) {
            // 弹窗外壳、留白与单行/多行差异都由弹窗样式位给；值在弹窗里先本地编辑，确认时才落盘
            var dialogText by remember { mutableStateOf(current) }
            currentDialogStyle().Input(
                title = title,
                value = dialogText,
                onValueChange = { dialogText = it },
                onDismissRequest = { showDialog = false },
                confirmLabel = stringResource(android.R.string.ok),
                onConfirm = {
                    showDialog = false
                    state.set(key, dialogText)
                    if (notify) sendValue(key, dialogText)
                    onChange?.invoke(dialogText)
                },
                message = dialogMessage,
                hint = hint,
                clearContentDescription = stringResource(R.string.clear),
                dismissLabel = stringResource(android.R.string.cancel),
                keyboardType = keyboardType,
                maxLines = maxLines,
            )
        }
    }

    /**
     * 页面入口（跳转其他作用域页；同时登记搜索索引）。
     * 目标页不可达（spec.isVisible false / 单 App 作用域未安装 / 目标页无可见条目）时不发射入口，
     * 入口可见性沿用「选项=0 → 入口消失」规则（判断集中在 [ScopePageRegistry.isPageReachable]）。
     */
    fun page(
        title: String,
        target: String,
        summary: String? = null,
        enabled: Boolean = true,
    ) {
        val ctx = context
        if (ctx != null && !ScopePageRegistry.isPageReachable(ctx, ScopePageRegistry.pageTargetMap[target] ?: target)) {
            return
        }
        emit("page:$target", "page:$target", title, summary, pageTarget = target) { slot ->
            PrefRowInset {
                PrefRow(
                    title = title,
                    summary = summary,
                    onClick = { navigate?.invoke(target, title) },
                    enabled = enabled,
                    trailing = { PrefRowChevron() },
                    colors = itemColors(slot),
                )
            }
        }
    }

    /**
     * 纯点击入口（跳转外部 Activity / 设置 / shell 命令等，非作用域页导航、无值/开关）：分组卡片内的一行。
     *
     * 点击回调是最后一个参数，所以 `click(title) { … }` 可以直接写尾随 lambda；这也让条件行写成
     * `click(title, visible = 条件) { … }` 而不必再包一层本地函数。
     */
    fun click(
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        leading: (@Composable () -> Unit)? = null,
        value: String? = null,
        trailing: (@Composable () -> Unit)? = null,
        /**
         * 稳定的列表项 key（默认按槽位编号）：同一业务行在相邻行增删时槽位会漂移，
         * 需要「删掉的就是这一行」的动画时传业务键。
         */
        itemKey: String? = null,
        /** 该行参与列表增删动画（配合稳定 [itemKey]；默认关闭，其余页面行为不变） */
        animate: Boolean = false,
        /**
         * 该行是否显示（默认显示）：false 时原地收起、true 时原地展开，两条线都有行级动画。
         * 条件行用它代替 `if (条件) { click(…) }`，并配一个稳定的 [itemKey]。
         */
        visible: Boolean = true,
        onLongClick: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null,
    ) = emit(itemKey, null, title, summary, animate = animate, visible = visible) { slot ->
        PrefRowInset {
            PrefRow(
                title = title,
                summary = summary,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                leading = leading,
                trailing = trailing ?: value?.let { text -> { PrefRowValue(text, enabled) } },
                colors = itemColors(slot),
            )
        }
    }

    /**
     * 逃生舱：任意自定义 Composable（ColorPicker、应用选择器等特殊控件用），receiver 可访问 state 等。
     *
     * 默认就是一个普通分组行：并入所在分组卡片、参与自动分隔线；要它独占一张卡片就连着 [group] 用
     *（`group { custom { ... } }`，渲染结果与普通单行成组完全一致）。分组内不要再自己画卡片。
     * 组外调用时不套卡片、不与相邻条目拼卡（整页自定义内容、宿主容器就是这么写的）；
     * [raw] = true 用于「在组内但要标题、又不能进卡片体」的内容（整页拖拽区这类），
     * 它永远不参与卡片体，只按顺序裸渲染。
     */
    fun custom(
        key: String? = null,
        title: String? = null,
        summary: String? = null,
        raw: Boolean = false,
        content: @Composable PrefScopeBuilder.(slot: Int) -> Unit,
    ) = emit(
        key, key, title, summary,
        groupable = !raw,
        raw = raw,
        custom = true,
        render = { slot -> content(this, slot) },
    )
}

/**
 * 作用域偏好页主体（无 Scaffold，标题栏由页面宿主提供）。
 *
 * 行为约定：
 * - [sendValue] 对应 `Context.sendPrefsValue(packName, key, value)`（dataChannel 通知宿主）
 * - [scrollTarget] 对应搜索跳转（scrollKey/scrollPosition）：滚动到槽位 + 高亮闪烁
 * - [onNavigate] 对应页面入口跳转（page DSL 项）
 */
@Composable
fun ScopeScreen(
    state: PrefState,
    modifier: Modifier = Modifier,
    sendValue: (key: String, value: Any) -> Unit = { _, _ -> },
    scrollTarget: ScrollTarget? = null,
    onNavigate: ((target: String, title: String?) -> Unit)? = null,
    onRefresh: (suspend () -> Unit)? = null,
    fullContent: (@Composable LazyItemScope.(PrefScopeBuilder) -> Unit)? = null,
    content: PrefScopeBuilder.() -> Unit,
) {
    // 订阅 revision：任何偏好写入都会重组本页 → 构建 lambda 重跑 → 条件可见性自动重求值。
    // 必须读取 .value：仅调用 collectAsStateWithLifecycle() 并丢弃返回值，快照订阅不会建立，
    // 条件可见性（基于 state.getXxx() 的 if/let 分支）将不会随偏好写入重求值。
    // 同一个 State 对象还要交给列表内容 lambda 读取一次（见 listItems）：
    // 只有列表内容自己也订阅 revision，偏好写入后条目才会重算、条件行才会出现。
    val revisionState = state.revision.collectAsStateWithLifecycle()
    revisionState.value

    val builder = remember(state) { PrefScopeBuilder(state) }
    builder.sendValue = sendValue
    builder.navigate = onNavigate
    builder.context = LocalContext.current.applicationContext
    builder.beginBuild()
    builder.content()
    builder.computeSegments()

    val listState = rememberLazyListState()
    val coui = LocalUiMode.current == UiMode.Coui
    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        val slot = if (target.key.isNotBlank()) {
            // 按索引 key 解析槽位（条目在构建后位置可能因条件可见性变化而偏移）
            builder.entries.firstOrNull { it.indexKey == target.key }?.slot ?: return@LaunchedEffect
        } else {
            target.position.takeIf { it >= 0 } ?: return@LaunchedEffect
        }
        // COUI 线的列表 item 是分组块而不是单行，槽位要先换成块下标
        val itemIndex = if (coui) builder.blockIndexOf(slot) else slot
        if (itemIndex < 0) return@LaunchedEffect
        listState.animateScrollToItem(itemIndex)
        builder.highlightSlot.value = slot
        delay(2500.milliseconds)
        if (builder.highlightSlot.value == slot) builder.highlightSlot.value = null
    }

    // 页面 DSL lambda 用 rememberUpdatedState 跟踪，而不是让下面的 buildEntries 直接闭包捕获：
    // listItems 被 remember 冻住后，冻住的那份 buildEntries 捕获的是首次组合的 content。页面若把值
    // 读进局部变量再闭包传进来（搜索页的 queryNow / filteredNow 就是这样），重组产生的新 lambda
    // 永远不会被执行 —— 输入框 value 恒为旧值（输入法弹得出来、字却打不上去），结果列表也不刷新。
    // 读 State 的 latest 版本可让 LazyList 的 derivedStateOf 在 lambda 换代时重算，同时保留
    // 「页面每帧重组不重建列表内容」的性能收益。
    val latestContent = rememberUpdatedState(content)
    val latestFullContent = rememberUpdatedState(fullContent)

    // 唯一的 LazyColumn item 缝：条目描述（槽位/分段/元数据）只构建一次，主题差异只在这里分派
    val buildEntries: LazyListScope.() -> Unit = {
        // 条目重建必须在 LazyColumn 的 content lambda 内执行（而不是 ScopeScreen 函数体）：
        // 本 lambda 由 LazyList 的 derivedStateOf 包裹，builder.content() 里对页面级状态
        // （systemInfo / rows / entries / filtered 等 mutableStateOf）的读取因此进入快照订阅，
        // 任一变化都会让条目重算、列表刷新。放在函数体里这些读取不参与列表追踪：
        // 功能树 rows 由空变满不刷新（白屏）、搜索结果不出现、SYSTEMINFO 切页后才显示。
        //
        // revision 也必须在 lambda 内部读一次：偏好写入只让 ScopeScreen 重组（函数体里那次
        // beginBuild/content 会用新值重建 entries），列表内容本身不重算就不会去用新的 entries，
        // 行声明里的 if (state.getString(...)) 条件分支不重求值，条件条目永远不出现
        //（实测：不读 revision 时写入偏好后只有函数体重建、条目内容不重组）。
        // 读一次 revision 让列表内容直接依赖它，与 systemInfo / rows 走同一条快照链。
        revisionState.value
        val pageContent = latestContent.value
        val pageFullContent = latestFullContent.value
        builder.beginBuild()
        pageContent(builder)
        builder.computeSegments()
        if (pageFullContent != null) {
            item(key = "full") { pageFullContent(this, builder) }
        } else {
            if (coui) {
                // COUI 线：一个分组块就是一个 LazyColumn item，整张卡片交给 [PrefSectionRows]
                //（与 [PrefGroup] 同一个实现：标题/说明/底部说明 + 分组上沿留白 + 行间分隔线），行内容只负责自己那一行。
                builder.blocks.forEach { block ->
                    item(key = block.itemKey) {
                        val head = block.head
                        val animated = head?.animate == true || block.rows.any { it.animate }
                        val animModifier = if (animated) Modifier.animateItem() else Modifier
                        if (block.rows.isEmpty()) {
                            // 裸条目（组外的 custom、`raw = true` 的页面级内容、没有行的分组标题）：自身不套卡片。
                            val topGap = if (head!!.slot == 0) 0.dp else CouiPageDefaults.categoryTopSpacing
                            Box(modifier = animModifier.padding(top = topGap)) {
                                head.render(head.slot)
                            }
                        } else {
                            val overlay = couiHighlightOverlay(CouiPageDefaults.themeColors().card)
                            val highlighted = builder.highlightSlot.value
                            PrefSectionRows(
                                rows = block.rows.map { entry ->
                                    PrefCardRow(
                                        key = entry.itemKey,
                                        visible = entry.visible,
                                        background = if (highlighted == entry.slot) overlay else null,
                                    ) { entry.render(entry.slot) }
                                },
                                modifier = animModifier,
                                title = head?.sectionTitle,
                                summary = head?.sectionSummary,
                                footer = block.footer,
                            )
                        }
                    }
                }
            } else {
                builder.entries.forEach { entry ->
                    item(key = entry.itemKey) {
                        // 逐行增删动画：只挂到 DSL 显式声明 animate 的行，其余页面行为完全不变。
                        // 淡出/淡入按 key 认身份，所以这类行必须同时传稳定的 itemKey（默认槽位 key 会漂移）。
                        val animModifier = if (entry.animate) Modifier.animateItem() else Modifier
                        // 行级显隐：收起时整个 item 收到零高，相邻行跟着连续位移（分段圆角由可见行决定，
                        // 见 [computeSegments]）。默认可见的行第一帧就按可见落位，不产生动画。
                        AnimatedVisibility(
                            visible = entry.visible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            if (entry.groupBreak) {
                                // 分组结束标记：带 footer 的画卡片下方那行说明，其余保留零高度槽位，
                                // 让搜索索引和滚动定位仍与 entry.slot 一致。
                                entry.render(entry.slot)
                            } else if (entry.groupStart) {
                                // 分组起始标记：material 线由它画分类标题（COUI 线由 CouiPrefGroup 画）
                                if (entry.sectionTitle != null || entry.sectionSummary != null) {
                                    val topGap = if (entry.slot == 0) 0.dp else DesignTokens.GroupGap
                                    Box(modifier = animModifier.padding(top = topGap)) {
                                        entry.render(entry.slot)
                                    }
                                }
                            } else if (entry.raw) {
                                // 裸条目：不套卡片，裸渲染在分组卡片之上。
                                val topGap = if (entry.slot == 0) 0.dp else DesignTokens.GroupGap
                                Box(modifier = animModifier.padding(top = topGap)) {
                                    entry.render(entry.slot)
                                }
                            } else {
                                AppItem(index = entry.segIndex, count = entry.segCount) {
                                    Box(
                                        modifier = animModifier.padding(
                                            top = when {
                                                entry.slot == 0 -> 0.dp
                                                entry.segIndex == 0 -> DesignTokens.GroupGap
                                                // 组内条目：按当前外观线的行间距分开（material 靠间距分段，不画分隔线）
                                                else -> currentUiStyle().listRowSpacing
                                            }
                                        )
                                    ) {
                                        entry.render(entry.slot)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 条目 lambda 必须记住：页面骨架的大标题折叠会把 innerPadding 每帧换一遍，
    // ScopeScreen 因此随滚动每帧重组；若列表内容 lambda 也跟着每帧新建，LazyList 的
    // LazyListIntervalContent 会失效，所有可见条目的内容 lambda 一起换新实例 —— 每帧重组 +
    // 重测量全部可见行。记住后页面重组只影响 ScopeScreen 自身，列表内容不再跟着重建。
    // （行内主线程解码见 PrefImageRow.kt 的 previewImage，那才是滑到 OTA 卡片时的停顿来源。）
    // 键里必须带上外观线判定（coui）：buildEntries 闭包捕获了它来选列表分支（COUI 整块 / material 逐行），
    // 而运行时切主题只改 LocalUiMode，builder 与 fullContent 都不变。漏掉这个键就永远复用首次组合的
    // 那一支：COUI 切 Material 后仍走整块分支（material 分组内缩叠上行内缩 = 两侧 32dp），
    // Material 切 COUI 后仍走逐行分支（行的内缩归零 = 贴边），只有重启应用才恢复。
    val listItems: LazyListScope.() -> Unit = remember(builder, fullContent, coui) { buildEntries }

    val uiStyle = currentUiStyle()
    // 跟手回弹由外观线决定：COUI 线挂库的回弹效果，material 线给 null 走平台默认的拉伸效果
    val overscroll = uiStyle.overscrollEffect()

    // 悬浮胶囊底栏盖在正文之上：列表底部额外留出底栏高度，最后一条才不会被永久遮住。
    // COUI 底栏在标签外壳的栏位里，正文已经排到它上方，这里取到 0。
    val listBottomPadding = 8.dp + LocalShellBottomInset.current
    // 列表参数两条线一致（卡片横向内缩与分组前后留白都在卡片身上），差别只有回弹效果，
    // 所以这里只有一份 LazyColumn，外观差异不再分叉成两个分支
    val list: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(
            modifier = listModifier,
            state = listState,
            contentPadding = PaddingValues(
                top = LocalScopeTopInset.current,
                bottom = listBottomPadding,
            ),
            overscrollEffect = overscroll,
            content = listItems,
        )
    }
    // 列表状态下发到页面内容：LazyColumn 的 item 内容是本 provider 的后代，因此页面里的
    // 自定义内容（如排序列表）也能拿到它，用于「拖到列表边缘自动滚动」这类主动滚动交互。
    CompositionLocalProvider(LocalScopeListState provides listState) {
        if (onRefresh != null) {
            var refreshing by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            // 刷新状态与回调只有这一份，两条线只在容器上分派：
            // COUI 线下拉手势与指示器都由 COUI 容器自绘，不能用 M3 的下拉容器。
            val triggerRefresh: () -> Unit = {
                scope.launch {
                    refreshing = true
                    try {
                        onRefresh()
                    } finally {
                        refreshing = false
                    }
                }
            }
            uiStyle.RefreshBox(
                isRefreshing = refreshing,
                onRefresh = triggerRefresh,
                enabled = LocalTopBarCollapsedFraction.current <= 0f,
                modifier = modifier,
            ) { list(Modifier.fillMaxSize()) }
        } else {
            list(modifier)
        }
    }
}
