package com.luckyzyx.luckytool.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconsExt
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.scroll.rememberCouiOverscrollEffect
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.ui.compose.components.material.LocalShellBottomInset
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.shell.LocalCategoryBadgeColors
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppIconCache

/**
 * 功能行摘要最多两行：摘要是「该页前三个功能标题」，一行装不下三个就已截断成省略号，
 * 两行能完整露出前三个标题；再长也没有信息增量，仍按省略号收尾。
 */
private const val FUNCTION_SUMMARY_MAX_LINES = 2

@Immutable
internal data class FunctionRow(
    val pageKey: String,
    val title: String,
    val summary: String?,
    /** null = 分类页（应用相关/状态栏相关/…），渲染 [categoryBadges] 徽标而不是 App 图标 */
    val packName: String?,
)

/** 应用式功能列表：稳定行标识、48dp 图标，单行标题与最多两行摘要。 */
@Composable
internal fun FunctionList(
    rows: List<FunctionRow>,
    padding: PaddingValues,
    onOpenPage: (pageKey: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // 顶栏高度（padding 的 top）放进列表 contentPadding：视口铺满整屏，
    // 滚动中的行能进入顶栏下方；水平与底部内边距仍留在视口外。
    val layoutDirection = LocalLayoutDirection.current
    val listModifier = modifier
        .fillMaxSize()
        .consumeWindowInsets(padding)
        .padding(
            start = padding.calculateStartPadding(layoutDirection),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = padding.calculateBottomPadding(),
        )
    val contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        // COUI 的首张分段卡片与折叠顶栏之间留出分组间距；Material 仍贴着顶栏起排
        top = padding.calculateTopPadding() + if (LocalUiMode.current == UiMode.Coui) CouiPageDefaults.groupSpacing else 0.dp,
        bottom = 16.dp + LocalShellBottomInset.current,
    )
    val isCoui = LocalUiMode.current == UiMode.Coui
    // 跟手回弹：一个 OverscrollEffect 只能挂在一个 LazyColumn 上，因此两线各自在 @Composable 里取好后传下去。
    val listOverscroll = if (isCoui) rememberCouiOverscrollEffect() else rememberOverscrollEffect()
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = contentPadding,
        // COUI 行自带分段卡片，段间不能留缝，否则一张卡片被切断；Material 仍按 2dp 分段。
        verticalArrangement = if (isCoui) Arrangement.Top else Arrangement.spacedBy(2.dp),
        overscrollEffect = listOverscroll,
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.pageKey },
            contentType = { _, _ -> "function" },
        ) { index, row ->
            val onClick = { onOpenPage(row.pageKey, row.title) }
            AppItem(index = index, count = rows.size) {
                AppListItem(
                    onClick = onClick,
                    iconSize = 48.dp,
                    headlineContent = {
                        // 行内排版按外观分派：COUI 线用 COUI 语义字体（ItemTitle/ItemSummary）
                        // 与页面配色，Material 线保持 M3 Text。
                        if (isCoui) {
                            CouiText(
                                text = row.title,
                                kind = CouiTextKind.ItemTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(row.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    supportingContent = row.summary?.let { summary ->
                        {
                            if (isCoui) {
                                CouiText(
                                    text = summary,
                                    kind = CouiTextKind.ItemSummary,
                                    color = CouiPageDefaults.themeColors(CouiTheme.isDark).summary,
                                    maxLines = FUNCTION_SUMMARY_MAX_LINES,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Text(
                                    text = summary,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = FUNCTION_SUMMARY_MAX_LINES,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    leadingContent = { FunctionRowIcon(row) },
                    trailingContent = {
                        if (isCoui) {
                            // 尾部箭头尺寸与 COUI 偏好行一致（细长箭头），颜色弱于正文前景
                            Icon(
                                CouiIconsExt.Next,
                                contentDescription = null,
                                modifier = Modifier.size(width = 12.dp, height = 24.dp),
                                tint = CouiPageDefaults.themeColors(CouiTheme.isDark).disabled,
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FunctionRowIcon(row: FunctionRow) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        val packName = row.packName
        if (packName == null) {
            CategoryBadgeIcon(categoryBadges[row.pageKey])
        } else {
            AppIcon(packName)
        }
    }
}

@Composable
private fun AppIcon(packName: String) {
    val context = LocalContext.current
    val size = with(LocalDensity.current) { 48.dp.roundToPx() }
    val iconState = remember(context, packName, size) {
        AppIconCache.state(context, packName, size)
    }
    val icon = iconState.value
    val alpha = remember(iconState) { Animatable(if (icon != null) 1f else 0f) }
    LaunchedEffect(icon) {
        if (icon != null && alpha.value < 1f) alpha.animateTo(1f, tween(150))
    }
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value },
        )
    }
}

/**
 * 分类徽标外框：槽位是 48dp，但应用图标的位图自带透明留白（自适应图标可见内容约 72/108），
 * 实心方块铺满 48dp 会显得比 App 图标大一圈，故缩到 42dp。
 */
private val CategoryBadgeSize = 42.dp

/** 徽标内部图形：沿用 42dp × 2/3 的比例（与应用图标可见内容的占比一致） */
private val CategoryBadgeGlyphSize = 28.dp

/** 徽标圆角：随外框等比缩放 */
private val CategoryBadgeCornerSize = 10.dp

/**
 * 分类徽标：圆角对齐应用图标的可见裁切，整体尺寸与应用图标一致。
 *
 * 默认用 [CategoryBadge] 的固定色（多彩）；关闭「多彩分类图标」后退化成主题色单色徽标。
 */
@Composable
private fun CategoryBadgeIcon(badge: CategoryBadge?) {
    val colorful = LocalCategoryBadgeColors.current
    // 单色徽标的底色与前景按外观取：COUI 线用强调色与其反色（色板保证可读），Material 线用容器色与其前景
    val background: Color
    val foreground: Color
    if (LocalUiMode.current == UiMode.Coui) {
        val colorScheme = CouiTheme.colorScheme
        background = if (colorful) badge?.color ?: Color(0xFF607D8B) else colorScheme.accent
        foreground = if (colorful) Color.White else colorScheme.onAccent
    } else {
        // Material 线专用：多彩色相同，单色徽标取 M3 容器色
        val colorScheme = MaterialTheme.colorScheme
        background = if (colorful) badge?.color ?: Color(0xFF607D8B) else colorScheme.secondaryContainer
        foreground = if (colorful) Color.White else colorScheme.onSecondaryContainer
    }
    Box(
        modifier = Modifier
            .size(CategoryBadgeSize)
            .clip(RoundedCornerShape(CategoryBadgeCornerSize))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = badge?.icon ?: Icons.Filled.Apps,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(CategoryBadgeGlyphSize),
        )
    }
}
