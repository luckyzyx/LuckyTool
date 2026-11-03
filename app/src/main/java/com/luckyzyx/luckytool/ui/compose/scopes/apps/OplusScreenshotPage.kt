package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * 截屏页：FLAG_SECURE 屏蔽、长截图页数上限、PNG 保存格式与包名 MD5 加密的开关。
 * remove_system_screenshot_delay 与 remove_screenshot_privacy_limit 均 isVisible=false 从不展示，本页不含这两项。
 */
object OplusScreenshotPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_screenshot",
        prefsName = ModulePrefs,
        packName = "com.oplus.screenshot",
        scopes = arrayOf("com.oplus.screenshot"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "disable_flag_secure",
            title = c.getString(R.string.disable_flag_secure),
            summary = arraySummaryLine(
                c.getString(R.string.disable_flag_secure_summary),
                c.getString(R.string.need_restart_system)
            ),
        )
        switch(
            key = "remove_page_limit_for_long_screenshots",
            title = c.getString(R.string.remove_page_limit_for_long_screenshots),
            summary = c.getString(R.string.remove_page_limit_for_long_screenshots_summary),
        )
        switch(
            key = "enable_png_save_format",
            title = c.getString(R.string.enable_png_save_format),
        )
        switch(
            key = "disable_screenshot_packagename_md5_encrypt",
            title = c.getString(R.string.disable_screenshot_packagename_md5_encrypt),
        )
    }
}
