package com.luckyzyx.luckytool.ui.compose.scopes.apps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.components.SingleChoiceDialog
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.showToast

/**
 * 旧 ui.fragment.scopes.apps.OplusSecuritypPermission 的 Compose 等价物（机械翻译 loadPreferences）。
 * enable_always_allow_app_start_dialog 受 app_start_dialog_use_old_version 依赖；
 * remove_always_allow_app_start_list 为用户多选对话框 custom 条目。
 */
object OplusSecuritypPermissionPage {
    /** 「All」行的 ChoiceItem key：其余行的 key 是十进制 userId，不会与之相撞 */
    private const val AllUsersKey = "all"

    private data class UserDialogData(
        val title: String,
        val items: List<Pair<String, Int>>,
        val allUserIds: List<Int>,
    )

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
            key = "remove_app_start_intercept_whitelist",
            title = c.getString(R.string.remove_app_start_intercept_whitelist),
            onChange = {
                c.sendPrefsValue("android", "remove_app_start_intercept_whitelist", it)
            },
        )
        switch(
            key = "app_start_dialog_use_old_version",
            title = c.getString(R.string.app_start_dialog_use_old_version),
        )
        switch(
            key = "enable_always_allow_app_start_dialog",
            title = c.getString(R.string.enable_always_allow_app_start_dialog),
            summary = c.getString(R.string.need_restart_system),
            enabled = !state.getBoolean("app_start_dialog_use_old_version"),
        )
        if (state.getBoolean("enable_always_allow_app_start_dialog")) {
            val removeListTitle = c.getString(R.string.remove_always_allow_app_start_list)
            custom(key = "remove_always_allow_app_start_list", title = removeListTitle) {
                var userDialogData by remember { mutableStateOf<UserDialogData?>(null) }
                PrefRowInset {
                    PrefRow(
                        title = removeListTitle,
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
                                userDialogData = UserDialogData(
                                    title = removeListTitle,
                                    items = items,
                                    allUserIds = users.map { info -> info.id },
                                )
                            }
                        },
                    )
                }

                userDialogData?.let { data ->
                    // items 用 remember(data) 固定身份：SingleChoiceDialog 的临时选中按 items 记忆，
                    // 每次重组重建列表会把用户刚点的行重置回初始值
                    val items = remember(data) {
                        data.items.mapIndexed { index, item ->
                            ChoiceItem(
                                // 第 0 项是「All」，其余行的数值 key 是 userId，不会撞
                                key = if (index == 0) AllUsersKey else item.second.toString(),
                                title = item.first,
                            )
                        }
                    }
                    SingleChoiceDialog(
                        title = data.title,
                        items = items,
                        // 初始选中恒为「All」：打开前 curUserId 刚被置为全部 userId
                        selectedKey = AllUsersKey,
                        onDismiss = { userDialogData = null },
                        onConfirm = { key ->
                            // 与原实现一致：All 写回全部 userId，单行写回只含该 userId 的列表
                            val userIds = if (key == AllUsersKey) ArrayList(data.allUserIds)
                            else arrayListOf(key.toInt())
                            c.sendPrefsValue("android", "remove_always_allow_app_start_list", userIds)
                            userDialogData = null
                        },
                    )
                }
            }
        }
        switch(
            key = "auto_unlock_app_ecm_permission_restrict",
            title = c.getString(R.string.auto_unlock_app_ecm_permission_restrict),
        )
    }
}
