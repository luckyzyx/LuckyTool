package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.icons.CouiIcons
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R

/**
 * COUI 顶栏返回按钮：圆形点击面 + 与圆角一致形状的按压高光。
 *
 * 不用 COUI 的 TopAppBarButton：它按 Mesh 材质绘制按钮底色与阴影，
 * 在无 Mesh 的运行环境（Robolectric 单测、部分低版本设备）里构造即抛异常；
 * 返回键只需要按压反馈，[couiButtonFeedback] 已足够。
 */
@Composable
internal fun CouiTopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.back),
) {
    val dark = CouiTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
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
        Icon(
            imageVector = CouiIcons.Back,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = CouiTheme.colorScheme.onSurface,
        )
    }
}
