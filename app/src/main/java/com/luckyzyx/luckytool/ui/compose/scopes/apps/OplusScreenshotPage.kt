package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 截屏页：长截图页数上限、PNG 保存格式与包名 MD5 加密的开关。
 * remove_system_screenshot_delay 不可见（isVisible=false）且从未展示，本页不含该项。
 */
object OplusScreenshotPage {

    val spec = PageSpec(
        pageKey = "oplus_screenshot",
        prefsName = ModulePrefs,
        packName = "com.oplus.screenshot",
        scopes = arrayOf("com.oplus.screenshot"),
    ) {
        val c = requireNotNull(context) { "PageScreen 未注入 Context" }
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
