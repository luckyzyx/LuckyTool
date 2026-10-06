package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 旧 ui.fragment.scopes.apps.OplusSpeechAssist 的 Compose 等价物（机械翻译 loadPreferences）。
 */
object OplusSpeechAssistPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_speech_assist",
        prefsName = ModulePrefs,
        packName = "com.heytap.speechassist",
        scopes = arrayOf("com.heytap.speechassist"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        if (getOSVersionCode >= 30) {
            switch(
                key = "force_enable_ai_speechassist_call",
                title = c.getString(R.string.force_enable_ai_speechassist_call),
            )
        }
    }
}
