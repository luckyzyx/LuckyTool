package com.luckyzyx.luckytool.hook.scopes.metis

import android.content.Intent
import android.net.Uri
import com.highcapable.yukihookapi.hook.param.PackageParam
import org.lsposed.lsparanoid.Obfuscate

/**
 * 卡片重定向的判定与改写。
 *
 * 判定顺序固定，每条命中即返回：应用替换（含商店引导卡降级）→ 协议卡 → 文本搜索卡 →
 * 内置浏览器 http 卡 → 其他浏览器卡片。判定前先做廉价预筛，绝大多数卡片在预筛就被挡掉，
 * 不会为每个 Intent 跑全套 URL 解析。
 *
 * 改写只动目标包名、action、data 与组件字段，宿主原方法一律照常放行 —— 这里只是把「点开哪个 App」换掉。
 */
@Obfuscate
object MetisEngine {
    const val RULE_SWAP = "swap"
    const val RULE_SWAP_INSTALL = "swap_install"
    const val RULE_PROTOCOL = "rule1_protocol"
    const val RULE_TEXT_SEARCH = "rule2_text_search"
    const val RULE_HEYTAP_HTTP = "rule3_heytap_http"
    const val RULE_BROWSER_CARD = "rule4_browser_card"

    /**
     * 一次命中的结果。
     * url 非空表示要把卡片改成打开这个地址；clearData 表示清空 data 只拉目标 App 到前台；
     * plan 是商店降级用的方案标签，仅用于日志。
     */
    data class Decision(
        val rule: String,
        val targetPackage: String,
        val url: String? = null,
        val clearData: Boolean = false,
        val plan: String? = null
    )

    /**
     * 改写卡片 bean（主路径）。
     * 返回命中的规则名，未命中返回 null。bean 为 null 或结构不符时安静返回 null。
     */
    fun rewriteBean(param: PackageParam, bean: Any?, from: String): String? {
        val card = ReflectedTextIntent.of(bean) ?: return null
        val intent = card.intent() as? Intent
        val sourcePackage = card.packageName()
        val decision = decide(param, sourcePackage, intent?.dataString, card.textType()) ?: return null
        card.setPackageName(decision.targetPackage)
        apply(param, intent, decision)
        logDecision(param, from, decision, sourcePackage)
        return decision.rule
    }

    /**
     * 改写裸 Intent（兜底路径）。
     * 部分机型卡片根本不走 bean 那条路，只能在 startActivity 之前改 Intent；
     * 源包名取 getPackage()，显式组件情况下回落 component 的包名。
     */
    fun rewriteIntent(param: PackageParam, intent: Intent?, from: String): String? {
        val target = intent ?: return null
        val sourcePackage = target.getPackage() ?: target.component?.packageName
        val decision = decide(param, sourcePackage, target.dataString, null) ?: return null
        apply(param, target, decision)
        logDecision(param, from, decision, sourcePackage)
        return decision.rule
    }

