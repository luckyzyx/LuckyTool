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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
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

/**
 * 十六进制输入框的占位提示。
 *
 * 刻意用字面量而非字符串资源：`select_color` / `current_color` 之外新增资源要补齐 13 个语言目录，
 * 而十六进制格式本身与语言无关。
 */
private const val HEX_HINT = "#AARRGGBB"

/** 十六进制输入用软键盘：纯 ASCII、无自动纠错、完成后收起键盘 */
private val HexKeyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Ascii,
    capitalization = KeyboardCapitalization.Characters,
    imeAction = ImeAction.Done,
)

/**
 * 取色模式切换的按钮文案。
 *
 * 同 [HEX_HINT]：`SV` / `ARGB` 都是与语言无关的技术名词，用字面量以免为这两条文案补齐 13 个语言目录。
 */
private const val MODE_SV_LABEL = "SV"
private const val MODE_ARGB_LABEL = "ARGB"

/**
 * ARGB 四通道在颜色 int 中的位偏移（与存储格式 `#AARRGGBB` 一致）。
 *
 * `internal` 仅为单元测试可见（`ColorPickerChannelTest`），组件内按文件私有使用。
 */
internal const val ALPHA_SHIFT = 24
internal const val RED_SHIFT = 16
internal const val GREEN_SHIFT = 8
internal const val BLUE_SHIFT = 0

/** 取色面板模式：色相 + SV 面板，或 A/R/G/B 四通道滑杆 */
private enum class PickerMode { Sv, Argb }

/** 颜色选择器内部状态（ARGB / 色相 / 十六进制输入文本 / 面板模式），两条外观线共用 */
private class ColorPickerUiState(initialColor: Int) {
    var colorInt by mutableIntStateOf(initialColor)
        private set

    /** 输入框文本：用户输入期间保持原始输入，取色时同步为规范的 `#AARRGGBB` */
    var hexText by mutableStateOf(formatHex(initialColor))
        private set

    var hue by mutableFloatStateOf(hsvOf(initialColor)[0])
        private set

    /** 当前取色模式（纯界面状态，不参与确认时的颜色计算） */
    var mode by mutableStateOf(PickerMode.Sv)

    val rgb: Int get() = colorInt and 0xFFFFFF

    /** 透明度（0f..1f），供透明度条定位 */
    val alphaFraction: Float get() = channelFraction(ALPHA_SHIFT)

    /** 输入文本能否解析为颜色；false 时两条线各用原生方式显示错误态 */
    val isHexValid: Boolean get() = parseHex(hexText) != null

    /** 单通道取值（0..255） */
    fun channel(shift: Int): Int = (colorInt ushr shift) and 0xFF

    /** 单通道归一到 0f..1f，供通道滑杆定位 */
    fun channelFraction(shift: Int): Float = channel(shift) / 255f

    fun colorFromHsv(h: Float, s: Float, v: Float) {
        setColor(
            (alpha() shl 24) or
                (android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) and 0xFFFFFF)
        )
    }

    fun applyHue(h: Float) {
        hue = h
        val curHsv = hsvOf(rgb)
        colorFromHsv(h, curHsv[1], curHsv[2])
    }

    fun applyAlpha(fraction: Float) = applyChannel(ALPHA_SHIFT, fraction)

    /** 拖动单通道滑杆：只改该通道字节，其余通道保持不变 */
    fun applyChannel(shift: Int, fraction: Float) {
        val color = withChannel(colorInt, shift, (fraction * 255f).roundToInt())
        setColor(color)

        // R/G/B 变化会改变色相：同步给色相条与 SV 面板底色，切回 SV 模式时不至于停在旧色相。
        // 灰色（饱和度 0）时色相无意义，保留原值，避免色相条凭空跳回 0°。
        if (shift != ALPHA_SHIFT) {
            val hsv = hsvOf(color)
            if (hsv[1] > 0f) hue = hsv[0]
        }
    }

    /**
     * 十六进制输入：文本始终跟随用户原始输入（只过滤非法字符），
     * 解析成功才更新颜色与色相 —— 失败时保留文本以便继续输入，并进入错误态。
     */
    fun onHexInput(raw: String) {
        hexText = filterHexInput(raw)
        parseHex(hexText)?.let { color ->
            colorInt = color
            hue = hsvOf(color)[0]
        }
    }

    /** 取色统一入口：颜色与输入框文本一起更新，避免拖动后文本框仍是旧值 */
    private fun setColor(color: Int) {
        colorInt = color
        hexText = formatHex(color)
    }

    private fun alpha(): Int = (colorInt ushr 24) and 0xFF
}

