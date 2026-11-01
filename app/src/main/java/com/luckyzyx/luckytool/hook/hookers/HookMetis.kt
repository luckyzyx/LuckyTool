package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.metis.MetisCardTextHook
import com.luckyzyx.luckytool.hook.scopes.metis.MetisLog
import com.luckyzyx.luckytool.hook.scopes.metis.MetisPolicy
import com.luckyzyx.luckytool.hook.scopes.metis.MetisSettings
import com.luckyzyx.luckytool.hook.scopes.metis.MetisTextIntentHook
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云（com.oplus.metis）卡片重定向与文案自定义的装载入口。
 *
 * 只在卡片解析与点击所在的 :text_intent 子进程装载 —— 主进程没有这些类，装上去只会白费一次查找。
 * 进程名取模块加载回调给的 processName：PM 层的 applicationInfo.processName 只有 "com.oplus.metis"，
 * 不带子进程后缀，用它判断会永远认为当前是主进程。取不到时保守装载并记一条 warn，方便用户反馈。
 *
 * 开关类偏好这里只读一次决定装载哪些 hooker，改完开关需要重启目标进程（流体云进程由系统托管，
 * 用户下次拉起卡片时会重新走这条路径）；目标浏览器、替换表、文案模板等连续值在拦截器里读，改完即时生效。
 */
@Obfuscate
object HookMetis : YukiBaseHooker() {

    override fun onHook() {
        val process = runCatching { processName }.getOrNull().orEmpty()
        if (process.isEmpty()) {
            MetisLog.warn("onHook", "无法确认当前进程名，保守装载全部 hook")
        } else if (!process.endsWith(MetisPolicy.TARGET_PROCESS_SUFFIX)) {
            MetisLog.info("onHook", "跳过非目标进程: $process")
            return
        }

        val redirect = MetisSettings.redirectEnabled(this)
        val swap = MetisSettings.packageSwapEnabled(this)
        val cardText = MetisSettings.cardTextEnabled(this)
        if (redirect || swap) loadHooker(MetisTextIntentHook)
        if (cardText) loadHooker(MetisCardTextHook)
        MetisLog.info(
            "onHook",
            "process=$process redirect=$redirect swap=$swap cardText=$cardText"
        )
    }
}
