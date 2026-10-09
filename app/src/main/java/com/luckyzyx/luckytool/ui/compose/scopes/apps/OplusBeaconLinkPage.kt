package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 一碰互联页（旧 ui.fragment.scopes.apps.OplusBeaconLink 的 Compose 等价物）。
 */
object OplusBeaconLinkPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_beacon_link",
        prefsName = ModulePrefs,
        packName = "com.oplus.beaconlink",
        scopes = arrayOf("com.oplus.beaconlink"),
        restartEnabled = true,
        isVisible = { getOSVersionCode >= 33 && checkPackName("com.oplus.beaconlink") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 33) {
            switch(
                key = "remove_beacon_link_time_limit",
                title = c.getString(R.string.remove_beacon_link_time_limit),
            )
        }
    }
}
