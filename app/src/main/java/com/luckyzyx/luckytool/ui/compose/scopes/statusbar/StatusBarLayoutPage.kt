package com.luckyzyx.luckytool.ui.compose.scopes.statusbar

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK

/**
 * 状态栏布局页（旧 ui.fragment.scopes.statusbar.StatusBarLayout 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性（SDK == A13 才展示全部条目）、margin 滑条依赖兼容模式开关。
 */
object StatusBarLayoutPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_layout",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        // 旧 root 条目与本页所有条目均 isVisible = SDK == A13
        if (SDK != A13) return@ScopePageSpec
        list(
            key = "statusbar_layout_mode",
            title = c.getString(R.string.statusbar_layout_mode),
            entries = c.resources.getStringArray(R.array.statusbar_layout_mode_entries),
            entryValues = arrayOf("0", "1"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
        switch(
            key = "statusbar_layout_compatible_mode",
            title = c.getString(R.string.statusbar_layout_compatible_mode),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("statusbar_layout_compatible_mode", false)) {
            slider(
                key = "statusbar_layout_left_margin",
                title = c.getString(R.string.statusbar_layout_left_margin),
                valueRange = 0..150,
                default = 0,
                summary = c.getString(R.string.statusbar_layout_margin_tip),
            )
            slider(
                key = "statusbar_layout_right_margin",
                title = c.getString(R.string.statusbar_layout_right_margin),
                valueRange = 0..150,
                default = 0,
                summary = c.getString(R.string.statusbar_layout_margin_tip),
            )
        }
    }
}
