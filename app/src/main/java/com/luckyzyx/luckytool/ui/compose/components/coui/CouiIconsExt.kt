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

    /** 导航首页选中态：保持相同外轮廓，填实屋身并留出门洞。 */
    val HomeSelected: ImageVector by lazy {
        outline("CouiExt.HomeSelected") {
            fill("M4,10.5L12,3.5L20,10.5V20.5H14.5V15A2.5,2.5 0 0 0 9.5,15V20.5H4Z")
        }
    }

    /** 对应官方 Grid 的四格轮廓，右上格沿用较大的尺寸。 */
    val GridOutline: ImageVector by lazy {
        outline("CouiExt.GridOutline") {
            stroke("M4.8,3.7H9.2Q10.3,3.7 10.3,4.8V9.2Q10.3,10.3 9.2,10.3H4.8Q3.7,10.3 3.7,9.2V4.8Q3.7,3.7 4.8,3.7Z")
            stroke("M14.8,2.7H20.2Q21.3,2.7 21.3,3.8V9.2Q21.3,10.3 20.2,10.3H14.8Q13.7,10.3 13.7,9.2V3.8Q13.7,2.7 14.8,2.7Z")
            stroke("M4.8,13.7H9.2Q10.3,13.7 10.3,14.8V19.2Q10.3,20.3 9.2,20.3H4.8Q3.7,20.3 3.7,19.2V14.8Q3.7,13.7 4.8,13.7Z")
            stroke("M14.8,13.7H19.2Q20.3,13.7 20.3,14.8V19.2Q20.3,20.3 19.2,20.3H14.8Q13.7,20.3 13.7,19.2V14.8Q13.7,13.7 14.8,13.7Z")
        }
    }

    /** 与 SliderCustom 同位置的调节杆，选中时填实两个滑块。 */
    val SlidersSelected: ImageVector by lazy {
        outline("CouiExt.SlidersSelected") {
            stroke("M2,7H22M2,17H22")
            fill("M11.15,7a3.85,3.85 0 1,0 7.7,0a3.85,3.85 0 1,0 -7.7,0")
            fill("M5.15,17a3.85,3.85 0 1,0 7.7,0a3.85,3.85 0 1,0 -7.7,0")
        }
    }

    /** 搜索选中态加厚镜框，保留透空镜面以维持放大镜辨识度。 */
    val SearchSelected: ImageVector by lazy {
        strokeVector("CouiExt.SearchSelected", 20.dp, 20.dp, 20f, 20f) {
            fill("M1.422,8.191a6.769,6.769 0 1,0 13.538,0a6.769,6.769 0 1,0 -13.538,0 M4.122,8.191a4.069,4.069 0 1,0 8.138,0a4.069,4.069 0 1,0 -8.138,0")
            fill("M11.8,13.6L16.7,18.5Q17.7,19.5 18.6,18.6Q19.5,17.7 18.5,16.7L13.6,11.8Z")
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

/** 实心导航图形使用满不透明度，状态透明度由底栏统一应用。 */
private fun ImageVector.Builder.fill(pathData: String) {
    addPath(
        PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd,
    )
}
