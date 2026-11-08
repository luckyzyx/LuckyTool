package com.luckyzyx.luckytool.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiCircularProgressIndicator
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Logo 入场：缩放 + 淡入时长（ms） */
private const val SplashLogoEnterMillis = 400

/** 应用名入场：相对闪屏开始的延迟 / 淡入时长（ms） */
private const val SplashNameDelayMillis = 120L
private const val SplashNameEnterMillis = 300

/** 闪屏驻留时长（ms）：Logo 与应用名入场完毕后继续停留，总计约 1s */
private const val SplashHoldMillis = 700L

/** 闪屏淡出（交叉淡出到主界面）时长（ms） */
private const val SplashExitMillis = 300

/** Logo 尺寸与圆角：与启动图标观感对齐 */
private val SplashLogoSize = 112.dp
private val SplashLogoCorner = 28.dp

/**
 * 启动闪屏宿主：把闪屏覆盖层叠在 [content] 之上，[content] 从第一帧就参与组合
 *（后台初始化与闪屏动画并行），闪屏淡出后主界面已就绪，不会出现「闪屏结束后再等一次」。
 *
 * 与窗口/系统 splash 的衔接：MainActivity 使用 `Theme.Luckyzyx.Splash`，首帧前的窗口背景
 * （Android 12+ 为系统 splash 背景）和本层背景都取 `@color/splash_background`，同色衔接，
 * 因此冷启动没有白屏闪烁，观感是「纯色 → Logo 入场 → 淡出到主界面」。
 *
 * 仅冷启动播放一次：播放状态保存到 savedInstanceState，旋转屏幕 / restart() 重建不重播。
 *
 * 本层绘制在 [content] 之上，启动阻塞门对话框（Xposed 未激活 / 无 root / 系统不支持）
 * 是场景内弹层、就在下方等着，闪屏淡出后即露出，两者不冲突。
 */
@Composable
fun SplashHost(content: @Composable () -> Unit) {
    var isSplashVisible by rememberSaveable { mutableStateOf(true) }
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(SplashHoldMillis)
        // 整体淡出：主界面已经在下方组合完成，交叉淡出即可
        splashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(SplashExitMillis, easing = FastOutSlowInEasing),
        )
        isSplashVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (isSplashVisible) {
            SplashLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = splashAlpha.value }
                    // 闪屏期间吞掉触摸事件，避免误触覆盖层之下的主界面
                    .pointerInput(Unit) { detectTapGestures { } },
            )
        }
    }
}

/**
 * 启动期激活检测加载层：框架服务绑定是异步的，检测未出结果时盖住主界面，
 * 避免用户对着「回落本地 prefs」的默认值操作；检测出结果（激活或超时）即从组合中移除，
 * 所以不需要淡出动画。
 *
 * 配色与闪屏同源（[R.color.splash_background] / [R.color.splash_foreground]）：闪屏淡出后
 * 只多出一个进度圈，不换底色。本层由 [SplashHost] 的 content 组合、位于闪屏之下，
 * 闪屏还在时用户看不到它。
 */
@Composable
fun ActivationLoadingLayer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.splash_background))
            // 吞掉触摸事件，避免误触覆盖层之下的主界面
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        val foreground = colorResource(R.color.splash_foreground)
        // 两条外观线各用各自的原生进度圈：M3 的尺寸与粗细由调用点给，COUI 的固定 32dp。
        when (LocalUiMode.current) {
            UiMode.Material -> CircularProgressIndicator(
                color = foreground,
                strokeWidth = 3.dp,
            )

            UiMode.Coui -> CouiCircularProgressIndicator(color = foreground)
        }
    }
}

/** 闪屏内容：纯色背景 + Logo 缩放入场 + 应用名上浮淡入 */
@Composable
private fun SplashLayer(modifier: Modifier = Modifier) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.74f) }
    val nameAlpha = remember { Animatable(0f) }
    val nameTranslationY = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(SplashLogoEnterMillis, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(SplashLogoEnterMillis, easing = FastOutSlowInEasing),
            )
        }
        delay(SplashNameDelayMillis)
        launch {
            nameAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(SplashNameEnterMillis, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            nameTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(SplashNameEnterMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    Column(
        modifier = modifier.background(colorResource(R.color.splash_background)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(SplashLogoSize)
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
                .clip(RoundedCornerShape(SplashLogoCorner)),
        )
        Spacer(modifier = Modifier.height(20.dp))
        // 文字走 BasicText：闪屏不依赖 Material / COUI 任意外观线（两条线都可能被用户切换）
        BasicText(
            text = stringResource(R.string.app_name),
            modifier = Modifier.graphicsLayer {
                alpha = nameAlpha.value
                translationY = nameTranslationY.value.dp.toPx()
            },
            style = TextStyle(
                color = colorResource(R.color.splash_foreground),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}
