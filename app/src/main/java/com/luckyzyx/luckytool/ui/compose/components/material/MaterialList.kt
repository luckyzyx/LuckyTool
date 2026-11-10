/*
 * Material 分段列表组件（GPL-3.0）。
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
import androidx.compose.material3.SegmentedListItem as M3SegmentedListItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextField
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalListItemShapes = compositionLocalOf<ListItemShapes?> { null }
private val SegmentedOuterRadius = DesignTokens.CardRadius
private val SegmentedInnerRadius = DesignTokens.ItemInnerRadius
private const val SegmentedSpringStiffness = 800f
private const val SegmentedSpringDamping = 0.9f

/**
 * 把形状包成非 [androidx.compose.foundation.shape.CornerBasedShape]，让 material3 的 ListItem 不对它做形状动画。
 *
 * material3 1.5.0-beta01 内部的 AnimatedShapeState 用 Interpolatable.lerp 插值圆角、且完全不钳制：
 * 阻尼比 0.9 的 FastSpatial 弹簧在「按压形状 16dp → 我们的分段形状（内边 [SegmentedInnerRadius] = 0dp）」
 * 之间过渡时会过冲，把圆角插成负值（16dp→0dp 过冲约 2.45% = -0.39dp，@3x 即 -1.1742859px），
 * BackgroundNode 取 outline 时抛 "Corner size in Px can't be negative"。
 * 条目短按后会立刻导航/销毁，长按则留在组合里，所以长按更容易命中这个过冲帧。
 *
 * ListItemDefaults.shapeForInteraction 只在六个变体全是 RoundedCornerShape / CornerBasedShape 时才调
 * rememberAnimatedShape，因此这里把六个变体统一换成非 CornerBasedShape 的包装形状：M3 会直接使用它、
 * 完全不做插值；圆角动画仍由本文件自己的 animateDpAsState + 钳制负责。
 */
internal class StaticShape(private val delegate: Shape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        delegate.createOutline(size, layoutDirection, density)

    override fun equals(other: Any?): Boolean =
        this === other || (other is StaticShape && delegate == other.delegate)

    override fun hashCode(): Int = delegate.hashCode()
}

/** 六个状态统一为 [shapeOverride]（默认取当前 [ListItemShapes.shape]）的静态形状，见 [StaticShape] */
internal fun ListItemShapes.withoutShapeAnimation(shapeOverride: Shape = shape): ListItemShapes {
    val static = StaticShape(shapeOverride)
    return copy(
        shape = static,
        selectedShape = static,
        pressedShape = static,
        focusedShape = static,
        hoveredShape = static,
        draggedShape = static,
    )
}

@DslMarker
annotation class MaterialGroupDsl

