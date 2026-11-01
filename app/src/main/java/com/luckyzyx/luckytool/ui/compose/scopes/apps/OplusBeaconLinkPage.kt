package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 一碰互联页：解除一碰互联时长限制的开关。
 * 仅在 osCode >= 33 且装有 com.oplus.beaconlink 时出现（见 spec.isVisible），开关本身也在同一条件下渲染。
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
