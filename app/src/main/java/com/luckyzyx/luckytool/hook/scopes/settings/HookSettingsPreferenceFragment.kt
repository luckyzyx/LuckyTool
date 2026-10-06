package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookSettingsPreferenceFragment : YukiBaseHooker() {
    override fun onHook() {
        //启用应用专属媒体音量
        val specificMediaVolume =
            preferences(ModulePrefs).getBoolean("enable_app_specific_media_volume", false)

        //Source SettingsPreferenceFragment
        "com.android.settings.SettingsPreferenceFragment".toClass().resolve().apply {
            firstMethod {
                name = "removePreference"
                parameters(String::class)
            }.hook {
                before {
                    when (firstArg().get<String>() ?: "") {
                        "voice_mode_category" -> if (specificMediaVolume) result = true
                    }
                }
            }
        }
    }
}