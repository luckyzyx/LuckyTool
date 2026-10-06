package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 旧 ui.fragment.scopes.apps.OplusPermissionController 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusPermissionControllerPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_permission_controller",
        prefsName = ModulePrefs,
        packName = "com.android.permissioncontroller",
        scopes = arrayOf("com.android.permissioncontroller"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "unlock_default_desktop_limit",
            title = c.getString(R.string.unlock_default_desktop_limit),
        )
        if (getOSVersionCode < 37) {
            switch(
                key = "remove_storage_permission_exception_dialog",
                title = c.getString(R.string.remove_storage_permission_exception_dialog),
            )
        }
    }
}
