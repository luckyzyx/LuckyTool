package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine

/**
 * 旧 ui.fragment.scopes.apps.OplusMyDevices 的 Compose 等价物（机械翻译 loadPreferences）。
 * 键、默认值、可见性与旧页逐项一致。
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
