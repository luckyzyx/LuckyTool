package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.health.RemoveHealthRootCheck
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookHealth : YukiBaseHooker() {
    override fun onHook() {

        //移除Root检测对话框
        if (preferences(ModulePrefs).getBoolean("remove_health_root_check_dialog", false)) {
            loadHooker(RemoveHealthRootCheck)
        }

    }
}