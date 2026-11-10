package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/**
 * 小徽标：count 为空时是纯圆点，否则是显示数字的胶囊。
 *
 * 底色 accent、文字 onAccent。徽标自己不带边距，摆到哪里由外层决定；
 * 需要叠在图标右上角时用 CouiBadgedBox，它会补上偏移。
 *
 * contentDescription 给读屏用；不传且 count 非空时用数字本身兜底，纯圆点没有文案就不播报。
 */
@Composable
internal fun CouiBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
    contentDescription: String? = null,
) {
    val colors = CouiTheme.colorScheme
    val description = contentDescription ?: count?.toString()
    val themed = modifier.then(
        if (description != null) {
            Modifier.semantics { this.contentDescription = description }
        } else {
            Modifier
        }
    )
    if (count == null) {
        Box(modifier = themed.size(8.dp).clip(CircleShape).background(colors.accent))
    } else {
        Box(
            modifier = themed
                .heightIn(min = 16.dp)
                .widthIn(min = 16.dp)
                .clip(CouiCapsuleShape)
                .background(colors.accent)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            CouiText(
                text = count.toString(),
                kind = CouiTextKind.GroupFooter,
                color = colors.onAccent,
                maxLines = 1,
            )
        }
    }
}

/**
 * 把徽标叠到内容右上角的容器。
 *
 * 右上角偏移量是固定值（向右下让出 6dp），所以内容本身要在右侧留够内边距，
 * 否则徽标会贴到邻居上。徽标不参与父容器的尺寸计算，布局不会因为它变大而抖动。
 */
@Composable
internal fun CouiBadgedBox(
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp),
        ) {
            badge()
        }
    }
}
