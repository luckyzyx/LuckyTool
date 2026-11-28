package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 状态栏图标排序页（独立功能页）。
 *
 * 排序列表有 50 项（49 个宿主槽位 + 时钟伪条目），拖拽换行需要整屏高度，因此单独成页，
 * 由「状态栏图标」页的入口行进入（`StatusBarIconPage` 里的 `page(...)` 项）。
 * 总开关与拖拽列表同页，顶栏的「重启作用域」按钮用于让开关的装载状态生效。
 */
object StatusBarIconOrderPage {

    /** 页面注册键（[com.luckyzyx.luckytool.ui.compose.PageRegistry] 与 pageTargetMap） */
    const val PAGE_KEY = "statusbar_icon_order"

    val spec = PageSpec(
        pageKey = PAGE_KEY,
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "PageScreen 未注入 Context" }
        // 开关
        group(title = c.getString(R.string.statusbar_icon_order_category_switch)) {
            // 总开关：hooker 只在开启时装载（onHook 里判断），因此关→开、开→关都需要重载宿主作用域才生效。
            // 该装载状态在 com.android.systemui 进程里，重建本 Activity 影响不到它，
            // 必须用顶栏的「重启作用域」按钮（RestartMenuUtils.RestartScopeDialog），故只在 summary 里提示。
            switch(
                key = STATUS_BAR_ICON_ORDER_ENABLED_KEY,
                title = c.getString(R.string.statusbar_icon_order_enable),
                summary = c.getString(R.string.statusbar_icon_order_enable_summary) +
                    "\n" + c.getString(R.string.need_restart_scope),
            )
            if (state.getBoolean(STATUS_BAR_ICON_ORDER_ENABLED_KEY, false)) {
                // raw：跳过 COUI 卡片包装——卡片自带的 clip 图层会让约 8192px 之后的内容命中不到
                // 指针（右区第 39 行往后拖不动），卡片视觉改由 Zones 在自己的 Column 上绘制。
                // 排列：左右两个区域的拖拽列表，松手即写回偏好，宿主侧经 dataChannel 实时生效
                group(title = c.getString(R.string.statusbar_icon_order_category_arrange)) {
                    custom(raw = true) {
                        val left by state.stringFlow(STATUS_BAR_ICON_ORDER_LEFT_KEY, "")
                            .collectAsStateWithLifecycle()
                        val right by state.stringFlow(STATUS_BAR_ICON_ORDER_RIGHT_KEY, "")
                            .collectAsStateWithLifecycle()
                        StatusBarIconOrderZones(
                            leftRaw = left,
                            rightRaw = right,
                            onApply = { newLeft, newRight ->
                                state.set(STATUS_BAR_ICON_ORDER_LEFT_KEY, newLeft)
                                sendValue(STATUS_BAR_ICON_ORDER_LEFT_KEY, newLeft)
                                state.set(STATUS_BAR_ICON_ORDER_RIGHT_KEY, newRight)
                                sendValue(STATUS_BAR_ICON_ORDER_RIGHT_KEY, newRight)
                            },
                        )
                    }
                }
            }
        }
    }
}
