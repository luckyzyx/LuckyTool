package com.luckyzyx.luckytool.hook.hookers

import android.os.SystemProperties
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.hook.scopes.ota.EnableOpexLocalInstall
import com.luckyzyx.luckytool.hook.scopes.ota.HookNotificationHelper
import com.luckyzyx.luckytool.hook.scopes.ota.HookOTADialogHelper
import com.luckyzyx.luckytool.hook.scopes.ota.RemoveOTALocalUpdateVerity
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookOplusOta : YukiBaseHooker() {
    override fun onHook() {
        loadHooker(HookGlobalSystemProperties)

        val osCode = getOSVersionCode


        //local_update_failed_not_match 安装包不匹配
        //local_update_failed_read_exception 读取文件错误
        //local_update_failed_not_exist 文件不存在
        //local_update_low_memory 存储空间不足不能安装

        //local_update_verify_failed 验证失败
        //unzip_file_failed 解压失败

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //HookOTANotification
            loadHooker(HookNotificationHelper(dexKitBridge))
            //HookOTADialog
            loadHooker(HookOTADialogHelper(dexKitBridge))
            //移除OTA本地更新校验
            if (preferences(ModulePrefs).getBoolean("remove_ota_local_update_verity", false)) {
                loadHooker(RemoveOTALocalUpdateVerity(dexKitBridge))
            }
            //启用Opex本地安装
            if (preferences(ModulePrefs).getBoolean("enable_opex_local_install", false)) {
                val opex = SystemProperties.getBoolean("oplus.opex.merge", false)
                if (osCode >= 30 && opex) loadHooker(EnableOpexLocalInstall(dexKitBridge))
            }
        }

    }
}