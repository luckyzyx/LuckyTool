package com.luckyzyx.luckytool.ui.compose.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

/** 颜色选择器内部状态（ARGB / 色相），两条外观线共用；语义与迁移前逐行一致。 */
private class ColorPickerUiState(initialColor: Int) {
    var colorInt by mutableIntStateOf(initialColor)
    var hue by mutableFloatStateOf(
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }[0],
    )

    val alpha: Int get() = (colorInt ushr 24) and 0xFF
    val rgb: Int get() = colorInt and 0xFFFFFF

    fun colorFromHsv(h: Float, s: Float, v: Float) {
        colorInt = (alpha shl 24) or (android.graphics.Color.HSVToColor(
            floatArrayOf(
                h,
                s,
                v
            )
        ) and 0xFFFFFF)
    }

    fun applyHue(h: Float) {
        hue = h
        val curHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rgb, curHsv)
        colorFromHsv(h, curHsv[1], curHsv[2])
    }
}

/**
 * Compose 颜色选择器对话框（旧 colorpicker 模块 ColorPickerDialog 的现代化等价物）。
 *
 * 面板结构：SV 面板（x=饱和度，y=明度，hue 固定）+ 色相条 + 透明度滑杆 +
 * 预览/十六进制文本。回调 onColorSelected(colorInt, hexString)。
 *
 * 外观按 [LocalUiMode] 分派：Miuix 线走 Miuix 弹层（OverlayDialog + Miuix 滑条），
 * Material 线行为不变；自绘 SV 位图与面板手势两条线共用同一实现。
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

    val svBitmap: ImageBitmap = remember(state.hue) { createSvBitmap(state.hue, 360, 288) }

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixColorPickerDialog(
            state = state,
            svBitmap = svBitmap,
            onDismiss = onDismiss,
            onColorSelected = onColorSelected,
        )

        UiMode.Material -> MaterialColorPickerDialog(
            state = state,
            svBitmap = svBitmap,
            onDismiss = onDismiss,
            onColorSelected = onColorSelected,
        )
    }
}

/** Material 线：M3 AlertDialog（本任务不改行为） */
@Composable
private fun MaterialColorPickerDialog(
    state: ColorPickerUiState,
    svBitmap: ImageBitmap,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_color)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                SvPanel(
                    state = state,
                    svBitmap = svBitmap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 288f),
                )

                HueBar(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(top = 12.dp),
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.current_color, formatHex(state.colorInt)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Slider(
                    state = rememberSliderState(
                        value = state.alpha / 255f
                    ),
                    onValueChange = { a ->
                        state.colorInt = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or state.rgb
                    }
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Color(state.colorInt), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        formatHex(state.colorInt),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
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

/** Miuix 线：OverlayDialog（根部 popup host 承载）+ Miuix 滑条与文本件 */
@Composable
private fun MiuixColorPickerDialog(
    state: ColorPickerUiState,
    svBitmap: ImageBitmap,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.select_color),
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth()) {
            SvPanel(
                state = state,
                svBitmap = svBitmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(360f / 288f),
            )

            HueBar(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(top = 12.dp),
            )

            MiuixText(
                text = stringResource(R.string.current_color, formatHex(state.colorInt)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
            MiuixSlider(
                value = state.alpha / 255f,
                onValueChange = { a ->
                    state.colorInt = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or state.rgb
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color(state.colorInt), CircleShape)
                        .border(1.dp, MiuixTheme.colorScheme.outline, CircleShape)
                )
                MiuixText(
                    text = formatHex(state.colorInt),
                    color = MiuixTheme.colorScheme.onBackground,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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

/** SV 面板：x=饱和度，y=明度；位图由 [createSvBitmap] 生成，点按/拖拽共用面板手势 */
@Composable
private fun SvPanel(
    state: ColorPickerUiState,
    svBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Image(
            bitmap = svBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.hue) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        state.colorFromHsv(
                            state.hue,
                            (offset.x / w).coerceIn(0f, 1f),
                            1f - (offset.y / h).coerceIn(0f, 1f),
                        )
                    }
                }
                .pointerInput(state.hue) {
                    detectDragGestures { change, _ ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val pos = change.position
                        state.colorFromHsv(
                            state.hue,
                            (pos.x / w).coerceIn(0f, 1f),
                            1f - (pos.y / h).coerceIn(0f, 1f),
                        )
                    }
                },
        )
        // 指示器
        val hsv = remember(state.colorInt) {
            FloatArray(3).also { android.graphics.Color.colorToHSV(state.rgb, it) }
        }
        Canvas(Modifier.fillMaxSize()) {
            val cx = hsv[1] * size.width
            val cy = (1f - hsv[2]) * size.height
            drawCircle(
                color = Color(state.rgb or 0xFF000000.toInt()),
                radius = 12.dp.toPx(),
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color.White,
                radius = 16.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    }
}

/** 色相条：彩虹渐变 + 当前色相指示圈，点按/拖拽改色相 */
@Composable
private fun HueBar(
    state: ColorPickerUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        state.applyHue(
                            360f * (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        state.applyHue(
                            360f * (change.position.x / size.width.toFloat())
                                .coerceIn(0f, 1f)
                        )
                    }
                }
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                    )
                )
            )
            val hx = (state.hue / 360f) * size.width
            drawCircle(
                color = Color(state.rgb or 0xFF000000.toInt()),
                radius = 10.dp.toPx(),
                center = Offset(hx, size.height / 2f),
            )
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = Offset(hx, size.height / 2f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

private fun formatHex(color: Int): String = String.format("#%08X", color)

private fun createSvBitmap(hue: Float, width: Int, height: Int): ImageBitmap {
    val colors = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            colors[y * width + x] = android.graphics.Color.HSVToColor(
                floatArrayOf(hue, x.toFloat() / width, 1f - y.toFloat() / height)
            )
        }
    }
    return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
