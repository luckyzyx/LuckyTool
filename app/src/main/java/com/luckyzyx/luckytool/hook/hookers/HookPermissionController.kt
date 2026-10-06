package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.scopes.permissioncontroller.RemoveStoragePermissionExceptionDialog
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookPermissionController : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        loadHooker(HookGlobalFeatureConfig)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->

            val storageDialog =
                preferences(ModulePrefs).getBoolean("remove_storage_permission_exception_dialog", false)
            if (osCode < 37 && storageDialog) {
                loadHooker(RemoveStoragePermissionExceptionDialog(dexKitBridge))
            }

        }
    }
}