/** 读取颜色的 HSV（色相/饱和度/明度），不修改传入值 */
private fun hsvOf(color: Int): FloatArray =
    FloatArray(3).also { android.graphics.Color.colorToHSV(color and 0xFFFFFF, it) }

/**
 * Compose 颜色选择器对话框。
 *
 * 面板结构：模式切换（SV / ARGB）+ 预览色块 + 十六进制输入框；SV 模式为 SV 面板（x=饱和度，
 * y=明度，hue 固定）+ 色相条 + 透明度条，ARGB 模式为 A/R/G/B 四通道滑杆。两种模式改的是同一个
 * 颜色，切换不影响已选颜色。
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
 * 模式切换 → SV 面板 + 色相条 + 透明度条（或 A/R/G/B 四通道滑杆）→ 预览色块 + 十六进制输入框。
 */
@Composable
private fun ColorPickerBody(state: ColorPickerUiState) {
    val isMiuix = LocalUiMode.current == UiMode.Miuix

    Column(Modifier.fillMaxWidth()) {
        PickerModeSwitch(state = state, isMiuix = isMiuix)

        Spacer(Modifier.height(12.dp))

        when (state.mode) {
            PickerMode.Sv -> SvPickers(state = state)
            PickerMode.Argb -> ArgbChannelBars(state = state, isMiuix = isMiuix)
        }

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
            HexField(
                state = state,
                isMiuix = isMiuix,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 取色模式切换：SV 面板与 ARGB 四通道滑杆二选一。
 *
 * 两条外观线各用各自的选择控件（Material 走 M3 `FilterChip`，Miuix 走 `MiuixToggleChip`），
 * 与 `AppPickerDialog` 的筛选行保持一致的观感与实现。
 */
@Composable
private fun PickerModeSwitch(
    state: ColorPickerUiState,
    isMiuix: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerMode.entries.forEach { mode ->
                val selected = state.mode == mode
                val onClick = { state.mode = mode }
                val label = if (mode == PickerMode.Sv) MODE_SV_LABEL else MODE_ARGB_LABEL

                if (isMiuix) {
                    MiuixToggleChip(selected = selected, onClick = onClick, label = label)
                } else {
                    FilterChip(
                        selected = selected,
                        onClick = onClick,
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

/** SV 模式：SV 面板（饱和度/明度）+ 色相条 + 透明度条 */
@Composable
private fun SvPickers(state: ColorPickerUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
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
    }
}

/**
 * ARGB 模式：A/R/G/B 四条通道滑杆，顺序与存储格式 `#AARRGGBB` 一致。
 *
 * 通道字母（A/R/G/B）是国际通用记法，与 [MODE_SV_LABEL] 同理用字面量，不引入字符串资源。
 */
@Composable
private fun ArgbChannelBars(
    state: ColorPickerUiState,
    isMiuix: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelBar(state = state, isMiuix = isMiuix, label = "A", shift = ALPHA_SHIFT)
        ChannelBar(state = state, isMiuix = isMiuix, label = "R", shift = RED_SHIFT)
        ChannelBar(state = state, isMiuix = isMiuix, label = "G", shift = GREEN_SHIFT)
        ChannelBar(state = state, isMiuix = isMiuix, label = "B", shift = BLUE_SHIFT)
    }
}

/**
 * 单通道滑杆：通道字母 + 渐变胶囊 + 当前值（0..255）。
 *
 * 数值只用于读准当前通道，精确输入仍走十六进制框；渐变直接展示该通道 0→255 的效果。
 */
@Composable
private fun ChannelBar(
    state: ColorPickerUiState,
    isMiuix: Boolean,
    label: String,
    shift: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChannelText(text = label, isMiuix = isMiuix, modifier = Modifier.width(16.dp))

        Spacer(Modifier.width(10.dp))

        GradientBar(
            fraction = state.channelFraction(shift),
            colors = channelGradientColors(state.rgb, shift),
            onFractionChange = { state.applyChannel(shift, it) },
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .clip(CircleShape),
            checkerboard = shift == ALPHA_SHIFT,
        )

        Spacer(Modifier.width(10.dp))

        ChannelText(
            text = state.channel(shift).toString(),
            isMiuix = isMiuix,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End,
        )
    }
}

/** 通道字母与数值文本：两条外观线各用自己的排版与前景色，等宽字体避免数值变化时抖动 */
@Composable
private fun ChannelText(
    text: String,
    isMiuix: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    if (isMiuix) {
        MiuixText(
            text = text,
            modifier = modifier,
            color = MiuixTheme.colorScheme.onBackground,
            style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
            textAlign = textAlign,
            maxLines = 1,
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            textAlign = textAlign,
            maxLines = 1,
        )
    }
}

/**
 * 十六进制输入框：接受 `#RRGGBB` / `#AARRGGBB`（`#` 可省略，6 位按不透明处理），
 * 输入合法即刻同步颜色，非法时两条线各用原生方式提示（M3 `isError` / Miuix 错误色）。
 *
 * 沿用 `AppListUi.AppSearchField` 的双线惯例：Miuix 线用 `MiuixTextField` 的 label 当占位符，
 * Material 线用 `OutlinedTextField` 的 placeholder。
 */
@Composable
private fun HexField(
    state: ColorPickerUiState,
    isMiuix: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isMiuix) {
        MiuixTextField(
            value = state.hexText,
            onValueChange = state::onHexInput,
            label = HEX_HINT,
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
            colors = if (state.isHexValid) {
                MiuixTextFieldDefaults.textFieldColors()
            } else {
                MiuixTextFieldDefaults.textFieldColors(
                    backgroundColor = MiuixTheme.colorScheme.errorContainer,
                    labelColor = MiuixTheme.colorScheme.onErrorContainer,
                    borderColor = MiuixTheme.colorScheme.error,
                )
            },
            textStyle = MiuixTheme.textStyles.main.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = HexKeyboardOptions,
            modifier = modifier,
        )
    } else {
        val hexTextStyle =
            MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)

        OutlinedTextField(
            value = state.hexText,
            onValueChange = state::onHexInput,
            singleLine = true,
            isError = !state.isHexValid,
            placeholder = { Text(HEX_HINT, style = hexTextStyle) },
            textStyle = hexTextStyle,
            keyboardOptions = HexKeyboardOptions,
            modifier = modifier,
        )
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

/**
 * 单通道条底色渐变：A 通道复用 [alphaGradientColors]（本来就要看透明效果）；
 * R/G/B 三通道强制不透明，否则 alpha 为 0 时整条滑杆看不见颜色。
 */
private fun channelGradientColors(rgb: Int, shift: Int): List<Color> {
    if (shift == ALPHA_SHIFT) return alphaGradientColors(rgb)

    val mask = 0xFF shl shift
    val low = (rgb or (0xFF shl ALPHA_SHIFT)) and mask.inv()
    return listOf(Color(low), Color(low or mask))
}

private fun formatHex(color: Int): String = String.format("#%08X", color)

/**
 * 解析 `#RRGGBB` / `#AARRGGBB`（`#` 可省略），6 位按不透明补 `FF`；无法解析时返回 null。
 *
 * `internal` 仅为单元测试可见（`ColorPickerHexTest`），组件内按文件私有使用。
 */
internal fun parseHex(text: String): Int? {
    val body = text.trim().removePrefix("#")
    if (body.length != 6 && body.length != 8) return null
    if (!body.all(::isHexDigit)) return null
    val value = body.toLongOrNull(16) ?: return null
    return if (body.length == 6) (0xFF shl 24) or value.toInt() else value.toInt()
}

/**
 * 输入过滤：保留开头的 `#` 与全部十六进制数字，并截断到 `#AARRGGBB`
 * （无 `#` 时上限 8 位），避免用户在错误长度上停留却看不出原因。
 *
 * `internal` 仅为单元测试可见（`ColorPickerHexTest`），组件内按文件私有使用。
 */
internal fun filterHexInput(raw: String): String {
    val builder = StringBuilder()
    raw.forEach { char ->
        if (isHexDigit(char)) {
            builder.append(char)
        } else if (char == '#' && builder.isEmpty()) {
            builder.append(char)
        }
    }
    val limit = if (builder.startsWith("#")) 9 else 8
    return builder.toString().take(limit)
}

private fun isHexDigit(char: Char): Boolean =
    char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'

/**
 * 替换颜色的单个通道（[shift] 取 [ALPHA_SHIFT] / [RED_SHIFT] / [GREEN_SHIFT] / [BLUE_SHIFT]），
 * 其余通道保持不变，越界值收敛到 0..255。
 *
 * `internal` 仅为单元测试可见（`ColorPickerChannelTest`），组件内按文件私有使用。
 */
internal fun withChannel(color: Int, shift: Int, value: Int): Int =
    (color and (0xFF shl shift).inv()) or (value.coerceIn(0, 255) shl shift)
