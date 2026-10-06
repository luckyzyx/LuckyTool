package com.luckyzyx.luckytool.hook.scopes.android

import android.media.AudioAttributes
import android.media.AudioManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableAudioFocus : YukiBaseHooker() {
    override fun onHook() {
        //Source MediaFocusControl
        "com.android.server.audio.MediaFocusControl".toClass().resolve().apply {
            firstMethod {
                name = "requestAudioFocus"
                returnType = Int::class
            }.hook {
                before {
                    val audioAttributes = firstArg().get<AudioAttributes>() ?: return@before
                    if (audioAttributes.usage != AudioAttributes.USAGE_VOICE_COMMUNICATION) {
                        result = AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    }
                }
            }
        }
    }
}