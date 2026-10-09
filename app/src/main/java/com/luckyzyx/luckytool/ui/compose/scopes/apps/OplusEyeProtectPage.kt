package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 护眼页（旧 ui.fragment.scopes.apps.OplusEyeProtect 的 Compose 等价物）。
 */
object OplusEyeProtectPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_eye_protect",
        prefsName = ModulePrefs,
        packName = "com.oplus.eyeprotect",
        scopes = arrayOf("com.oplus.eyeprotect"),
        restartEnabled = true,
        isVisible = { getOSVersionCode >= 33 && checkPackName("com.oplus.eyeprotect") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 33) {
            switch(
                key = "enable_eyeprotect_paper_texture_support",
                title = c.getString(R.string.enable_eyeprotect_paper_texture_support),
                summary = c.getString(R.string.need_restart_system),
            )
        }
    }
}
