package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.utils.PrefState

/**
 * 卡片行 / 卡原语，支持分段卡片与独立卡片。
 *
 * **分组只有一个入口、一份实现**：[PrefGroup] 与 DSL 的 group() 都落到 [PrefSectionRows]，
 * 区别只在位置 —— group() 把行拆成 LazyColumn 的逐行 item（逐行动画、搜索高亮、槽位定位靠它），
 * [PrefGroup] 把整组一次性交给渲染层。页面是 PageScreen 时优先写 group()。
 *
 * **行只有一套写法**：两个位置都写 DSL 行 —— switch() / check() / select() / list() / slider() /
 * editText() / page() / click() / note() / custom()，一段分组内容可以在两处之间原样搬动；
 * 自定义 composable 就是一行，用 custom() 装，不要另立「行原语 + item {}」的写法。
 *
 * [PrefGroup] 必须给 state：行读写哪个偏好文件由它决定，即使这一段全是展示行也照传所在页面的 state。
 *
 * 本文件其余部分是 custom() 逃生舱里的卡原语：[PrefCard] + [PrefSwitchCard]
 *（不属于任何分段组的独立卡片）、[PrefIcon]（行首图标）、[PrefColorPreview]（行尾颜色预览色块）。
 * 行本身（[PrefRow] / [PrefSwitchRow] / [PrefCheckRow] / [PrefSelectRow]）在 PrefRows.kt。
 *
 * 外观线分派：字形、配色、按钮、滑条、卡片与分组的两个实现对象都在 components/style/，
 * 本文件与 PrefRows.kt 只把调用转给 [currentPrefStyle]，因此新增一种外观样式时只写一个新的
 * [PrefStyle] 实现、在 currentPrefStyle() 里接一条分支，页面与这里的原语都不用改。
 *
 * **版式三层归属**（页面 / 分组 / 行各管一段，跨层补就会叠内缩、叠卡片）：
 * 1. 页面：只决定分组的先后与分组之外的内容，整页不判外观线；需要与卡片左右对齐时用 [pageInset]；
 * 2. 分组：唯一入口是 DSL 的 group() 与本文件的 [PrefGroup]（两者都落到 [PrefSectionRows]），
 *    标题 / 说明 / 卡片 / 行间分隔线 / 分组上沿留白都在那里给；
 * 3. 行：偏好行写 switch() / check() / select() / list() / editText() / page()，其余写 click() / note() /
 *    custom()；custom() 的内容里才用 [PrefRow] / [PrefSwitchRow] / [PrefCheckRow] / [PrefSelectRow]
 *    （见 PrefRows.kt），行本身不再判外观线内缩。
 *
 * 特殊组件（滑条、图表、自定义卡）放 custom() / fullContent —— 那是 DSL 的正规逃生舱：
 * 只放内容与交互，卡片、分组标题、分组间距仍由上面分层给。
 */

/**
 * 设置区块（组合位置）：行与 DSL 页面里的 group() 一字不差，渲染也同一份 [PrefSectionRows]。
 *
 * PrefGroup(state = state, title = "标题", summary = "说明") {
 *     switch("key", "开关")
 *     if (state.getBoolean("advanced", false)) {   // 条件行：偏好写入后自动重求值
 *         slider("level", "等级", 1..5)
 *     }
 *     custom { MyChart() }                          // 自定义 composable 就是一行
 * }
 *
 * 与 group() 的差别只在承载方式：group() 逐行进 LazyColumn（行级 animateItem、搜索高亮、
 * 槽位滚动定位都靠它），本函数把整组一次性交给 [PrefSectionRows]（一张卡，行级显隐仍有动画，
 * 但没有逐行 item 动画与搜索定位）。参数与 group() 一致，只多出 state —— 行读写哪个偏好文件由它决定。
 *
 * 版式归属：水平内缩与分组上沿留白都由本入口给（COUI 线卡片自带内缩，material 线这里补 16dp），
 * 调用方不要再加水平 padding（会叠成 32dp），也不要在相邻分组之间插 Spacer。
 *
 * @param state 行的读写源；同时订阅它的 [PrefState.revision]，条件行才会随偏好写入重求值
 * @param sendValue 写值后给宿主进程的通知，含义与 PageScreen 的 sendValue 相同
 * @param onNavigate page 行（页面跳转）的目标解析，不给则该行不出现
 */
@Composable
fun PrefGroup(
    state: PrefState,
    title: String? = null,
    summary: String? = null,
    footer: String? = null,
    sendValue: (key: String, value: Any) -> Unit = { _, _ -> },
    onNavigate: ((target: String, title: String?) -> Unit)? = null,
    content: PrefPageBuilder.() -> Unit,
) {
    // 必须读取 .value：只调用 collectAsStateWithLifecycle() 并丢弃返回值不会建立快照订阅，
    // 组内 if (state.getXxx(...)) 这类条件行就再也不会随偏好写入重求值。
    val revisionState = state.revision.collectAsStateWithLifecycle()
    revisionState.value

    // 复用页面 DSL 的构建器：行 API、行原语、写值语义因此只有一份，不需要在这里重写一遍
    val builder = remember(state) { PrefPageBuilder(state) }
    builder.sendValue = sendValue
    builder.navigate = onNavigate
    builder.context = LocalContext.current.applicationContext
    builder.highlightSlot.value = null
    builder.beginBuild()
    builder.content()
    builder.computeSegments()

    PrefSectionRows(
        title = title,
        summary = summary,
        footer = footer,
        rows = builder.entries.map { entry ->
            // 没给 itemKey 的行用槽位兜底：行 key 不带 null，显隐动画与列表复用才有稳定的键
            PrefCardRow(key = entry.itemKey ?: entry.slot, visible = entry.visible) { entry.render(entry.slot) }
        },
    )
}

