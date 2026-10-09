package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config

object AssetManagerHook : BaseHook() {
    override val name = "AssetManagerHook"

    @SuppressLint("BlockedPrivateApi")
    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val assetManagerClazz = "android.content.res.AssetManager".toClass()

        // Targeting R+ (version " + Build.VERSION_CODES.R + " and above) requires"
        // + " the resources.arsc of installed APKs to be stored uncompressed"
        // + " and aligned on a 4-byte boundary
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/core/java/android/content/res/AssetManager.java;l=828
        // public boolean containsAllocatedTable()
        val containsAllocatedTableMethod =
            assetManagerClazz.getDeclaredMethod("containsAllocatedTable")
        containsAllocatedTableMethod.hook {
            before {
                if (Config.isBypassResourceArscRestrictionsEnabled()) {
                    result = false
                }
            }
        }
    }
}
