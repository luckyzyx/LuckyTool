package com.luckyzyx.luckytool.ui.compose.components.coui

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionColors
import com.itos.coui.dialog.CouiDialogColors
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.dialog.Dialog
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme

/** 横屏放大时卡片的宽度上限：比竖屏的 392dp 更宽，让取色面板在平板横屏上铺得更开。 */
private val LandscapeMaxWidth = 480.dp

/** 横屏放大时卡片上下各留出的空隙；卡片高度上限 = 屏幕高度 − 该值。 */
private val LandscapeVerticalMargin = 48.dp

/**
 * COUI 线的确认弹窗，默认居中摆放。
 *
 * 标题、正文与按钮都由 COUI 的 Dialog 绘制：正文超长时只有正文滚动，按钮区固定在窗口底部；
 * 按钮放不下时自动改成竖排。content 落进 Dialog 的正文槽，用来塞列表、输入框等自定义内容，
 * 它与 message 可以同时给，自定义内容排在 message 之后。
 *
 * 横屏时统一改用自建外壳绘制弹窗：库把横屏上限硬编码成 landscapeMaxHeight（306dp，为手机横屏
 * 设计），平板横屏可用高度远大于它，正文会被压扁滚动。自建外壳把上限放开到「屏幕高度减上下
 * 留白」，并加宽卡片，让取色器、列表这类内容在平板横屏上一次铺开。
 *
 * 注意：主操作按钮的 style 若是 Primary，构造 CouiDialogAction 时就必须带 colors
 * （CouiDialogDefaults.primaryActionColors(accent)），该 data class 的 init 会直接校验，
 * 缺了会抛异常，这里无法替调用方补。
 */
@Composable
internal fun CouiAlertDialog(
    onDismissRequest: () -> Unit,
    confirmAction: CouiDialogAction,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    dismissAction: CouiDialogAction? = null,
    neutralAction: CouiDialogAction? = null,
    placement: CouiDialogPlacement = CouiDialogPlacement.Center,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val dark = CouiTheme.isDark
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeDialog(
            onDismissRequest = onDismissRequest,
            confirmAction = confirmAction,
            dismissAction = dismissAction,
            neutralAction = neutralAction,
            title = title,
            message = message,
            dark = dark,
            modifier = modifier,
            content = content,
        )
    } else {
        Dialog(
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            confirmAction = confirmAction,
            // 显式传当前主题深浅色：宿主可能覆盖过色板，不能让 Dialog 的默认参数自己判断。
            dark = dark,
            title = title,
            message = message,
            dismissAction = dismissAction,
            neutralAction = neutralAction,
            placement = placement,
            content = content,
        )
    }
}

/**
 * 横屏放大用的自建弹窗外壳：窗口遮罩、圆角卡片、标题、正文滚动区与底部按钮按 COUI 弹窗的
 * 尺寸与配色绘制，只是卡片高度上限按屏幕高度放开，宽度加宽到 [LandscapeMaxWidth]。
 */
@Composable
private fun LandscapeDialog(
    onDismissRequest: () -> Unit,
    confirmAction: CouiDialogAction,
    dismissAction: CouiDialogAction?,
    neutralAction: CouiDialogAction?,
    title: String?,
    message: String?,
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CouiDialogDefaults.themeColors(dark)
    val maxCardHeight = (LocalConfiguration.current.screenHeightDp.dp - LandscapeVerticalMargin)
        .coerceAtLeast(CouiDialogDefaults.landscapeMaxHeight)
    ComposeDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(colors.dimAmount)
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        Box(
            modifier
                .widthIn(max = LandscapeMaxWidth)
                .fillMaxWidth()
                .heightIn(max = maxCardHeight)
                .clip(CouiDialogDefaults.shape)
                .background(colors.surface),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (!title.isNullOrEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().padding(
                                start = CouiDialogDefaults.contentPadding,
                                end = CouiDialogDefaults.contentPadding,
                                top = CouiDialogDefaults.titleTop,
                                bottom = CouiDialogDefaults.titleBottom,
                            ),
                        ) {
                            CouiText(
                                text = title,
                                kind = CouiTextKind.TopBar,
                                color = colors.title,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (!message.isNullOrEmpty()) {
                        CouiText(
                            text = message,
                            kind = CouiTextKind.ItemSummary,
                            color = colors.message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = CouiDialogDefaults.contentPadding)
                                .padding(bottom = CouiDialogDefaults.messageBottom),
                        )
                    }
                    content()
                }
                LandscapeDialogActions(
                    confirmAction = confirmAction,
                    dismissAction = dismissAction,
                    neutralAction = neutralAction,
                    colors = colors,
                    dark = dark,
                )
            }
        }
    }
}

