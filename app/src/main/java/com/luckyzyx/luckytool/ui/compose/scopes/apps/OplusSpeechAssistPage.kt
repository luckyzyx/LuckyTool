package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 语音助手页：强制开启 AI 语音助手通话的开关，只有 osCode >= 30 才渲染。
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
