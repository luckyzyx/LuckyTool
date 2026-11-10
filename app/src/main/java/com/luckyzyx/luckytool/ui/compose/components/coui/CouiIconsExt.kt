package com.luckyzyx.luckytool.ui.compose.components.coui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * COUI 图标集缺失的图形，按同一套规格补齐：24dp 视口基准、1.4 线宽、圆头圆角描边。
 *
 * 只在 [com.itos.coui.icons.CouiIcons] 确实没有对应图形时使用；有对应图形时直接用它，
 * 免得同一功能出现两种笔画粗细。官方集里缺的正好是三类：房屋轮廓（首页/导航），
 * 行尾折角箭头（进入 / 展开），以及筛选、置顶这类自绘入口。
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

    /** 置顶：圆头 + 针身。 */
    val Pin: ImageVector by lazy {
        outline("CouiExt.Pin") {
            circle(12f, 8.5f, 4f)
            stroke("M12,12.5V20.5")
        }
    }

    /**
     * 行尾进入箭头：细长右折角。
     *
     * 视口是 12×24 的窄条，调用方按 12dp × 24dp 摆，别按方形图标缩放。
     */
    val Next: ImageVector by lazy {
        strokeVector("CouiExt.Next", 24.dp, 48.dp, 12f, 24f, autoMirror = true) {
            stroke("M5,6L10.646,11.646C10.842,11.842 10.842,12.158 10.646,12.354L5,18")
        }
    }

    /**
     * 行尾展开提示：上下两个折角，用于下拉选择行。
     *
     * 视口同样是窄条（14×24），调用方按 14dp 宽摆。
     */
    val Popup: ImageVector by lazy {
        strokeVector("CouiExt.Popup", 24.dp, 41.142857f.dp, 14f, 24f) {
            stroke("M4.199,10L8.328,5.413C8.526,5.192 8.872,5.192 9.071,5.413L13.199,10")
            stroke("M13.199,14L9.071,18.587C8.872,18.808 8.526,18.808 8.328,18.587L4.199,14")
        }
    }
}

/** 建一个 24dp 方形的描边 ImageVector。 */
private fun outline(name: String, draw: ImageVector.Builder.() -> Unit): ImageVector =
    strokeVector(name, 24.dp, 24.dp, 24f, 24f, autoMirror = false, draw = draw)

/** 按给定尺寸与视口建一个描边 ImageVector；线宽、端点与拐角固定成 COUI 描边规格。 */
private fun strokeVector(
    name: String,
    width: Dp,
    height: Dp,
    viewportWidth: Float,
    viewportHeight: Float,
    autoMirror: Boolean = false,
    draw: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(name, width, height, viewportWidth, viewportHeight, autoMirror = autoMirror)
    .apply { draw() }
    .build()

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
