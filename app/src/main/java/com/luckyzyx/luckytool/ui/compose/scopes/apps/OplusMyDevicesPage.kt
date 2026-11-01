package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * 我的设备页：强制开启飞牛云 NAS 选项的开关。
 * 摘要逐条列出 com.heytap.mydevices、com.heytap.accessory、com.android.systemui、com.coloros.gallery3d
 * 四个包的应用名与其 support_fn_nas 元数据值。
 */
object OplusMyDevicesPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_my_devices",
        prefsName = ModulePrefs,
        packName = "com.heytap.mydevices",
        scopes = arrayOf("com.heytap.mydevices"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "force_enable_feiniu_cloud_nas_option",
            title = c.getString(R.string.force_enable_feiniu_cloud_nas_option),
            summary = arraySummaryLine(
                *arrayOf(
                    "com.heytap.mydevices",
                    "com.heytap.accessory",
                    "com.android.systemui",
                    "com.coloros.gallery3d"
                ).map { s ->
                    val label = AppUtils(c).getAppLabel(s)
                    val stat = AppUtils(c).getAppMeta(s, "support_fn_nas", "null")
                    "$label: $stat"
                }.toTypedArray()
            ),
        )
    }
}