    /**
     * 纯判定：给定卡片目标包名、data 与 textType，返回该怎么改；不该改返回 null。
     * 供 hook 之外的调用方（诊断、单测）直接使用。
     */
    fun decide(param: PackageParam, packageName: String?, data: String?, textType: Int?): Decision? {
        val source = packageName?.trim().orEmpty()
        val raw = data?.trim().orEmpty()

        //1 应用替换：卡片里写死的包名直接换成目标 App（复制口令卡这类没有网址，只能这样匹配）
        if (MetisSettings.packageSwapEnabled(param)) {
            PackageSwapRules.targetOf(param, source)?.let { target ->
                return Decision(RULE_SWAP, target)
            }
            //1.1 商店引导卡：源 App 已卸载，metis 改产去安装意图，源包名只能从 market://details?id= 反解
            if (MetisSettings.storeRedirectEnabled(param)) {
                val installSource = StoreLinkFallback.installSourcePackage(source, raw)
                val target = PackageSwapRules.targetOf(param, installSource)
                if (target != null) {
                    val plan = StoreLinkFallback.pick(param, raw, target)
                    if (plan != null) {
                        return Decision(RULE_SWAP_INSTALL, target, plan.url, plan.clearData, plan.label)
                    }
                }
            }
        }

        //2 浏览器规则前置条件：开关开着且用户选过目标浏览器
        if (!MetisSettings.redirectEnabled(param)) return null
        val browser = MetisSettings.targetBrowser(param)
        if (browser.isEmpty()) return null

        //3 廉价预筛：四个特征一个都不沾，后面的解析一律不必做
        val maybeProtocol = MetisPolicy.isProtocol(raw)
        val maybeHttp = MetisPolicy.isHttp(raw)
        val maybeNested = raw.contains("url=", true)
        val maybeTextSearch = textType == MetisPolicy.TEXT_TYPE_SEARCH
        if (!maybeProtocol && !maybeHttp && !maybeNested && !maybeTextSearch) return null

        //4 协议卡：heytapbrowser://webpage?url=<编码后的 http>，拆不出合法地址就跳过，保留原行为
        if (maybeProtocol && MetisSettings.ruleProtocol(param)) {
            MetisPolicy.extractNestedUrl(raw)?.let { url ->
                return Decision(RULE_PROTOCOL, browser, url)
            }
        }
        //5 文本搜索卡：卡片里没有网址，把搜索动作交给目标浏览器
        if (maybeTextSearch && MetisSettings.ruleTextSearch(param)) {
            return Decision(RULE_TEXT_SEARCH, browser)
        }
        //6 目标是内置浏览器且 data 是合法 http(s)
        if (maybeHttp && MetisSettings.ruleHeytapHttp(param) &&
            source == MetisPolicy.HEYTAP_BROWSER && MetisPolicy.isValidHttpUrl(raw)
        ) {
            return Decision(RULE_HEYTAP_HTTP, browser, raw)
        }
        //7 其他浏览器的卡片（实验性）：要求目标包名已知且不是目标浏览器本身，
        //  否则兜底路径会把上一步刚改好的结果再改一遍
        if (maybeHttp && MetisSettings.ruleBrowserCard(param) &&
            source.isNotEmpty() && source != browser && MetisPolicy.isValidHttpUrl(raw)
        ) {
            return Decision(RULE_BROWSER_CARD, browser, raw)
        }
        return null
    }

    /**
     * 把判定结果落到 Intent 上。
     * 顺序有讲究：Intent.setType() 内部会清空 data，所以先置 type 再写 data；
     * 组件与 selector 必须清掉，否则宿主原来的显式组件会压过 setPackage()。
     */
    fun apply(param: PackageParam, intent: Intent?, decision: Decision) {
        val target = intent ?: return
        if (decision.url != null) {
            target.type = null
            target.action = Intent.ACTION_VIEW
            target.data = Uri.parse(decision.url)
        } else if (decision.clearData) {
            target.type = null
            target.data = null
        }
        target.component = null
        target.selector = null
        target.setPackage(decision.targetPackage)
        if (decision.plan == StoreLinkFallback.PLAN_LAUNCHER) {
            //隐式 ACTION_MAIN + CATEGORY_LAUNCHER 在多数第三方 App 上起不来（它们只声明 CATEGORY_DEFAULT），
            //必须换成 PackageManager 给出的显式启动组件
            val launcher = launcherIntent(param, decision.targetPackage) ?: return
            target.action = launcher.action
            target.component = launcher.component
            target.categories?.toList()?.forEach { target.removeCategory(it) }
            launcher.categories?.toList()?.forEach { target.addCategory(it) }
            target.setPackage(decision.targetPackage)
        }
    }

    private fun launcherIntent(param: PackageParam, packageName: String): Intent? {
        val context = MetisSettings.contextOf(param) ?: return null
        return runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
    }

    private fun logDecision(param: PackageParam, from: String, decision: Decision, source: String?) {
        val text = buildString {
            append("rule=").append(decision.rule)
            append(" target=").append(decision.targetPackage)
            if (!source.isNullOrEmpty()) append(" source=").append(source)
            decision.plan?.let { append(" plan=").append(it) }
            decision.url?.let { append(" url=").append(it) }
        }
        MetisLog.debug(param, from, text)
    }
}
