package com.luckyzyx.luckytool.ui.compose.components.style

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.checkbox.Checkbox as CouiCheckbox
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.radiobutton.RadioButton as CouiRadioButton
import com.itos.coui.layout.Footer as CouiFooter
import com.itos.coui.layout.Divider as CouiDivider
import com.itos.coui.layout.Item as CouiItem
import com.itos.coui.sheet.BottomSheet as CouiBottomSheet
import com.itos.coui.sheet.CouiSheetButton
import com.itos.coui.slider.CouiSliderDefaults
import com.itos.coui.slider.Slider as CouiSlider
import com.itos.coui.switch.Switch as CouiSwitch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.FallbackBadgeColor
import com.luckyzyx.luckytool.ui.compose.components.IconBadgeColors
import com.luckyzyx.luckytool.ui.compose.components.NoteTone
import com.luckyzyx.luckytool.ui.compose.components.CouiCard
import com.luckyzyx.luckytool.ui.compose.components.CouiPrefGroup
import com.luckyzyx.luckytool.ui.compose.components.CouiRowSummary
import com.luckyzyx.luckytool.ui.compose.components.CouiRowTitle
import com.luckyzyx.luckytool.ui.compose.components.CouiRowValue
import com.luckyzyx.luckytool.ui.compose.components.CouiRowsColumn
import com.luckyzyx.luckytool.ui.compose.components.PrefCardRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefStyle
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilledButton
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiCircularProgressIndicator
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiFilterChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiSegmentedTabs
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiSuggestionChip
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextField
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextButton
import com.luckyzyx.luckytool.ui.compose.components.couiTrailingIconTint
import com.luckyzyx.luckytool.ui.compose.components.pageInset
import com.luckyzyx.luckytool.ui.compose.components.snapToStep
import kotlin.math.roundToInt

/**
 * COUI 线的设置区块实现：库的 Item / Slider / 按钮与 COUI 色板。
 *
 * 行内缩由库的 Item 自带，所以 [RowInset] 在这里不补边距；卡片是 [CouiCard]（24dp 圆角 + 卡片底色），
 * 分组用库的 Group（[CouiPrefGroup]）—— 标题、说明、footer、组内分隔线都由它画。
 */
internal object CouiPrefStyle : PrefStyle {

    @Composable
    override fun Title(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow) {
        CouiRowTitle(text, enabled, CouiPageDefaults.themeColors(), maxLines, overflow)
    }

    @Composable
    override fun Summary(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow) {
        CouiRowSummary(text, enabled, CouiPageDefaults.themeColors(), maxLines, overflow)
    }

    @Composable
    override fun Value(text: String, enabled: Boolean) {
        CouiRowValue(text = text, enabled = enabled, colors = CouiPageDefaults.themeColors())
    }

    @Composable
    override fun Chevron() {
        Icon(
            imageVector = CouiIconsExt.Next,
            contentDescription = null,
            modifier = Modifier.size(width = 12.dp, height = 24.dp),
            tint = couiTrailingIconTint(CouiTheme.isDark),
        )
    }

    @Composable
    override fun Icon(icon: ImageVector?, painter: Painter?) {
        val resolved = painter ?: icon?.let { rememberVectorPainter(it) }
        if (resolved != null) {
            Icon(
                painter = resolved,
                contentDescription = null,
                tint = CouiTheme.colorScheme.onSurface,
            )
        }
    }

    @Composable
    override fun Note(text: String, modifier: Modifier, textAlign: TextAlign?, tone: NoteTone) {
        CouiText(
            modifier = modifier,
            text = text,
            kind = CouiTextKind.ItemSummary,
            // COUI 色板没有 error 角色：错误态退回正文满强度，靠文案本身说明问题
            color = when (tone) {
                NoteTone.Summary -> CouiPageDefaults.themeColors().summary
                NoteTone.Error -> CouiTheme.colorScheme.onSurface
            },
            textAlign = textAlign,
        )
    }

