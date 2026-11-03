package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 文件管理页：去掉保存、压缩、命名文件时的字数限制。
 * 三项都只在 osCode >= 37 时渲染。
 */
object OplusFileManagerPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_file_manager",
        prefsName = ModulePrefs,
        packName = "com.coloros.filemanager",
        scopes = arrayOf("com.coloros.filemanager"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 37) {
            switch(
                key = "remove_word_limit_for_saving_files",
                title = c.getString(R.string.remove_word_limit_for_saving_files),
            )
            switch(
                key = "remove_word_limit_for_compress_files",
                title = c.getString(R.string.remove_word_limit_for_compress_files),
            )
            switch(
                key = "remove_word_limit_for_label_name_files",
                title = c.getString(R.string.remove_word_limit_for_label_name_files),
            )
        }
    }
}
