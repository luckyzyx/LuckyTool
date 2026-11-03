package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 hook 的偏好读取入口。
 *
 * 装载开关由 HookMetis 在 onHook 里读一次，改完需重启目标进程；
 * 目标浏览器与各规则开关在拦截器执行时读，改完即时生效 ——
 * 所以这里的读取方法都要传入当前 hooker 实例（PackageParam），不要提前缓存成常量。
 */
@Obfuscate
object MetisSettings {
    const val KEY_REDIRECT_ENABLED = "metis_redirect_enabled"
    const val KEY_TARGET_BROWSER = "metis_target_browser"
    const val KEY_RULE_PROTOCOL = "metis_rule_protocol"
    const val KEY_RULE_TEXT_SEARCH = "metis_rule_text_search"
    const val KEY_RULE_HEYTAP_HTTP = "metis_rule_heytap_http"
    const val KEY_RULE_BROWSER_CARD = "metis_rule_browser_card"

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
}
