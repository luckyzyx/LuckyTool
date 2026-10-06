package com.luckyzyx.luckytool.hook.hookers

import android.content.Context
import android.media.MediaMetadata
import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.systemui.HookSystemUIFeature
import com.luckyzyx.luckytool.utils.DexkitUtils
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
object HookSystemUI : YukiBaseHooker() {
    override fun onHook() {

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //系统界面Feature
            loadHooker(HookSystemUIFeature(dexKitBridge))

            //状态栏功能
            loadHooker(HookSystemUIStatusBar(dexKitBridge))
        }

        //锁屏
        loadHooker(HookSystemUILockScreen)

        //对话框相关
        loadHooker(HookSystemUIDialog)

        //全面屏手势相关
        loadHooker(HookSystemUIGesture)

        //指纹相关
        loadHooker(HookSystemUIFingerPrint)

        //杂项
        loadHooker(HookSystemUiMiscellaneous)

        //自启
        loadHooker(HookSystemUIAutoStart)

        //Source MediaActionPrioritySelectorImpl
        if (false) "com.oplus.systemui.media.controls.pipeline.MediaActionPrioritySelectorImpl".toClass()
            .resolve().apply {
                firstMethod {
                    name = "getLyricEntrance"
                    parameters(String::class)
                    returnType = Int::class
                }.hook {
                    after {
                        val res = result<Int>() ?: return@after
                        if (res != 0) return@after
                        result = invokeOriginal("com.heytap.music")
                    }
                }
            }

        //METADATA_OPLUS_LYRIC_INFO_KEY -> lyricInfo

        val DIR = "/sdcard/Musics/"

        //Source OplusMediaDataManagerExImpl
        if (false) "com.oplus.systemui.media.controls.pipeline.OplusMediaDataManagerExImpl".toClass()
            .resolve().apply {
                firstMethod { name = "loadLyricInBg" }.hook {
                    before {
                        val context = firstField { type = Context::class }.of(instance)
                            .get<Context>() ?: return@before
                        val metaData = arg(1).get<MediaMetadata>() ?: return@before
                        val lyricInfo = metaData.getString("lyricInfo")
                        if (lyricInfo != null) return@before

                        val bundle = metaData.asResolver().firstField { type = Bundle::class }
                            .get<Bundle>() ?: return@before

                        val dir = File(DIR)
                        if (dir.exists() && dir.isDirectory) {

                        }
                    }
                }
            }
    }
}