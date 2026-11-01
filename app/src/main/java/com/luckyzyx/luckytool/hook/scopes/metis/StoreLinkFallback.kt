package com.luckyzyx.luckytool.hook.scopes.metis

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.highcapable.yukihookapi.hook.param.PackageParam
import org.lsposed.lsparanoid.Obfuscate

/**
 * 商店引导卡（源 App 未安装）的识别与降级。
 *
 * 源 App 被卸载后 metis 会把卡片改产成「去安装」意图：action=VIEW、data=market://details?id=<源包名>，
 * package 为空或指向商店。这时卡片里已经没有任何目标包名，只能从 data 的 id 反解出源包名再去查替换表。
 *
 * 反解出来后按 shared-link → keep-data → nested-url → launcher 四级探测，每级都用 PackageManager
 * 确认目标 App 真的能解析这个意图才采纳，避免把卡片改成点不动的死意图；四级都不通用 null 交回原来的商店卡。
 */
@Obfuscate
object StoreLinkFallback {
    /** 各家商店的包名，命中即认为这张卡是在引导去安装 */
    private val STORE_PACKAGES = setOf(
        "com.heytap.market", "com.oppo.market", "com.oplus.market", "com.coloros.market",
        "com.android.vending", "com.xiaomi.market", "com.huawei.appmarket",
        "com.bbk.appstore", "com.tencent.android.qqdownloader"
    )

    /** 各家商店的 scheme */
    private val STORE_SCHEMES = setOf(
        "market", "oaps", "oppomarket", "appmarket", "appmarketclient", "itms-apps", "samsungapps"
    )

    /** 商店域名特征，scheme 缺失时靠它兜住 */
    private val STORE_HOST_HINTS = setOf("market", "appstore", "appgallery")

    const val PLAN_SHARED_LINK = "shared-link"
    const val PLAN_KEEP_DATA = "keep-data"
    const val PLAN_NESTED_URL = "nested-url"
    const val PLAN_LAUNCHER = "launcher"

    /** 一个可落地的降级方案：url 为空表示不改 data（keep-data 保留原值，launcher 清空） */
    data class Plan(val label: String, val url: String?, val clearData: Boolean)

    /**
     * 商店引导卡识别：命中返回它要去安装的源包名，普通卡片返回 null。
     * id 取不到（data 是 market:// 开头的搜索页之类）也返回 null，按普通卡片处理。
     */
    fun installSourcePackage(packageName: String?, data: String?): String? {
        val uri = data?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase().orEmpty()
        val looksLikeStore = (scheme != null && scheme in STORE_SCHEMES) ||
                (packageName != null && packageName in STORE_PACKAGES) ||
                STORE_HOST_HINTS.any { host.contains(it) }
        if (!looksLikeStore) return null
        return uri.getQueryParameter("id")?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * 为已命中的商店引导卡挑一个降级方案。
     *
     * targetPackage 是替换表给出的目标 App。Android 10 之后后台进程读剪贴板会被系统拒绝，
     * 读不到属正常情况，直接进下一级即可，不要因此放弃整张卡片。
     */
    fun pick(param: PackageParam, data: String?, targetPackage: String): Plan? {
        val context = MetisSettings.contextOf(param) ?: return null
        val manager = runCatching { context.packageManager }.getOrNull() ?: return null
        //1 剪贴板里还留着分享地址：直接跳它，最贴近卡片原本想打开的页面
        firstHttpUrl(readClipboard(context))?.let { url ->
            if (resolvableUrl(manager, url, targetPackage)) return Plan(PLAN_SHARED_LINK, url, false)
        }
        //2 原始 data 原样交给目标 App，适用目标 App 自己也认 market:// 之类的情况
        val original = data?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (original != null && resolvableData(manager, original, targetPackage)) {
            return Plan(PLAN_KEEP_DATA, null, false)
        }
        //3 从 data 的 query 里拆出真实地址
        MetisPolicy.extractNestedUrl(data)?.let { url ->
            if (resolvableUrl(manager, url, targetPackage)) return Plan(PLAN_NESTED_URL, url, false)
        }
        //4 什么都对不上就只把目标 App 拉到前台，至少用户点下去有反应
        if (runCatching { manager.getLaunchIntentForPackage(targetPackage) }.getOrNull() != null) {
            return Plan(PLAN_LAUNCHER, null, true)
        }
        return null
    }

    private fun readClipboard(context: Context): String? = runCatching {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = manager?.primaryClip
        if (clip == null || clip.itemCount <= 0) return@runCatching null
        clip.getItemAt(0).coerceToText(context)?.toString()
    }.getOrNull()

    /** 分享面板残留的常是「文案 + 链接」一整段，从第一个 http 起截到空白为止 */
    private fun firstHttpUrl(text: String?): String? {
        val raw = text?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val start = raw.indexOf("http", ignoreCase = true)
        if (start < 0) return null
        val candidate = raw.substring(start).takeWhile { !it.isWhitespace() }
        return candidate.takeIf { MetisPolicy.isValidHttpUrl(it) }
    }

    private fun resolvableUrl(manager: PackageManager, url: String, targetPackage: String): Boolean =
        runCatching {
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            probe.setPackage(targetPackage)
            manager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY) != null
        }.getOrDefault(false)

    private fun resolvableData(manager: PackageManager, data: Uri, targetPackage: String): Boolean =
        runCatching {
            val probe = Intent(Intent.ACTION_VIEW, data)
            //干掉原来的显式组件，否则 setPackage 之后仍然会照着旧组件找目标
            probe.component = null
            probe.selector = null
            probe.setPackage(targetPackage)
            manager.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY) != null
        }.getOrDefault(false)
}
