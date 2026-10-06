package com.luckyzyx.luckytool.ui.compose.scopes.apps

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.showToast

/**
 * 旧 ui.fragment.scopes.apps.OplusSecuritypPermission 的 Compose 等价物（机械翻译 loadPreferences）。
 * enable_always_allow_app_start_dialog 受 app_start_dialog_use_old_version 依赖；
 * remove_always_allow_app_start_list 为用户多选对话框 custom 条目。
 */
object OplusSecuritypPermissionPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_securityp_permission",
        prefsName = ModulePrefs,
        packName = "com.oplus.securitypermission",
        scopes = arrayOf("com.oplus.securitypermission"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 38) {
            switch(
                key = "disable_malicious_app_intercept",
                title = c.getString(R.string.disable_malicious_app_intercept),
                summary = c.getString(R.string.need_restart_system),
            )
        }
        switch(
            key = "app_start_dialog_use_old_version",
            title = c.getString(R.string.app_start_dialog_use_old_version),
        )
        switch(
            key = "enable_always_allow_app_start_dialog",
            title = c.getString(R.string.enable_always_allow_app_start_dialog),
            summary = c.getString(R.string.need_restart_system),
            enabled = !state.getBoolean("app_start_dialog_use_old_version"),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_always_allow_app_start_dialog")) {
            val removeListTitle = c.getString(R.string.remove_always_allow_app_start_list)
            custom(key = "remove_always_allow_app_start_list", title = removeListTitle) {
                ListItem(
                    onClick = {
                        UserService.get(c) {
                            val users = it?.users
                            if (users.isNullOrEmpty()) {
                                c.showToast("userId is null")
                                return@get
                            }
                            val items = arrayListOf(Pair("All", -1))
                            users.forEach { info ->
                                items.add(Pair("${info.name} [${info.id}]", info.id))
                            }
                            var curUserId = ArrayList(users.map { info -> info.id })
                            MaterialAlertDialogBuilder(c, dialogCentered).apply {
                                setTitle(removeListTitle)
                                setSingleChoiceItems(
                                    items.map { i -> i.first }.toTypedArray(), 0,
                                ) { _, which ->
                                    curUserId = when (which) {
                                        0 -> ArrayList(users.map { info -> info.id })
                                        else -> arrayListOf(items[which].second)
                                    }
                                }
                                setNeutralButton(android.R.string.cancel, null)
                                setPositiveButton(android.R.string.ok) { _, _ ->
                                    c.sendPrefsValue(
                                        "android", "remove_always_allow_app_start_list", curUserId
                                    )
                                }
                            }.show()
                        }
                    },
                ) { Text(removeListTitle) }
            }
        }
        switch(
            key = "auto_unlock_app_ecm_permission_restrict",
            title = c.getString(R.string.auto_unlock_app_ecm_permission_restrict),
        )
    }
}
