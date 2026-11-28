package com.luckyzyx.luckytool.ui.compose.components.style

import com.luckyzyx.luckytool.ui.compose.components.IconBadgeColors
import com.luckyzyx.luckytool.ui.compose.components.NoteTone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.PrefCardEntry
import com.luckyzyx.luckytool.ui.compose.components.PageInset
import com.luckyzyx.luckytool.ui.compose.components.PrefCardRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefStyle
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialItemContainer
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPreferenceGroup
import com.luckyzyx.luckytool.ui.compose.components.pageInset
import com.luckyzyx.luckytool.ui.compose.components.snapToStep
import kotlin.math.roundToInt

/**
 * Material 3 线的设置区块实现：Segmented* 原语与 MaterialTheme 的排版 / 配色。
 *
 * material 列表行不带水平 contentPadding，所以 [RowInset] 在这里补 16dp；分组是
 * [MaterialPreferenceGroup]（内部就是 MaterialGroup 的分段圆角）+ 自己画的标题与 footer。
 */
internal object MaterialPrefStyle : PrefStyle {

    @Composable
    override fun Title(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow) {
        Text(text, maxLines = maxLines, overflow = overflow)
    }

    @Composable
    override fun Summary(text: String, enabled: Boolean, maxLines: Int, overflow: TextOverflow) {
        Text(text, maxLines = maxLines, overflow = overflow)
    }

    @Composable
    override fun Value(text: String, enabled: Boolean) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @Composable
    override fun Chevron() {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    @Composable
    override fun Icon(icon: ImageVector?, painter: Painter?) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    @Composable
    override fun Note(text: String, modifier: Modifier, textAlign: TextAlign?, tone: NoteTone) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = when (tone) {
                NoteTone.Summary -> MaterialTheme.colorScheme.onSurfaceVariant
                NoteTone.Error -> MaterialTheme.colorScheme.error
            },
            textAlign = textAlign ?: TextAlign.Start,
        )
    }

    @Composable
    override fun MonoText(text: String, modifier: Modifier) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )
    }

    @Composable
    override fun ChoiceIndicator(radio: Boolean, checked: Boolean, enabled: Boolean) {
        // 指示图形自己不吃点击：整行的点击由所在行给，避免两个手势互相抢
        if (radio) {
            RadioButton(selected = checked, onClick = null, enabled = enabled)
        } else {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
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
        val sliderState = rememberSliderState(
            value = value.toFloat(),
            steps = steps,
            trackRange = min.toFloat()..max.toFloat(),
        )
        // 外部值变化时同步；拖动期间 value 不变，因此不会覆盖用户手势
        LaunchedEffect(value) { sliderState.value = value.toFloat() }
        Box(modifier = modifier.fillMaxWidth()) {
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
                            label(sliderState.value.roundToInt()),
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
                            val stepped = snapToStep(sliderState.value, min, max, step)
                            sliderState.value = stepped.toFloat()
                            onCommit(stepped)
                        },
                        enabled = enabled,
                    )
                }
            }
        }
    }

    @Composable
    override fun Button(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        Button(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
    }

    @Composable
    override fun TextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        TextButton(onClick = onClick, modifier = modifier, enabled = enabled) { Text(text) }
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
        val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            // 本线优先矢量图标：painter 那一份是给另一条线准备的自有图标集
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
            } else if (painter != null) {
                Icon(painter = painter, contentDescription = contentDescription, tint = tint)
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
        Column(modifier = modifier) {
            if (!title.isNullOrEmpty()) CategoryHeader(title = title, summary = summary, materialInset = PageInset)
            MaterialPreferenceGroup(
                entries = rows.map { PrefCardEntry(key = it.key, visible = it.visible, content = it.content) },
                modifier = Modifier.pageInset(),
            )
            if (!footer.isNullOrEmpty()) GroupFooter(footer)
        }
    }

    @Composable
    override fun CategoryHeader(title: String, summary: String?, materialInset: Dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
                    .padding(horizontal = materialInset)
                    .padding(top = 8.dp, bottom = if (summary == null) 8.dp else 4.dp),
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = materialInset)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }

    @Composable
    override fun GroupFooter(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp),
        )
    }

    @Composable
    override fun RowInset(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
        // material 列表行不自带水平内缩，分组之外的槽位必须自己补
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            content = content,
        )
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
        AppItem(index = 0, count = 1) {
            PrefRow(
                title = title,
                modifier = modifier,
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
        AppItem(index = 0, count = 1) {
            PrefSwitchRow(
                title = title,
                checked = checked,
                modifier = modifier,
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
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle ?: LocalTextStyle.current,
            modifier = modifier,
        )
    }

    @Composable
    override fun highlightContainer(active: Boolean): Color {
        val target = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        return animateColorAsState(target, label = "prefHighlight").value
    }

    @Composable
    override fun iconBadgeColors(colorful: Boolean, color: Color?): IconBadgeColors {
        // 多彩徽标只在 COUI 线出现，material 线忽略 colorful，底色与前景都取主题色的容器色对
        return IconBadgeColors(
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    @Composable
    override fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
        SecondaryTabRow(
            selected,
            modifier,
            TabRowDefaults.primaryContainerColor,
            TabRowDefaults.primaryContentColor,
            @Composable {
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(selected))
            },
            @Composable { HorizontalDivider() },
            {
                labels.forEachIndexed { index, label ->
                    Tab(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        text = { Text(label) },
                    )
                }
            },
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
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            modifier = modifier,
            enabled = enabled,
        )
    }

    @Composable
    override fun SuggestionChip(label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
        SuggestionChip(
            onClick = onClick,
            enabled = enabled,
            label = {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            },
            modifier = modifier,
        )
    }

    @Composable
    override fun Container(modifier: Modifier, content: @Composable () -> Unit) {
        MaterialItemContainer(modifier = modifier) { content() }
    }

    override fun containerInset(): Modifier = Modifier.pageInset()

    @Composable
    override fun Progress(modifier: Modifier, color: Color?) {
        CircularProgressIndicator(
            modifier = modifier,
            color = color ?: LocalContentColor.current,
        )
    }

    @Composable
    override fun Divider(modifier: Modifier) {
        HorizontalDivider(modifier = modifier)
    }

    @Composable
    override fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, enabled: Boolean) {
        MaterialSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Sheet(
        visible: MutableState<Boolean>,
        title: String,
        modifier: Modifier,
        closeLabel: String?,
        doneLabel: String?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        // closeLabel / doneLabel 是 COUI 顶栏的按钮，这里靠遮罩与返回键关闭
        if (!visible.value) return
        ModalBottomSheet(onDismissRequest = { visible.value = false }, modifier = modifier) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .pageInset()
                    .padding(bottom = 24.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
        }
    }
}