package com.luckyzyx.luckytool.ui.components

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R

/**
 * Compose 颜色选择器对话框（旧 colorpicker 模块 ColorPickerDialog 的现代化等价物）。
 *
 * 面板结构：SV 面板（x=饱和度，y=明度，hue 固定）+ 色相条 + 透明度滑杆 +
 * 预览/十六进制文本。回调 onColorSelected(colorInt, hexString)。
 */
@Composable
fun ColorPickerDialog(
    initialHex: String?,
    onDismiss: () -> Unit,
    onColorSelected: (Int, String) -> Unit,
) {
    val context = LocalContext.current

    val initialColor = remember(initialHex) {
        try {
            initialHex?.let { android.graphics.Color.parseColor(it) } ?: android.graphics.Color.WHITE
        } catch (_: IllegalArgumentException) {
            android.graphics.Color.WHITE
        }
    }

    var colorInt by remember { mutableIntStateOf(initialColor) }
    val hue = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsv)
        mutableFloatStateOf(hsv[0])
    }

    val alpha = (colorInt ushr 24) and 0xFF
    val rgb = colorInt and 0xFFFFFF

    val svBitmap: ImageBitmap = remember(hue.value) { createSvBitmap(hue.value, 360, 288) }

    fun colorFromHsv(h: Float, s: Float, v: Float) {
        colorInt = (alpha shl 24) or (android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) and 0xFFFFFF)
    }

    fun applyHue(h: Float) {
        hue.value = h
        val curHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt and 0xFFFFFF, curHsv)
        colorFromHsv(h, curHsv[1], curHsv[2])
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_color)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // ---- SV 面板 ----
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(360f / 288f)
                ) {
                    Image(
                        bitmap = svBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(hue.value) {
                                detectTapGestures { offset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    colorFromHsv(
                                        hue.value,
                                        (offset.x / w).coerceIn(0f, 1f),
                                        1f - (offset.y / h).coerceIn(0f, 1f),
                                    )
                                }
                            }
                            .pointerInput(hue.value) {
                                detectDragGestures { change, _ ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val pos = change.position
                                    colorFromHsv(
                                        hue.value,
                                        (pos.x / w).coerceIn(0f, 1f),
                                        1f - (pos.y / h).coerceIn(0f, 1f),
                                    )
                                }
                            },
                    )
                    // 指示器
                    val hsv = remember(colorInt) {
                        FloatArray(3).also { android.graphics.Color.colorToHSV(rgb, it) }
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = hsv[1] * size.width
                        val cy = (1f - hsv[2]) * size.height
                        drawCircle(
                            color = Color(rgb or 0xFF000000.toInt()),
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

                // ---- 色相条 ----
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(top = 12.dp)
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    applyHue(
                                        360f * (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    )
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    applyHue(
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
                        val hx = (hue.value / 360f) * size.width
                        drawCircle(
                            color = Color(rgb or 0xFF000000.toInt()),
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

                // ---- 透明度 ----
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.current_color, formatHex(colorInt)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Slider(
                    value = alpha / 255f,
                    onValueChange = { a ->
                        colorInt = ((a * 255f).toInt().coerceIn(0, 255) shl 24) or rgb
                    },
                )

                // ---- 预览 ----
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Color(colorInt), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Text(
                        formatHex(colorInt),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(colorInt, formatHex(colorInt)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
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

/** 十六进制（ARGB）字符串转 Int 颜色，失败回退 WHITE */
private fun String.toColorIntOrWhite(): Int = try {
    android.graphics.Color.parseColor(this)
} catch (_: IllegalArgumentException) {
    android.graphics.Color.WHITE
}
