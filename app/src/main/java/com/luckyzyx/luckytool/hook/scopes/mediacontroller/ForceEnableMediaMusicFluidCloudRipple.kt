package com.luckyzyx.luckytool.hook.scopes.mediacontroller

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceEnableMediaMusicFluidCloudRipple : YukiBaseHooker() {
    override fun onHook() {
        var isEnable =
            preferences(ModulePrefs).getBoolean("force_enable_media_music_fluid_cloud_ripple", false)
        dataChannel.wait<Boolean>("force_enable_media_music_fluid_cloud_ripple") { isEnable = it }

        //Source SeedlingTool
        "com.oplus.pantanal.seedling.util.SeedlingTool".toClass().resolve().apply {
            method {
                name { it.startsWith("update") && it.endsWith("Data") }
            }.hookAll {
                before {
                    if (!isEnable) return@before
                    val json = arg(1).get() ?: return@before
                    if (json is JSONObject) {
                        val staticVoicePrintShow = json.optBoolean("staticVoicePrintShow", true)
                        if (staticVoicePrintShow) {
                            json.put("staticVoicePrintShow", false)
                        }
                    }
                }
            }
        }
    }
}