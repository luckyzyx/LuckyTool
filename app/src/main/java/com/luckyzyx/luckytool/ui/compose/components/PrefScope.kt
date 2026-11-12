@file:Suppress("unused")

package com.luckyzyx.luckytool.ui.compose.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.InputDialog as CouiInputDialog
import com.itos.coui.effect.couiButtonFeedback
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.itos.coui.layout.CouiPageColors
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu as CouiMenu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.switch.CouiSwitchDefaults
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppDropdownItem
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiPullToRefreshBox
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.AppSwitchItem
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
 * [segIndex]/[segCount] 用于把相邻条目合并成同一张分组卡片（首条上端 16dp 外圆角、尾条下端 16dp 外圆角、
 * 组内条目不设内圆角与间距，见 [com.luckyzyx.luckytool.ui.theme.DesignTokens]）；
 * [groupable] = false 的条目独占一张卡片（分类标题，以及 [PrefScopeBuilder.custom] 显式声明 standalone 的逃生舱）。
 * [bare] = true 的条目（仅分类标题）完全裸渲染，不套任何卡片容器。
 * [groupBreak] 只分隔卡片组，保留零高度槽位以维持索引位置。
 */
internal class PrefEntry(
    val slot: Int,
    val itemKey: String,
    val indexKey: String?,
    val indexTitle: String?,
    val indexSummary: String?,
    val pageTarget: String?,
    val groupable: Boolean,
    val bare: Boolean,
    val groupBreak: Boolean = false,
    /** 该行参与列表增删动画（需要 [itemKey] 稳定，默认关闭） */
    val animate: Boolean,
    val render: @Composable (slot: Int) -> Unit,
) {
    var segIndex: Int = 0
    var segCount: Int = 1
}

/** COUI 分组卡片的外圆角半径（与 [CouiPageDefaults.shape] 同值） */
private val CouiCardCorner = 24.dp

/**
 * COUI 线的分组卡片横切片。
 *
 * COUI 的分组卡片是「一个 Column 装全部行」，而本页每行都是独立的 LazyColumn item，
 * 跨 item 无法共享同一个容器，因此按行切卡：组首圆上两角、组尾圆下两角、组中间直边，
 * 相邻 item 拼起来等于一张连续卡片；行间分隔线由除末行外的切片底部绘制。
 * 单行分组（[count] <= 1）直接用整卡形状。
 *
 * 水平 16dp 内缩在这里提供（与 COUI Group 的卡片内缩一致），列表层不再叠加。
 * 搜索跳转命中时用与手动长按一致的按压蒙层色叠在卡片底色上，卡片保持不透明，避免透出滚动内容。
 */
@Composable
internal fun CouiScopeCard(
    index: Int,
    count: Int,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = CouiPageDefaults.themeColors()
    val dark = CouiTheme.isDark
    val shape: Shape = when {
        count <= 1 -> CouiPageDefaults.shape
        index <= 0 -> RoundedCornerShape(topStart = CouiCardCorner, topEnd = CouiCardCorner)
        index >= count - 1 -> RoundedCornerShape(bottomStart = CouiCardCorner, bottomEnd = CouiCardCorner)
        else -> RectangleShape
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CouiPageDefaults.horizontalMargin)
            .clip(shape)
            .background(
                if (highlighted) {
                    // 搜索跳转高亮与手动长按用同一按压蒙层色（浅色 0x1F000000 / 深色 0x33FFFFFF），
                    // 叠在卡片底色上保持不透明。
                    Color(if (dark) 0x33FFFFFF else 0x1F000000).compositeOver(colors.card)
                } else {
                    colors.card
                },
            ),
    ) {
        content()
        if (index < count - 1) CouiDivider()
    }
}

/**
 * COUI 分类标题：对齐 coui demo 底栏设置页 Group 的标题样式
 * （GroupTitle 12sp 字重 500 + colors.summary，水平 32dp、上下 8dp、TextAlign.Start、heading 语义）。
 * 说明行是 LT 在标题下方的扩展：与标题同字号、弱化前景色，有说明时标题下沿收紧到 4dp、
 * 说明底部补 8dp，整体节奏沿用 AppPermissionPage 的 preference 分类标题。
 */
