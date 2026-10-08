package com.luckyzyx.luckytool.hook.scopes.systemui

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.legacyStringSet
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomMusicFluidCloudWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val prefs = preferences(ModulePrefs)
        val disabled = prefs.getBoolean("disable_music_fluid_cloud_display", false)
        // 集合键的历史数据可能是字符串形态（备份恢复会把 Set 写成 `{a, b}`）：直接
        // getStringSet 会抛 ClassCastException，这里回落解析同一份数据，别让宿主进程崩掉
        val set: Set<String> = runCatching {
            prefs.getStringSet("set_custom_music_fluid_cloud_whitelist", ArraySet()).toSet()
        }.getOrElse {
            runCatching {
                legacyStringSet(prefs.getString("set_custom_music_fluid_cloud_whitelist", ""))
            }.getOrDefault(emptySet())
        }

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