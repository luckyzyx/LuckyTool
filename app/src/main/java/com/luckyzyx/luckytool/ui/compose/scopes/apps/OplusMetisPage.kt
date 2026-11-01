package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.showToast
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

// prefs 键：流体云进程侧的 hook 按同一批键名读取，这里改名等于让对应策略静默失效
private const val KEY_REDIRECT_ENABLED = "metis_redirect_enabled"
private const val KEY_TARGET_BROWSER = "metis_target_browser"
private const val KEY_RULE_PROTOCOL = "metis_rule_protocol"
private const val KEY_RULE_TEXT_SEARCH = "metis_rule_text_search"
private const val KEY_RULE_HEYTAP_HTTP = "metis_rule_heytap_http"
private const val KEY_RULE_BROWSER_CARD = "metis_rule_browser_card"
private const val KEY_SWAP_ENABLED = "metis_package_swap_enabled"
private const val KEY_SWAP_RULES = "metis_package_swap_rules"
private const val KEY_STORE_REDIRECT_ENABLED = "metis_store_redirect_enabled"
private const val KEY_CARD_TEXT_ENABLED = "metis_card_text_enabled"
private const val KEY_CARD_TITLE = "metis_card_title"
private const val KEY_CARD_SUBTITLE = "metis_card_subtitle"
private const val KEY_CARD_BUTTON = "metis_card_button"
private const val KEY_CARD_CAPSULE = "metis_card_capsule"
private const val KEY_DEBUG_LOG = "metis_debug_log"

// 替换表的条目分隔符：包名里不会出现 ';' 与 '='，所以按这两个字符直接切分是安全的
private const val RULE_SEPARATOR = ";"
private const val RULE_ASSIGN = "="

/**
 * 流体云页：卡片点击重定向、应用替换、卡片文案与诊断开关。
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

        // 应用替换
        category(c.getString(R.string.metis_category_package_swap))
        switch(
            key = KEY_SWAP_ENABLED,
            title = c.getString(R.string.metis_package_swap_enabled),
            summary = c.getString(R.string.metis_package_swap_enabled_summary),
            notify = true,
        )
        custom(
            key = KEY_SWAP_RULES,
            title = c.getString(R.string.metis_package_swap_rules),
        ) {
            val raw by state.stringFlow(KEY_SWAP_RULES).collectAsStateWithLifecycle()
            val ctx = LocalContext.current
            // 应用名只在规则表变化后重查一次，避免每次重组都走 PackageManager 查包
            val rows = remember(raw, ctx) { describeSwapRules(ctx, raw) }
            val write = { key: String, value: String ->
                state.set(key, value)
                sendValue(key, value)
            }
            var pickingSource by remember { mutableStateOf(false) }
            var pendingSource by remember { mutableStateOf("") }
            var pickingTarget by remember { mutableStateOf(false) }

            Column {
                if (rows.isEmpty()) {
                    PrefRowInset {
                        PrefRow(
                            title = stringResource(R.string.metis_package_swap_rules_empty),
                            enabled = false,
                        )
                    }
                }
                rows.forEach { row ->
                    PrefRowInset {
                        PrefRow(
                            title = stringResource(
                                R.string.metis_package_swap_rule_title,
                                row.sourceLabel,
                                row.targetLabel,
                            ),
                            summary = stringResource(
                                R.string.metis_package_swap_rule_summary,
                                row.source,
                                row.target,
                            ),
                            trailing = {
                                RuleDeleteButton {
                                    write(KEY_SWAP_RULES, removeSwapRule(raw, row.source))
                                }
                            },
                        )
                    }
                }
                PrefRowInset {
                    PrefRow(
                        title = stringResource(R.string.metis_package_swap_add_rule),
                        onClick = { pickingSource = true },
                    )
                }
            }

            // 先选源应用再选目标应用：源应用按全量已安装可启动应用列出（AppPickerDialog 内部用 flags 0）
            if (pickingSource) {
                AppPickerDialog(
                    title = stringResource(R.string.metis_package_swap_pick_source),
                    onDismiss = { pickingSource = false },
                    onConfirm = { apps ->
                        pickingSource = false
                        val packName = apps.firstOrNull()?.packageName.orEmpty()
                        if (packName.isNotEmpty()) {
                            pendingSource = packName
                            pickingTarget = true
                        }
                    },
                )
            }
            if (pickingTarget) {
                AppPickerDialog(
                    title = stringResource(R.string.metis_package_swap_pick_target),
                    onDismiss = { pickingTarget = false },
                    onConfirm = { apps ->
                        val packName = apps.firstOrNull()?.packageName.orEmpty()
                        when {
                            packName.isEmpty() -> pickingTarget = false
                            packName == pendingSource -> {
                                pickingTarget = false
                                ctx.showToast(R.string.metis_package_swap_same_app)
                            }

                            else -> {
                                pickingTarget = false
                                write(KEY_SWAP_RULES, upsertSwapRule(raw, pendingSource, packName))
                            }
                        }
                    },
                )
            }
        }
        switch(
            key = KEY_STORE_REDIRECT_ENABLED,
            title = c.getString(R.string.metis_store_redirect_enabled),
            summary = c.getString(R.string.metis_store_redirect_enabled_summary),
            default = true,
            notify = true,
        )

        // 卡片文案
        category(c.getString(R.string.metis_category_card_text))
        switch(
            key = KEY_CARD_TEXT_ENABLED,
            title = c.getString(R.string.metis_card_text_enabled),
            summary = c.getString(R.string.metis_card_text_enabled_summary),
            notify = true,
        )
        // 模板留空 = 保留原文案，所以 hint 既作输入框占位也作说明；{app} 由 hook 侧替换成目标应用名
        val templateSummary = c.getString(R.string.metis_card_template_summary)
        val templateHint = c.getString(R.string.metis_card_template_empty)
        editText(
            key = KEY_CARD_TITLE,
            title = c.getString(R.string.metis_card_title),
            summary = templateSummary,
            hint = templateHint,
            dialogMessage = templateSummary,
        )
        editText(
            key = KEY_CARD_SUBTITLE,
            title = c.getString(R.string.metis_card_subtitle),
            summary = templateSummary,
            hint = templateHint,
            dialogMessage = templateSummary,
        )
        editText(
            key = KEY_CARD_BUTTON,
            title = c.getString(R.string.metis_card_button),
            summary = templateSummary,
            hint = templateHint,
            dialogMessage = templateSummary,
        )
        editText(
            key = KEY_CARD_CAPSULE,
            title = c.getString(R.string.metis_card_capsule),
            summary = templateSummary,
            hint = templateHint,
            dialogMessage = templateSummary,
        )

        // 诊断
        category(c.getString(R.string.metis_category_debug))
        switch(
            key = KEY_DEBUG_LOG,
            title = c.getString(R.string.metis_debug_log),
            summary = c.getString(R.string.metis_debug_log_summary),
            notify = true,
        )
    }
}

/** 浏览器候选：包名 + 当前应用名（名字仅在页面构建时查一次） */
private class BrowserApp(val packageName: String, val label: String)