@Composable
private fun CouiCategoryHeader(title: String, summary: String?) {
    val colors = CouiPageDefaults.themeColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        CouiText(
            text = title,
            kind = CouiTextKind.GroupTitle,
            color = colors.summary,
            // GroupTitle 在库内默认水平居中，这里与库内 Group 一样强制起始对齐
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
                .padding(start = 32.dp, end = 32.dp, top = 8.dp, bottom = if (summary == null) 8.dp else 4.dp),
        )
        if (summary != null) {
            CouiText(
                text = summary,
                kind = CouiTextKind.GroupFooter,
                color = colors.summary,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 8.dp),
            )
        }
    }
}

/** COUI 行的标题文字（16 号 ItemTitle）：禁用态改用 colors.disabled */
@Composable
internal fun CouiRowTitle(text: String, enabled: Boolean, colors: CouiPageColors) {
    CouiText(
        text = text,
        kind = CouiTextKind.ItemTitle,
        color = if (enabled) colors.title else colors.disabled,
    )
}

/** COUI 行的说明文字（13 号 ItemSummary）：禁用态改用 colors.disabled */
@Composable
internal fun CouiRowSummary(text: String, enabled: Boolean, colors: CouiPageColors) {
    CouiText(
        text = text,
        kind = CouiTextKind.ItemSummary,
        color = if (enabled) colors.summary else colors.disabled,
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
 * 偏好页声明式 DSL：用普通 lambda 声明分类、开关、列表、滑条、输入与页面入口。
 *
 * 用法：
 * ```
 * ScopeScreen(state, sendValue = { k, v -> context.sendPrefsValue(pack, k, v) }) {
 *     category("基础")
 *     switch("key1", "标题", "摘要")
 *     if (state.getBoolean("master", false)) {   // 条件可见性：任意偏好变更自动重求值
 *         slider("key2", "滑条", 1..100)
 *     }
 *     page("其他页面", "statusbar_clock")
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

    /** 每次重建前清空（ScopeScreen 每次重组前调用；外部索引收集同样先调用） */
    fun beginBuild() {
        entries.clear()
    }

    /** 当前构建轮次的搜索索引快照 */
    fun snapshotIndex(): List<PrefIndexItem> = entries.mapNotNull { entry ->
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

    @Composable
    internal fun highlightColor(slot: Int): Color {
        // COUI 线的高亮由 CouiScopeCard 自己用强调色混卡片底色绘制，这里不读 MaterialTheme 取色。
        // 外部页面的 custom 内容可能在 COUI 下也调用本函数（它们自绘的列表项不由本文件分派），
        // 因此守卫放在函数入口，而不是依赖调用点。
        if (LocalUiMode.current == UiMode.Coui) return Color.Transparent
        val target = highlightSlot.value == slot
        return animateColorAsState(
            targetValue = if (target) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ).value
    }

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
        bare: Boolean = false,
        groupBreak: Boolean = false,
        animate: Boolean = false,
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
            bare = bare,
            groupBreak = groupBreak,
            animate = animate,
            render = render,
        )
    }

    /**
     * 计算分组：连续且 [PrefEntry.groupable] 的条目合并为一组。
     *
     * 分类标题（裸渲染）把相邻条目切成不同分组；滑条与普通自定义控件都属于组内选项行，
     * 不再独占卡片——只有显式声明 standalone 的逃生舱条目才单独成卡。
     */
    internal fun computeSegments() {
        var i = 0
        while (i < entries.size) {
            if (!entries[i].groupable) {
                entries[i].segIndex = 0
                entries[i].segCount = 1
                i++
                continue
            }
            var j = i
            while (j + 1 < entries.size && entries[j + 1].groupable) j++
            val count = j - i + 1
            for (k in i..j) {
                entries[k].segIndex = k - i
                entries[k].segCount = count
            }
            i = j + 1
        }
    }

    // ---------------- DSL 项 ----------------

    /** 开始一张无标题分组卡片；沿用列表的组间距，不额外绘制分类标题。 */
    fun group() = emit(
        null, null, null, null,
        groupable = false,
        bare = true,
        groupBreak = true,
    ) { }

    /**
     * 分类标题：不套卡片，裸渲染在分组卡片之上。
     * [summary] 与标题同组渲染（标题在上、说明在下），
     * 不再作为独立的文字组件悬在分类之下。
     */
    fun category(title: String, summary: String? = null) =
        emit(null, null, null, null, groupable = false, bare = true) { _ ->
            if (LocalUiMode.current == UiMode.Coui) {
                CouiCategoryHeader(title = title, summary = summary)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                            .padding(vertical = 8.dp),
                    )
                    if (summary != null) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                }
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
        if (LocalUiMode.current == UiMode.Coui) {
            // COUI 行：整行 toggleable（Role.Switch）承载点击与无障碍语义，
            // 尾部开关只做视觉与拖拽提交；两者共用一个 interactionSource，避免点击态与拖动态各画一套反馈。
            val colors = CouiPageDefaults.themeColors()
            val dark = CouiTheme.isDark
            val source = remember { MutableInteractionSource() }
            CouiItem(
                modifier = Modifier
                    .couiButtonFeedback(
                        dark = dark,
                        interactionSource = source,
                        enabled = enabled,
                        list = true,
                    )
                    .toggleable(
                        value = checked,
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    ),
                icon = leading,
                title = { CouiRowTitle(title, enabled, colors) },
                summary = summary?.let { text -> { CouiRowSummary(text, enabled, colors) } },
                trailing = {
                    CouiSwitch(
                        modifier = Modifier.padding(start = 16.dp),
                        checked = checked,
                        onCheckedChange = null,
                        dark = dark,
                        colors = CouiSwitchDefaults.colors(colors.accent, dark),
                        enabled = enabled,
                        interactionSource = source,
                        onDragCheckedChange = onCheckedChange,
                    )
                },
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                AppSwitchItem(
                    title = title,
                    summary = summary,
                    colors = itemColors(slot),
                    checked = checked,
                    enabled = enabled,
                    leading = leading,
                    onCheckedChange = onCheckedChange,
                )
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
        if (LocalUiMode.current == UiMode.Coui) {
            // COUI 下拉行：整行点击展开 COUI 菜单，行尾显示当前项文字 + 展开箭头
            val colors = CouiPageDefaults.themeColors()
            val dark = CouiTheme.isDark
            val source = remember { MutableInteractionSource() }
            val anchor = rememberCouiMenuAnchor()
            var expanded by remember { mutableStateOf(false) }
            val selectedIndex = entryValues.indexOf(current)
            val menuItems = entries.mapIndexed { index, label ->
                CouiMenuItem(value = index, text = label, selected = index == selectedIndex)
            }
            Box {
                CouiItem(
                    modifier = Modifier
                        .couiMenuAnchor(anchor)
                        .couiButtonFeedback(
                            dark = dark,
                            interactionSource = source,
                            enabled = enabled,
                            list = true,
                            // 菜单展开期间保持按压底色，避免行在弹层打开时失去高亮
                            hoverLocked = expanded,
                        )
                        .clickable(
                            interactionSource = source,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                        ) { anchor.open { expanded = true } },
                    icon = leading,
                    title = { CouiRowTitle(title, enabled, colors) },
                    summary = shownSummary?.let { text -> { CouiRowSummary(text, enabled, colors) } },
                    trailing = {
                        CouiRowValue(text = currentLabel, enabled = enabled, colors = colors)
                        Icon(
                            imageVector = CouiIconsExt.Popup,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp).size(width = 14.dp, height = 24.dp),
                            tint = couiTrailingIconTint(dark),
                        )
                    },
                )
                CouiMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    items = menuItems,
                    onItemClick = { item ->
                        expanded = false
                        val newValue = entryValues.getOrNull(item.value)
                        if (newValue != null) {
                            state.set(key, newValue)
                            if (notify) sendValue(key, newValue)
                            onChange?.invoke(newValue)
                        }
                    },
                    anchor = anchor,
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
        if (LocalUiMode.current == UiMode.Coui) {
            val colors = CouiPageDefaults.themeColors()
            val dark = CouiTheme.isDark
            val min = valueRange.first
            val max = valueRange.last
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val storedValue = stored.coerceIn(min, max).toFloat()
            val shown = if (dragging) dragValue else storedValue
            // 拖动结束才落盘，并按 step 取整：COUI 滑条按像素连续取值，不吸附刻度
            fun persist(value: Float) {
                dragging = false
                val stepped = (((value - min) / step).roundToInt() * step + min).coerceIn(min, max)
                dragValue = stepped.toFloat()
                state.set(key, stepped)
                if (notify) sendValue(key, stepped)
                onChange?.invoke(stepped)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                // 标题行：标题 + 当前值，行高与开关行一致
                CouiItem(
                    title = { CouiRowTitle(title, enabled, colors) },
                    summary = summary?.let { text -> { CouiRowSummary(text, enabled, colors) } },
                    trailing = {
                        CouiRowValue(
                            text = valueLabel?.invoke(shown.roundToInt())
                                ?: shown.roundToInt().toString(),
                            enabled = enabled,
                            colors = colors,
                        )
                    },
                )
                // 滑条独立占一行，左右内缩与行内容对齐
                CouiSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = CouiPageDefaults.rowPadding,
                            end = CouiPageDefaults.rowPadding,
                            bottom = 8.dp,
                        ),
                    value = shown,
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    colors = CouiSliderDefaults.colors(colors.accent, dark, enabled),
                    dark = dark,
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = ((max - min) / step - 1).coerceAtLeast(0),
                    enabled = enabled,
                    onValueChangeFinished = { persist(dragValue) },
                )
            }
        } else {
            // Material 线专用：COUI 已在上面早返回自己的滑条实现，本块不会被 COUI 执行。
            val sliderState = rememberSliderState(
                value = stored.coerceIn(valueRange.first, valueRange.last).toFloat(),
                steps = ((valueRange.last - valueRange.first) / step - 1).coerceAtLeast(0),
                trackRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            )
            // 外部值变化同步：拖动期间 stored 不变，因此不会覆盖用户手势
            LaunchedEffect(stored) {
                sliderState.value = stored.coerceIn(valueRange.first, valueRange.last).toFloat()
            }
            val current = sliderState.value.roundToInt()
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                MaterialItemContainer(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                valueLabel?.invoke(current) ?: current.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        summary?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Slider(
                            state = sliderState,
                            onValueChange = { sliderState.value = it },
                            onValueChangeFinished = {
                                val min = valueRange.first
                                val stepped = (((sliderState.value - min) / step).roundToInt() * step + min)
                                    .coerceIn(valueRange.first, valueRange.last)
                                sliderState.value = stepped.toFloat()
                                state.set(key, stepped)
                                if (notify) sendValue(key, stepped)
                                onChange?.invoke(stepped)
                            },
                            enabled = enabled,
                        )
                    }
                }
            }
        }
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
        if (LocalUiMode.current == UiMode.Coui) {
            // COUI 输入行：点击弹 COUI 输入对话框（对话框内固定单行输入，多行内容需在输入框内整体编辑）
            val colors = CouiPageDefaults.themeColors()
            val dark = CouiTheme.isDark
            val source = remember { MutableInteractionSource() }
            CouiItem(
                modifier = Modifier
                    .couiButtonFeedback(
                        dark = dark,
                        interactionSource = source,
                        enabled = enabled,
                        list = true,
                    )
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                    ) { showDialog = true },
                title = { CouiRowTitle(title, enabled, colors) },
                summary = { CouiRowSummary(current.ifBlank { summary ?: "" }, enabled, colors) },
                trailing = {
                    Icon(
                        imageVector = CouiIconsExt.Next,
                        contentDescription = null,
                        modifier = Modifier.size(width = 12.dp, height = 24.dp),
                        tint = couiTrailingIconTint(dark),
                    )
                },
            )
            if (showDialog) {
                var text by remember { mutableStateOf(TextFieldValue(current)) }
                CouiInputDialog(
                    value = text,
                    onValueChange = { text = it },
                    onDismissRequest = { showDialog = false },
                    confirmAction = CouiDialogAction(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            showDialog = false
                            val newValue = text.text
                            state.set(key, newValue)
                            if (notify) sendValue(key, newValue)
                            onChange?.invoke(newValue)
                        },
                        style = CouiDialogActionStyle.Primary,
                        colors = CouiDialogDefaults.primaryActionColors(colors.accent),
                    ),
                    clearContentDescription = stringResource(R.string.clear),
                    title = title,
                    message = dialogMessage,
                    placeholder = hint,
                    dismissAction = CouiDialogAction(
                        text = stringResource(android.R.string.cancel),
                        onClick = { showDialog = false },
                    ),
                    dark = dark,
                    accent = colors.accent,
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                AppListItem(
                    onClick = { showDialog = true },
                    enabled = enabled,
                    colors = itemColors(slot),
                    headlineContent = { Text(title) },
                    supportingContent = { Text(current.ifBlank { summary ?: "" }) },
                )
            }
            // Material 线专用：COUI 的输入弹窗在上面分支里已独立实现，本块不会被 COUI 执行。
            if (showDialog) {
                var text by remember { mutableStateOf(current) }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text(title) },
                    text = {
                        // 长说明放进「可滚动 + 只吃剩余高度」区域，先测量输入框，
                        // 避免说明把输入框挤出对话框
                        Column {
                            dialogMessage?.takeIf { it.isNotEmpty() }?.let {
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = hint?.let { { Text(it) } },
                                singleLine = maxLines == 1,
                                maxLines = maxLines,
                                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDialog = false
                                state.set(key, text)
                                if (notify) sendValue(key, text)
                                onChange?.invoke(text)
                            },
                        ) { Text(stringResource(android.R.string.ok)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                        }) { Text(stringResource(android.R.string.cancel)) }
                    },
                )
            }
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
            if (LocalUiMode.current == UiMode.Coui) {
                // COUI 页面入口行：行尾为前进箭头，整行点击跳转
                val colors = CouiPageDefaults.themeColors()
                val dark = CouiTheme.isDark
                val source = remember { MutableInteractionSource() }
                CouiItem(
                    modifier = Modifier
                        .couiButtonFeedback(
                            dark = dark,
                            interactionSource = source,
                            enabled = enabled,
                            list = true,
                        )
                        .clickable(
                            interactionSource = source,
                            indication = null,
                            enabled = enabled,
                            role = Role.Button,
                        ) { navigate?.invoke(target, title) },
                    title = { CouiRowTitle(title, enabled, colors) },
                    summary = summary?.let { text -> { CouiRowSummary(text, enabled, colors) } },
                    trailing = {
                        Icon(
                            imageVector = CouiIconsExt.Next,
                            contentDescription = null,
                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                            tint = couiTrailingIconTint(dark),
                        )
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    AppListItem(
                        onClick = { navigate?.invoke(target, title) },
                        enabled = enabled,
                        colors = itemColors(slot),
                        headlineContent = { Text(title) },
                        supportingContent = summary?.let { { Text(it) } },
                        // Material 线专用：COUI 分支的行尾箭头用 couiTrailingIconTint 自绘，不经过这里。
                        trailingContent = {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
    }

    /** 纯点击入口（跳转外部 Activity / 设置 / shell 命令等，非作用域页导航、无值/开关）：分组卡片内的一行 */
    fun click(
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        leading: (@Composable () -> Unit)? = null,
        value: String? = null,
        trailing: (@Composable () -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        /**
         * 稳定的列表项 key（默认按槽位编号）：同一业务行在相邻行增删时槽位会漂移，
         * 需要「删掉的就是这一行」的动画时传业务键。
         */
        itemKey: String? = null,
        /** 该行参与列表增删动画（配合稳定 [itemKey]；默认关闭，其余页面行为不变） */
        animate: Boolean = false,
    ) = emit(itemKey, null, title, summary, animate = animate) { slot ->
        if (LocalUiMode.current == UiMode.Coui) {
            val colors = CouiPageDefaults.themeColors()
            val dark = CouiTheme.isDark
            val source = remember { MutableInteractionSource() }
            // 需要长按的行由修饰符自己接管手势（COUI 的 onClick 只表达单击），
            // 无长按的行交给 Item 自带的点击与按压反馈
            val gesture: Modifier = if (onLongClick != null) {
                Modifier
                    .couiButtonFeedback(
                        dark = dark,
                        interactionSource = source,
                        enabled = enabled,
                        list = true,
                    )
                    .combinedClickable(
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onLongClick = onLongClick,
                        onClick = { onClick?.invoke() },
                    )
            } else {
                Modifier
            }
            CouiItem(
                modifier = gesture,
                icon = leading,
                onClick = if (onLongClick != null) null else onClick,
                enabled = enabled,
                title = { CouiRowTitle(title, enabled, colors) },
                summary = summary?.let { text -> { CouiRowSummary(text, enabled, colors) } },
                trailing = {
                    if (trailing != null) {
                        trailing()
                    } else if (value != null) {
                        CouiRowValue(text = value, enabled = enabled, colors = colors)
                    }
                },
            )
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                AppListItem(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    enabled = enabled,
                    colors = itemColors(slot),
                    headlineContent = { Text(title) },
                    supportingContent = summary?.let { { Text(it) } },
                    leadingContent = leading,
                    // Material 线专用：COUI 分支的当前值用 CouiRowValue 绘制，不经过这里。
                    trailingContent = trailing ?: value?.let { v ->
                        {
                            Text(
                                text = v,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * 逃生舱：任意自定义 Composable（ColorPicker、应用选择器等特殊控件用），receiver 可访问 state 等。
     *
     * [standalone] = true 时独占一张卡片（非「选项行」内容，如搜索框、页面级 body）；
     * 默认 false 时并入所在分组卡片——分类分组内不允许出现独立卡片。
     * [bare] = true 的条目自身不套卡片容器，因而永远不参与分组合并。
     */
    fun custom(
        key: String? = null,
        title: String? = null,
        summary: String? = null,
        bare: Boolean = false,
        standalone: Boolean = false,
        content: @Composable PrefScopeBuilder.(slot: Int) -> Unit,
    ) = emit(
        key, key, title, summary,
        groupable = !standalone && !bare,
        bare = bare,
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
    LaunchedEffect(scrollTarget) {
        val target = scrollTarget ?: return@LaunchedEffect
        val slot = if (target.key.isNotBlank()) {
            // 按索引 key 解析槽位（条目在构建后位置可能因条件可见性变化而偏移）
            builder.entries.firstOrNull { it.indexKey == target.key }?.slot ?: return@LaunchedEffect
        } else {
            target.position.takeIf { it >= 0 } ?: return@LaunchedEffect
        }
        listState.animateScrollToItem(slot)
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
            builder.entries.forEachIndexed { index, entry ->
                item(key = entry.itemKey) {
                    if (entry.groupBreak) {
                        // 保留零高度槽位，让搜索索引和滚动定位仍与 entry.slot 一致。
                        return@item
                    }
                    // 逐行增删动画：只挂到 DSL 显式声明 animate 的行，其余页面行为完全不变。
                    // 淡出/淡入按 key 认身份，所以这类行必须同时传稳定的 itemKey（默认槽位 key 会漂移）。
                    val animModifier = if (entry.animate) Modifier.animateItem() else Modifier
                    if (entry.bare) {
                        // 分类标题：不套卡片，裸渲染在分组卡片之上。
                        // 顶部间距沿用原卡片路径的取值，纵向节奏与改动前一致。
                        val topGap = when {
                            // COUI 线：首个分类不额外留白（页面骨架已给顶栏留白），
                            // 其余分类与上一组之间用 COUI 的分组前间距
                            LocalUiMode.current == UiMode.Coui ->
                                if (entry.slot == 0) 0.dp else CouiPageDefaults.categoryTopSpacing
                            else -> if (entry.slot == 0) 0.dp else 8.dp
                        }
                        Box(modifier = animModifier.padding(top = topGap)) {
                            entry.render(entry.slot)
                        }
                    } else if (LocalUiMode.current == UiMode.Coui) {
                        CouiScopeCard(
                            index = entry.segIndex,
                            count = entry.segCount,
                            highlighted = builder.highlightSlot.value == entry.slot,
                            modifier = animModifier.padding(
                                top = when {
                                    entry.slot == 0 -> 0.dp
                                    // 组首与上一组之间用 COUI 的分组间距，组内条目直接连成一张卡片。
                                    // 但组首上方是分类标题时不再叠加：标题自身带下留白，叠加后离卡片太远，
                                    // 与 coui demo Group 标题行贴紧卡片不一致。
                                    entry.segIndex == 0 -> {
                                        val previous = builder.entries.getOrNull(index - 1)
                                        if (previous != null && previous.bare && !previous.groupBreak) 0.dp
                                        else CouiPageDefaults.groupSpacing
                                    }
                                    else -> 0.dp
                                }
                            ),
                        ) {
                            entry.render(entry.slot)
                        }
                    } else {
                        AppItem(index = entry.segIndex, count = entry.segCount) {
                            Box(
                                modifier = animModifier.padding(
                                    top = when {
                                        entry.slot == 0 -> 0.dp
                                        entry.segIndex == 0 -> 8.dp
                                        // 组内条目：0dp，同一分组拼成一张连续卡片
                                        else -> DesignTokens.ItemGap
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

    // 条目 lambda 必须记住：页面骨架的大标题折叠会把 innerPadding 每帧换一遍，
    // ScopeScreen 因此随滚动每帧重组；若列表内容 lambda 也跟着每帧新建，LazyList 的
    // LazyListIntervalContent 会失效，所有可见条目的内容 lambda 一起换新实例 —— 每帧重组 +
    // 重测量全部可见行。记住后页面重组只影响 ScopeScreen 自身，列表内容不再跟着重建。
    // （行内主线程解码见 PrefImageRow.kt 的 previewImage，那才是滑到 OTA 卡片时的停顿来源。）
    val listItems: LazyListScope.() -> Unit = remember(builder, fullContent) { buildEntries }

    // COUI 的跟手回弹只在 COUI 线挂载；Material 线留空走平台默认的拉伸效果。
    val couiOverscroll = if (LocalUiMode.current == UiMode.Coui) rememberCouiOverscrollEffect() else null

    // 悬浮胶囊底栏盖在正文之上：列表底部额外留出底栏高度，最后一条才不会被永久遮住。
    // COUI 底栏在标签外壳的栏位里，正文已经排到它上方，这里取到 0。
    val listBottomPadding = 8.dp + LocalShellBottomInset.current
    val list: @Composable (Modifier) -> Unit = { listModifier ->
        if (LocalUiMode.current == UiMode.Coui) {
            // COUI 线：卡片自带 16dp 横向内缩（见 CouiScopeCard），列表不再叠一层；
            // 顶部额外留一组间距，让首卡与顶栏之间保持和其他列表页（FunctionList）一致的间距
            LazyColumn(
                modifier = listModifier,
                state = listState,
                contentPadding = PaddingValues(
                    top = LocalScopeTopInset.current + CouiPageDefaults.groupSpacing,
                    bottom = listBottomPadding,
                ),
                overscrollEffect = couiOverscroll,
                content = listItems,
            )
        } else {
            // Material 线：首行贴着顶栏起排（与 FunctionList 一致），只留底部间距
            LazyColumn(
                modifier = listModifier,
                state = listState,
                contentPadding = PaddingValues(
                    top = LocalScopeTopInset.current,
                    bottom = listBottomPadding,
                ),
                content = listItems,
            )
        }
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
            if (LocalUiMode.current == UiMode.Coui) {
                CouiPullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = triggerRefresh,
                    modifier = modifier,
                ) { list(Modifier.fillMaxSize()) }
            } else {
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = triggerRefresh,
                    modifier = modifier,
                    enabled = LocalTopBarCollapsedFraction.current <= 0f,
                ) { list(Modifier.fillMaxSize()) }
            }
        } else {
            list(modifier)
        }
    }
}
