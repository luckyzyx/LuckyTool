package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveStartRecordingOrCastingDialog : YukiBaseHooker() {
    override fun onHook() {
        //Source MediaProjectionServiceHelper
        "com.android.systemui.mediaprojection.MediaProjectionServiceHelper".toClass().resolve()
            .apply {
                firstMethod {
                    name = "hasProjectionPermission"
                    parameters(Int::class, String::class)
                    returnType = Boolean::class
                }.hook {
                    intercept(true)
                }
            }
    }
}