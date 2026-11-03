package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * 对话框正文容器：正文可滚动，底部操作行固定不参与压缩。
 *
 * 之所以需要它：应用所有弹层都走 miuix 的 OverlayDialog，它不是独立窗口，而是由根部 Scaffold 的
 * popup host 承载。miuix 的 DialogContentLayout 在手机上用 Alignment.BottomCenter 底对齐，外层链是
 * `.imePadding().navigationBarsPadding().captionBarPadding().fillMaxSize()`，正文 Column 的
 * `heightIn(max =)` 在非展开形态下是 Dp.Unspecified 且正文没有滚动。键盘一弹，外层被抬到键盘上方，
 * 可用高度只剩「屏幕 − 键盘」，Column 把所有固定高度的子项逐项压扁，靠后的子项（输入框、按钮行）
 * 直接被压成 0 高度，用户读不到也点不到。
 *
 * 这里的做法：
 * - 正文用 `weight(1f, fill = false)`：有剩余高度时正文拿到「有界剩余高度」，fill = false 保证内容
 *   短时对话框还是自然高度，不会被撑满。
 * - actions 作为非加权子项放在正文之后：Column 先测量非加权子项，剩余高度才分给加权子项，
 *   所以压缩时按钮行优先保留，永远不会被压成 0。
 * - 正文额外加 `heightIn(max = 窗口高度 − 输入法高度 − 预留)`：weight 只在**父级约束有界**时才有
 *   高度可分，而 miuix 在手机态把正文的 `heightIn(max =)` 设成 Dp.Unspecified
 *   (DialogContentLayout.kt:351)，底对齐 + 无界约束下正文会按自然高度撑开，对话框顶部被顶出屏幕
 *   （键盘弹出后表现为「上部内容看不见」）。这里自己算一个上限，两种父级约束下都不会超出可见区域；
 *   输入法高度取 WindowInsets.ime（edge-to-edge 下有效），非 edge-to-edge 时窗口本身已缩短，
 *   容器高度已经变小，两种情形都被覆盖。
 *
 * [scrollable] 决定正文要不要由本容器来滚：
 * - `true`（默认）：正文是普通固定高度子项（输入框、说明文字、开关行等），由本容器加
 *   verticalScroll 在内部滚动。固定高度子项在无界高度下只按自身高度摆放，不会被压扁。
 * - `false`：正文自己就是惰性列表或自带滚动的容器（典型：LazyColumn 应用列表、内层
 *   `weight(1f, fill = false)` 的 Card）。**这时绝不能加 verticalScroll**——verticalScroll 用
 *   `Constraints.Infinity` 测量子项，LazyColumn 拿不到有界高度会直接抛
 *   "Vertically scrollable component was measured with an infinity maximum height constraints"，
 *   内层 weighted 子项在无界高度下也无从分配。关掉之后正文只剩 `weight(1f, fill = false)`，
 *   拿到的是有界剩余高度，滚动交给正文自己。
 * - 判断方法：正文里有 `LazyColumn` / `LazyRow` / `verticalScroll` / 依赖父级有界高度才能工作的
 *   `weight` 子项 → 传 `false`；否则保持默认 `true`。
 */
@Composable
fun MiuixDialogScrollBody(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerDpSize.height
    val imeHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val maxBodyHeight = (windowHeight - imeHeight - HeightReserve).coerceAtLeast(MinBodyHeight)

    Column(modifier = modifier.fillMaxWidth()) {
        val bodyModifier = if (scrollable) {
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .heightIn(max = maxBodyHeight)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .heightIn(max = maxBodyHeight)
        }
        Column(modifier = bodyModifier, content = content)
        if (actions != null) {
            Spacer(Modifier.height(12.dp))
            actions()
        }
    }
}

/** 正文上限里预留给标题、按钮行与弹层内外边距的高度。 */
private val HeightReserve = 140.dp

/** 正文上限的下限，避免输入法占满屏幕时正文被算成 0 高度而无法滚动。 */
private val MinBodyHeight = 160.dp
