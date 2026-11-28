package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.material3.ListItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.luckyzyx.luckytool.ui.compose.components.material.materialGroupColors

/**
 * 设置行：外观线在此收敛，页面与 DSL 只挑行类型、不再分派。
 *
 * 四种行覆盖全部标准条目——[PrefRow]（点击 / 跳转）、[PrefSwitchRow]（开关）、
 * [PrefCheckRow]（复选框）、[PrefSelectRow]（单选项）；另有 [PrefSliderRow]（滑条）、
 * [PrefButton] / [PrefTextButton] / [PrefIconButton]（按钮）、[PrefNote]（页内说明）。
 * 外观线与交互都在这里定，页面与 DSL 因此不必再判 LocalUiMode，也不必自拼 COUI 的 Item 与手势。
 *
 * 字形 / 配色 / 按钮形态的差异全部落在 [PrefStyle] 的实现对象里（components/style/）；
 * 行容器本身（COUI 的 Item + 点击反馈 / material 的分段列表行）在 AppListItem 一族里，
 * 两条线的容器结构不同，无法用同一套参数表达，所以留在 AppKit.kt。
 *
 * 版式归属：行只画正文与行尾，卡片（圆角、底色、行间分隔线）归分组（group() / [PrefGroup]）。
 * 行的水平内缩由所属外观线给——COUI 的 Item 自带，material 列表行不带，
 * 所以在 ScopeScreen 的 LazyColumn 槽位里要用 [PrefRowInset] 补；分组内（[PrefGroup]）不要再补。
 */

/**
 * 行标题：COUI 线用库的 ItemTitle 字号与配色，material 线用 M3 列表行的默认文本。
 *
 * [maxLines] 用于长标题（应用名、包名）：超出的部分按 [overflow] 省略，不让行被撑高。
 */
@Composable
internal fun PrefRowTitle(
    text: String,
    enabled: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    currentPrefStyle().Title(text, enabled, maxLines, overflow)
}

/** 行说明：与 [PrefRowTitle] 同一分派，COUI 线取 ItemSummary 字号；[maxLines] 同 [PrefRowTitle] */
@Composable
internal fun PrefRowSummary(
    text: String,
    enabled: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    currentPrefStyle().Summary(text, enabled, maxLines, overflow)
}

/** 行尾当前值：COUI 线右对齐并限宽，material 线取 M3 的主题色正文 */
@Composable
internal fun PrefRowValue(text: String, enabled: Boolean = true) {
    currentPrefStyle().Value(text, enabled)
}

/** 行尾前进箭头：表示这一行会进入下一级页面 */
@Composable
internal fun PrefRowChevron() {
    currentPrefStyle().Chevron()
}

/**
 * 点击行：正文 + 可选的说明 / 行首图标 / 行尾内容，整行可点。
 *
 * 需要长按时由行自己接管手势（COUI 的 Item 只表达单击）；[trailing] 优先于内容，
 * 行尾要「当前值」用 [PrefRowValue]、要前进箭头用 [PrefRowChevron]。
 */
@Composable
fun PrefRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    colors: ListItemColors = materialGroupColors(),
) {
    AppListItem(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        headlineContent = { PrefRowTitle(title, enabled) },
        supportingContent = summary?.let { text -> { PrefRowSummary(text, enabled) } },
        leadingContent = leading,
        trailingContent = trailing,
    )
}

/** 开关行：整行切换，行尾为开关（拖动与点击共用同一个交互源） */
@Composable
fun PrefSwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    colors: ListItemColors = materialGroupColors(),
) {
    AppSwitchItem(
        title = title,
        summary = summary,
        colors = colors,
        checked = checked,
        enabled = enabled,
        leading = leading,
        onCheckedChange = onCheckedChange,
    )
}

/**
 * 页面级说明文字（分组之外的空态提示、长说明、警告）。
 *
 * 两条线共用同一处排版与配色：COUI 取列表说明字号 + 说明色，material 取 bodyMedium + onSurfaceVariant。
 * 内缩与对齐交给调用方（[modifier] 上给 padding / fillMaxWidth），本函数只管字形与颜色。
 */
@Composable
fun PrefNote(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    tone: NoteTone = NoteTone.Summary,
) {
    currentPrefStyle().Note(text, modifier, textAlign, tone)
}

/** 复选框行：整行切换，行尾为复选框；一组可多选的条目用它，单选条目用 [PrefSelectRow] */
@Composable
fun PrefCheckRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    colors: ListItemColors = materialGroupColors(),
) {
    AppCheckItem(
        title = title,
        summary = summary,
        colors = colors,
        checked = checked,
        enabled = enabled,
        leading = leading,
        onCheckedChange = onCheckedChange,
    )
}

/**
 * 滑条行：标题与当前值占一行，滑条独占下一行。
 *
 * 值不在 prefs 里（控制器状态、系统键）的页面直接用它，DSL 的 slider() 也是同一份实现；
 * 两条线共用的取整与落盘时机：拖动结束才回调 [onCommit]，并按 [step] 吸附到刻度。
 * 内缩与行同一套规矩：分组内不补（分组已给），ScopeScreen 的 LazyColumn 槽位用 [PrefRowInset] 补。
 */
@Composable
fun PrefSliderRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    summary: String? = null,
    step: Int = 1,
    valueLabel: ((Int) -> String)? = null,
    enabled: Boolean = true,
    onCommit: (Int) -> Unit,
) {
    val min = valueRange.first
    val max = valueRange.last
    val steps = ((max - min) / step - 1).coerceAtLeast(0)
    val stored = value.coerceIn(min, max)
    currentPrefStyle().SliderRow(
        title = title,
        value = stored,
        min = min,
        max = max,
        step = step,
        steps = steps,
        summary = summary,
        label = { shown -> valueLabel?.invoke(shown) ?: shown.toString() },
        enabled = enabled,
        modifier = modifier,
        onCommit = onCommit,
    )
}

/**
 * 主操作按钮（恢复默认、立即应用这类一次性动作）：COUI 线 accent 胶囊，material 线 M3 填充按钮。
 *
 * 一屏只放一个主操作；按钮与卡片对齐靠调用方的 pageInset()，本函数不补内缩。
 */
@Composable
fun PrefButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    currentPrefStyle().Button(text, onClick, modifier, enabled)
}

/**
 * 次要操作按钮（行内动作、对话框确认这类）：COUI 线文字按钮，material 线 M3 文字按钮。
 *
 * 行尾用时不传 modifier（由行容器给间距）；独立成行时由调用方补 pageInset()。
 */
@Composable
fun PrefTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    currentPrefStyle().TextButton(text, onClick, modifier, enabled)
}

/**
 * 图标按钮（行尾的动作，如删除）：COUI 线 40dp 圆形点击面，material 线 M3 图标按钮。
 *
 * 两条线的图标资源各给一份（[icon] 走矢量、[painter] 走图片），因为两条线的图标集不同；
 * [danger] = true 用于删除这类不可逆动作（material 用 error 色，COUI 无 error 角色，靠满强度前景色）。
 */
@Composable
fun PrefIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    painter: Painter? = null,
    danger: Boolean = false,
) {
    currentPrefStyle().IconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        painter = painter,
        danger = danger,
    )
}

/** 单选项行：整行选中，行首为单选按钮；同组内互斥的条目用它 */
@Composable
fun PrefSelectRow(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    colors: ListItemColors = materialGroupColors(),
) {
    AppRadioItem(
        title = title,
        summary = summary,
        colors = colors,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
    )
}
