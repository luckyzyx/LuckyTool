/*
 * LuckyTool 统一页面外壳：与 KernelSU 主题页（ThemeScreen）一致的 Expressive 外观。
 * 顶层大标题可折叠 AppBar + surfaceContainer 底色 + 16dp 内容边距。
 */
package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 宿主是否已经提供底部导航栏（MainShell 的 NavigationBar 已消费系统导航栏 inset）。
 * 为 true 时 [ExpressiveList] 不再重复叠加导航栏内边距，避免列表底部多出一段空白。
 */
val LocalBottomBarPresent = staticCompositionLocalOf { false }

/**
 * 悬浮底栏的高度（由 MainShell 提供）：内容列表在常规底部内边距之外再预留这么多，
 * 使条目可以滑到悬浮胶囊之下（模糊/玻璃效果因此有可采样的内容），同时最后一条不会被永久遮挡。
 */
val LocalShellBottomInset = staticCompositionLocalOf { 0.dp }

/**
 * 统一页面外壳（无返回键时传 onBack = null，用于底部导航主页面）。
 *
 * 用法：
 * ```
 * val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
 * ExpressivePageScaffold(title = stringResource(R.string.nav_setting), scrollBehavior = scrollBehavior) { padding ->
 *     ExpressiveList(scrollBehavior = scrollBehavior, modifier = Modifier.padding(padding)) { ... }
 * }
 * ```
 */
@Composable
fun ExpressivePageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState()),
    content: @Composable (PaddingValues) -> Unit,
) {
    ExpressiveScaffold(
        modifier = modifier,
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    if (onBack != null) TopBarBackButton(onClick = onBack)
                },
                title = { Text(title) },
                actions = actions,
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        content = content,
    )
}

/**
 * 统一内容列表：横向 16dp 边距（与 SegmentedColumn 卡片对齐）、条目间距 8dp、
 * 底部预留导航栏 / 手写笔工具栏高度，并把滚动联动到折叠 AppBar。
 */
@Composable
fun ExpressiveList(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: LazyListScope.() -> Unit,
) {
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val captionBar = WindowInsets.captionBar.asPaddingValues()
    // 宿主已有底部导航栏（已消费导航栏 inset）时不再重复叠加，避免底部空白
    val bottomInset = if (LocalBottomBarPresent.current) {
        0.dp
    } else {
        navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        state = state,
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 4.dp,
            bottom = 16.dp + bottomInset + LocalShellBottomInset.current,
        ),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
