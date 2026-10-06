package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 旧 ui.fragment.scopes.apps.OplusPictorial 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusPictorialPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_pictorial",
        prefsName = ModulePrefs,
        packName = "com.heytap.pictorial",
        scopes = arrayOf("com.heytap.pictorial"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_image_save_watermark",
            title = c.getString(R.string.remove_image_save_watermark),
        )
        switch(
            key = "remove_video_save_watermark",
            title = c.getString(R.string.remove_video_save_watermark),
        )
    }
}