/**
 * 自建外壳的底部操作区：两个按钮横排，单个或三个按钮竖排；横排时两按钮等宽，外边距与
 * 按钮间距取 [CouiDialogDefaults] 的 buttonEdgePadding / buttonInsidePadding。
 */
@Composable
private fun LandscapeDialogActions(
    confirmAction: CouiDialogAction,
    dismissAction: CouiDialogAction?,
    neutralAction: CouiDialogAction?,
    colors: CouiDialogColors,
    dark: Boolean,
) {
    val horizontal = listOfNotNull(dismissAction, neutralAction, confirmAction)
    if (horizontal.size == 2) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            horizontal.forEachIndexed { index, action ->
                Box(
                    Modifier.weight(1f).padding(
                        start = if (index == 0) CouiDialogDefaults.buttonEdgePadding
                            else CouiDialogDefaults.buttonInsidePadding,
                        end = if (index == horizontal.lastIndex) CouiDialogDefaults.buttonEdgePadding
                            else CouiDialogDefaults.buttonInsidePadding,
                        top = CouiDialogDefaults.buttonTopPadding,
                        bottom = CouiDialogDefaults.buttonBottomPadding,
                    ),
                ) {
                    LandscapeDialogButton(
                        action = action,
                        colors = if (action === neutralAction) colors.copy(action = colors.destructive) else colors,
                        dark = dark,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    } else {
        val vertical = listOfNotNull(neutralAction, confirmAction, dismissAction)
        Column(Modifier.fillMaxWidth()) {
            vertical.forEachIndexed { index, action ->
                Box(
                    Modifier.fillMaxWidth().padding(
                        start = CouiDialogDefaults.buttonEdgePadding,
                        end = CouiDialogDefaults.buttonEdgePadding,
                        top = CouiDialogDefaults.buttonTopPadding,
                        bottom = if (index == vertical.lastIndex) CouiDialogDefaults.buttonBottomPadding
                            else CouiDialogDefaults.buttonTopPadding,
                    ),
                ) {
                    LandscapeDialogButton(
                        action = action,
                        colors = if (action === neutralAction) colors.copy(action = colors.destructive) else colors,
                        dark = dark,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * 自建外壳的操作按钮：胶囊形、按启用态取表面色与文字色，主操作用调用方给的主题色，
 * 次级操作用中性配色。按下走默认涟漪反馈。
 */
@Composable
private fun LandscapeDialogButton(
    action: CouiDialogAction,
    colors: CouiDialogColors,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val actionColors = action.colors ?: secondaryActionColors(dark).copy(
        content = colors.action,
        disabledContent = colors.disabledAction,
    )
    Box(
        modifier
            .heightIn(min = CouiDialogDefaults.buttonHeight)
            .clip(CouiCapsuleShape)
            .background(if (action.enabled) actionColors.surface else actionColors.disabledSurface)
            .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
            .padding(horizontal = CouiDialogDefaults.buttonHorizontalPadding, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        CouiText(
            text = action.text,
            kind = CouiTextKind.ItemTitle,
            color = if (action.enabled) actionColors.content else actionColors.disabledContent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 次级操作按钮的中性配色：深色表面 #353535、浅色表面 #EBEBEB，禁用态降低对比度。 */
private fun secondaryActionColors(dark: Boolean) = CouiDialogActionColors(
    surface = Color(if (dark) 0xFF353535 else 0xFFEBEBEB),
    content = Color(if (dark) 0xE6FFFFFF else 0xE6000000),
    disabledSurface = Color(if (dark) 0x1AFFFFFF else 0x14000000),
    disabledContent = Color(if (dark) 0x28FFFFFF else 0x42000000),
)
