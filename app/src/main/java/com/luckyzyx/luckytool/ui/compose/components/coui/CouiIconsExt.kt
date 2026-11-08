package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * COUI 图标集缺失的功能页签图标，按同一套规格补齐：24dp 视口、1.4 线宽、圆头描边。
 *
 * 只在 COUI 图标集确实没有对应图形时使用；有对应图形时直接用 [com.itos.coui.icons.CouiIcons]，
 * 免得同一功能出现两种笔画粗细。
 */
internal object CouiIconsExt {

    /** 首页：房屋轮廓。 */
    val Home: ImageVector by lazy {
        outline("CouiExt.Home") {
            stroke("M4,10.5L12,3.5L20,10.5V20.5H14.5V15A2.5,2.5 0 0 0 9.5,15V20.5H4Z")
        }
    }

    /** 日志：文档轮廓 + 三行正文。 */
    val Notes: ImageVector by lazy {
        outline("CouiExt.Notes") {
            stroke("M6,3.5H18A2,2 0 0 1 20,5.5V18.5A2,2 0 0 1 18,20.5H6A2,2 0 0 1 4,18.5V5.5A2,2 0 0 1 6,3.5Z")
            stroke("M8,8.5H16")
            stroke("M8,12H16")
            stroke("M8,15.5H13")
        }
    }

    /** 功能页：两条滑杆 + 滑块。 */
    val Tune: ImageVector by lazy {
        outline("CouiExt.Tune") {
            stroke("M4,8H20")
            stroke("M4,16H20")
            circle(9.5f, 8f, 2.6f)
            circle(15f, 16f, 2.6f)
        }
    }

    /** 置顶：圆头 + 针身。 */
    val Pin: ImageVector by lazy {
        outline("CouiExt.Pin") {
            circle(12f, 8.5f, 4f)
            stroke("M12,12.5V20.5")
        }
    }
}

/** 建一个与 COUI 图标同规格的描边 ImageVector。 */
private fun outline(name: String, draw: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = false).apply {
        draw()
    }.build()

/** 追加一段描边路径。 */
private fun ImageVector.Builder.stroke(pathData: String) {
    addPath(
        PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Transparent), fillAlpha = 1f,
        stroke = SolidColor(Color(0xFF000000)), strokeAlpha = 1f,
        strokeLineWidth = 1.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
    )
}

/** 追加一个描边圆。 */
private fun ImageVector.Builder.circle(cx: Float, cy: Float, r: Float) {
    stroke("M${cx - r},${cy}a${r},${r} 0 1,0 ${r * 2},0a${r},${r} 0 1,0 ${-r * 2},0")
}
