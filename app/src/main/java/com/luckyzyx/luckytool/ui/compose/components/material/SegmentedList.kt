/*
 * Ported from KernelSU manager (GPL-3.0) `ui/component/material/SegmentedList.kt`.
 */
package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }
private val SegmentedOuterRadius = DesignTokens.CardRadius
private val SegmentedInnerRadius = DesignTokens.ItemInnerRadius
private const val SegmentedSpringStiffness = 800f
private const val SegmentedSpringDamping = 0.9f

@DslMarker
annotation class SegmentedColumnDsl

/** 分段条目配色（containerColor 可覆盖，用于搜索跳转高亮等场景） */
@Composable
fun defaultSegmentedColors(
    containerColor: Color = colorScheme.surfaceBright,
): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = containerColor,
    disabledContainerColor = containerColor,
    supportingContentColor = colorScheme.onSurfaceVariant,
    // 选中态对齐主题强调色，覆盖 material3 默认的 secondaryContainer（会偏离主题色）
    selectedContainerColor = colorScheme.primaryContainer,
    selectedContentColor = colorScheme.onPrimaryContainer,
    selectedLeadingContentColor = colorScheme.onPrimaryContainer,
    selectedTrailingContentColor = colorScheme.onPrimaryContainer,
    selectedSupportingContentColor = colorScheme.onPrimaryContainer,
)

/**
 * 分组内条目形状：单条卡片保持原有整块圆角；分组内相邻边取 [SegmentedInnerRadius]
 * （0dp，与 Miuix 线 [com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefItemShape] 一致），
 * 使同一分组的条目拼成一张连续卡片，而不是各自成卡。
 */
@Composable
private fun defaultSingleSegmentedShape(index: Int, count: Int): ListItemShapes {
    val base = ListItemDefaults.segmentedShapes(index, count)
    if (count <= 1) {
        return base.copy(shape = MaterialTheme.shapes.large)
    }
    return base.copy(
        shape = RoundedCornerShape(
            topStart = if (index <= 0) SegmentedOuterRadius else SegmentedInnerRadius,
            topEnd = if (index <= 0) SegmentedOuterRadius else SegmentedInnerRadius,
            bottomStart = if (index >= count - 1) SegmentedOuterRadius else SegmentedInnerRadius,
            bottomEnd = if (index >= count - 1) SegmentedOuterRadius else SegmentedInnerRadius,
        ),
    )
}

/** 静态分段列：传入已确定的条目内容列表（不可增删时使用） */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    visibleLen: Int = 0,
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            content.forEachIndexed { index, itemContent ->
                CompositionLocalProvider(
                    LocalListItemShapes provides defaultSingleSegmentedShape(
                        index = index,
                        count = if (visibleLen > 0) visibleLen else content.size,
                    ),
                ) {
                    itemContent()
                }
            }
        }
    }
}

@SegmentedColumnDsl
class SegmentedColumnScope {
    internal data class Entry(
        val key: Any?,
        val visible: Boolean,
        val content: @Composable () -> Unit,
    )

    internal val entries = mutableListOf<Entry>()

    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        entries.add(Entry(key ?: entries.size, visible, content))
    }
}

/** 动态分段列：条目可显隐，显隐时带弹性过渡（对齐 KernelSU） */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    content: SegmentedColumnScope.() -> Unit,
) {
    val entries = SegmentedColumnScope().apply(content).entries
    if (entries.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            )
        }

        val floatSpring = spring<Float>(SegmentedSpringDamping, SegmentedSpringStiffness)
        val dpSpring = spring<Dp>(SegmentedSpringDamping, SegmentedSpringStiffness)

        val progresses = entries.mapIndexed { index, entry ->
            key(entry.key ?: index) {
                animateFloatAsState(
                    targetValue = if (entry.visible) 1f else 0f,
                    animationSpec = floatSpring,
                    label = "SegmentedProgress",
                )
            }
        }

        val firstVisible = entries.indexOfFirst { it.visible }
        val lastVisible = entries.indexOfLast { it.visible }

        Layout(
            content = {
                entries.forEachIndexed { index, entry ->
                    key(entry.key ?: index) {
                        val isFirst = if (firstVisible == -1) index == 0 else index == firstVisible
                        val isLast = if (lastVisible == -1) index == entries.lastIndex else index == lastVisible

                        val topRadius by animateDpAsState(
                            if (isFirst) SegmentedOuterRadius else SegmentedInnerRadius,
                            dpSpring, label = "SegmentedTopRadius",
                        )
                        val bottomRadius by animateDpAsState(
                            if (isLast) SegmentedOuterRadius else SegmentedInnerRadius,
                            dpSpring, label = "SegmentedBottomRadius",
                        )
                        val gap by animateDpAsState(
                            if (isFirst) 0.dp else DesignTokens.ItemGap,
                            dpSpring, label = "SegmentedGap",
                        )

                        val shape = RoundedCornerShape(
                            topStart = topRadius, topEnd = topRadius,
                            bottomStart = bottomRadius, bottomEnd = bottomRadius,
                        )

                        Box(
                            modifier = Modifier
                                .zIndex(if (entry.visible) (entries.size - index).toFloat() else -index.toFloat())
                                .graphicsLayer {
                                    val progress = progresses[index].value.coerceAtLeast(0f)
                                    clip = true
                                    this.shape = object : Shape {
                                        override fun createOutline(
                                            size: Size,
                                            layoutDirection: LayoutDirection,
                                            density: Density,
                                        ): Outline = Outline.Rectangle(Rect(0f, 0f, size.width, size.height * progress))
                                    }
                                    alpha = (progress * 1.5f).coerceIn(0f, 1f)
                                },
                        ) {
                            CompositionLocalProvider(
                                LocalListItemShapes provides ListItemDefaults.segmentedShapes(0, 1).copy(shape = shape),
                            ) {
                                Column(modifier = Modifier.padding(top = gap)) {
                                    entry.content()
                                }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val positions = IntArray(placeables.size)
            var y = 0f
            placeables.forEachIndexed { index, placeable ->
                positions[index] = y.roundToInt()
                y += placeable.height * progresses[index].value.coerceAtLeast(0f)
            }
            layout(constraints.maxWidth, y.roundToInt().coerceAtLeast(0)) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(0, positions[index])
                }
            }
        }
    }
}

@Composable
fun SegmentedItem(
    index: Int,
    count: Int,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalListItemShapes provides defaultSingleSegmentedShape(index, count),
    ) {
        content()
    }
}

