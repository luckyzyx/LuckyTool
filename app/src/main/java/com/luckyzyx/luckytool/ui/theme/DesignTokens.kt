package com.luckyzyx.luckytool.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 共享设计令牌（design tokens）：Material 与 Miuix 两条组件线共用的尺寸常量单一来源。
 *
 * 引入背景：UI 组件风格更换后，同样的圆角与间距被分散定义在
 * [com.luckyzyx.luckytool.ui.compose.components.material.SegmentedList]（material 线）、
 * [com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefList]（Miuix 线）与
 * [com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCompactTopBar]（Miuix 顶栏）三处，
 * 语义相同但值各自维护，后续新增主题/外观线时需多处同步、极易漂移。
 *
 * 统一到此处后，新增外观线只需引用本对象即可对齐几何观感。命名对齐「视觉语义」而非
 * 某条组件线的历史常量名，避免 Material 组件去引用带 Miuix 语义的旧常量。
 */
object DesignTokens {

    /** 卡片 / 分组外层圆角：单条卡片四角，分组首条上端、尾条下端。 */
    val CardRadius: Dp = 16.dp

    /**
     * 分组内部相邻条目的圆角：0dp 让组内条目与组卡片连成一体
     * （对齐旧 PreferenceCategory 观感：Category 与 Category 之间是一个分组，
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

    /** 单行顶栏高度（对齐 Miuix 库内 TopAppBarDefaults.CollapsedHeight）。 */
    val TopBarHeight: Dp = 52.dp

    /** 顶栏返回键起始 / 动作键结束的水平内边距。 */
    val TopBarHorizontalPadding: Dp = 16.dp

    /** 顶栏标题与返回键之间的间距。 */
    val TopBarTitleStartPadding: Dp = 16.dp

    /** 顶栏标题字号：Material 与 Miuix 两条外观线共用的统一标题字号。 */
    val TopBarTitleSize: TextUnit = 24.sp
}
