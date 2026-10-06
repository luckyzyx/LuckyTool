package com.luckyzyx.luckytool.hook.statusbar

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.AllowLongPressNotificationModifiable
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object StatusBarNotifiyLimit : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //允许长按通知可修改
        if (preferences(ModulePrefs).getBoolean("allow_long_press_notification_modifiable", false)) {
            if (osCode <= 30) loadHooker(AllowLongPressNotificationModifiable)
        }
    }
}