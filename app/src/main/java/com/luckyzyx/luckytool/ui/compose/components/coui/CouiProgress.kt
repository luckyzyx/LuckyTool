package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme

/**
 * COUI 线的环形进度，只有不确定进度形态：整圈匀速旋转，可见弧为 90°。
 *
 * 底色轨道先画整圆，再画旋转的圆弧盖在上面；尺寸由调用方的 modifier 决定，
 * 未指定时按 32dp 绘制，线宽默认 3dp（内缩一半，避免被外框裁掉）。
 * 动画在无设备环境（Robolectric）下也只依赖组合里的帧时钟，不需要 Mesh 或手写线程。
 */
@Composable
internal fun CouiCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = CouiTheme.colorScheme.accent,
    trackColor: Color = CouiTheme.colorScheme.outlineVariant,
    strokeWidth: Dp = 3.dp,
) {
    val transition = rememberInfiniteTransition(label = "couiCircularProgress")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    Canvas(modifier = modifier.size(32.dp)) {
        val stroke = strokeWidth.toPx()
        val diameter = (size.minDimension - stroke).coerceAtLeast(0f)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        drawCircle(
            color = trackColor,
            radius = diameter / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = stroke),
        )
        rotate(degrees = angle, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * COUI 线的条形进度。
 *
 * progress 非空时按比例填充（0..1 之外的值会被夹住），为空时跑一段 40% 宽的往复扫光；
 * 两种形态都在胶囊裁剪里绘制，圆角不会漏出方角。
 */
@Composable
internal fun CouiLinearProgressIndicator(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = CouiTheme.colorScheme.accent,
    trackColor: Color = CouiTheme.colorScheme.outlineVariant,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CouiCapsuleShape)
            .background(trackColor),
    ) {
        if (progress == null) {
            val transition = rememberInfiniteTransition(label = "couiLinearProgress")
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1300, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "phase",
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segment = size.width * 0.4f
                val travel = size.width + segment
                // 从完全藏到左侧开始扫到完全出去，视觉上是一段光从左到右穿过轨道。
                val left = -segment + travel * phase
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, 0f),
                    size = Size(segment, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}
