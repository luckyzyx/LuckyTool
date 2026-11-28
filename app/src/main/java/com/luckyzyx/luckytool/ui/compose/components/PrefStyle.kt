package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.luckyzyx.luckytool.ui.compose.components.style.CouiPrefStyle
import com.luckyzyx.luckytool.ui.compose.components.style.MaterialPrefStyle
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import kotlin.math.roundToInt

/**
 * 一套设置区块外观的实现对象。
 *
 * 页面与 DSL 只调用 PrefRows.kt / PrefCards.kt 里的公开函数，那些函数再交给本接口的当前实现去画：
 * 新增外观样式时只写一个新的实现对象（放 components/style/ 下）并在 [currentPrefStyle] 里接上分支，
 * 页面、DSL、行型的调用点都不用改。与骨架层的 UiStyle（UiStyle.kt）是同一套思路，
 * 两处的分支都是对 [UiMode] 的穷尽 when：新增外观时编译器直接在这里报错，漏写的实现跑不起来。
 *
 * 接口里只有「两条线画法不同」的东西：字号与配色、卡片容器、行内缩、按钮形态。
 * 版式归属与交互语义（哪一行可点、开关整行切换、拖动结束才回调）不属于接口，
 * 它们归行型本身，见 [PrefRow] / [PrefSwitchRow] 的说明。
 */
internal interface PrefStyle {

    /** 行标题 */
    @Composable
    fun Title(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow)

    /** 行说明 */
    @Composable
    fun Summary(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow)

    /** 行尾当前值 */
    @Composable
    fun Value(text: String, enabled: Boolean)

    /** 行尾前进箭头 */
    @Composable
    fun Chevron()

    /**
     * 行首 / 行尾图标（[icon] 与 [painter] 至少给一个）。
     * 两条的外观线各有一套图标资源，调用点两份都给，由实现挑自己那套：本线优先 [painter]，
     * 另一条线优先 [icon]，缺哪份就回落另一份。
     */
    @Composable
    fun Icon(icon: ImageVector?, painter: Painter?)

    /**
     * 分组之外的说明文字。
     *
     * [tone] 只有两档：[NoteTone.Summary] 是普通说明，[NoteTone.Error] 用于错误态。
     */
    @Composable
    fun Note(text: String, modifier: Modifier, textAlign: TextAlign?, tone: NoteTone = NoteTone.Summary)

    /**
     * 等宽字体的一行文字：命令、十六进制数值这类需要逐字符对齐的内容。
     * 字号与颜色仍随本线，调用方只管换行与位置。
     */
    @Composable
    fun MonoText(text: String, modifier: Modifier)

    /**
     * 行首的选中指示器：[radio] 为单选钮（[checked] 是选中态），否则为勾选框。
     * 只画图形：点击与无障碍角色由所在行自己给。
     */
    @Composable
    fun ChoiceIndicator(radio: Boolean, checked: Boolean, enabled: Boolean)

    /** 行的容器底色：[active] 时高亮（搜索命中、当前项），否则透明 */
    @Composable
    fun highlightContainer(active: Boolean): Color

    /**
     * 图标底座的配色：[colorful] 时用 [color]（为空回落 [FallbackBadgeColor]）铺底，
     * 否则用本线的强调色；前景色由实现挑自己那套。
     * 多彩只在支持它的线生效（目前是 COUI），其余线忽略 [colorful] 走强调色。
     */
    @Composable
    fun iconBadgeColors(colorful: Boolean, color: Color?): IconBadgeColors

    /** 滑条行：标题与当前值一行、滑条一行；取整与回调时机由调用方给 */
    @Composable
    fun SliderRow(
        title: String,
        value: Int,
        min: Int,
        max: Int,
        step: Int,
        steps: Int,
        summary: String?,
        label: (Int) -> String,
        enabled: Boolean,
        modifier: Modifier,
        onCommit: (Int) -> Unit,
    )

    /** 主操作按钮 */
    @Composable
    fun Button(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean)

