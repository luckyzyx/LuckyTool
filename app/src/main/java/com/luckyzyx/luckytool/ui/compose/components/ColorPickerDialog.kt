package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import kotlin.math.roundToInt

/** SV 面板宽高比，保持与旧实现一致的 360:288 */
private const val SV_ASPECT_RATIO = 360f / 288f

/** 色相条 / 透明度条高度（胶囊） */
private val BarHeight = 28.dp

/** 色相条的彩虹渐变（首尾同为红色，保证 0°/360° 连续） */
private val HueColors = listOf(
    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
)

/** 透明棋盘格配色（与 Miuix drawCheckerboard 取值一致，避免两种外观线观感不一致） */
private val CheckerLight = Color(0xFFCCCCCC)
private val CheckerDark = Color(0xFFAAAAAA)

/** 颜色选择器内部状态（ARGB / 色相），两条外观线共用 */
private class ColorPickerUiState(initialColor: Int) {
    var colorInt by mutableIntStateOf(initialColor)
    var hue by mutableFloatStateOf(hsvOf(initialColor)[0])

    val rgb: Int get() = colorInt and 0xFFFFFF

    /** 透明度（0f..1f），供透明度条定位 */
    val alphaFraction: Float get() = ((colorInt ushr 24) and 0xFF) / 255f

    fun colorFromHsv(h: Float, s: Float, v: Float) {
        colorInt = (alpha() shl 24) or
            (android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) and 0xFFFFFF)
    }

    fun applyHue(h: Float) {
        hue = h
        val curHsv = hsvOf(rgb)
        colorFromHsv(h, curHsv[1], curHsv[2])
    }

    fun applyAlpha(fraction: Float) {
        val a = (fraction * 255f).roundToInt().coerceIn(0, 255)
        colorInt = (a shl 24) or rgb
    }

    private fun alpha(): Int = (colorInt ushr 24) and 0xFF
}

/** 读取颜色的 HSV（色相/饱和度/明度），不修改传入值 */
private fun hsvOf(color: Int): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(color and 0xFFFFFF, it) }

/**
 * Compose 颜色选择器对话框。
 *
 * 面板结构：SV 面板（x=饱和度，y=明度，hue 固定）+ 色相条 + 透明度条 + 预览色块/十六进制文本。
 * 回调 onColorSelected(colorInt, hexString)，十六进制格式为 `#AARRGGBB`。
 *
 * 外观按 [LocalUiMode] 分派：Miuix 线走 Miuix 弹层，Material 线走 M3 AlertDialog；
 * SV 面板与两条渐变条两条线共用同一实现（自绘渐变 + 胶囊圆环滑块）。
 * Miuix 弹层统一由根部 Miuix Scaffold 的默认 popup host 承载，本组件不自装 host、
 * 不传 `renderInRootScaffold = false`。
 */
@Composable
fun ColorPickerDialog(
    initialHex: String?,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    val initialColor = remember(initialHex) {
        try {
            initialHex?.toColorInt() ?: android.graphics.Color.WHITE
        } catch (_: IllegalArgumentException) {
            android.graphics.Color.WHITE
        }
    }

    val state = remember { ColorPickerUiState(initialColor) }

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixColorPickerDialog(
            state = state,
            onDismiss = onDismiss,
            onColorSelected = onColorSelected,
        )

        UiMode.Material -> MaterialColorPickerDialog(
            state = state,
            onDismiss = onDismiss,
            onColorSelected = onColorSelected,
        )
    }
}

