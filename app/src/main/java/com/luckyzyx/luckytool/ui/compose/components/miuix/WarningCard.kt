package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luckyzyx.luckytool.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.isDynamicColor
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 警告级别（迁移自 KernelSU `ui/component/WarningLevel.kt`）。
 *
 * 基准仓库将其声明在 `ui/component/` 包内；本批任务的 inScope 只含 miuix 组件目录，
 * 故与 [WarningCard] 同文件声明，语义与取值完全一致。
 */
enum class WarningLevel {
    Error,
    Notice,
}

/**
 * Miuix 警告卡片（迁移自 KernelSU `ui/component/miuix/WarningCard.kt`）。
 *
 * @param message 警告正文。
 * @param modifier 应用于卡片的修饰符。
 * @param level 警告级别，决定容器色与文字色。
 * @param onClick 点击整卡的附加行为；为 null 时不显示按压指示。
 * @param action 尾部动作槽位。
 */
@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier,
    level: WarningLevel = WarningLevel.Error,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        colors = CardDefaults.defaultColors(
            color = level.containerColor(),
            contentColor = level.contentColor(),
        ),
        showIndication = onClick != null,
        pressFeedbackType = PressFeedbackType.Sink,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
            )
            action?.invoke()
        }
    }
}

@Composable
private fun WarningLevel.containerColor(): Color = when {
    isDynamicColor -> when (this) {
        WarningLevel.Error -> colorScheme.errorContainer
        WarningLevel.Notice -> colorScheme.tertiaryContainer
    }

    isInDarkTheme -> when (this) {
        WarningLevel.Error -> Color(0xFF310808)
        WarningLevel.Notice -> Color(0xFF3E2F1B)
    }

    else -> when (this) {
        WarningLevel.Error -> Color(0xFFF8E2E2)
        WarningLevel.Notice -> Color(0xFFFFF0DB)
    }
}

@Composable
private fun WarningLevel.contentColor(): Color = when {
    isDynamicColor -> when (this) {
        WarningLevel.Error -> colorScheme.onErrorContainer
        WarningLevel.Notice -> colorScheme.onTertiaryContainer
    }

    else -> when (this) {
        WarningLevel.Error -> Color(0xFFF72727)
        WarningLevel.Notice -> Color(0xFFF5A623)
    }
}
