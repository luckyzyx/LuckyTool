package com.luckyzyx.luckytool.hook.statusbar

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.BluetoothIconRelated
import com.luckyzyx.luckytool.hook.scopes.systemui.CustomFluidCloudIconBackgroundTransparency
import com.luckyzyx.luckytool.hook.scopes.systemui.HideInActiveSignalLabelsGen2x2
import com.luckyzyx.luckytool.hook.scopes.systemui.MobileDataIconRelated
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveGreenDotPrivacyPrompt
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveHighPerformanceModeIcon
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveStatusBarSecurePayment
import com.luckyzyx.luckytool.hook.scopes.systemui.RemoveSystemPromptIcon
import com.luckyzyx.luckytool.hook.scopes.systemui.StatusBarIconVerticalCenter
import com.luckyzyx.luckytool.hook.scopes.systemui.WiFiDataIconRelated
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object StatusBarIcon : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //WiFi图标相关
        loadHooker(WiFiDataIconRelated)
        //移动数据图标相关
        loadHooker(MobileDataIconRelated)
        //未连接蓝牙时隐藏图标
        loadHooker(BluetoothIconRelated)

        //移除状态栏支付保护图标
        if (preferences(ModulePrefs).getBoolean("remove_statusbar_securepayment_icon", false)) {
            loadHooker(RemoveStatusBarSecurePayment)
        }
        //移除高性能模式图标
        if (preferences(ModulePrefs).getBoolean("remove_high_performance_mode_icon", false)) {
            loadHooker(RemoveHighPerformanceModeIcon)
        }
        //移除绿点隐私提示
        if (preferences(ModulePrefs).getBoolean("remove_green_dot_privacy_prompt", false)) {
            loadHooker(RemoveGreenDotPrivacyPrompt)
        }
        //移除系统提示图标
        if (preferences(ModulePrefs).getBoolean("remove_system_prompt_icon", false)) {
            loadHooker(RemoveSystemPromptIcon)
        }
        //状态栏图标垂直居中
        if (preferences(ModulePrefs).getBoolean("status_bar_icon_vertical_center", false)) {
            if (SDK <= A13) loadHooker(StatusBarIconVerticalCenter)
        }
        //隐藏未使用信号标签
        if (preferences(ModulePrefs).getBoolean("hide_inactive_signal_labels_gen2x2", false)) {
            loadHooker(HideInActiveSignalLabelsGen2x2)
        }
        //自定义流体云图标背景透明度
        if (osCode in 30..33) loadHooker(CustomFluidCloudIconBackgroundTransparency)

    }
}