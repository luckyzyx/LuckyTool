package com.luckyzyx.luckytool.hook.scopes.smartsidebar

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookFeatureOption : YukiBaseHooker() {
    override fun onHook() {
        val transferDock = preferences(ModulePrefs).getBoolean("unlock_transfer_dock", false)
        val recentFiles = preferences(ModulePrefs).getBoolean("unlock_recent_files", false)
        val fluidCloud = preferences(ModulePrefs).getBoolean("unlock_fluid_cloud", false)

        //Source EdgePanelFeatureOption
        "com.coloros.edgepanel.utils.EdgePanelFeatureOption".toClass().resolve().apply {
            firstMethod { name = "loadFeatureOption" }.hook {
                after {
                    if (recentFiles) firstField { name = "IS_SHIELD_FILE_BAG" }.set(false)
                    if (fluidCloud) firstField { name = "IS_SHIELD_FLUID_CLOUD" }.set(false)
                    if (transferDock) firstField { name = "IS_SHIELD_TRANSFER_DOCK" }.set(false)
                }
            }
        }
    }
}