/**
 * 分组渲染的唯一实现：[PrefGroup] 与 DSL 的 group() 都走这里，行由 [PrefCardRow] 描述。
 *
 * 版式差异（COUI 线一张卡 + 库的 Group；material 线分段圆角 + 自画标题与 footer）在
 * [PrefStyle.Section] 的两个实现里；[PrefCardRow.background] 只在 COUI 线生效，用于搜索命中的行底色。
 */
@Composable
internal fun PrefSectionRows(
    rows: List<PrefCardRow>,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    footer: String? = null,
) {
    currentPrefStyle().Section(rows = rows, modifier = modifier, title = title, summary = summary, footer = footer)
}

/**
 * 页内非分组内容的水平内缩：搜索栏、独立按钮、滑条卡、面板里的搜索框，都靠它把左右边界对到 [PrefGroup] 的卡片上。
 *
 * 全应用只有这一处给「内容对齐卡片边界」的数值 —— material 线分段卡与 COUI 库 Group 同为 16dp，两条线因此共用同一个数。
 * **分组内容不要用它**：内缩已由 [PrefGroup] / DSL 的 group() 给，再套一层会叠成 32dp。
 */
internal val PageInset = 16.dp

/** 见 [PageInset]。 */
internal fun Modifier.pageInset(): Modifier = padding(horizontal = PageInset)

/** 设置入口的单色图标：COUI 线用 [painter]（Lucide 资源），Material 线用 [icon]；二者至少提供一个。 */
@Composable
fun PrefIcon(icon: ImageVector? = null, painter: Painter? = null) {
    currentPrefStyle().Icon(icon = icon, painter = painter)
}

/** 行尾颜色预览的边长：与行尾图片缩略图 PrefImagePreview 的行高同取 40dp */
private val ColorPreviewSize = 40.dp

/** 行尾颜色预览的圆角：正方形 + 6dp 圆角（与 PrefImagePreview 的圆角一致） */
private val ColorPreviewCornerRadius = 6.dp

/**
 * 行尾颜色预览色块：正方形 + 圆角，用于颜色偏好条目右侧。
 *
 * 形态对齐 PrefImagePreview：同一 40dp 边长、同一 6dp 圆角，只是没有按图片比例拉宽，
 * 因此是正方形 + 圆角而不是圆点、胶囊或宽扁的矩形。
 *
 * 描边不能取主题色的 outline：主题色本身可以选到白色，此时列表行与弹层都是浅色，
 * 白色描边等于没有描边，而这两个颜色偏好的默认值正是 #FFFFFFFF，色块会整块看不见。
 */
@Composable
fun PrefColorPreview(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ColorPreviewCornerRadius)

    Box(
        modifier
            .size(ColorPreviewSize)
            .clip(shape)
            .background(color)
            .border(1.dp, contrastBorder(color), shape)
    )
}

/**
 * 与 [color] 形成对比的描边色（[PrefColorPreview] 与颜色选择器的预览色块共用）。
 *
 * 只按颜色自身取反、不跟主题色走：浅色用深描边，深色用浅描边；半透明色一律用深描边，
 * 因为它底下透出的是浅色内容（选择器里是浅灰棋盘格）。这样无论主题色与所选颜色怎么组合，
 * 色块本体与轮廓至少有一个看得清 —— 「白色主题色 + 白色颜色」原先是两者都看不见的组合。
 */
internal fun contrastBorder(color: Color): Color =
    if (color.alpha < 0.5f || color.luminance() > 0.5f) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }

/**
 * custom() 逃生舱里行内容的标准水平内缩包装，与 DSL 的 switch() / list() / click() 等分派一致。
 *
 * 补不补内缩由外观线决定（COUI 的 Item 自带 16dp 行内缩，material 列表行不带），
 * 具体在 [PrefStyle.RowInset] 的实现里，调用方只管套上。
 */
@Composable
fun PrefRowInset(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    currentPrefStyle().RowInset(modifier = modifier, content = content)
}

/**
 * 独立卡片（不属于任何分段组时使用）。
 * 外观与主题页一致：material 线 surfaceBright 容器 + 16dp 外圆角（AppItem 单条分段卡），
 * COUI 线整卡（24dp 圆角 + 卡片底色，不成组，故不套库的 Group）。
 */
@Composable
fun PrefCard(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    currentPrefStyle().Card(
        title = title,
        modifier = modifier,
        summary = summary,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        trailing = trailing,
    )
}

/** 独立开关卡片（不属于任何分段组时使用） */
@Composable
fun PrefSwitchCard(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    currentPrefStyle().SwitchCard(
        title = title,
        checked = checked,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}
