package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.FullScreenGestureSideSlideBar
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveBackGestureConfirmationLimit
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveRotateScreenButton
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSystemUIGesture : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //全面屏手势侧滑条
        loadHooker(FullScreenGestureSideSlideBar)

        //移除旋转屏幕按钮
        if (preferences(ModulePrefs).getBoolean("remove_rotate_screen_button", false)) {
            loadHooker(RemoveRotateScreenButton)
        }
        //移除返回手势确认限制
        if (preferences(ModulePrefs).getBoolean("remove_back_gesture_confirmation_limit", false)) {
            if (osCode in 35..36) loadHooker(RemoveBackGestureConfirmationLimit)
        }
    }
}