/*
 * LuckyTool 页面外壳的公共局部值与底部内边距公式；骨架本身见 components/UiStyle.kt。
 */
package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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
 * 内容列表底部内边距的统一公式：调用点的常规基准量 + 系统导航栏 / caption bar
 * （宿主底栏未消费 inset 时叠加）+ 悬浮胶囊底栏占位（[LocalShellBottomInset]）。
 *
 * 供 [ExpressiveList] 以及自带 LazyColumn 的页面复用同一份取值：
 * 各自硬编码 8/16dp 会让最后一条在悬浮底栏开启时被胶囊永久遮挡。
 */
@Composable
fun materialBottomInset(base: Dp = 16.dp): Dp {
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val captionBar = WindowInsets.captionBar.asPaddingValues()
    // 宿主已有底部导航栏（已消费导航栏 inset）时不再重复叠加，避免底部空白
    val navInset = if (LocalBottomBarPresent.current) {
        0.dp
    } else {
        navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
    }
    return base + navInset + LocalShellBottomInset.current
}
