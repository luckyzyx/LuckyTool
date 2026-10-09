package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Expressive 切换按钮（迁移自 KernelSU `ui/component/material/MaterialToggleButton.kt`），
 * 配合 ButtonGroupDefaults 的分段形状可组成按钮组。
 */
@Composable
fun MaterialToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = materialToggleButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shapes = shapes,
        content = content,
    )
}

@Composable
fun materialToggleButtonColors(
    checkedContainerColor: Color = MaterialTheme.colorScheme.primary,
    checkedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
): ToggleButtonColors = ToggleButtonDefaults.colors(
    checkedContainerColor = checkedContainerColor,
    checkedContentColor = checkedContentColor,
    containerColor = containerColor,
    contentColor = contentColor,
)