    @Composable
    override fun MonoText(text: String, modifier: Modifier) {
        BasicText(
            text = text,
            modifier = modifier,
            style = rememberCouiTextStyle(CouiTextKind.Search).copy(
                color = CouiTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }

    @Composable
    override fun ChoiceIndicator(radio: Boolean, checked: Boolean, enabled: Boolean) {
        if (radio) {
            CouiRadioButton(selected = checked, enabled = enabled)
        } else {
            CouiCheckbox(checked = checked, enabled = enabled)
        }
    }

    @Composable
    override fun highlightContainer(active: Boolean): Color = Color.Transparent

    @Composable
    override fun iconBadgeColors(colorful: Boolean, color: Color?): IconBadgeColors {
        val colors = CouiPageDefaults.themeColors()
        return if (colorful) {
            IconBadgeColors(container = color ?: FallbackBadgeColor, content = Color.White)
        } else {
            // CouiPageColors 只有 accent，前景色取主题色板的 onAccent
            IconBadgeColors(container = colors.accent, content = CouiTheme.colorScheme.onAccent)
        }
    }

    @Composable
    override fun SliderRow(
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
    ) {
        val dark = CouiTheme.isDark
        val pageColors = CouiPageDefaults.themeColors()
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(value.toFloat()) }
        // 外部值变化时同步回页内值；拖动期间 value 不变，因此不会覆盖用户手势
        LaunchedEffect(value) { if (!dragging) dragValue = value.toFloat() }
        val shown = if (dragging) dragValue else value.toFloat()
        Column(modifier = modifier.fillMaxWidth()) {
            // 标题行：标题 + 当前值，行高与开关行一致
            CouiItem(
                title = { CouiRowTitle(title, enabled, pageColors) },
                summary = summary?.let { text -> { CouiRowSummary(text, enabled, pageColors) } },
                trailing = {
                    CouiRowValue(
                        text = label(shown.roundToInt()),
                        enabled = enabled,
                        colors = pageColors,
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
                colors = CouiSliderDefaults.colors(pageColors.accent, dark, enabled),
                dark = dark,
                valueRange = min.toFloat()..max.toFloat(),
                steps = steps,
                enabled = enabled,
                onValueChangeFinished = {
                    dragging = false
                    val stepped = snapToStep(dragValue, min, max, step)
                    dragValue = stepped.toFloat()
                    onCommit(stepped)
                },
            )
        }
    }

    @Composable
    override fun Button(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        CouiFilledButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    }

    @Composable
    override fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        CouiTextButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    }

    @Composable
    override fun IconButton(
        onClick: () -> Unit,
        contentDescription: String,
        modifier: Modifier,
        enabled: Boolean,
        icon: ImageVector?,
        painter: Painter?,
        danger: Boolean,
    ) {
        // 描述挂在点击面上：图标自己不再声明，避免读屏重复播报
        CouiIconButton(
            onClick = onClick,
            contentDescription = contentDescription,
            modifier = modifier,
            enabled = enabled,
        ) {
            val resolved = painter ?: icon?.let { rememberVectorPainter(it) }
            if (resolved != null) {
                Icon(
                    painter = resolved,
                    contentDescription = null,
                    tint = if (danger) {
                        CouiTheme.colorScheme.onSurface
                    } else {
                        CouiTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }

    @Composable
    override fun Section(
        rows: List<PrefCardRow>,
        modifier: Modifier,
        title: String?,
        summary: String?,
        footer: String?,
    ) {
        CouiPrefGroup(title = title, summary = summary, footer = footer, modifier = modifier) {
            CouiRowsColumn(rows = rows)
        }
    }

    @Composable
    override fun CategoryHeader(title: String, summary: String?, materialInset: Dp) {
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

    @Composable
    override fun GroupFooter(text: String) {
        CouiFooter(text = text, color = CouiPageDefaults.themeColors().summary)
    }

    @Composable
    override fun RowInset(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
        // 库的 Item 自带 16dp 行内缩，这里只负责把整行撑满
        Box(modifier = modifier.fillMaxWidth(), content = content)
    }

    @Composable
    override fun Card(
        title: String,
        modifier: Modifier,
        summary: String?,
        onClick: (() -> Unit)?,
        onLongClick: (() -> Unit)?,
        enabled: Boolean,
        trailing: (@Composable () -> Unit)?,
    ) {
        // 单条即整张卡：调用方的 modifier 交给卡片（横向内缩与底色都在卡上），行内不再叠
        CouiCard(modifier = modifier) {
            PrefRow(
                title = title,
                summary = summary,
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                trailing = trailing,
            )
        }
    }

    @Composable
    override fun SwitchCard(
        title: String,
        checked: Boolean,
        modifier: Modifier,
        summary: String?,
        enabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        CouiCard(modifier = modifier) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                summary = summary,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }

    @Composable
    override fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        singleLine: Boolean,
        leadingIcon: (@Composable () -> Unit)?,
        label: String?,
        placeholder: String?,
        readOnly: Boolean,
        trailingIcon: (@Composable () -> Unit)?,
        isError: Boolean,
        keyboardOptions: KeyboardOptions,
        textStyle: TextStyle?,
    ) {
        // 库的输入框没有只读态：只由别的控件改写的格子在这里仍可聚焦，落盘时机依旧由调用方守着；
        // 字体也由库统一决定，textStyle 用不上
        CouiTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            label = label,
            placeholder = placeholder,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }

    @Composable
    override fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
        CouiSegmentedTabs(
            labels = labels,
            selectedIndex = selected,
            onSelect = onSelect,
            modifier = modifier,
        )
    }

    @Composable
    override fun FilterChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
    ) {
        CouiFilterChip(
            label = label,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun SuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        CouiSuggestionChip(label = label, onClick = onClick, modifier = modifier, enabled = enabled)
    }

    @Composable
    override fun Container(modifier: Modifier, content: @Composable () -> Unit) {
        Column(modifier = modifier.fillMaxWidth()) { content() }
    }

    override fun containerInset(): Modifier = Modifier

    @Composable
    override fun Progress(modifier: Modifier, color: Color?) {
        CouiCircularProgressIndicator(
            modifier = modifier,
            color = color ?: CouiTheme.colorScheme.accent,
        )
    }

    @Composable
    override fun Divider(modifier: Modifier) {
        // 两端内缩仍由调用点通过 modifier 决定，这里只换成库内分隔线
        CouiDivider(modifier = modifier, startIndent = 0.dp, endIndent = 0.dp)
    }

    @Composable
    override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, enabled: Boolean) {
        CouiSwitch(
            modifier = modifier,
            checked = checked,
            // 轻点与拖动都只上报切换动作，取反由调用方持有
            onCheckedChange = onCheckedChange,
            dark = CouiTheme.isDark,
            enabled = enabled,
            onDragCheckedChange = onCheckedChange,
        )
    }

    @Composable
    override fun Sheet(
        visible: MutableState<Boolean>,
        title: String,
        modifier: Modifier,
        closeLabel: String?,
        doneLabel: String?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val dismiss = { visible.value = false }
        CouiBottomSheet(
            modifier = modifier,
            visible = visible,
            // 半屏足够容纳排序与筛选两段；内容再多只在面板内部滚动
            sheetHeight = LocalConfiguration.current.screenHeightDp.dp / 2,
            showDragHandle = true,
            leadingButton = closeLabel?.let { CouiSheetButton(label = it, onClick = dismiss) },
            titleContent = { CouiText(text = title, kind = CouiTextKind.SheetTitle) },
            trailingButton = doneLabel?.let { CouiSheetButton(label = it, onClick = dismiss) },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .pageInset()
                    .padding(bottom = 24.dp)
            ) {
                content()
            }
        }
    }
}