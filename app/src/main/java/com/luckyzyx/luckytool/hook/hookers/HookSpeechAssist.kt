package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.speechassist.ForceEnableAISpeechAssistCall
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSpeechAssist : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //强制启用小布通话
        if (preferences(ModulePrefs).getBoolean("force_enable_ai_speechassist_call", false)) {
            if (osCode >= 30) loadHooker(ForceEnableAISpeechAssistCall)
        }
    }
}