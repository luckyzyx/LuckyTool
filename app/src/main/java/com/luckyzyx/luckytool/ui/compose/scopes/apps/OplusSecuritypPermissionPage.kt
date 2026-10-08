package com.luckyzyx.luckytool.ui.compose.scopes.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.showToast
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/**
 * 旧 ui.fragment.scopes.apps.OplusSecuritypPermission 的 Compose 等价物（机械翻译 loadPreferences）。
 * enable_always_allow_app_start_dialog 受 app_start_dialog_use_old_version 依赖；
 * remove_always_allow_app_start_list 为用户多选对话框 custom 条目。
 */
object OplusSecuritypPermissionPage {

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
                var userDialogData by remember { mutableStateOf<UserDialogData?>(null) }
                var curUserId by remember { mutableStateOf(arrayListOf<Int>()) }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
                                curUserId = ArrayList(users.map { info -> info.id })
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
                    when (LocalUiMode.current) {
                        UiMode.Miuix -> OverlayDialog(
                            show = true,
                            title = data.title,
                            onDismissRequest = { userDialogData = null },
                        ) {
                            Column {
                                data.items.forEachIndexed { index, item ->
                                    val selected = when {
                                        index == 0 -> curUserId.size == data.allUserIds.size
                                        else -> curUserId.size == 1 && curUserId.firstOrNull() == item.second
                                    }
                                    MiuixRadioItem(
                                        title = item.first,
                                        selected = selected,
                                        onClick = {
                                            curUserId = when (index) {
                                                0 -> ArrayList(data.allUserIds)
                                                else -> arrayListOf(item.second)
                                            }
                                        },
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                MiuixTextButton(
                                    text = stringResource(android.R.string.cancel),
                                    onClick = { userDialogData = null },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(20.dp))
                                MiuixTextButton(
                                    text = stringResource(android.R.string.ok),
                                    onClick = {
                                        c.sendPrefsValue(
                                            "android", "remove_always_allow_app_start_list", curUserId
                                        )
                                        userDialogData = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                            }
                        }

                        UiMode.Material -> AlertDialog(
                            onDismissRequest = { userDialogData = null },
                            title = { Text(data.title) },
                            text = {
                                Column {
                                    data.items.forEachIndexed { index, item ->
                                        val selected = when {
                                            index == 0 -> curUserId.size == data.allUserIds.size
                                            else -> curUserId.size == 1 && curUserId.firstOrNull() == item.second
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    curUserId = when (index) {
                                                        0 -> ArrayList(data.allUserIds)
                                                        else -> arrayListOf(item.second)
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RadioButton(selected = selected, onClick = null)
                                            Text(item.first)
                                        }
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { userDialogData = null }) {
                                    Text(stringResource(android.R.string.cancel))
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        c.sendPrefsValue(
                                            "android", "remove_always_allow_app_start_list", curUserId
                                        )
                                        userDialogData = null
                                    },
                                ) { Text(stringResource(android.R.string.ok)) }
                            },
                        )
                    }
                }
            }
        }
        switch(
            key = "auto_unlock_app_ecm_permission_restrict",
            title = c.getString(R.string.auto_unlock_app_ecm_permission_restrict),
        )
    }
}
