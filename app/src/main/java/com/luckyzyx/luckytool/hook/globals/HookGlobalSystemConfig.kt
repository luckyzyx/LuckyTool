package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalSystemConfig : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        val list = ArrayList<String>().apply {
            //Source OplusRateLimitClassfier AppTypeClassfier initFeature
            //Source CredentialManagerService
            //Source OplusClearDataProtectManager interceptClearUserDataIfNeeded
            //Source PackageManagerServiceExtImpl shouldRemoveUpdatedMainlineApk
            if (preferences(ModulePrefs).getBoolean("remove_gms_usage_restrictions", false)) {
                add("cn.google.services")
                add("com.google.android.feature.services_updater")
            }
        }
        loadHooker(SystemConfigFeature(list))
    }

    @Obfuscate
    class SystemConfigFeature(private val features: ArrayList<String>) : YukiBaseHooker() {
        override fun onHook() {
            //Source SystemConfig
            "com.android.server.SystemConfig".toClass().resolve().apply {
                firstMethod {
                    name = "getAvailableFeatures"
                    returnType = ArrayMap::class
                }.hook {
                    after {
                        val map = result<ArrayMap<String, Any>>() ?: return@after
                        map.removeAll(features)
                    }
                }
            }
        }
    }
}