    /** 次要操作按钮 */
    @Composable
    fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean)

    /** 图标按钮，[icon] / [painter] 的取舍同 [Icon] */
    @Composable
    fun IconButton(
        onClick: () -> Unit,
        contentDescription: String,
        modifier: Modifier,
        enabled: Boolean,
        icon: ImageVector?,
        painter: Painter?,
        danger: Boolean,
    )

    /** 分组：标题 / 说明 / 卡片 / 行间分隔线 / 分组上沿留白都在这里给 */
    @Composable
    fun Section(
        rows: List<PrefCardRow>,
        modifier: Modifier,
        title: String?,
        summary: String?,
        footer: String?,
    )

    /**
     * 分组标题 + 可选说明（画在卡片上方）。
     *
     * [materialInset] 只影响 material 线：宿主列表本身没有水平 contentPadding，所以自带 16dp；
     * 宿主已经给过 16dp（例如 custom() 里的 PrefRowInset）就传 0dp，避免叠成 32dp。
     */
    @Composable
    fun CategoryHeader(title: String, summary: String?, materialInset: Dp)

    /** 分组底部说明（画在卡片下方，与标题下方那行说明同一档字号与内缩） */
    @Composable
    fun GroupFooter(text: String)

    /** 行内容的水平内缩包装（分组之外的行要自带内缩） */
    @Composable
    fun RowInset(modifier: Modifier, content: @Composable BoxScope.() -> Unit)

    /** 不属于任何分组的独立卡片 */
    @Composable
    fun Card(
        title: String,
        modifier: Modifier,
        summary: String?,
        onClick: (() -> Unit)?,
        onLongClick: (() -> Unit)?,
        enabled: Boolean,
        trailing: (@Composable () -> Unit)?,
    )

    /** 不属于任何分组的独立开关卡片 */
    @Composable
    fun SwitchCard(
        title: String,
        checked: Boolean,
        modifier: Modifier,
        summary: String?,
        enabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    )

    /**
     * 单行输入框：只换控件与配色，值、校验与落盘时机仍由调用方持有。
     *
     * [leadingIcon] 原样透传，图标自己的着色由调用方给——两条线的输入框对图标默认色的处理不同。
     *
     * [label] 是输入框自己的字段名，填入值后仍然显示；[placeholder] 只在空值时顶替它。
     * [readOnly] 用于值只由别的控件改写的格子：COUI 的输入框没有只读态，实现里忽略即可。
     * [isError] 标出格式不合法的输入；[textStyle] 只对 material 线生效（例如十六进制框要等宽字体，
     * COUI 的字体由库统一决定）。[trailingIcon] 放清空键、筛选入口这类行尾控件。
     */
    @Composable
    fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        singleLine: Boolean,
        leadingIcon: (@Composable () -> Unit)?,
        label: String? = null,
        placeholder: String? = null,
        readOnly: Boolean = false,
        trailingIcon: (@Composable () -> Unit)? = null,
        isError: Boolean = false,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        textStyle: TextStyle? = null,
    )

    /** 顶部分段切换：Material 线是带下划线的次级 Tab 行，COUI 线是库的分段选择器 */
    @Composable
    fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier)

    /** 带选中态的按钮（筛选条件用） */
    @Composable
    fun FilterChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    )

    /** 只执行动作、不带选中态的按钮（计数与动作入口用） */
    @Composable
    fun SuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean)

    /**
     * 非行内容的卡片容器。
     *
     * COUI 线的卡片由调用点的分段卡片（AppItem）给，这里只排纵向内容；material 线的卡片不自带底色，
     * 由这里补一块卡片表面，否则内容会露在页面背景上。
     */
    @Composable
    fun Container(modifier: Modifier, content: @Composable () -> Unit)

    /** 卡片容器的水平内缩：COUI 卡片底色自带 16dp，material 卡片不自带，需要自己补 */
    fun containerInset(): Modifier

    /**
     * 不停转的进度指示器：尺寸与居中由调用方给（[Note] 之类是静态文字，替代不了它）。
     * [color] 为空时用各线自己的强调色，闪屏这类压在自家底色上的地方可以指定前景色。
     */
    @Composable
    fun Progress(modifier: Modifier, color: Color? = null)

    /** 列表与面板里的横向分隔线 */
    @Composable
    fun Divider(modifier: Modifier)

    /** 面板里的独立开关：只画控件，整行切换请用 [PrefSwitchRow] */
    @Composable
    fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, enabled: Boolean)

    /**
     * 底部面板。
     *
     * [visible] 常驻传入：material 线只在可见时组合，COUI 线始终组合、显隐交给面板自身。
     * 标题进顶栏；[closeLabel] / [doneLabel] 只是 COUI 顶栏的左右按钮（都只收起面板），
     * material 线靠遮罩与返回键关闭，两个标签用不上。
     * [content] 拿到的 Column 已带页面水平内缩与底部留白，调用方不用再补。
     */
    @Composable
    fun Sheet(
        visible: MutableState<Boolean>,
        title: String,
        modifier: Modifier,
        closeLabel: String?,
        doneLabel: String?,
        content: @Composable ColumnScope.() -> Unit,
    )
}

/** 当前外观对应的设置区块实现：新增样式只在这里加分支，并在 [UiMode] 里加条目（穷尽 when 会强制） */
@Composable
internal fun currentPrefStyle(): PrefStyle = when (LocalUiMode.current) {
    UiMode.Coui -> CouiPrefStyle
    UiMode.Material -> MaterialPrefStyle
}

/** 把连续拖动值吸附到 [step] 刻度并夹进区间：滑条拖动结束时两条线共用同一套取整 */
internal fun snapToStep(raw: Float, min: Int, max: Int, step: Int): Int =
    (((raw - min) / step).roundToInt() * step + min).coerceIn(min, max)

/** 说明文字的语气：[Summary] 是普通说明，[Error] 用于错误态 */
enum class NoteTone { Summary, Error }

/** 图标底座没给颜色时的兜底底色（与调用点原先的写死值一致） */
internal val FallbackBadgeColor = Color(0xFF607D8B)

/** 图标底座的底色与前景色 */
internal data class IconBadgeColors(val container: Color, val content: Color)
