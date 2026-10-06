package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.DisableSysUIOTGAutoOff
import com.luckyzyx.luckytool.hook.scopes.systemui.RemovePowerMenuSOSButton
import com.luckyzyx.luckytool.hook.scopes.systemui.ShowChargingRipple
import com.luckyzyx.luckytool.hook.scopes.systemui.ShowManualLockButtonPowerMenu
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSystemUiMiscellaneous : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //充电纹波
        if (preferences(ModulePrefs).getBoolean("show_charging_ripple", false)) {
            if (SDK >= A12) loadHooker(ShowChargingRipple)
        }
        //禁用OTG自动关闭
        if (preferences(ModulePrefs).getBoolean("disable_otg_auto_off", false)) {
            if (osCode < 30) loadHooker(DisableSysUIOTGAutoOff)
        }
        //电源菜单显示手动锁定按钮
        if (preferences(ModulePrefs).getBoolean("show_manual_lock_button_power_menu", false)) {
            if (SDK >= A14) loadHooker(ShowManualLockButtonPowerMenu)
        }
        //移除电源菜单SOS按钮
        if (preferences(ModulePrefs).getBoolean("remove_power_menu_sos_button", false)) {
            if (SDK >= A13) loadHooker(RemovePowerMenuSOSButton)
        }
    }
}