package com.luckyzyx.luckytool.ui.compose.scopes.others

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * KSWeb 页（旧 ui.fragment.scopes.others.KsWeb 的 Compose 等价物）。
 */
object KsWebPage {

    val spec = ScopePageSpec(
        pageKey = "ks_web",
        prefsName = ModulePrefs,
        packName = "ru.kslabs.ksweb",
        scopes = arrayOf("ru.kslabs.ksweb"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "ksweb_remove_check_license",
            title = c.getString(R.string.remove_pro_license),
        )
    }
}
