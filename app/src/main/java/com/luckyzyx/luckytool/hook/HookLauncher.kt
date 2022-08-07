package com.luckyzyx.luckytool.hook

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.apps.launcher.RemoveAppUpdateDot
import com.luckyzyx.luckytool.hook.apps.launcher.UnlockTaskLocks
import com.luckyzyx.luckytool.utils.XposedPrefs

class HookLauncher : YukiBaseHooker(){
    override fun onHook() {
        //解锁最近任务限制
        if (prefs(XposedPrefs).getBoolean("unlock_task_locks",false)) loadHooker(UnlockTaskLocks())

        //移除APP更新蓝点
        if (prefs(XposedPrefs).getBoolean("remove_appicon_dot",false)) loadHooker(RemoveAppUpdateDot())
    }
}