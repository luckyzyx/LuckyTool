package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookLauncherFeatureFlags : YukiBaseHooker() {
    override fun onHook() {
        val twoLine = preferences(ModulePrefs).getBoolean("enable_drawer_layout_double_line_names", false)

        //Source FeatureFlags
        "com.android.launcher3.config.FeatureFlags".toClass().resolve().apply {
            firstMethod {
                name = "getDebugFlag"
                parameterCount = 3
            }.hook {
                before {
                    val key = firstArg().get<String>() ?: ""
//                    val defValue = arg(1).get<Boolean>() ?: false
//                    val description = lastArg().get<String>() ?: ""
                    when (key) {
                        "ENABLE_TWOLINE_ALLAPPS" -> if (twoLine) arg(1).set(true)
                    }
                }
            }
        }
    }
}