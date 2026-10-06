package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.scopes.smartsidebar.EnableRunInBackground
import com.luckyzyx.luckytool.hook.scopes.smartsidebar.ForceEnableBuoyAutomaticallyHides
import com.luckyzyx.luckytool.hook.scopes.smartsidebar.HookFeatureOption
import com.luckyzyx.luckytool.hook.utils.getAppVerInfo
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSmartSidebar : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookGlobalFeatureConfig)

        val osCode = getOSVersionCode
        val appVer = preferences(ModulePrefs).getAppVerInfo(packageName)


        val v14 = appVer?.versionCode?.let { it >= 14000000 } ?: false

        //HookFeatureOption
        if (SDK == A13 && v14) loadHooker(HookFeatureOption)

        //强制启用浮标自动隐藏
        if (preferences(ModulePrefs).getBoolean("force_enable_buoy_automatically_hides", false)) {
            if (SDK == A12) loadHooker(ForceEnableBuoyAutomaticallyHides)
        }

        //启用后台挂机
        if (preferences(ModulePrefs).getBoolean("enable_run_in_background", false)) {
            if (osCode >= 27) loadHooker(EnableRunInBackground)
        }
    }
}