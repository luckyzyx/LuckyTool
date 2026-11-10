package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itos.coui.icons.CouiIcons
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R

/**
 * COUI 顶栏返回按钮：样式与 demo 的返回按钮一致（有 Mesh 时画材质按钮，
 * 否则退回纯点击面），图标用 onSurface 着色。
 */
@Composable
internal fun CouiTopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.back),
) {
    CouiTopBarIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = CouiIcons.Filled.Back,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = CouiTheme.colorScheme.onSurface,
        )
    }
}
