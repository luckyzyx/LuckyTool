package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.hook.scopes.claw.RemoveRootDetection
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookClaw : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookGlobalSystemProperties)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->

            //移除Root检测
            if (preferences(ModulePrefs).getBoolean("remove_root_detection", false)) {
                loadHooker(RemoveRootDetection(dexKitBridge))
            }

        }

    }
}
