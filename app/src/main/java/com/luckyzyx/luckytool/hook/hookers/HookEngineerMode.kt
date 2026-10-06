package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.engineermode.UnlockSomeHiddenOptions
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookEngineerMode : YukiBaseHooker() {
    override fun onHook() {
        //解锁部分隐藏选项
        if (preferences(ModulePrefs).getBoolean("unlock_some_hidden_options",false)) {
            loadHooker(UnlockSomeHiddenOptions)
        }
    }
}