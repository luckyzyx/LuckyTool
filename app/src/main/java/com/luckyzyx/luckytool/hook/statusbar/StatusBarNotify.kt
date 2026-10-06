package com.luckyzyx.luckytool.hook.statusbar

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.DisableHighVolumeWarningNotifications
import com.luckyzyx.luckytool.hook.scopes.systemui.EnableGlobalNotificationSimpleBannerMode
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveChargingCompleted
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveDanmakuNotificationWhitelist
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveDoNotDisturbModeNotification
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveFlashlightOpenNotification
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveGTModeNotification
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveNotificationCleanupButton
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveNotificationForMuteNotifications
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveSmallWindowReplyWhitelist
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object StatusBarNotify : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //移除充电完成通知
        if (preferences(ModulePrefs).getBoolean("remove_charging_completed", false)) {
            loadHooker(RemoveChargingCompleted)
        }
        //移除手电筒已开启通知
        if (preferences(ModulePrefs).getBoolean("remove_flashlight_open_notification", false)) {
            loadHooker(RemoveFlashlightOpenNotification)
        }
        //移除免打扰模式通知
        if (preferences(ModulePrefs).getBoolean("remove_do_not_disturb_mode_notification", false)) {
            loadHooker(RemoveDoNotDisturbModeNotification)
        }
        //移除通知勿扰通知
        if (preferences(ModulePrefs).getBoolean("remove_notifications_for_mute_notifications", false)) {
            loadHooker(RemoveNotificationForMuteNotifications)
        }
        //移除GT模式通知
        if (preferences(ModulePrefs).getBoolean("remove_gt_mode_notification", false)) {
            if (osCode < 40) loadHooker(RemoveGTModeNotification)
        }
        //浮窗回复白名单
        if (preferences(ModulePrefs).getBoolean("remove_small_window_reply_whitelist", false)) {
            if (osCode < 34) loadHooker(RemoveSmallWindowReplyWhitelist)
        }
        //弹幕通知白名单
        if (preferences(ModulePrefs).getBoolean("remove_danmaku_notification_whitelist", false)) {
            if (SDK < A14) loadHooker(RemoveDanmakuNotificationWhitelist)
        }
        //移除通知清理按钮
        if (preferences(ModulePrefs).getBoolean("remove_notification_cleanup_button", false)) {
            loadHooker(RemoveNotificationCleanupButton)
        }
        //禁用高音量警告通知
        if (preferences(ModulePrefs).getBoolean("disable_high_volume_warning_notifications", false)) {
            loadHooker(DisableHighVolumeWarningNotifications)
        }
        //启用全局通知横幅简洁模式
        if (preferences(ModulePrefs).getBoolean("enable_global_notification_simple_banner_mode", false)) {
            loadHooker(EnableGlobalNotificationSimpleBannerMode)
        }
    }
}