package com.luckyzyx.luckytool.hook.scopes.android

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object MultiAppConfig : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        loadHooker(MultiAppAllowList(osCode))

        if (preferences(ModulePrefs).getBoolean("remove_multi_app_blacklist", false)) {
            if (osCode >= 31) loadHooker(MultiAppBlackList)
        }
    }

    @Obfuscate
    class MultiAppAllowList(val osCode: Int) : YukiBaseHooker() {

        var mode = "0"
        val list = ArrayList<String>()
        var limitUser = false
        var limitApp = false

        private fun loadData() {
            mode = preferences(ModulePrefs).getString("set_multi_app_support_mode", "0")
            dataChannel.wait<String>("set_multi_app_support_mode") {
                mode = it
                YLog.debug("update multi app configs status -> $it")
            }

            list.clear()
            list.addAll(preferences(ModulePrefs).getStringSet("multi_app_custom_list", ArraySet()))
            dataChannel.wait("multi_app_custom_list") {
                val new = preferences(ModulePrefs).getStringSet("multi_app_custom_list", ArraySet())
                YLog.debug("update multi app whitelist configs -> ${list.size} | ${new.size}")
                list.clear()
                list.addAll(new)
            }
            limitUser =
                preferences(ModulePrefs).getBoolean("remove_multi_app_created_num_limit_for_users", false)
            limitApp =
                preferences(ModulePrefs).getBoolean("remove_multi_app_created_num_limit_for_users", false)
            YLog.debug("init multi app configs success -> ${list.size}")
        }

        override fun onHook() {
            loadData()

            //Source OplusMultiAppConfig
            "com.oplus.multiapp.OplusMultiAppConfig".toClass().resolve().apply {
                firstMethod { name = "getAllowedPkgList" }.hook {
                    before {
                        if (mode != "1" || list.isEmpty()) return@before
                        result = java.util.ArrayList(list)
                    }
                }
                if (osCode >= 38) {
                    firstMethod { name = "getMaxCloneUserNum" }.hook {
                        if (limitUser) {
                            intercept(10)
                        }
                    }
                }
                if (osCode >= 31) {
                    firstMethod { name = "getMaxCreatedNum" }.hook {
                        if (limitApp) {
                            intercept(1000)
                        }
                    }
                }
            }

//            if (osCode < 38) return

            //Source OplusMultiAppManagerService
//            "com.android.server.am.OplusMultiAppManagerService".toClass().resolve().apply {
//                firstMethod {
//                    name = "isValidMultiAppUserId"
//                    parameters(Int::class)
//                    returnType = Boolean::class
//                }.hook {
//                    before {
//                        val userId = firstArg().get<Int>() ?: 0
//                        val maxNum = firstMethod { name = "getMaxCloneUserNum" }.of(instance)
//                            .invoke<Int>() ?: return@before
//                        result = if (userId !in 970..999) false
//                        else maxNum == 0 || 999 - userId < maxNum
//                    }
//                }
//            }

            //Source UserManagerService
//            "com.android.server.pm.UserManagerService".toClass().resolve().apply {
//                firstMethod { name = "isUserLimitReached" }.hook {
//                    replaceToFalse()
//                }
//                firstMethod { name = "canAddMoreManagedProfiles" }.hook {
//                    replaceToTrue()
//                }
//                firstMethod { name = "canAddMoreProfilesToUser" }.hook {
//                    replaceToTrue()
//                }
//                if (osCode >= 30) {
//                    firstMethod { name = "isCreationOverrideEnabled" }.hook {
//                        replaceToTrue()
//                    }
//                }
//            }
        }
    }

    @Obfuscate
    object MultiAppBlackList : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusMultiAppDataManager
            "com.android.server.pm.OplusMultiAppDataManager".toClass().resolve().apply {
                firstMethod { name = "initBlackAppList" }.hook {
                    intercept()
                }
            }
        }
    }
}