@Composable
fun SegmentedItemContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colorScheme.surfaceBright,
        shape = shapes.shape,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SegmentedListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ListItemColors = defaultSegmentedColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = Alignment.CenterVertically,
        content = headlineContent,
    )
}

/** 开关条目：点击整行切换，尾部为 Expressive 开关 */
@Composable
fun SegmentedSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    colors: ListItemColors = defaultSegmentedColors(),
    checked: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    SegmentedListItem(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onCheckedChange(!checked)
        },
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors,
        headlineContent = { Text(title) },
        leadingContent = leading ?: icon?.let { { Icon(it, title) } },
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                interactionSource = interactionSource,
            )
        },
        supportingContent = summary?.let { { Text(it) } },
    )
}

/** 下拉选择条目：尾部显示当前值，点击在按压点弹出菜单 */
@Composable
fun SegmentedDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = defaultSegmentedColors(),
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var anchorOffset by remember { mutableStateOf(IntOffset.Zero) }

    val hasItems = items.isNotEmpty()
    val safeIndex = if (hasItems) {
        selectedIndex.coerceIn(0, items.lastIndex)
    } else {
        -1
    }

    Box(modifier = Modifier.trackPressPosition { anchorOffset = it.round() }) {
        SegmentedListItem(
            onClick = if (enabled) {
                {
                    onClick?.invoke()
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    expanded = true
                }
            } else null,
            enabled = enabled,
            colors = colors,
            leadingContent = leading ?: icon?.let { { Icon(it, title) } },
            headlineContent = { Text(text = title) },
            supportingContent = summary?.let { { Text(it) } },
            trailingContent = {
                Text(
                    text = if (hasItems && safeIndex >= 0) items[safeIndex] else "",
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(0.3f),
                    color = if (enabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    // material3 1.5.0 尾部默认 ItemTrailingSupportingTextFont=LabelSmall(11sp)，
                    // 覆盖为 bodyMedium(14sp)，与取值条目 PrefValueRow 的尾部值字号一致
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        OffsetAnchoredExpressiveMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchorOffset = anchorOffset,
        ) {
            items.forEachIndexed { index, text ->
                SelectableDropdownMenuItem(
                    text = { Text(text) },
                    selected = index == safeIndex,
                    onClick = {
                        if (index in items.indices) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onItemSelected(index)
                        }
                        expanded = false
                    },
                    shapes = MenuDefaults.itemShape(index = index, count = items.size),
                    // 选中态对齐主题强调色，覆盖 material3 默认的 tertiaryContainer（粉色调）
                    colors = MenuDefaults.selectableItemColors(
                        selectedTextColor = colorScheme.onPrimaryContainer,
                        selectedContainerColor = colorScheme.primaryContainer,
                        selectedLeadingIconColor = colorScheme.onPrimaryContainer,
                    ),
                    selectedLeadingIcon = {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                        )
                    },
                )
            }
        }
    }
}

/** 单选项条目：点击整行选中，头部为单选按钮 */
@Composable
fun SegmentedRadioItem(
    title: String,
    summary: String? = null,
    colors: ListItemColors = defaultSegmentedColors(),
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SegmentedListItem(
        selected = selected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        enabled = enabled,
        colors = colors,
        content = { Text(title) },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

/** 文本输入条目：整行聚焦，头部为输入框 */
@Composable
fun SegmentedTextField(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    colors: ListItemColors = defaultSegmentedColors(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(colorScheme.primary),
    placeholder: @Composable (() -> Unit)? = { Text("-") },
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    SegmentedListItem(
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester),
        colors = colors,
        onClick = { focusRequester.requestFocus() },
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        headlineContent = {
            Column {
                if (label.isNotEmpty()) {
                    Text(text = label, color = if (isError) colorScheme.error else colors.contentColor)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle.copy(
                        colors.contentColor,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    visualTransformation = visualTransformation,
                    onTextLayout = onTextLayout,
                    interactionSource = interactionSource,
                    cursorBrush = cursorBrush,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && placeholder != null) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                CompositionLocalProvider(
                                    LocalContentColor provides colors.contentColor
                                ) {
                                    ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                        placeholder()
                                    }
                                }
                            }
                        }
                        innerTextField()
                    }
                )
            }
        }
    )
}
