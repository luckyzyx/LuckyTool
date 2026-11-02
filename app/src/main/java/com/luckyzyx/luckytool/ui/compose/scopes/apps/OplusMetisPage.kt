package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.checkPackName

// prefs 键：流体云进程侧的 hook 按同一批键名读取，这里改名等于让对应策略静默失效
private const val KEY_REDIRECT_ENABLED = "metis_redirect_enabled"
private const val KEY_TARGET_BROWSER = "metis_target_browser"
private const val KEY_RULE_PROTOCOL = "metis_rule_protocol"
private const val KEY_RULE_TEXT_SEARCH = "metis_rule_text_search"
private const val KEY_RULE_HEYTAP_HTTP = "metis_rule_heytap_http"
private const val KEY_RULE_BROWSER_CARD = "metis_rule_browser_card"

/**
 * 流体云页：卡片点击重定向。
 * 页面只负责写 prefs，真正的重定向由 metis 进程里的 hook 读取这批键执行；
 * 整页仅在装有 com.oplus.metis 时出现（见 spec.isVisible）。
 */
object OplusMetisPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_metis",
        prefsName = ModulePrefs,
        packName = "com.oplus.metis",
        scopes = arrayOf("com.oplus.metis"),
        restartEnabled = true,
        isVisible = { checkPackName("com.oplus.metis") },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        // 卡片重定向
        category(c.getString(R.string.metis_category_redirect))
        switch(
            key = KEY_REDIRECT_ENABLED,
            title = c.getString(R.string.metis_redirect_enabled),
            summary = c.getString(R.string.metis_redirect_enabled_summary),
            notify = true,
        )
        // 目标浏览器：候选只列能响应 https 链接的应用，存包名；首项空串表示未设置（不重定向）
        val browsers = queryBrowserApps(c)
        list(
            key = KEY_TARGET_BROWSER,
            title = c.getString(R.string.metis_target_browser),
            entries = arrayOf(c.getString(R.string.not_set)) + browsers.map { it.label }.toTypedArray(),
            entryValues = arrayOf("") + browsers.map { it.packageName }.toTypedArray(),
            default = "",
            summary = c.getString(R.string.metis_target_browser_summary),
            notify = true,
        )
        switch(
            key = KEY_RULE_PROTOCOL,
            title = c.getString(R.string.metis_rule_protocol),
            summary = c.getString(R.string.metis_rule_protocol_summary),
            default = true,
            notify = true,
        )
        switch(
            key = KEY_RULE_TEXT_SEARCH,
            title = c.getString(R.string.metis_rule_text_search),
            summary = c.getString(R.string.metis_rule_text_search_summary),
            default = true,
            notify = true,
        )
        switch(
            key = KEY_RULE_HEYTAP_HTTP,
            title = c.getString(R.string.metis_rule_heytap_http),
            summary = c.getString(R.string.metis_rule_heytap_http_summary),
            default = true,
            notify = true,
        )
        switch(
            key = KEY_RULE_BROWSER_CARD,
            title = c.getString(R.string.metis_rule_browser_card),
            summary = c.getString(R.string.metis_rule_browser_card_summary),
            default = false,
            notify = true,
        )
    }
}

/** 浏览器候选：包名 + 当前应用名（名字仅在页面构建时查一次） */
private class BrowserApp(val packageName: String, val label: String)

/**
 * 查能打开 https 链接的应用，作为重定向目标浏览器候选。
 * 这里用 MATCH_DEFAULT_ONLY：只保留系统认定的默认处理者，图库、下载器这类
 * 「碰巧也能打开 http」的应用不会混进来。
 */
private fun queryBrowserApps(context: Context): List<BrowserApp> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        .addCategory(Intent.CATEGORY_BROWSABLE)
    val appUtils = AppUtils(context)
    return PackageUtils(context.packageManager)
        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .mapNotNull { it.activityInfo?.packageName }
        .distinct()
        .filter { it != context.packageName }
        .map { BrowserApp(it, appUtils.getAppLabel(it).toString()) }
        .sortedBy { it.label }
}
