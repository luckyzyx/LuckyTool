package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import android.media.AudioManager
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableHeadphoneHighVolumeWarning : YukiBaseHooker() {
    override fun onHook() {
        //Sourcce VolumeDialogImplEx
        VariousClass(
            "com.oplusos.systemui.volume.VolumeDialogImplEx", //C13
            "com.oplus.systemui.volume.OplusVolumeDialogImpl" //C14
        ).toClass().resolve().apply {
            firstMethod { name = "init" }.hook {
                after {
                    val mContext = firstField { name = "mContext" }.of(instance).get<Context>()
                        ?: return@after
                    val audioManager = mContext.getSystemService(classOf<AudioManager>())
                        ?: return@after
                    audioManager.asResolver().firstMethod { name = "disableSafeMediaVolume" }.invoke()
                }
            }
        }
    }
}