/** 分段条目配色（containerColor 可覆盖，用于搜索跳转高亮等场景） */
@Composable
fun materialGroupColors(
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
 * 分组内条目形状：单条卡片保持原有整块圆角；分组内相邻边取 [SegmentedInnerRadius]（0dp），
 * 使同一分组的条目拼成一张连续卡片，而不是各自成卡。
 *
 * 六个状态统一为该形状、不走 material3 的形状动画（见 [StaticShape]）：内边 0dp 与 M3 的按压形状
 * （16dp）之间做过冲插值会算出负圆角并崩溃。
 */
@Composable
private fun defaultSingleSegmentedShape(index: Int, count: Int): ListItemShapes {
    val base = ListItemDefaults.segmentedShapes(index, count)
    val shape = if (count <= 1) {
        MaterialTheme.shapes.large
    } else {
        RoundedCornerShape(
            topStart = if (index <= 0) SegmentedOuterRadius else SegmentedInnerRadius,
            topEnd = if (index <= 0) SegmentedOuterRadius else SegmentedInnerRadius,
            bottomStart = if (index >= count - 1) SegmentedOuterRadius else SegmentedInnerRadius,
            bottomEnd = if (index >= count - 1) SegmentedOuterRadius else SegmentedInnerRadius,
        )
    }
    return base.withoutShapeAnimation(shape)
}

/** 静态分段列：传入已确定的条目内容列表（不可增删时使用） */
@Composable
fun MaterialGroup(
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

@MaterialGroupDsl
class MaterialGroupScope {
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

/** 动态分段列：条目可显隐，显隐时带弹性过渡 */
@Composable
fun MaterialGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    content: MaterialGroupScope.() -> Unit,
) {
    val entries = MaterialGroupScope().apply(content).entries
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

                        // 阻尼比 0.9 < 1 的弹簧会冲过头：16dp→0dp 的过程中 radius 可短暂为负，
                        // 负圆角会触发 RoundedCornerShape 的「Corner size in Px can't be negative」崩溃
                        val shape = RoundedCornerShape(
                            topStart = topRadius.coerceAtLeast(0.dp),
                            topEnd = topRadius.coerceAtLeast(0.dp),
                            bottomStart = bottomRadius.coerceAtLeast(0.dp),
                            bottomEnd = bottomRadius.coerceAtLeast(0.dp),
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
                                LocalListItemShapes provides ListItemDefaults.segmentedShapes(0, 1).withoutShapeAnimation(shape),
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
fun MaterialItem(
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
fun MaterialItemContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1).withoutShapeAnimation()
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
fun MaterialListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: ListItemColors = materialGroupColors(),
    interactionSource: MutableInteractionSource? = null,
    headlineContent: @Composable () -> Unit,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    M3SegmentedListItem(
        onClick = onClick ?: {},
        onLongClick = onLongClick,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1).withoutShapeAnimation(),
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
fun MaterialSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    colors: ListItemColors = materialGroupColors(),
    checked: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    MaterialListItem(
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
            MaterialSwitch(
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
fun MaterialDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    colors: ListItemColors = materialGroupColors(),
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
        MaterialListItem(
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
        MaterialAnchoredMenu(
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
fun MaterialRadioItem(
    title: String,
    summary: String? = null,
    colors: ListItemColors = materialGroupColors(),
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    M3SegmentedListItem(
        selected = selected,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1).withoutShapeAnimation(),
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
fun MaterialTextField(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    colors: ListItemColors = materialGroupColors(),
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
    // 缺省占位文案跟随当前线：COUI 线取 COUI 字号与 onSurfaceVariant 字色，Material 线保持原样
    placeholder: @Composable (() -> Unit)? = {
        when (LocalUiMode.current) {
            UiMode.Material -> Text("-")
            UiMode.Coui -> CouiText(
                text = "-",
                kind = CouiTextKind.ItemSummary,
                color = CouiTheme.colorScheme.onSurfaceVariant,
            )
        }
    },
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    // 外部可注入焦点请求器（如搜索页进入即自动聚焦）；缺省内部自持，行为与原来一致
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    when (LocalUiMode.current) {
        UiMode.Material -> MaterialTextRow(
            modifier = modifier,
            label = label,
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            colors = colors,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = visualTransformation,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            cursorBrush = cursorBrush,
            placeholder = placeholder,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            isError = isError,
            focusRequester = focusRequester,
        )

        UiMode.Coui -> CouiTextRow(
            modifier = modifier,
            label = label,
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            placeholder = placeholder,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            isError = isError,
            focusRequester = focusRequester,
        )
    }
}

/**
 * Material 线的文本输入行：整行是 material3 的分段条目，条目本身给出卡片底色与点击聚焦；
 * 内部用 [BasicTextField] 承载输入，占位文案只在输入为空时画在输入层下面。
 *
 * 只在 Material 线调用；[CouiTextRow] 是 COUI 线的对应实现。
 */
@Composable
private fun MaterialTextRow(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    readOnly: Boolean,
    colors: ListItemColors,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    visualTransformation: VisualTransformation,
    onTextLayout: (TextLayoutResult) -> Unit,
    interactionSource: MutableInteractionSource,
    cursorBrush: Brush,
    placeholder: @Composable (() -> Unit)?,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    supportingContent: @Composable (() -> Unit)?,
    isError: Boolean,
    focusRequester: FocusRequester,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    MaterialListItem(
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

/**
 * COUI 线的文本输入行。
 *
 * 内容直接铺在调用方的分组卡片里：COUI 线由外层卡片统一上卡片底色与圆角，
 * 这里再画一层会叠出双层圆角，所以只补 COUI 的行内缩。
 *
 * 与 Material 行的差异：
 * - 输入框用 [CouiTextField]，自带描边、胶囊形状与 48dp 最小高度；
 * - COUI 没有 error 配色角色，[isError] 用 onSurface 满强度表达（标题与描边一起变）；
 * - CouiTextField 没有只读、minLines、onTextLayout 参数：[readOnly] 用「吞掉编辑」实现，
 *   另外两项是 Material 行专有的排版细节，COUI 行不参与。
 */
@Composable
private fun CouiTextRow(
    modifier: Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    visualTransformation: VisualTransformation,
    interactionSource: MutableInteractionSource,
    placeholder: @Composable (() -> Unit)?,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    supportingContent: @Composable (() -> Unit)?,
    isError: Boolean,
    focusRequester: FocusRequester,
) {
    val colors = CouiTheme.colorScheme
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            // 焦点落在内部输入框上，容器只观察「子树拿到焦点」并把它滚进可视区
            .onFocusChanged {
                if (it.hasFocus) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }
            .padding(
                horizontal = CouiPageDefaults.rowPadding,
                vertical = CouiPageDefaults.contentPadding,
            ),
    ) {
        if (label.isNotEmpty()) {
            CouiText(
                text = label,
                kind = CouiTextKind.ItemSummary,
                color = if (isError) colors.onSurface else colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                maxLines = 1,
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            CouiTextField(
                value = value,
                // 只读等价于吞掉编辑：CouiTextField 没有 readOnly 参数
                onValueChange = { if (!readOnly) onValueChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                enabled = enabled,
                isError = isError,
                singleLine = singleLine,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                leadingIcon = leadingContent,
                trailingIcon = trailingContent,
                interactionSource = interactionSource,
            )
            // 调用方给的是组合项，CouiTextField 的占位文案只收字符串：
            // 按输入框内边距（水平 16dp + 前后图标槽各 32dp）把组合项叠在同一位置。
            // 空值时输入层没有字形，叠上去不遮内容；这层 Box 不消费触摸，点击照常落到输入框。
            if (value.isEmpty() && placeholder != null) {
                CompositionLocalProvider(
                    // 调用方可能传裸 material3 Text，进 COUI 行前先把它默认取的 contentColor 换成 COUI 次级字色
                    LocalContentColor provides colors.onSurfaceVariant,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(
                                start = if (leadingContent != null) 48.dp else 16.dp,
                                end = if (trailingContent != null) 48.dp else 16.dp,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        placeholder()
                    }
                }
            }
        }
        if (supportingContent != null) {
            Box(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                supportingContent()
            }
        }
    }
}

