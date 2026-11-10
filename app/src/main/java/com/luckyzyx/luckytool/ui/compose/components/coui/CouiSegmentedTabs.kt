package com.luckyzyx.luckytool.ui.compose.components.coui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.segmentbutton.SegmentedControl
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/**
 * 页签形态的分段切换：胶囊容器里按标签分段，替代 Material 的 SecondaryTabRow。
 *
 * API 34 起下发官方 [SegmentedControl]（按标签自然宽度收窄、可按住拖动切换、指示器跟随滑动），
 * 外层仍占满可用宽度并居中，调用方传的 modifier（内边距等）作用在外层；
 * 低版本取不到该控件（官方标注 @RequiresApi(34)），退回等宽胶囊分段：只有选中段铺 accent 底色。
 *
 * 注意：
 * - labels 为空时什么都不画（整个 composable 提前返回），调用方不必自己判空。
 * - selectedIndex 越界时官方控件会 require 失败，所以越界一律走等宽分支，表现为没有任何段被选中。
 * - 选中态由调用方持有，点击只回传下标。
 */
@Composable
internal fun CouiSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && selectedIndex in labels.indices) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SegmentedControl(
                labels = labels,
                selectedIndex = selectedIndex,
                onSelected = onSelect,
            )
        }
        return
    }

    EqualWidthSegments(
        labels = labels,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}

/** 低版本回退：等宽分段，段数固定、不滚动，标签过长时省略而不是挤压布局 */
@Composable
private fun EqualWidthSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = CouiTheme.isDark
    val colors = CouiTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiCapsuleShape)
            .background(colors.container)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            key(index) {
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 32.dp)
                        .clip(CouiCapsuleShape)
                        .background(if (selected) colors.accent else Color.Transparent)
                        .couiButtonFeedback(dark = dark, interactionSource = interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = { onSelect(index) },
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CouiText(
                        text = label,
                        kind = CouiTextKind.ItemValue,
                        color = if (selected) colors.onAccent else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
