package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.keyguardpersonality.KeyGuardPersonalityRedMode
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookKeyguardPersonality : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //锁屏红一
        if (osCode >= 40) loadHooker(KeyGuardPersonalityRedMode)

    }
}
