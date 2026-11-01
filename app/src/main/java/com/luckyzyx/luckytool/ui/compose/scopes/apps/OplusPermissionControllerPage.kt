package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 权限管理页：解锁默认桌面数量上限、去掉存储权限例外对话框的开关。
 * 后一项只在 osCode < 37 时渲染。
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
