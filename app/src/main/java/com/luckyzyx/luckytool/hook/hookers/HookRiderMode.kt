package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.ridermode.RiderModeAppListWhitelist
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookRiderMode : YukiBaseHooker() {

    override fun onHook() {
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //移除高能户外模式App列表白名单
            if (preferences(ModulePrefs).getBoolean("remove_rider_mode_whitelist", false)) {
                loadHooker(RiderModeAppListWhitelist(dexKitBridge))
            }
        }
    }
}
