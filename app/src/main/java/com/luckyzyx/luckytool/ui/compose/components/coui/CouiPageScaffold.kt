package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.layout.Scaffold
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiCollapsingAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.CouiTopAppBarScrollBehavior
import com.itos.coui.topappbar.LargeTopAppBar
import com.luckyzyx.luckytool.ui.compose.components.ChromeInsets

/**
 * COUI 线的页面外壳。
 *
 * COUI 的 Scaffold 自己录制正文背景源，并把顶栏 / 底栏 / 悬浮按钮放在采样层之外，
 * 所以这里只需要接线标题、返回键、动作区，以及大标题折叠时的正文内边距补偿：
 * 顶栏收起过程中正文要跟随 padding 变化，否则内容会被顶栏盖住。
 *
 * 折叠连接挂在正文外层，页面内容只消费传入的 PaddingValues。
 */
@Composable
internal fun CouiPageScaffold(
    title: String,
    scrollBehavior: CouiTopAppBarScrollBehavior,
    restoreCollapsedFraction: Float = 0f,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = scrollBehavior
    val density = LocalDensity.current
    val state = behavior.state
    // 从另一种外观切回来时标题应保持收起：COUI 没有对外的状态写入入口，
    // 这里按折叠比例往顶栏的滚动连接里补一次等效滚动，之后的帧里由弹簧收敛到位。
    LaunchedEffect(behavior, restoreCollapsedFraction) {
        val fraction = restoreCollapsedFraction.coerceIn(0f, 1f)
        if (fraction <= 0f) return@LaunchedEffect
        // 无搜索栏时顶栏的可折叠行程是「展开高度 - 收起高度」，与 COUI 自己算 range 的方式一致
        val range = with(density) {
            (CouiCollapsingAppBarDefaults.titleOnlyHeight - CouiCollapsingAppBarDefaults.collapsedTitleHeight).toPx()
        }
        state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -range * fraction),
            source = NestedScrollSource.UserInput,
        )
    }
    val dark = CouiTheme.isDark
    val pageColors = CouiPageDefaults.themeColors(dark)
    Scaffold(
        modifier = modifier,
        // COUI 的页面底色与卡片色是两档：外壳铺页面底色，列表卡片才浮得起来。
        containerColor = pageColors.background,
        windowInsets = ChromeInsets,
        topBar = {
            LargeTopAppBar(
                // 用 COUI 的语义字体而不是 M3 Text：顶栏只提供 LocalCouiTextStyle，
                // M3 Text 读不到它，标题会退回 Material 的正文字号。HomeTitle 与顶栏折叠时用来算缩放的基准一致。
                title = {
                    CouiText(
                        text = title,
                        kind = CouiTextKind.HomeTitle,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (onBack != null) CouiTopBarBackButton(onClick = onBack)
                },
                actions = actions,
                scrollBehavior = behavior,
                dark = dark,
                colors = CouiTopAppBarDefaults.colors(titleColor = pageColors.title, dark = dark),
                windowInsets = ChromeInsets.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = { floatingActionButton() },
    ) { innerPadding ->
        val contentPadding = remember(behavior, innerPadding, density) {
            state.contentPadding(innerPadding, density)
        }
        // 顶栏高度只能靠帧收敛，切回来后的第一帧里正文若还用展开态的留白，
        // 列表就会先被大标题压住再跳一次；所以先把留白放到交接过来的收起位置。
        val restoreFraction = restoreCollapsedFraction.coerceIn(0f, 1f)
        var restoreSettled by remember(restoreFraction) { mutableStateOf(restoreFraction <= 0f) }
        if (!restoreSettled && state.collapsedFraction >= restoreFraction - 0.01f) {
            SideEffect { restoreSettled = true }
        }
        val restoredPadding = if (restoreSettled) {
            contentPadding
        } else {
            val restoredTop = remember(contentPadding, density, restoreFraction) {
                with(density) {
                    (innerPadding.calculateTopPadding() -
                        (CouiCollapsingAppBarDefaults.titleOnlyHeight -
                            CouiCollapsingAppBarDefaults.collapsedTitleHeight) * restoreFraction)
                        .coerceAtLeast(0.dp)
                }
            }
            remember(contentPadding, restoredTop) {
                contentPadding.withTop(minOf(contentPadding.calculateTopPadding(), restoredTop))
            }
        }
        Box(Modifier.nestedScroll(behavior.nestedScrollConnection)) {
            content(restoredPadding)
        }
    }
}

/** 只改顶部留白，其余边沿继续走原 [PaddingValues]。 */
private fun PaddingValues.withTop(top: Dp): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        this@withTop.calculateLeftPadding(layoutDirection)

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        this@withTop.calculateRightPadding(layoutDirection)

    override fun calculateTopPadding() = top

    override fun calculateBottomPadding() = this@withTop.calculateBottomPadding()
}
