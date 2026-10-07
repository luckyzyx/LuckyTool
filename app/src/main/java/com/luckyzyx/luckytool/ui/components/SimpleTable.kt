package com.luckyzyx.luckytool.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 简易 Compose 表格：程序生成数据的 Markdown 表格（Markwon TablePlugin）等价物。
 * 首行渲染为表头（加粗居中）并跟分割线；固定列宽 + 横向滚动，单元格单行省略。
 * 列宽可传入，未传时按列数取默认值（2 列 150/110，3 列 120/220/220，4 列 110/140/96/110）。
 */
@Composable
fun SimpleTable(
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    columnWidths: List<Dp>? = null,
) {
    if (rows.isEmpty()) return
    val widths = columnWidths ?: when (rows.first().size) {
        2 -> listOf(150.dp, 110.dp)
        3 -> listOf(120.dp, 220.dp, 220.dp)
        4 -> listOf(110.dp, 140.dp, 96.dp, 110.dp)
        else -> List(rows.first().size) { 140.dp }
    }
    Column(modifier.horizontalScroll(rememberScrollState())) {
        rows.forEachIndexed { rowIndex, row ->
            val isHeader = rowIndex == 0
            Row {
                row.forEachIndexed { colIndex, cell ->
                    Text(
                        text = cell,
                        textAlign = if (isHeader) TextAlign.Center else TextAlign.Start,
                        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(widths.getOrElse(colIndex) { 140.dp })
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            if (isHeader) HorizontalDivider()
        }
    }
}
