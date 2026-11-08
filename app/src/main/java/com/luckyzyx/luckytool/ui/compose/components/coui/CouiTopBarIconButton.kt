package com.luckyzyx.luckytool.ui.compose.components.coui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarButtonDefaults
import com.itos.coui.topappbar.TopAppBarButton
import com.itos.coui.topappbar.TopAppBarSurface

/**
 * COUI 顶栏图标按钮（返回键与动作键共用的下发点）。
 *
 * 有 Mesh 渲染器的设备按 demo 顶栏按钮的样式画浮动材质与阴影：外层用
 * [TopAppBarSurface] 提供按钮需要的顶栏作用域，背景透明、不额外叠模糊层。
 * 渲染器拿不到的环境（Robolectric、低版本设备）退回 48dp 纯点击面 + 按压高光，
 * 与 demo 的低版本分支一致。
 */
@Composable
internal fun CouiTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val renderer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        remember { runCatching { CouiFloatingRenderer.standalone() }.getOrNull() }
    } else {
        null
    }
    if (renderer != null) {
        val density = LocalDensity.current.density
        val contour = LocalCouiContourGlowEnabled.current
        TopAppBarSurface(
            modifier = modifier,
            backgroundAlpha = 0f,
            gradientColor = Color.Transparent,
        ) {
            TopAppBarButton(
                onClick = onClick,
                renderer = renderer,
                style = CouiTopAppBarButtonDefaults.style(dark).copy(
                    material = { CouiTopAppBarButtonDefaults.systemMaterial(dark, it) },
                    shadow = {
                        CouiTopAppBarButtonDefaults.shadow(
                            dark,
                            it,
                            density,
                            contour,
                            alphaScale = 1f,
                        )
                    },
                ),
            ) {
                icon()
            }
        }
        return
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CouiShape(24.dp, 2f))
            .couiButtonFeedback(dark = dark, interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}
