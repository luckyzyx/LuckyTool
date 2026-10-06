package com.luckyzyx.luckytool.hook.scopes.systemui

import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSmallWindowReplyWhitelist : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 34) loadHooker(SmallWindowReplyWhitelist)
        else loadHooker(SmallWindowReplyWhitelistV14)
    }

    @Obfuscate
    object SmallWindowReplyWhitelist : YukiBaseHooker() {
        override fun onHook() {
            //Source HeadsUpToZoomUtils
            "com.android.systemui.util.HeadsUpToZoomUtils".toClass().resolve().apply {
                firstMethod { name { it.startsWith("isZoom") } }.hook {
                    intercept(true)
                }
            }
        }
    }

    @Obfuscate
    object SmallWindowReplyWhitelistV14 : YukiBaseHooker() {
        override fun onHook() {
            var set: Set<String> =
                preferences(ModulePrefs).getStringSet("set_small_window_reply_blacklist_list", ArraySet())
            dataChannel.wait<Set<String>>("set_small_window_reply_blacklist_list") { set = it }

            //Source BaseNotificationContentInflater / NotificationListenerExtImpl
            VariousClass(
                "com.oplusos.systemui.notification.base.BaseNotificationContentInflater", //C13
                "com.oplus.systemui.statusbar.NotificationListenerExtImpl" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "showSmallWindowReply" }.hook {
                    before {
                        if (set.isEmpty()) return@before
                        val packName = firstArg().get<String>() ?: ""
                        result = set.contains(packName).not()
                    }
                }
            }
        }
    }
}