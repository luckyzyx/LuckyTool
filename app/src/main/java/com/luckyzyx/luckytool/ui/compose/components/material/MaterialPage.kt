/*
 * LuckyTool 统一页面外壳：与 KernelSU 主题页（ThemeScreen）一致的 Expressive 外观。
 * 顶层固定标题 AppBar（标题与返回键/动作键同行，不随上滑折叠）+ surfaceContainer 底色 + 16dp 内容边距。
 */
package com.luckyzyx.luckytool.ui.compose.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixBlurredBar
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCompactTopBar
import com.luckyzyx.luckytool.ui.compose.components.miuix.rememberMiuixBlurBackdrop
import com.luckyzyx.luckytool.ui.shell.LocalEnableBlur
import com.luckyzyx.luckytool.ui.theme.DesignTokens
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
// Miuix 线骨架用的库件（与 KernelSU SettingsMiuix.kt / 本仓库 ThemeScreenMiuix.kt 同一批）
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 宿主是否已经提供底部导航栏（MainShell 的 NavigationBar 已消费系统导航栏 inset）。
 * 为 true 时 [ExpressiveList] 不再重复叠加导航栏内边距，避免列表底部多出一段空白。
 */
val LocalBottomBarPresent = staticCompositionLocalOf { false }

/**
 * 悬浮底栏的高度（由 MainShell 提供）：内容列表在常规底部内边距之外再预留这么多，
 * 使条目可以滑到悬浮胶囊之下（模糊/玻璃效果因此有可采样的内容），同时最后一条不会被永久遮挡。
 */
val LocalShellBottomInset = staticCompositionLocalOf { 0.dp }

/**
 * 内容列表底部内边距的统一公式：调用点的常规基准量 + 系统导航栏 / caption bar
 * （宿主底栏未消费 inset 时叠加）+ 悬浮胶囊底栏占位（[LocalShellBottomInset]）。
 *
 * 供 [ExpressiveList]、[MiuixExpressiveList] 以及自带 LazyColumn 的页面
 * （作用域特殊页的 Miuix 布局，见 `ScopePageSpec.contentMiuix`）复用同一份取值：
 * 各自硬编码 8/16dp 会让最后一条在悬浮底栏开启时被胶囊永久遮挡。
 */
@Composable
fun materialBottomInset(base: Dp = 16.dp): Dp {
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val captionBar = WindowInsets.captionBar.asPaddingValues()
    // 宿主已有底部导航栏（已消费导航栏 inset）时不再重复叠加，避免底部空白
    val navInset = if (LocalBottomBarPresent.current) {
        0.dp
    } else {
        navBars.calculateBottomPadding() + captionBar.calculateBottomPadding()
    }
    return base + navInset + LocalShellBottomInset.current
}

/**
 * Miuix 线页面骨架下发给内容的真实内边距。
 * material 线恒为 null（[ExpressiveList] 因此走 m3 分支，取值与分派前完全一致）。
 */
@Immutable
private class MiuixPageScaffoldValues(
    val innerPadding: PaddingValues,
)

/** Miuix 线页面骨架的内部传递通道：由 [MaterialPageScaffold] 提供、[ExpressiveList] 消费。 */
private val LocalMiuixPageScaffold = staticCompositionLocalOf<MiuixPageScaffoldValues?> { null }

/**
 * 统一页面外壳（无返回键时传 onBack = null，用于底部导航主页面）。
 */
@Composable
fun MaterialPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // 切换外观时把同一份页面内容在两条骨架之间「搬移」（movable content）：页面自身的
    // remember 状态（列表滚动位置、页内临时状态）随内容走，只有外侧骨架被替换。
    // rememberUpdatedState 防止固化首次组合的 lambda（否则页内回调会引用过期取值）。
    val currentContent = rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf<PaddingValues> { innerPadding -> currentContent.value(innerPadding) }
    }
    // Miuix 线：换成 KernelSU 骨架。material 线分支保持原样（只加分支，取值未改）。
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPageScaffold(
            title = title,
            modifier = modifier,
            onBack = onBack,
            actions = actions,
            floatingActionButton = floatingActionButton,
            bottomBar = bottomBar,
            content = movableContent,
        )
        return
    }
    MaterialScaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                expandedHeight = DesignTokens.TopBarHeight,
                navigationIcon = {
                    if (onBack != null) MaterialTopBarBackButton(onClick = onBack)
                },
                title = {
                    Text(
                        title,
                        fontSize = DesignTokens.TopBarTitleSize,
                        fontWeight = FontWeight.Medium,
                    )
                },
                actions = actions,
                colors = materialTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        content = movableContent,
    )
}

/**
 * Miuix 线页面骨架（KernelSU SettingsMiuix.kt:75-107 定式，与本仓库 ThemeScreenMiuix.kt:279-326 同款）：
 * 模糊采样层 → MiuixBlurredBar{MiuixCompactTopBar} → Scaffold(popupHost = {}) → 内容列表。
 *
 * 与调用点的契约（调用点冻结、零改动）：content 拿到的仍是真实骨架内边距（top = 顶栏实测高度），
 * 因此把内边距当 `Modifier.padding(padding)` 用的调用点保持正确 —— LogPage 的居中 Box，
 * 以及 FunctionPage 里自带 LazyColumn 的 ScopeScreen（不能再套 ExpressiveList）。
 * 列表页的 [ExpressiveList] 另经 [LocalMiuixPageScaffold] 取到同一份内边距，
 * 自行抵消外层已施加的顶部偏移，从而复刻 KernelSU「列表铺到顶栏之下」的滚动观感。
 * 顶栏不再接 MiuixScrollBehavior，标题与返回键同行、左对齐固定显示、不随上滑折叠（MiuixCompactTopBar 单行样式）。
 *
 * 硬约束：popupHost 保持空槽位（全应用唯一 Miuix popup host 由根部 Miuix Scaffold 的默认槽位提供，
 * MainShell.kt:187-191）；采样层录制盒只包内容、绝不含绘制该图层的顶栏（MainShell.kt:160-163）。
 */
@Composable
private fun MiuixPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberMiuixBlurBackdrop(LocalEnableBlur.current)
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    Scaffold(
        modifier = modifier,
        topBar = {
            MiuixBlurredBar(backdrop) {
                MiuixCompactTopBar(
                    title = title,
                    color = barColor,
                    navigationIcon = {
                        if (onBack != null) MiuixTopBarBackButton(onClick = onBack)
                    },
                    actions = actions,
                )
            }
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        // 空槽位：弹层冒泡到根部 Miuix Scaffold 的默认 popupHost（各页不得自装 host）
        popupHost = {},
        // KernelSU 写法为 systemBars.add(displayCutout)；本仓库 Compose 版本没有 WindowInsets.add，
        // 用同一语义的 union（= miuix basic/Scaffold.kt:90 自己的默认值写法）
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalMiuixPageScaffold provides remember(innerPadding) {
                MiuixPageScaffoldValues(innerPadding)
            },
        ) {
            // 模糊采样层录制盒：只录制本页内容；顶栏绘制在该层之内，不能放进盒内（渲染树循环引用）
            Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
                content(innerPadding)
            }
        }
    }
}

/** Miuix 线返回按钮（KernelSU AboutMiuix.kt:130-144 同款：RTL 时图标水平镜像）。 */
@Composable
private fun MiuixTopBarBackButton(onClick: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    IconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = MiuixIcons.Back,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