/**
 * 查能打开 https 链接的应用，作为重定向目标浏览器候选。
 * 这里用 MATCH_DEFAULT_ONLY：只保留系统认定的默认处理者，图库、下载器这类
 * 「碰巧也能打开 http」的应用不会混进来；应用替换里的源应用选择走的是另一套全量列表，两者不要混用。
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

/** 替换规则行：包名 + 现查的应用名（未安装时应用名回落成包名） */
private data class SwapRuleRow(
    val source: String,
    val target: String,
    val sourceLabel: String,
    val targetLabel: String,
)

private fun describeSwapRules(context: Context, raw: String): List<SwapRuleRow> {
    val appUtils = AppUtils(context)
    return parseSwapRules(raw).map { (source, target) ->
        SwapRuleRow(
            source = source,
            target = target,
            sourceLabel = appUtils.getAppLabel(source).toString(),
            targetLabel = appUtils.getAppLabel(target).toString(),
        )
    }
}

/** 解析替换表；格式错误、空项与源等于目标的条目直接丢掉，保证渲染与写回的数据是干净的 */
private fun parseSwapRules(raw: String): List<Pair<String, String>> =
    raw.split(RULE_SEPARATOR)
        .mapNotNull { entry ->
            val parts = entry.split(RULE_ASSIGN, limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val source = parts[0].trim()
            val target = parts[1].trim()
            if (source.isEmpty() || target.isEmpty() || source == target) null else source to target
        }

/** 序列化替换表：同一个源应用只保留第一条，避免 hook 侧取到互相矛盾的映射 */
private fun serializeSwapRules(rules: List<Pair<String, String>>): String =
    rules.distinctBy { it.first }.joinToString(RULE_SEPARATOR) { "${it.first}=${it.second}" }

/** 新增或改签一条规则：源应用已存在时覆盖目标应用，而不是追加出第二份映射 */
private fun upsertSwapRule(raw: String, source: String, target: String): String {
    val rules = parseSwapRules(raw).toMutableList()
    val index = rules.indexOfFirst { it.first == source }
    if (index >= 0) rules[index] = source to target else rules += source to target
    return serializeSwapRules(rules)
}

private fun removeSwapRule(raw: String, source: String): String =
    serializeSwapRules(parseSwapRules(raw).filterNot { it.first == source })

/** 规则行的删除入口：两套外观的图标按钮形态不同，集中在这里分派，调用方不用各写一份 */
@Composable
private fun RuleDeleteButton(onClick: () -> Unit) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixIconButton(onClick = onClick) {
            MiuixIcon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.metis_package_swap_delete_rule),
                tint = MiuixTheme.colorScheme.onBackground,
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.metis_package_swap_delete_rule),
            )
        }
    }
}
