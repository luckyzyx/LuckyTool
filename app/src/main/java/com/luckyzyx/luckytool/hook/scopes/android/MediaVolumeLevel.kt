package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object MediaVolumeLevel : YukiBaseHooker() {
    override fun onHook() {
        val mediaVolumeLevel = preferences(ModulePrefs).getInt("media_volume_level", 0)
        val minVolumeZero = preferences(ModulePrefs).getBoolean("minimum_volume_level_can_be_zero", false)

        //Source AudioServiceExtImpl
        "com.android.server.audio.AudioServiceExtImpl".toClass().resolve().apply {
            firstMethod { name = "resetSystemVolume" }.hook {
                after {
                    if (mediaVolumeLevel != 0) {
                        val maxField = if (SDK >= A13) "mMaxStreamVolume" else "MAX_STREAM_VOLUME"
                        val maxArray = firstField { name = maxField }.of(instance).get<IntArray>()
                        maxArray?.set(3, mediaVolumeLevel)
                    }

                    if (minVolumeZero) {
                        val minField = if (SDK >= A13) "mMinStreamVolume" else "MIN_STREAM_VOLUME"
                        val minArray = firstField { name = minField }.of(instance).get<IntArray>()
                        minArray?.forEachIndexed { index, i ->
                            if (i > 0) minArray[index] = 0
                        }
                    }
                }
            }
        }
    }
}