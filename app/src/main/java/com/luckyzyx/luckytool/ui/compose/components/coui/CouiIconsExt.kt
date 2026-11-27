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
 * COUI 图标集缺失的图形，按同一套规格补齐：24dp 视口基准、1.4 线宽、圆头圆角描边；
 * 实心形态沿用同一轮廓，只在填色上区分。
 *
 * 只在 [com.itos.coui.icons.CouiIcons] 确实没有对应图形时使用；有对应图形时直接用它，
 * 免得同一功能出现两种笔画粗细。官方集里缺的有三类：房屋轮廓（首页/导航），
 * 行尾折角箭头（进入 / 展开），以及筛选、置顶这类自绘入口。
 *
 * 底栏要用「未选中描边、选中实心」成对表达选中态，而官方集只给了齿轮一对，
 * 其余页签缺的那一半（实心房屋 / 实心镜子 / 实心滑杆 / 描边九宫格）也补在这里。
 */
internal object CouiIconsExt {

    /** 首页：房屋轮廓。 */
    val Home: ImageVector by lazy {
        outline("CouiExt.Home") {
            stroke("M4,10.5L12,3.5L20,10.5V20.5H14.5V15A2.5,2.5 0 0 0 9.5,15V20.5H4Z")
        }
    }

    /** 首页选中态：同一轮廓填满，门洞仍是挖空。 */
    val HomeFilled: ImageVector by lazy {
        solid("CouiExt.HomeFilled") {
            fill("M4,10.5L12,3.5L20,10.5V20.5H14.5V15A2.5,2.5 0 0 0 9.5,15V20.5H4Z")
        }
    }

    /** 九宫格未选中态：四格描边，与官方实心九宫格的格位一致。 */
    val GridOutlined: ImageVector by lazy {
        outline("CouiExt.GridOutlined") {
            stroke("M4.8,3H9.2A1.8,1.8 0 0 1 11,4.8V9.2A1.8,1.8 0 0 1 9.2,11H4.8A1.8,1.8 0 0 1 3,9.2V4.8A1.8,1.8 0 0 1 4.8,3Z")
            stroke("M14.8,3H19.2A1.8,1.8 0 0 1 21,4.8V9.2A1.8,1.8 0 0 1 19.2,11H14.8A1.8,1.8 0 0 1 13,9.2V4.8A1.8,1.8 0 0 1 14.8,3Z")
            stroke("M4.8,13H9.2A1.8,1.8 0 0 1 11,14.8V19.2A1.8,1.8 0 0 1 9.2,21H4.8A1.8,1.8 0 0 1 3,19.2V14.8A1.8,1.8 0 0 1 4.8,13Z")
            stroke("M14.8,13H19.2A1.8,1.8 0 0 1 21,14.8V19.2A1.8,1.8 0 0 1 19.2,21H14.8A1.8,1.8 0 0 1 13,19.2V14.8A1.8,1.8 0 0 1 14.8,13Z")
        }
    }

    /**
     * 搜索选中态：实心镜片 + 加粗手柄。
     *
     * 视口与官方 Search 一样是 20×20，描边版靠细圆环显得轻，实心版要放大一点镜片才不会显小。
     */
    val SearchFilled: ImageVector by lazy {
        solidVector("CouiExt.SearchFilled", 20.dp, 20.dp, 20f, 20f) {
            fill(circlePath(8.191f, 8.191f, 6.05f))
            stroke("M12.3,13.6L17.1,18.3", width = 2.2f)
        }
    }

    /** 滑杆选中态：两条轨道上放实心滑块，滑块位置与描边版一致。 */
    val SliderCustomFilled: ImageVector by lazy {
        solid("CouiExt.SliderCustomFilled") {
            stroke("M1.15,7H22.85")
            stroke("M1.15,17H22.85")
            fill(circlePath(15f, 7f, 2.5f))
            fill(circlePath(9f, 17f, 2.5f))
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

/** 建一个 24dp 方形的实心 ImageVector，用于底栏这类需要「选中即填满」的场合。 */
private fun solid(name: String, draw: ImageVector.Builder.() -> Unit): ImageVector =
    solidVector(name, 24.dp, 24.dp, 24f, 24f, draw = draw)

/** 按给定尺寸与视口建一个实心 ImageVector；轮廓仍由调用点用同一段路径数据给出。 */
private fun solidVector(
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

/** 追加一段填充路径。 */
private fun ImageVector.Builder.fill(pathData: String) {
    addPath(
        PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color(0xFF000000)), fillAlpha = 1f,
        pathFillType = PathFillType.NonZero,
    )
}

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

/** 追加一段描边路径；实心图标里的加粗部件靠 [width] 调整。 */
private fun ImageVector.Builder.stroke(pathData: String, width: Float = 1.4f) {
    addPath(
        PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Transparent), fillAlpha = 1f,
        stroke = SolidColor(Color(0xFF000000)), strokeAlpha = 1f,
        strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
    )
}

/** 圆的路径数据；描边圆与实心圆共用，免得两处半径各写一遍。 */
private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M${cx - r},${cy}a${r},${r} 0 1,0 ${r * 2},0a${r},${r} 0 1,0 ${-r * 2},0"

/** 追加一个描边圆。 */
private fun ImageVector.Builder.circle(cx: Float, cy: Float, r: Float) {
    stroke(circlePath(cx, cy, r))
}
