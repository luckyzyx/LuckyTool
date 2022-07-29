package com.luckyzyx.colorosext.hook.android

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.loggerE
import com.luckyzyx.colorosext.utils.XposedPrefs

class RemoveStatusBarTopNotification : YukiBaseHooker() {
    override fun onHook() {
        findClass("com.android.server.wm.AlertWindowNotification").hook {
            injectMember {
                method {
                    name = "onPostNotification"
                }
                beforeHook {
                    if (prefs(XposedPrefs).getBoolean("remove_statusbar_top_notification", false)) resultNull()
                }
            }
        }.onHookClassNotFoundFailure {
            loggerE(msg = "ClassNotFound->AlertWindowNotification")
        }
    }
}