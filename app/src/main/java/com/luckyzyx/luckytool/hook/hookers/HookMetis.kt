package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.metis.MetisLog
import com.luckyzyx.luckytool.hook.scopes.metis.MetisPolicy
import com.luckyzyx.luckytool.hook.scopes.metis.MetisSettings
import com.luckyzyx.luckytool.hook.scopes.metis.MetisTextIntentHook
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云（com.oplus.metis）卡片重定向的装载入口。
 *
 * 只在卡片解析与点击所在的 :text_intent 子进程装载 —— 主进程没有这些类，装上去只会白费一次查找。
 * 进程名取模块加载回调给的 processName：PM 层的 applicationInfo.processName 只有 "com.oplus.metis"，
 * 不带子进程后缀，用它判断会永远认为当前是主进程。取不到时保守装载并记一条 warn，方便用户反馈。
 *
 * 总开关这里只读一次决定是否装载，改完需要重启目标进程（流体云进程由系统托管，
 * 用户下次拉起卡片时会重新走这条路径）；目标浏览器与各规则开关在拦截器里读，改完即时生效。
 */
@Obfuscate
object HookMetis : YukiBaseHooker() {

    override fun onHook() {
        val process = runCatching { processName }.getOrNull().orEmpty()
        if (process.isEmpty()) {
            MetisLog.warn("onHook", "无法确认当前进程名，保守装载")
        } else if (!process.endsWith(MetisPolicy.TARGET_PROCESS_SUFFIX)) {
            MetisLog.info("onHook", "跳过非目标进程: $process")
            return
        }

        if (!MetisSettings.redirectEnabled(this)) {
            MetisLog.info("onHook", "process=$process 未启用卡片重定向，跳过装载")
            return
        }
        loadHooker(MetisTextIntentHook)
        MetisLog.info("onHook", "process=$process 已装载卡片重定向")
    }
}
