package com.luckyzyx.luckytool.hook.scopes.games

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class CustomMediaPlayerSupport(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val set = preferences(ModulePrefs).getStringSet("custom_media_player_support_list", ArraySet())
        if (set.isEmpty()) return

        //Source MediaSessionHelper
        //V10.43+ 本体类名已被混淆（com.oplus.games.musicplayer.main.c），靠类名硬编码下次更新就会失效
        //这里改用类里必然存在的两个字符串定位：配置键 media_controller_config + 日志里的 isOpenedNotification
        val configClassList = dexKitBridge.findClass {
            matcher { usingStrings("media_controller_config") }
        }.map { it.name }
        val tagClassList = dexKitBridge.findClass {
            matcher { usingStrings("isOpenedNotification") }
        }.map { it.name }
        val mediaSessionHelperList = configClassList.filter { tagClassList.contains(it) }
        val clazz = if (mediaSessionHelperList.size == 1) {
            mediaSessionHelperList.first().toClass()
        } else {
            YLog.error("CustomMediaPlayerSupport -> $mediaSessionHelperList")
            //DexKit 没命中时回落到历史类名
            VariousClass(
                "business.module.media.MediaSessionHelper", //V8 V9
                "com.oplus.games.musicplayer.main.MediaSessionHelper", //V10
                "com.oplus.games.musicplayer.main.c" //V10.43+
            ).toClass()
        }
        clazz.resolve().apply {
            firstMethod {
                emptyParameters()
                returnType = List::class
            }.hook {
                after {
                    val list = result<List<String>>() ?: return@after
                    result = list.toMutableList().apply {
                        addAll(set)
                    }
                }
            }
        }
    }
}
