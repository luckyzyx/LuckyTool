package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.arraySummaryLine
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isZh

/**
 * 旧 ui.fragment.scopes.apps.OplusSoundRecorder 的 Compose 等价物（机械翻译 loadPreferences）。
 * 两个开关均有 isEnabled=checkPackName(...) 条件，按规则译为 enabled 参数。
 */
object OplusSoundRecorderPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_sound_recorder",
        prefsName = ModulePrefs,
        packName = "com.coloros.soundrecorder",
        scopes = arrayOf(
            "com.coloros.soundrecorder",
            "com.oplus.audiomonitor",
            "com.oplus.atlas",
            "com.oplus.audio.effectcenter"
        ),
        restartEnabled = false,
        isVisible = { getOSVersionCode >= 30 && checkPackName("com.coloros.soundrecorder") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        if (osCode == 30 && isZh(c)) {
            switch(
                key = "enable_record_calls_on_third_party_apps",
                title = c.getString(R.string.enable_record_calls_on_third_party_apps),
                summary = arraySummaryLine(
                    c.getString(R.string.need_restart_system),
                    c.getString(R.string.enable_record_calls_on_third_party_apps_tips),
                    c.getString(R.string.enable_record_calls_on_third_party_apps_tips_2),
                ),
                enabled = c.checkPackName("com.oplus.audiomonitor") && c.checkPackName("com.oplus.atlas"),
            )
        }
        if (osCode in 31..33 && isZh(c)) {
            switch(
                key = "expand_voip_recorder_whitelist",
                title = c.getString(R.string.expand_voip_recorder_whitelist),
                summary = "企业微信,TIM,飞书,抖音",
                enabled = c.checkPackName("com.oplus.audiomonitor"),
            )
        }
    }
}
