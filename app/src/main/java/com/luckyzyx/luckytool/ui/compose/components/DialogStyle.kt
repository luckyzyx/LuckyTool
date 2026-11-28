package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.luckyzyx.luckytool.ui.compose.components.style.CouiDialogStyle
import com.luckyzyx.luckytool.ui.compose.components.style.MaterialDialogStyle
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/**
 * 弹窗外壳的版式接口：一切「标题 + 正文 + 操作按钮」的对话框都从这里出，页面与业务组件不判外观线。
 *
 * 与 PrefStyle（设置区块的行与卡片）、UiStyle（页面骨架与列表外壳）分工不重叠。
 * 新增外观样式 = 新写一个实现对象 + 在 currentDialogStyle() 里接一条分支（穷尽 when 会强制补上），
 * 调用点的参数与写法都不需要改。
 */
internal interface DialogStyle {

    /**
     * 确认类弹窗：标题、正文（一段文字和/或自定义内容）与最多两个操作。
     *
     * content 与 message 可以同时给，自定义内容排在正文之后。[bodyScrollable] 为 false 时不加
     * 「上限高度 + 纵向滚动」这一层：正文自带滚动（惰性列表、自定义滚动区）时必须关掉，
     * 否则内外同向滚动会抢手势、滚不到底。
     *
     * secondaryLabel 是次级操作（取消、忽略、查看明细这类）。它落在取消位而不是中性位：COUI 的
     * 中性位按警示色渲染，与「点它只是关掉」的语义不符；只有一个次级按钮时也不会被染成警示色。
     *
     * dismissible 为 false 时返回键与点击外部都不关闭，只能按按钮离开（启动阻塞门这类）。
     *
     * 操作位可以缺：[confirmLabel] 为空时不画确认位；它与 [secondaryLabel] 同时为空就是没有按钮的
     * 展示型弹层，只能靠返回键或点击外部离开（[dismissible] 照旧管着这一点）。只有次级操作时，
     * COUI 线把它放进必填的确认位、仍按次级样式渲染，material 线照常落在取消位。
     * [confirmEnabled] / [secondaryEnabled] 分别管两个操作位的可用态（例如加载中禁用）。
     */
    @Composable
    fun Alert(
        onDismissRequest: () -> Unit,
        confirmLabel: String? = null,
        onConfirm: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        title: String? = null,
        message: String? = null,
        secondaryLabel: String? = null,
        onSecondary: (() -> Unit)? = null,
        confirmEnabled: Boolean = true,
        secondaryEnabled: Boolean = true,
        dismissible: Boolean = true,
        bodyScrollable: Boolean = true,
        content: (@Composable ColumnScope.() -> Unit)? = null,
    )

    /**
     * 忙碌弹窗：只有转圈与一行进度说明，没有可点动作、也不能关掉。
     *
     * [message] 是当前进度文字，为空时只画转圈。
     */
    @Composable
    fun Busy(title: String, modifier: Modifier = Modifier, message: String = "")

    /**
     * 弹窗正文里的一段文字。
     *
     * 只有两条线的正文文字角色不同、又需要调用方自己带手势（长按解锁）时才用它；
     * 普通正文交给 [Alert] 的 message 槽。
     */
    @Composable
    fun BodyText(text: String, modifier: Modifier)

    /**
     * 单值输入弹窗：值、占位、校验与落盘时机仍由调用方持有。
     *
     * COUI 的输入弹窗固定单行，[maxLines] 与 [keyboardType] 只对 material 线生效。
     * [clearContentDescription] 是 COUI 线输入框清空键的无障碍描述，material 线用不上。
     */
    @Composable
    fun Input(
        title: String,
        value: String,
        onValueChange: (String) -> Unit,
        onDismissRequest: () -> Unit,
        confirmLabel: String,
        onConfirm: () -> Unit,
        modifier: Modifier = Modifier,
        message: String? = null,
        hint: String? = null,
        clearContentDescription: String? = null,
        dismissLabel: String? = null,
        keyboardType: KeyboardType = KeyboardType.Text,
        maxLines: Int = 1,
    )
}

/** 当前外观线对应的弹窗版式 */
@Composable
internal fun currentDialogStyle(): DialogStyle = when (LocalUiMode.current) {
    UiMode.Coui -> CouiDialogStyle
    UiMode.Material -> MaterialDialogStyle
}
