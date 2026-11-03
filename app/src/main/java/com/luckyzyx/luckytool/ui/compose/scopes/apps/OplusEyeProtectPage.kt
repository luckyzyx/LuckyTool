package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 护眼页：开启纸张纹理效果的开关。
 * 仅在 osCode >= 33 且装有 com.oplus.eyeprotect 时出现（见 spec.isVisible），开关本身也在同一条件下渲染；
 * 改动需要重启系统。
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
