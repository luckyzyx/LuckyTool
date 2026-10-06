package com.luckyzyx.luckytool.hook.scopes.android

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ScrollToTopWhiteList : YukiBaseHooker() {
    override fun onHook() {
        val mode = preferences(ModulePrefs).getString("set_click_statusbar_scroll_to_top_mode", "0")

        //Source OplusScrollToTopRusHelper -> OplusScrollToTopSystemManager
        "com.android.server.OplusScrollToTopRusHelper".toClass().resolve().apply {
            firstMethodOrNull { name = "isInWhiteList" }?.hook {
                before {
                    when (mode) {
                        "1" -> result = false
                        "2" -> result = true
                    }
                }
            }
        }
    }
}