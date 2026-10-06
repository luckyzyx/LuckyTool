package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.AutoWakeUpFaceUnlockNotification
import com.luckyzyx.luckytool.hook.scopes.systemui.ForceEnableScreenOffMusicSupport
import com.luckyzyx.luckytool.hook.scopes.systemui.HideLockScreenStatusBarDisplay
import com.luckyzyx.luckytool.hook.scopes.systemui.HidePanoramicAodStatusBar
import com.luckyzyx.luckytool.hook.scopes.systemui.LockScreenBottomButton
import com.luckyzyx.luckytool.hook.scopes.systemui.LockScreenCarriers
import com.luckyzyx.luckytool.hook.scopes.systemui.LockScreenChargingComponent
import com.luckyzyx.luckytool.hook.scopes.systemui.LockScreenClock
import com.luckyzyx.luckytool.hook.scopes.systemui.LockScreenComponentStyle
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveAodMusicWhitelist
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveLockScreenBottomSOSButton
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveLockScreenCloseNotificationButton
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveTopLockScreenIcon
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSystemUILockScreen : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //锁屏时钟
        loadHooker(LockScreenClock)

        //锁屏时钟组件样式
        loadHooker(LockScreenComponentStyle)

        //锁屏充电组件
        loadHooker(LockScreenChargingComponent)

        //锁屏底部按钮
        loadHooker(LockScreenBottomButton)

        //锁屏状态栏运营商
        loadHooker(LockScreenCarriers)

        //隐藏锁屏状态栏显示
        if (preferences(ModulePrefs).getBoolean("hide_lock_screen_status_bar_display", false)) {
            loadHooker(HideLockScreenStatusBarDisplay)
        }
        //隐藏全景息屏状态栏
        if (preferences(ModulePrefs).getBoolean("hide_panoramic_aod_status_bar", false)) {
            if (osCode >= 37) loadHooker(HidePanoramicAodStatusBar)
        }
        //移除SOS紧急联络按钮
        if (preferences(ModulePrefs).getBoolean("remove_lock_screen_bottom_sos_button", false)) {
            if (SDK >= A13) loadHooker(RemoveLockScreenBottomSOSButton)
        }
        //移除锁屏顶部图标
        if (preferences(ModulePrefs).getBoolean("remove_top_lock_screen_icon", false)) {
            loadHooker(RemoveTopLockScreenIcon)
        }
        //移除锁屏关闭通知按钮
        if (preferences(ModulePrefs).getBoolean("remove_lock_screen_close_notification_button", false)) {
            if (osCode < 33) loadHooker(RemoveLockScreenCloseNotificationButton)
        }
        //移除息屏音乐白名单
        if (preferences(ModulePrefs).getBoolean("remove_aod_music_whitelist", false)) {
            if (SDK >= A13) loadHooker(RemoveAodMusicWhitelist)
        }
        //强制启用息屏音乐支持
        if (preferences(ModulePrefs).getBoolean("force_enable_screen_off_music_support", false)) {
            if (osCode in 26..33) loadHooker(ForceEnableScreenOffMusicSupport)
        }
        //通知自动唤醒面部解锁
        if (preferences(ModulePrefs).getBoolean("auto_wake_up_face_unlock_notification", false)) {
            loadHooker(AutoWakeUpFaceUnlockNotification)
        }
    }
}