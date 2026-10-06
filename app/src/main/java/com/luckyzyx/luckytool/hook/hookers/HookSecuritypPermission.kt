package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.securitypermission.AppStartDialogUseOldVersion
import com.luckyzyx.luckytool.hook.scopes.securitypermission.AutoUnlockAppEcmPermissionRestrict
import com.luckyzyx.luckytool.hook.scopes.securitypermission.EnableAlwaysAllowAppStartDialog
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSecuritypPermission : YukiBaseHooker() {
    override fun onHook() {

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //启用始终允许启动应用对话框
            if (preferences(ModulePrefs).getBoolean("enable_always_allow_app_start_dialog", false)) {
                loadHooker(EnableAlwaysAllowAppStartDialog(dexKitBridge))
            }
        }

        //使用旧版跳转应用对话框
        if (preferences(ModulePrefs).getBoolean("app_start_dialog_use_old_version", false)) {
            loadHooker(AppStartDialogUseOldVersion)
        }
        //自动解锁应用权限管理授权限制
        if (preferences(ModulePrefs).getBoolean("auto_unlock_app_ecm_permission_restrict", false)) {
            loadHooker(AutoUnlockAppEcmPermissionRestrict)
        }
    }
}