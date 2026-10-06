package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AllowLockingUnLockingOfExcludedActivity : YukiBaseHooker() {
    override fun onHook() {
        //Search OplusTaskShortcutsFactory -> showLock / showUnlock C13
        //Search OplusLockManager -> isTaskAllowLock / isTaskAllowUnlock C14
        //Source OplusLockManager -> Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS (8388608 / 0x00800000)
        "com.oplus.quickstep.applock.OplusLockManager".toClass().resolve().apply {
            (firstMethodOrNull { name = "isAppLockable" }
                ?: firstMethod { name = "isAppSupportLock" }).hook {
                before {
                    val intent = lastArg().get<Intent>() ?: return@before
                    val flag = intent.flags
                    if (flag and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) {
                        intent.removeFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    }
                }
            }
        }
    }
}