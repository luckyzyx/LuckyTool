package com.luckyzyx.luckytool.hook.scopes.metis

import android.content.Context
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 hook 的偏好读取入口。
 *
 * 开关类键（是否装载某个 hooker）由 HookMetis 在 onHook 里读一次，改完需重启目标进程；
 * 连续值（目标浏览器、替换表、四个文案模板）必须在拦截器执行时读，改完即时生效 ——
 * 所以这里的读取方法都要求传入当前 hooker 实例（PackageParam），不要提前缓存成常量。
 */
@Obfuscate
object MetisSettings {
    const val KEY_REDIRECT_ENABLED = "metis_redirect_enabled"
    const val KEY_TARGET_BROWSER = "metis_target_browser"
    const val KEY_RULE_PROTOCOL = "metis_rule_protocol"
    const val KEY_RULE_TEXT_SEARCH = "metis_rule_text_search"
    const val KEY_RULE_HEYTAP_HTTP = "metis_rule_heytap_http"
    const val KEY_RULE_BROWSER_CARD = "metis_rule_browser_card"
    const val KEY_PACKAGE_SWAP_ENABLED = "metis_package_swap_enabled"
    const val KEY_PACKAGE_SWAP_RULES = "metis_package_swap_rules"
    const val KEY_STORE_REDIRECT_ENABLED = "metis_store_redirect_enabled"
    const val KEY_CARD_TEXT_ENABLED = "metis_card_text_enabled"
    const val KEY_CARD_TITLE = "metis_card_title"
    const val KEY_CARD_SUBTITLE = "metis_card_subtitle"
    const val KEY_CARD_BUTTON = "metis_card_button"
    const val KEY_CARD_CAPSULE = "metis_card_capsule"
    const val KEY_DEBUG_LOG = "metis_debug_log"

    /** 卡片点击重定向总开关（装载开关） */
    fun redirectEnabled(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_REDIRECT_ENABLED, false)

    /** 目标浏览器包名；空串表示用户还没选，此时不做重定向 */
    fun targetBrowser(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_TARGET_BROWSER, "").orEmpty().trim()

    fun ruleProtocol(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_RULE_PROTOCOL, true)

    fun ruleTextSearch(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_RULE_TEXT_SEARCH, true)

    fun ruleHeytapHttp(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_RULE_HEYTAP_HTTP, true)

    /** 实验性：目标是其他浏览器时也改写，默认关，避免把用户自己配好的浏览器又绕一圈 */
    fun ruleBrowserCard(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_RULE_BROWSER_CARD, false)

    /** 应用替换总开关（装载开关） */
    fun packageSwapEnabled(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_PACKAGE_SWAP_ENABLED, false)

    /** 替换表原文，"源包名=目标包名" 多条用 ; 分隔，解析交给 PackageSwapRules */
    fun packageSwapRules(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_PACKAGE_SWAP_RULES, "").orEmpty()

    /** 商店引导卡降级开关，依赖替换表判断源 App 是否被用户换成了别的 App */
    fun storeRedirectEnabled(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_STORE_REDIRECT_ENABLED, true)

    /** 卡片文案自定义总开关（装载开关） */
    fun cardTextEnabled(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_CARD_TEXT_ENABLED, false)

    fun cardTitle(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_CARD_TITLE, "").orEmpty()

    fun cardSubtitle(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_CARD_SUBTITLE, "").orEmpty()

    fun cardButton(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_CARD_BUTTON, "").orEmpty()

    fun cardCapsule(param: PackageParam): String =
        param.preferences(ModulePrefs).getString(KEY_CARD_CAPSULE, "").orEmpty()

    /** 诊断日志开关；关闭时只留异常与缺类日志 */
    fun debugLog(param: PackageParam): Boolean =
        param.preferences(ModulePrefs).getBoolean(KEY_DEBUG_LOG, false)

    /**
     * 宿主 Context，用于 PackageManager / 剪贴板这类需要 Context 的查询。
     * 进程还没 attach 时返回 null，调用方按「本次不改写」处理即可，不要抛异常。
     */
    fun contextOf(param: PackageParam): Context? = runCatching { param.hostApplication }.getOrNull()
}
