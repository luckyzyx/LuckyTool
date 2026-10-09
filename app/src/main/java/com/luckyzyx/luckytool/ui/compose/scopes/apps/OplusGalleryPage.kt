package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isZh

/**
 * 相册页（旧 ui.fragment.scopes.apps.OplusGallery 的 Compose 等价物）。
 * 旧 open 菜单（openApp）不在 Compose 页呈现。
 */
object OplusGalleryPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_gallery",
        prefsName = ModulePrefs,
        packName = "com.coloros.gallery3d",
        scopes = arrayOf("com.coloros.gallery3d", "com.oplus.aiunit"),
        restartEnabled = true,
        isVisible = { getOSVersionCode >= 27 && checkPackName("com.coloros.gallery3d") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode < 27) return@ScopePageSpec
        // 水印
        category(c.getString(R.string.GalleryWaterMark))
        switch(
            key = "replace_oneplus_model_watermark",
            title = c.getString(R.string.replace_oneplus_model_watermark),
            summary = c.getString(R.string.replace_oneplus_model_watermark_summary),
        )
        if (getOSVersionCode < 30) {
            switch(
                key = "remove_gallery_watermark_word_limit",
                title = c.getString(R.string.remove_watermark_word_limit),
            )
        }
        if (getOSVersionCode >= 34) {
            switch(
                key = "enable_ai_master_watermark",
                title = c.getString(R.string.enable_ai_master_watermark),
            )
        }
        switch(
            key = "enable_hassel_watermark",
            title = c.getString(R.string.enable_hassel_watermark),
        )
        switch(
            key = "enable_privacy_watermark",
            title = c.getString(R.string.enable_privacy_watermark),
        )
        if (getOSVersionCode in 27..33 && isZh(c)) {
            switch(
                key = "enable_spring_festival_watermark",
                title = c.getString(R.string.enable_spring_festival_watermark),
                summary = c.getString(R.string.enable_spring_festival_watermark_summary),
            )
            switch(
                key = "enable_national_day_watermark",
                title = c.getString(R.string.enable_national_day_watermark),
                summary = c.getString(R.string.enable_national_day_watermark_summary),
            )
        }
        // 滤镜
        if (getOSVersionCode < 34) {
            category(c.getString(R.string.CameraFilter))
            switch(
                key = "enable_gallery_jiangwen_filter",
                title = c.getString(R.string.camera_filter_jiangwen),
            )
        }
        // 视图
        category(c.getString(R.string.GalleryView))
        switch(
            key = "enable_photo_listview_senior_picked",
            title = c.getString(R.string.enable_photo_listview_senior_picked),
        )
        list(
            key = "set_photo_view_thumb_line_display_mode",
            title = c.getString(R.string.set_photo_view_thumb_line_display_mode),
            entries = c.resources.getStringArray(R.array.universal_switch_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
        // 编辑器
        category(c.getString(R.string.GalleryEditor))
        switch(
            key = "enable_photo_editor_gif_synthesis",
            title = c.getString(R.string.enable_photo_editor_gif_synthesis),
        )
        switch(
            key = "enable_lns_cut_photo",
            title = c.getString(R.string.enable_lns_cut_photo),
        )
        if (SDK < A15) {
            switch(
                key = "remove_aigc_elimination_limit",
                title = c.getString(R.string.remove_aigc_elimination_limit),
                summary = c.getString(R.string.remove_aigc_elimination_limit_summary),
            )
        }
    }
}
