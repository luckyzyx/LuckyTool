package com.luckyzyx.luckytool.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 共享设计令牌（design tokens）：COUI 与 Material 两条组件线共用的尺寸常量单一来源。
 *
 * 引入背景：同样的圆角与间距若分散在各自的组件实现里，语义相同但值各自维护，
 * 新增外观线时需多处同步、极易漂移。
 *
 * 统一到此处后，新增外观线只需引用本对象即可对齐几何观感。命名对齐「视觉语义」而非
 * 某条组件线的历史常量名，避免组件实现去引用带具体外观语义的常量。
 */
object DesignTokens {

    /** 分段行圆角：组内每一行各自是一张卡片，四角统一取该值。 */
    val CardRadius: Dp = 16.dp

    /** 分组之间的垂直间距（由组内首条承载）。 */
    val GroupGap: Dp = 12.dp

    /** 卡片相对屏幕左右两侧的水平内缩。 */
    val CardHorizontalInset: Dp = 12.dp

}
