package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.themestore.UnlockThemeStoreVip
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookThemeStore : YukiBaseHooker() {
    override fun onHook() {
        //解锁主题商店VIP
        if (preferences(ModulePrefs).getBoolean("unlock_themestore_vip", false)) {
            loadHooker(UnlockThemeStoreVip)
        }
    }
}