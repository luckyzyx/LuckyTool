package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 云服务页：解除网络限速与关闭强制备份应用列表的开关。
 */
object OplusCloudServicePage {

    val spec = ScopePageSpec(
        pageKey = "oplus_cloud_service",
        prefsName = ModulePrefs,
        packName = "com.heytap.cloud",
        scopes = arrayOf("com.heytap.cloud"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_network_limit",
            title = c.getString(R.string.remove_network_limit),
            summary = c.getString(R.string.remove_network_limit_summary),
        )
        switch(
            key = "disable_forced_backup_app_list",
            title = c.getString(R.string.disable_forced_backup_app_list),
        )
    }
}
