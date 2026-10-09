/*
 * Miuix 外观线底栏（迁移自 KernelSU `ui/component/bottombar/BottomBarMiuix.kt` 的 UI 部分）。
 * KernelSU 用 pager 选页，LuckyTool 用 NavHost 路由，故选中项与点击回调由调用方传入。
 */
package com.luckyzyx.luckytool.ui.compose.components.miuix

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix 底栏条目：标题 + 图标 + 可选角标 */
data class MiuixBarItem(
    val label: String,
    val icon: ImageVector,
    val badge: (@Composable () -> Unit)? = null,
)

/**
 * Miuix 底栏：`floating = false` 时为贴底导航栏（可叠加背景模糊），
 * `floating = true` 时为 Apple 风格悬浮胶囊（[floatingBlur] 开启液态玻璃）。
 */
@Composable
fun MiuixBottomBar(
    items: List<MiuixBarItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    floating: Boolean,
    floatingBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!floating) {
        MiuixBlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = selectedIndex == index,
                            onClick = { onSelected(index) },
                            badge = item.badge,
                        )
                    }
                },
            )
        }
    } else {
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            .let { inset -> if (inset != 0.dp) 8.dp + inset else 28.dp }
        MiuixFloatingBottomBar(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            tabsCount = items.size,
            isBlurEnabled = floatingBlur,
        ) { activateTab ->
            items.forEachIndexed { index, item ->
                MiuixFloatingBottomBarItem(
                    selected = selectedIndex == index,
                    onClick = { activateTab(index) },
                    modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                ) {
                    // 图标与文字取 LocalContentColor，胶囊指示器内的副本才能被重着色
                    val icon: @Composable () -> Unit = {
                        Icon(imageVector = item.icon, contentDescription = item.label)
                    }
                    if (item.badge != null) {
                        BadgedBox(badge = { item.badge() }) { icon() }
                    } else {
                        icon()
                    }
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }
    }
}
