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

    /** 卡片 / 分组外层圆角：单条卡片四角，分组首条上端、尾条下端。 */
    val CardRadius: Dp = 16.dp

    /**
     * 分组内部相邻条目的圆角：0dp 让组内条目与组卡片连成一体
     * （分组边界即分类边界：Category 与 Category 之间是一个分组，
     * 分组内不允许任何条目单独呈现为卡片）。
     */
    val ItemInnerRadius: Dp = 0.dp

    /**
     * 分组内相邻条目之间的垂直间距：0dp 与 [ItemInnerRadius] 配合，
     * 使同一个分组内的条目合并渲染为一张连续卡片（组间间距由 [GroupGap] 承载）。
     */
    val ItemGap: Dp = 0.dp

    /** 分组之间的垂直间距（由组内首条承载）。 */
    val GroupGap: Dp = 12.dp

    /** 卡片相对屏幕左右两侧的水平内缩。 */
    val CardHorizontalInset: Dp = 12.dp

}
