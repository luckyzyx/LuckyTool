package com.luckyzyx.luckytool.hook.scopes.systemui

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomMusicFluidCloudWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val disabled = preferences(ModulePrefs).getBoolean("disable_music_fluid_cloud_display", false)
        val set =
            preferences(ModulePrefs).getStringSet("set_custom_music_fluid_cloud_whitelist", ArraySet())

        //Source OplusMediaRusUpdateManager
        "com.oplus.systemui.media.seedling.rus.OplusMediaRusUpdateManager".toClass().resolve()
            .apply {
                (firstMethodOrNull {
                    name = "getRusWhiteList"
                    returnType = List::class
                } ?: firstMethod {
                    name = "parsePackageListFromStringSet"
                    parameters(Set::class)
                    returnType = List::class
                }).hook {
                    after {
                        val originalList = result<java.util.ArrayList<String>>() ?: return@after
                        if (disabled) {
                            originalList.clear()
                        } else if (set.isNotEmpty()) {
                            val finalList = LinkedHashSet<String>().apply {
                                addAll(originalList)
                                addAll(set)
                            }
                            originalList.clear()
                            originalList.addAll(finalList)
                        }
                    }
                }
            }
    }
}