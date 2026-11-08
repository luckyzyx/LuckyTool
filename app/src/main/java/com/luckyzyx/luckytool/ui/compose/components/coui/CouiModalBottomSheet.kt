package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.itos.coui.theme.CouiTheme

/**
 * COUI 线的底部弹出面板。
 *
 * 用一个铺满全屏、不占系统栏的 Dialog 承载：底色是半透明遮罩，面板贴着屏幕底部，
 * 顶部两角圆角、底部两角不圆（贴边，圆了反而露出一条遮罩缝）。
 *
 * 注意：
 * - 面板高度上限是屏幕的 80%，超出部分靠 verticalScroll 滚动，手指在面板内滚动不会误触关闭。
 * - 遮罩层单独一个 Box 接收点击，点击面板本身的事件不会冒泡到遮罩，所以拖动内容不会把面板关掉。
 * - 底部补 navigationBars 内边距，避免最后一行被手势条盖住。
 */
@Composable
internal fun CouiModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val scrimInteraction = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = if (dark) 0.62f else 0.42f))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = onDismissRequest,
                    ),
            )
            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = screenHeight * 0.8f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(colors.container)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                content = content,
            )
        }
    }
}
