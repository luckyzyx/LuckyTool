package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.oshare.RemoveOShareCloseCountDown
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookOShare : YukiBaseHooker() {
    override fun onHook() {

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->

            //移除互传关闭倒计时
            if (preferences(ModulePrefs).getBoolean("remove_oshare_close_countdown", false)) {
                loadHooker(RemoveOShareCloseCountDown(dexKitBridge))
            }

        }

    }
}