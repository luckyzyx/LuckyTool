package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName

/**
 * 高能户外模式页：com.oplus.riderMode 的功能开关。
 * 「移除App列表白名单」开启后任意 App 进入前台都能激活高能户外模式，开关需重启系统作用域生效。
 */
object OplusRiderModePage {

    val spec = PageSpec(
        pageKey = "oplus_rider_mode",
        prefsName = ModulePrefs,
        packName = "system",
        displayPackName = "com.oplus.riderMode",
        scopes = arrayOf("system", "com.oplus.riderMode"),
        restartEnabled = true,
        isVisible = { checkPackName("com.oplus.riderMode") },
    ) {
        val c = requireNotNull(context) { "PageScreen 未注入 Context" }

        switch(
            key = "remove_rider_mode_whitelist",
            title = c.getString(R.string.remove_rider_mode_whitelist),
            summary = arraySummaryLine(
                c.getString(R.string.remove_rider_mode_whitelist_summary),
                c.getString(R.string.need_restart_system),
            ),
        )
    }
}