/** Material 线：M3 AlertDialog（确认/取消走对话框按钮槽位） */
@Composable
private fun MaterialColorPickerDialog(
    state: ColorPickerUiState,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_color)) },
        text = { ColorPickerBody(state) },
        confirmButton = {
            TextButton(onClick = { onColorSelected(state.colorInt, formatHex(state.colorInt)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** Miuix 线：OverlayDialog（根部 popup host 承载）+ Miuix 文本件 */
@Composable
private fun MiuixColorPickerDialog(
    state: ColorPickerUiState,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.select_color),
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth()) {
            ColorPickerBody(state)

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MiuixTextButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                MiuixTextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = { onColorSelected(state.colorInt, formatHex(state.colorInt)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

/**
 * 选择器主体（标题与按钮之外的全部内容），两条外观线共用，保证两线布局完全一致：
 * SV 面板 → 色相条 → 透明度条 → 预览色块 + 十六进制文本。
 */
@Composable
private fun ColorPickerBody(state: ColorPickerUiState) {
    val isMiuix = LocalUiMode.current == UiMode.Miuix

    Column(Modifier.fillMaxWidth()) {
        SvPanel(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SV_ASPECT_RATIO)
                .clip(RoundedCornerShape(16.dp)),
        )

        Spacer(Modifier.height(16.dp))

        GradientBar(
            fraction = state.hue / 360f,
            colors = HueColors,
            onFractionChange = { state.applyHue(it * 360f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(CircleShape),
        )

        Spacer(Modifier.height(12.dp))

        GradientBar(
            fraction = state.alphaFraction,
            colors = alphaGradientColors(state.rgb),
            onFractionChange = state::applyAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .clip(CircleShape),
            checkerboard = true,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(
                colorInt = state.colorInt,
                shape = RoundedCornerShape(14.dp),
                borderColor = if (isMiuix) {
                    MiuixTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(14.dp))
            if (isMiuix) {
                MiuixText(
                    text = formatHex(state.colorInt),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontSize = MiuixTheme.textStyles.title4.fontSize,
                )
            } else {
                Text(
                    text = formatHex(state.colorInt),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

/**
 * SV 面板：x=饱和度、y=明度，底色用两层渐变实时绘制（不再生成位图，分辨率无关且无主线程卡顿）；
 * 数学上等价于 HSV 取 s=x/宽、v=1-y/高。
 */
@Composable
private fun SvPanel(
    state: ColorPickerUiState,
    modifier: Modifier = Modifier,
) {
    // 指示圈位置跟随当前颜色；hue 改动时颜色也会变，因此只依赖 colorInt
    val hsv = remember(state.colorInt) { hsvOf(state.rgb) }

    Canvas(
        modifier.positionGesture { position, size ->
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            state.colorFromHsv(
                state.hue,
                (position.x / w).coerceIn(0f, 1f),
                1f - (position.y / h).coerceIn(0f, 1f),
            )
        }
    ) {
        val pureHue = Color(android.graphics.Color.HSVToColor(floatArrayOf(state.hue, 1f, 1f)))
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHue)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        // 指示圈：圆心收缩到半径以内，避免贴边时被裁掉一半
        val radius = 9.dp.toPx()
        val cx = (hsv[1] * size.width).coerceIn(radius, (size.width - radius).coerceAtLeast(radius))
        val cy = ((1f - hsv[2]) * size.height)
            .coerceIn(radius, (size.height - radius).coerceAtLeast(radius))
        drawIndicator(center = Offset(cx, cy), radius = radius, stroke = 3.dp.toPx())
    }
}

/**
 * 渐变条：胶囊底 + 圆环滑块，用于色相与透明度；[checkerboard] 为真时先铺透明棋盘格。
 *
 * 渐变两端按滑块半径内缩（与 Miuix ColorSlider 一致），使滑块圆心处取到的颜色即为当前值。
 */
@Composable
private fun GradientBar(
    fraction: Float,
    colors: List<Color>,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    checkerboard: Boolean = false,
) {
    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .positionGesture { position, size ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    onFractionChange((position.x / w).coerceIn(0f, 1f))
                }
        ) {
            val radius = (size.height - 8.dp.toPx()) / 2f
            val startX = radius
            val endX = (size.width - radius).coerceAtLeast(startX + 1f)

            if (checkerboard) drawCheckerboard()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = colors,
                    startX = startX,
                    endX = endX,
                    tileMode = TileMode.Clamp,
                )
            )
            // 浅色渐变条（如明黄）在浅色弹层里需要一点边界，避免糊在背景上
            drawRect(color = Color.Black.copy(alpha = 0.10f), style = Stroke(1.dp.toPx()))

            val cx = startX + fraction.coerceIn(0f, 1f) * (endX - startX)
            drawIndicator(center = Offset(cx, size.height / 2f), radius = radius, stroke = 3.dp.toPx())
        }
    }
}

/** 预览色块：棋盘格垫底 + 实际颜色叠加 + 1dp 描边 */
@Composable
private fun ColorSwatch(
    colorInt: Int,
    shape: Shape,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(shape).border(1.dp, borderColor, shape)) {
        Canvas(Modifier.fillMaxSize()) { drawCheckerboard() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(colorInt))
        )
    }
}

/**
 * 按住即生效、拖动持续生效的取点手势（没有 detectTapGestures/detectDragGestures 的 touch slop 延迟）。
 *
 * [onPosition] 只在 pointerInput 启动时捕获一次，因此调用点传入的闭包只能引用稳定对象
 * （如 remember 出来的 [ColorPickerUiState]），不要捕获会随重组变化的值。
 */
private fun Modifier.positionGesture(
    onPosition: (Offset, IntSize) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onPosition(down.position, size)
        down.consume()
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            onPosition(change.position, size)
            change.consume()
        }
    }
}

/** 圆环指示器：白色描边 + 外侧深色发丝线，保证在深色与浅色底上都清晰 */
private fun DrawScope.drawIndicator(center: Offset, radius: Float, stroke: Float) {
    val hairline = 1.dp.toPx()
    drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = radius + stroke / 2f + hairline / 2f,
        center = center,
        style = Stroke(width = hairline),
    )
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center,
        style = Stroke(width = stroke),
    )
}

/** 透明棋盘格底（自实现，避免 Material 线耦合 Miuix）；溢出部分由父级 clip 负责裁掉 */
private fun DrawScope.drawCheckerboard(cellDp: Float = 4f) {
    val cell = cellDp.dp.toPx().coerceAtLeast(1f)
    drawRect(color = CheckerLight)
    var y = 0f
    var row = 0
    while (y < size.height) {
        var x = if (row % 2 == 0) cell else 0f
        while (x < size.width) {
            drawRect(color = CheckerDark, topLeft = Offset(x, y), size = Size(cell, cell))
            x += cell * 2f
        }
        y += cell
        row++
    }
}

/** 透明度条底色渐变：同一 RGB 的 0% → 100% */
private fun alphaGradientColors(rgb: Int): List<Color> {
    val base = Color(rgb)
    return listOf(base.copy(alpha = 0f), base.copy(alpha = 1f))
}

private fun formatHex(color: Int): String = String.format("#%08X", color)
