package com.luckyzyx.luckytool.ui.compose.special

import com.luckyzyx.luckytool.data.DonateDetailInfo
import com.luckyzyx.luckytool.data.DonateInfo
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.SettingsPrefs
import org.json.JSONArray
import java.text.DecimalFormat

/**
 * Donate 页（旧 ui.fragment.settings.DonateFragment 的 Compose 等价物）。
 * 捐赠数据经 GitHub API 检查更新、GitMirror 下载、AESCrypt 加密缓存后由
 * Markwon(+TablePlugin) 渲染为 Markdown 表格；筛选/排序选项在排序过滤弹层中。
 * 下拉刷新（onRefresh）重跑 initData 管线。
 *
 * 三文件制（t21）：本文件只保留 pageKey/常量/加载器与纯数据格式化函数，
 * 渲染体按 LocalUiMode 分派到 DonateBodyMaterial / DonateBodyMiuix（同包）。
 */

internal val showDetailedKey = "show_detailed_donate_data"
internal val showOtherCurrencyKey = "show_other_currency_donate_data"
internal val lastUpdateKey = "last_update_dd_date"
internal val developKey = "hidden_function"
internal const val DONATE_DATA_URL =
    "https://api.github.com/repos/LuckyOSTeam/LuckyOSTeam.github.io/releases/tags/luckytool_donates"
internal const val DONATE_JSON_URL =
    "https://raw.gitmirror.com/LuckyOSTeam/LuckyOSTeam.github.io/main/LuckyTool/donate.json"

/** 由内容组合时注册的加载器驱动 onRefresh */
internal var donateReloader: (suspend () -> Unit)? = null

object DonatePage {

    val spec = ScopePageSpec(
        pageKey = "donate",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { donateReloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "donate_body") {
            when (LocalUiMode.current) {
                UiMode.Miuix -> DonateBodyMiuix(c)
                UiMode.Material -> DonateBodyMaterial(c)
            }
        }
    }
}
/** 简表：按用户聚合（Name/Money） */
internal fun formatUserInfo(
    jsonArray: JSONArray,
    markdownList: ArrayList<String>,
    otherCurrency: Boolean,
    sortMode: Int,
    isReverse: Boolean,
    filterString: String,
    develop: Boolean,
) {
    markdownList.add("| Name | Money |")
    markdownList.add("| :------: | :------: |")
    val userInfoList = ArrayList<DonateInfo>()
    var totalRmbCount = 0.0
    var totalOtherCount = 0.0
    var totalDetailCount = 0
    var totalOtherDetailCount = 0
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.optJSONObject(i) ?: continue
        val name = jsonObject.optString("name")
        val details = jsonObject.optJSONArray("details")
        var userRmbCount = 0.0
        var userOtherCount = 0.0
        var userDetailCount = 0
        var userOtherDetailCount = 0
        if (details != null) {
            for (j in 0 until details.length()) {
                val detailObject = details.optJSONObject(j) ?: continue
                val money = detailObject.optDouble("money")
                val unit = detailObject.optString("unit")
                if (unit == "RMB") {
                    userDetailCount++
                    userRmbCount += money
                    totalDetailCount++
                    totalRmbCount += money
                } else if (unit == "$") {
                    userOtherDetailCount++
                    userOtherCount += money
                    totalOtherDetailCount++
                    totalOtherCount += money
                }
            }
        }
        if (!otherCurrency && userRmbCount > 0) {
            userInfoList.add(DonateInfo(name, userRmbCount, userDetailCount))
        }
        if (otherCurrency && userOtherCount > 0) {
            userInfoList.add(DonateInfo(name, userOtherCount, userOtherDetailCount, "$"))
        }
    }
    when (sortMode) {
        1 -> userInfoList.sortBy { it.money }
        2 -> userInfoList.sortBy { it.details }
    }
    if (isReverse) userInfoList.reverse()
    if (develop && userInfoList.isNotEmpty()) {
        val formatRmb = DecimalFormat("0.00").format(totalRmbCount).toDouble()
        val formatOth = DecimalFormat("0.00").format(totalOtherCount).toDouble()
        if (formatRmb > 0) userInfoList.add(0, DonateInfo("develop", formatRmb, totalDetailCount))
        if (formatOth > 0) {
            userInfoList.add(1, DonateInfo("develop", formatOth, totalOtherDetailCount, "$"))
        }
    }
    for (info in userInfoList) {
        if (filterString.isBlank() || info.name.contains(filterString, true)) {
            markdownList.add("| ${info.name} | ${info.money} ${info.unit} |")
        }
    }
}

/** 详表：逐条明细（Name/Time/Money/Channel） */
internal fun formatUserDetailInfo(
    jsonArray: JSONArray,
    markdownList: ArrayList<String>,
    otherCurrency: Boolean,
    sortMode: Int,
    isReverse: Boolean,
    filterString: String,
    develop: Boolean,
) {
    markdownList.add("| Name | Time | Money | Channel |")
    markdownList.add("| :------: | :------: | :------: | :------: |")
    val userInfoList = ArrayList<DonateDetailInfo>()
    var totalRmbCount = 0.0
    var totalOtherCount = 0.0
    var totalDetailCount = 0
    var totalOtherDetailCount = 0
    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.optJSONObject(i) ?: continue
        val name = jsonObject.optString("name")
        val details = jsonObject.optJSONArray("details")
        if (details != null) {
            for (j in 0 until details.length()) {
                val detailObject = details.optJSONObject(j) ?: continue
                val time = detailObject.optString("time")
                val channel = detailObject.optString("channel")
                val money = detailObject.optDouble("money")
                val order = detailObject.optString("order")
                val unit = detailObject.optString("unit")
                if (unit == "RMB") {
                    totalDetailCount++
                    totalRmbCount += money
                    if (otherCurrency) continue
                } else if (unit == "$") {
                    totalOtherDetailCount++
                    totalOtherCount += money
                    if (!otherCurrency) continue
                }
                if (money > 0) {
                    userInfoList.add(DonateDetailInfo(name, time, channel, money, order, unit))
                }
            }
        }
    }
    if (sortMode == 1) userInfoList.sortBy { it.money }
    if (isReverse) userInfoList.reverse()
    if (develop) {
        val formatRmb = DecimalFormat("0.00").format(totalRmbCount).toDouble()
        val formatOth = DecimalFormat("0.00").format(totalOtherCount).toDouble()
        if (formatRmb > 0) userInfoList.add(0, DonateDetailInfo("develop", "all", "all", formatRmb, "null"))
        if (formatOth > 0) {
            userInfoList.add(1, DonateDetailInfo("develop", "all", "all", formatOth, "null", "$"))
        }
    }
    for (info in userInfoList) {
        if (filterString.isBlank() || info.name.contains(filterString)) {
            markdownList.add("| ${info.name} | ${info.time} | ${info.money} ${info.unit} | ${info.channel} |")
        }
    }
}
