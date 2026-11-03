package com.luckyzyx.luckytool.hook.scopes.metis

import java.net.URI
import java.net.URLDecoder
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云卡片判定用的纯规则集合：包名与协议常量、内嵌地址提取、地址合法性校验。
 *
 * 全部是不带副作用的纯函数，判定的各个入口共用同一套实现，改规则只改这里。
 * 用的时候注意：任何解析失败都返回 null / false 表示「不干预」，绝不要抛出异常给宿主。
 */
@Obfuscate
object MetisPolicy {
    /** 流体云服务包名 */
    const val TARGET_PACKAGE = "com.oplus.metis"

    /** 卡片解析与点击所在的子进程后缀；主进程没有这些类，不能在里面装载 */
    const val TARGET_PROCESS_SUFFIX = ":text_intent"

    /** 内置浏览器包名，卡片默认都指向它 */
    const val HEYTAP_BROWSER = "com.heytap.browser"

    /** 协议卡前缀，形如 heytapbrowser://webpage?url=<编码后的 http> */
    const val PROTOCOL_PREFIX = "heytapbrowser:"

    /** 文本搜索卡的 textType */
    const val TEXT_TYPE_SEARCH = 29

    /**
     * 私有协议把真实地址放在这些 query 参数里，命中即视为可提取的内嵌地址。
     * 顺序即优先级：同名参数同时存在时取靠前的那个。
     */
    private val URL_PARAM_NAMES = listOf(
        "url", "u", "link", "target", "redirect", "redirecturl",
        "h5url", "jumpurl", "weburl", "openurl", "web_url", "open_url"
    )

    /** 原始 data 是否已经是 http(s) 地址 */
    fun isHttp(raw: String?): Boolean {
        val text = raw?.trim().orEmpty()
        return text.startsWith("http://", true) || text.startsWith("https://", true)
    }

    /** 原始 data 是否是流体云协议卡 */
    fun isProtocol(raw: String?): Boolean =
        raw?.trim()?.startsWith(PROTOCOL_PREFIX, true) == true

    /**
     * 校验是不是可打开的 http(s) 地址。
     * 只认 http/https 且 host 有实内容，market:// 一类交给商店降级逻辑，这里一律判否。
     */
    fun isValidHttpUrl(raw: String?): Boolean {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return false
        return try {
            val uri = URI(text)
            val scheme = uri.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && uri.host?.isNotBlank() == true
        } catch (_: Exception) {
            false
        }
    }

    /** 单次百分号解码；非法编码返回 null，由调用方按「拆不出来」处理 */
    fun decode(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        return try {
            URLDecoder.decode(text, "UTF-8")
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * 从任意卡片 data 里拆出真实 http(s) 地址。
     *
     * 覆盖两种情况：协议卡 heytapbrowser://webpage?url=... 与 qklink / uc 一类把地址塞在 query 里的私有协议。
     * 值可能是双重百分号编码，所以每个值按「解两次」与「解一次」分别试；先按参数白名单顺序取，
     * 白名单都不中再接受任意参数的合法地址。拆不出来返回 null。
     */
    fun extractNestedUrl(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val query = text.substringAfter('?', "").substringBefore('#')
        if (query.isEmpty()) return null
        val candidates = LinkedHashMap<String, String>()
        query.split('&').forEach { pair ->
            if (pair.isEmpty()) return@forEach
            val key = decode(pair.substringBefore('='))?.lowercase().orEmpty()
            val value = pair.substringAfter('=', "")
            if (value.isEmpty()) return@forEach
            val once = decode(value) ?: return@forEach
            val twice = decode(once) ?: once
            when {
                isValidHttpUrl(twice) -> candidates[key] = twice
                isValidHttpUrl(once) -> candidates[key] = once
            }
        }
        if (candidates.isEmpty()) return null
        URL_PARAM_NAMES.forEach { name -> candidates[name]?.let { return it } }
        return candidates.values.firstOrNull()
    }
}
