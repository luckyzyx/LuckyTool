package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.beaconlink.RemoveBeaconLinkTimeLimit
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookBeaconLink : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->

            //移除无网畅聊时间限制
            if (preferences(ModulePrefs).getBoolean("remove_beacon_link_time_limit", false)) {
             if (osCode >= 33)   loadHooker(RemoveBeaconLinkTimeLimit(dexKitBridge))
            }

        }

    }
}