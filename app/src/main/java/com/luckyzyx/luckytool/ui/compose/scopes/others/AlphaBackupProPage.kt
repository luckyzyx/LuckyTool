package com.luckyzyx.luckytool.ui.compose.scopes.others

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * Alpha Backup Pro 页（旧 ui.fragment.scopes.others.AlphaBackupPro 的 Compose 等价物）。
 */
object AlphaBackupProPage {

    val spec = ScopePageSpec(
        pageKey = "alpha_backup_pro",
        prefsName = ModulePrefs,
        packName = "com.ruet_cse_1503050.ragib.appbackup.pro",
        scopes = arrayOf("com.ruet_cse_1503050.ragib.appbackup.pro"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_check_license",
            title = c.getString(R.string.remove_pro_license),
        )
    }
}
