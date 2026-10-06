package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveAppUpdateGreenDot : YukiBaseHooker() {
    override fun onHook() {
        val mode = preferences(ModulePrefs).getString("set_app_update_dot_display_mode", "0")
        if (mode != "2") return

        //Source BubbleTextView
        "com.android.launcher3.BubbleTextView".toClass().resolve().apply {
            firstMethod { name = "isShouldShowGreenDot" }.hook {
                intercept(false)
            }
        }
    }
}