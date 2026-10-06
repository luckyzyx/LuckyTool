package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.notificationmanager.ForceDisplayClockStyleOptionsV14
import com.luckyzyx.luckytool.hook.scopes.notificationmanager.RemoveNotificationManagerLimit
import com.luckyzyx.luckytool.hook.scopes.notificationmanager.RemoveNotificationPinNumberLimit
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookNotificationManager : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //移除通知管理限制
        if (preferences(ModulePrefs).getBoolean("remove_notification_manager_limit", false)) {
            loadHooker(RemoveNotificationManagerLimit)
        }
        //移除通知置顶数量限制
        if (preferences(ModulePrefs).getBoolean("remove_notification_pin_number_limit", false)) {
            if (osCode >= 33) loadHooker(RemoveNotificationPinNumberLimit)
        }
        //强制显示时钟样式选项
        if (preferences(ModulePrefs).getBoolean("force_display_clock_style_options", false)) {
            if (SDK == A14) loadHooker(ForceDisplayClockStyleOptionsV14)
        }
    }
}