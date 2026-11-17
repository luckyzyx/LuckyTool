package com.luckyzyx.luckytool.ui.compose.components

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 对话框正文容器：正文可滚动，底部操作行固定不参与压缩。
 *
 * 弹层的正文高度由宿主给的约束决定，键盘弹出后又会被抬到键盘上方，
 * 可用高度只剩「屏幕 − 键盘」。若正文与操作行都是固定高度子项，
 * Column 会把它们逐项压扁，靠后的输入框和按钮行直接被压成 0 高度。
 *
 * 这里的做法：
 * - 正文用 `weight(1f, fill = false)`：有剩余高度时正文拿到「有界剩余高度」，fill = false 保证内容
 *   短时对话框还是自然高度，不会被撑满。
 * - actions 作为非加权子项放在正文之后：Column 先测量非加权子项，剩余高度才分给加权子项，
 *   所以压缩时按钮行优先保留，永远不会被压成 0。
 * - 正文额外加 `heightIn(max = 窗口高度 − 输入法高度 − 预留)`：weight 只在**父级约束有界**时才有
 *   高度可分；无界约束下正文会按自然高度撑开，对话框顶部被顶出屏幕（键盘弹出后表现为「上部内容看不见」）。
 *   这里自己算一个上限，两种父级约束下都不会超出可见区域；
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
fun DialogScrollBody(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxBodyHeight = dialogBodyMaxHeight(HeightReserve)

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

/**
 * 弹层正文（Material AlertDialog 的 text 槽等）可用的最大高度。
 *
 * 与 [dialogListMaxHeight] 的差别：列表有「设计上限」，这里的内容没有设计上限，只按窗口可用
 * 高度收敛；超出部分由调用方加 verticalScroll 滚动。横屏/分屏窗口变矮时，标题 + 正文 + 按钮
 * 若还用内容自然高度会把按钮顶出屏幕，这里算出正文最多能占的高度。
 *
 * @param chrome 弹层里除正文外占用的高度：标题、按钮行与弹层内外边距，按调用点结构传，宁可偏大别偏小。
 * @param min 正文高度下限，窗口极矮时至少保留这个高度可滚动，而不是塌成 0。
 */
@Composable
fun dialogBodyMaxHeight(chrome: Dp, min: Dp = MinBodyHeight): Dp =
    (windowBodyBudget() - chrome).coerceAtLeast(min)

/** 弹层正文可用高度预算：窗口高度减去键盘高度（edge-to-edge 下有效）。 */
@Composable
internal fun windowBodyBudget(): Dp {
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerDpSize.height
    val imeHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    return windowHeight - imeHeight
}

/**
 * 弹层内列表的高度上限：设计上限与「窗口可用高度」取较小值。
 *
 * 横屏 / 分屏等窗口变矮时，正文列表若还用固定上限（如 360/420.dp），标题 + 列表 + 按钮
 * 会超出屏幕、底部 item 被裁掉。这里按窗口高度动态收敛，让列表自己滚动，保证弹层整体
 * 不超出屏幕；竖屏窗口足够高时仍用设计上限，行为不变。
 *
 * @param designMax 设计上的列表最大高度（竖屏等窗口足够高时生效的上限）。
 * @param chrome 弹层里除列表外占用的高度：标题、搜索/状态行、按钮行与弹层内外边距，
 *   每个调用点按自己的结构传；宁可偏大别偏小，偏小会把按钮顶出屏幕。
 * @param min 列表高度下限，窗口极矮时也至少保留这个高度可滚动，而不是塌成 0。
 */
@Composable
fun dialogListMaxHeight(designMax: Dp, chrome: Dp, min: Dp = MinListHeight): Dp =
    (windowBodyBudget() - chrome).coerceIn(min, designMax)

/** 弹层内列表的高度下限，避免窗口极矮时列表被算成 0 高度而无法滚动。 */
private val MinListHeight = 120.dp
