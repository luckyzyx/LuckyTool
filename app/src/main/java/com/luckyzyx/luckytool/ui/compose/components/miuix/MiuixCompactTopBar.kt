/*
 * Miuix 外观线单行顶栏：`[返回键] 标题 …… 动作键`，标题紧随返回键左对齐。
 *
 * 不能直接用库内 top.yukonga.miuix.kmp.basic.SmallTopAppBar：它把标题水平居中
 * （SmallTopAppBarLayout 里 baseX = (maxWidth - titleWidth) / 2），与「标题一律左对齐」不符。
 * 这里按库内同一套尺寸令牌自行布局：高度 52dp（CollapsedHeight）、返回键起始 16dp、
 * 动作键结束 16dp，标题字号 TopBarTitleSize / Medium，并保留相同的 window inset 处理。
 */
package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 单行顶栏高度：对齐库内 TopAppBarDefaults.CollapsedHeight。 */
private val CompactBarHeight = DesignTokens.TopBarHeight

/** 返回键起始 / 动作键结束内边距：对齐库内 NavigationIconPadding / ActionIconPadding。 */
private val BarHorizontalPadding = DesignTokens.TopBarHorizontalPadding

/** 标题与返回键之间的间距。 */
private val TitleStartPadding = DesignTokens.TopBarTitleStartPadding

/**
 * Miuix 外观线单行顶栏：标题与返回键、动作键同一行，且标题左对齐。
 *
 * 无返回键时（主页面）[navigationIcon] 传空即可，此时标题左侧仅保留基础内边距。
 */
@Composable
fun MiuixCompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color)
            // 与库内顶栏一致：横屏时避让刘海 / 手势条的左右 inset
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .height(CompactBarHeight)
            .clipToBounds(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.padding(start = BarHorizontalPadding)) {
            navigationIcon()
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = TitleStartPadding),
            color = titleColor,
            fontSize = DesignTokens.TopBarTitleSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
        Row(
            modifier = Modifier.padding(end = BarHorizontalPadding),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
