package com.luckyzyx.luckytool.hook.scopes.metis

import android.content.Intent
import android.net.Uri
import com.highcapable.yukihookapi.hook.param.PackageParam
import org.lsposed.lsparanoid.Obfuscate

/**
 * 卡片重定向的判定与改写。
 *
 * 判定顺序固定，每条命中即返回：协议卡 → 文本搜索卡 → 内置浏览器 http 卡 → 其他浏览器卡片。
 * 判定前先做廉价预筛，绝大多数卡片在预筛就被挡掉，不会为每个 Intent 跑全套 URL 解析。
 *
 * 改写只动目标包名、action、data 与组件字段，宿主原方法一律照常放行 —— 这里只是把「点开哪个 App」换掉。
 */
@Obfuscate
object MetisEngine {
    const val RULE_PROTOCOL = "rule1_protocol"
    const val RULE_TEXT_SEARCH = "rule2_text_search"
    const val RULE_HEYTAP_HTTP = "rule3_heytap_http"
    const val RULE_BROWSER_CARD = "rule4_browser_card"

    /** 一次命中的结果：url 非空表示要把卡片改成打开这个地址，为空则只换目标包名 */
    data class Decision(
        val rule: String,
        val targetPackage: String,
        val url: String? = null
    )

    /** 改写卡片 bean（主路径）。bean 为 null 或结构不符时安静返回，不把异常抛给宿主 */
    fun rewriteBean(param: PackageParam, bean: Any?) {
        val card = ReflectedTextIntent.of(bean) ?: return
        val intent = card.intent() as? Intent
        val decision = decide(param, card.packageName(), intent?.dataString, card.textType()) ?: return
        card.setPackageName(decision.targetPackage)
        apply(intent, decision)
    }

    /**
     * 改写裸 Intent（兜底路径）。
     * 部分机型卡片根本不走 bean 那条路，只能在 startActivity 之前改 Intent；
     * 源包名取 getPackage()，显式组件情况下回落 component 的包名。
     */
    fun rewriteIntent(param: PackageParam, intent: Intent?) {
        val target = intent ?: return
        val sourcePackage = target.getPackage() ?: target.component?.packageName
        val decision = decide(param, sourcePackage, target.dataString, null) ?: return
        apply(target, decision)
    }

    /**
     * 纯判定：给定卡片目标包名、data 与 textType，返回该怎么改；不该改返回 null。
     * 不碰任何 Intent，改什么由调用方自己落到 bean 或 Intent 上。
     */
    fun decide(param: PackageParam, packageName: String?, data: String?, textType: Int?): Decision? {
        val source = packageName?.trim().orEmpty()
        val raw = data?.trim().orEmpty()

        //1 前置条件：总开关开着且用户选过目标浏览器
        if (!MetisSettings.redirectEnabled(param)) return null
        val browser = MetisSettings.targetBrowser(param)
        if (browser.isEmpty()) return null

        //2 廉价预筛：四个特征一个都不沾，后面的解析一律不必做
        val maybeProtocol = MetisPolicy.isProtocol(raw)
        val maybeHttp = MetisPolicy.isHttp(raw)
        val maybeNested = raw.contains("url=", true)
        val maybeTextSearch = textType == MetisPolicy.TEXT_TYPE_SEARCH
        if (!maybeProtocol && !maybeHttp && !maybeNested && !maybeTextSearch) return null

        //3 协议卡：heytapbrowser://webpage?url=<编码后的 http>，拆不出合法地址就跳过，保留原行为
        if (maybeProtocol && MetisSettings.ruleProtocol(param)) {
            MetisPolicy.extractNestedUrl(raw)?.let { url ->
                return Decision(RULE_PROTOCOL, browser, url)
            }
        }
        //4 文本搜索卡：卡片里没有网址，把搜索动作交给目标浏览器
        if (maybeTextSearch && MetisSettings.ruleTextSearch(param)) {
            return Decision(RULE_TEXT_SEARCH, browser)
        }
        //5 目标是内置浏览器且 data 是合法 http(s)
        if (maybeHttp && MetisSettings.ruleHeytapHttp(param) &&
            source == MetisPolicy.HEYTAP_BROWSER && MetisPolicy.isValidHttpUrl(raw)
        ) {
            return Decision(RULE_HEYTAP_HTTP, browser, raw)
        }
        //6 其他浏览器的卡片（实验性）：要求目标包名已知且不是目标浏览器本身，
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
    fun apply(intent: Intent?, decision: Decision) {
        val target = intent ?: return
        if (decision.url != null) {
            target.type = null
            target.action = Intent.ACTION_VIEW
            target.data = Uri.parse(decision.url)
        }
        target.component = null
        target.selector = null
        target.setPackage(decision.targetPackage)
    }
}
