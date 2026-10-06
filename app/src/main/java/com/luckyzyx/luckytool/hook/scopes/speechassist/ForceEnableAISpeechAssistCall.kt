package com.luckyzyx.luckytool.hook.scopes.speechassist

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableAISpeechAssistCall : YukiBaseHooker() {
    override fun onHook() {
        //Source AiCallCommonBean
        "com.heytap.speechassist.aicall.setting.config.AiCallCommonBean".toClass().resolve().apply {
            firstMethod { name = "getSupportAiCall" }.hook {
                intercept(true)
            }
            firstMethod { name = "getSupportAiCallV2" }.hook {
                intercept(true)
            }
        }
    }
}