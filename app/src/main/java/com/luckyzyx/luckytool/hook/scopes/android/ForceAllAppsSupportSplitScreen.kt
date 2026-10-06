package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceAllAppsSupportSplitScreen : YukiBaseHooker() {
    override fun onHook() {
        var isEnable =
            preferences(ModulePrefs).getBoolean("force_all_apps_support_split_screen", false)
        dataChannel.wait<Boolean>("force_all_apps_support_split_screen") { isEnable = it }

        //Source OplusSplitScreenManagerService
        "com.android.server.wm.OplusSplitScreenManagerService".toClass().resolve().apply {
            method {
                name = "supportsSplitScreenByVendorPolicy"
                parameters { it[0] == classOf<String>() && it[1] == classOf<String>() }
                parameterCount { it in 3..4 }
            }.hookAll {
                before {
                    if (!isEnable) return@before
                    val packageName = firstArg().get<String>() ?: ""
                    val activityName = arg(1).get<String>() ?: ""
//                    val candidate = arg(2).get<Boolean>() ?: false

                    if (packageName.isBlank()) return@before

                    val isSafeSenterUI = firstMethod {
                        name = "isSafeSenterUI"
                        parameterCount = 1
                    }.of(instance).invoke<Boolean>(activityName) ?: false
                    if (isSafeSenterUI) return@before

                    if (method.parameterCount == 4) {
                        val userId = lastArg().get<Int>() ?: 0
                        val isHidenPackage = firstMethod {
                            name = "isHidenPackage"
                            parameterCount = 2
                        }.of(instance).invoke<Boolean>(packageName, userId) ?: false
                        if (isHidenPackage) return@before
                    }

                    result = true
                }
            }
            firstMethod { name = "isInForbidActivityList" }.hook {
                if (isEnable) {
                    intercept(false)
                }
            }
            firstMethod { name = "supportsSplitScreenWindowingMode" }.hook {
                if (isEnable) {
                    intercept(true)
                }
            }
        }
    }
}