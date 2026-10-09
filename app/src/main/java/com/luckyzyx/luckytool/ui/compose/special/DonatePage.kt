@file:OptIn(ExperimentalMaterial3Api::class)

package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drake.net.Get
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DonateDetailInfo
import com.luckyzyx.luckytool.data.DonateInfo
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveSwitch
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCheckboxItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AESCrypt
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.io.File
import java.text.DecimalFormat

/**
 * Donate 页（旧 ui.fragment.settings.DonateFragment 的 Compose 等价物）。
 * 捐赠数据经 GitHub API 检查更新、jsDelivr 镜像下载、AESCrypt 加密缓存后由
 * Markwon(+TablePlugin) 渲染为 Markdown 表格；筛选/排序选项在排序过滤弹层中。
 * 下拉刷新（onRefresh）重跑 initData 管线。
 *
 * 原「三文件制」已合并为单文件：数据管线唯一一份，渲染差异集中在
 * [DonateBody] 的 `if (miuix)` 分派上（搜索框复用 [AppSearchField]，排序过滤弹层
 * Material 走 FilterChip / Miuix 走 MiuixRadioItem + MiuixCheckboxItem）。
 */

internal val showDetailedKey = "show_detailed_donate_data"
internal val showOtherCurrencyKey = "show_other_currency_donate_data"
internal val lastUpdateKey = "last_update_dd_date"
internal val developKey = "hidden_function"
internal const val DONATE_DATA_URL =
    "https://api.github.com/repos/LuckyOSTeam/LuckyOSTeam.github.io/releases/tags/luckytool_donates"
// raw.gitmirror.com 域名已下线，改用 jsDelivr 镜像（国内可访问）
internal const val DONATE_JSON_URL =
    "https://cdn.jsdelivr.net/gh/LuckyOSTeam/LuckyOSTeam.github.io@main/LuckyTool/donate.json"

/** 备用下载镜像：主地址不可用时依次回退（同一文件的不同 CDN 节点） */
internal val DONATE_JSON_URLS = listOf(
    DONATE_JSON_URL,
    "https://fastly.jsdelivr.net/gh/LuckyOSTeam/LuckyOSTeam.github.io@main/LuckyTool/donate.json",
)

/**
 * 依次尝试各镜像下载 donate.json，返回第一个成功落盘的临时文件；
 * 全部失败返回 null（每个地址的失败原因记录日志，由调用方提示用户）。
 */
internal suspend fun CoroutineScope.downloadDonateJson(tempDir: File): File? {
    for (url in DONATE_JSON_URLS) {
        try {
            val file = Get<File>(url) {
                setDownloadDir(tempDir)
                setDownloadMd5Verify()
                setDownloadTempFile()
            }.await()
            if (file.exists()) return file
        } catch (e: Exception) {
            LogUtils.e("downloadDonateJson", url, e.toString(), true)
        }
    }
    return null
}

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
        custom(key = "donate_body") { DonateBody(c) }
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

/**
 * Donate 页渲染体：数据管线唯一一份（两线旧实现逐条等价），渲染差异集中在此：
 * 搜索框复用 [AppSearchField]（label 传入 "Name"）；排序过滤弹层 Material 线用
 * `ModalBottomSheet` + `FilterChip`，Miuix 线用 `OverlayBottomSheet` + `MiuixRadioItem`
 * 单选排序 + `MiuixCheckboxItem` 过滤 + `MiuixSwitchItem` 反转。
 */
@Composable
internal fun PrefScopeBuilder.DonateBody(c: Context) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val miuix = LocalUiMode.current == UiMode.Miuix

    var filterString by remember { mutableStateOf("") }
    var isReverse by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(0) }
    var showDetail by remember { mutableStateOf(state.getBoolean(showDetailedKey, false)) }
    var otherCurrency by remember { mutableStateOf(state.getBoolean(showOtherCurrencyKey, false)) }
    var markdown by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    val develop = state.getBoolean(developKey, false)

    // ---- 数据管线（对齐旧 DonateFragment；网络/文件 IO 放 IO 调度器） ----

    suspend fun loadJson(): String? {
        val dd = File(appContext.filesDir, "dd")
        val jsonObject = withContext(Dispatchers.IO) {
            safeOfNull {
                val jsonDecrypt = AESCrypt.decrypt(dd.readText())
                JSONObject(jsonDecrypt)
            }
        }
        if (jsonObject == null) {
            withContext(Dispatchers.IO) { dd.delete() }
            c.showToast(c.getString(R.string.donate_data_decode_error))
            return null
        }
        val markdownList = ArrayList<String>()
        val datas = jsonObject.optJSONArray("datas") ?: JSONArray()
        if (!showDetail) {
            formatUserInfo(datas, markdownList, otherCurrency, sortMode, isReverse, filterString, develop)
        } else {
            formatUserDetailInfo(datas, markdownList, otherCurrency, sortMode, isReverse, filterString, develop)
        }
        return formatStringAuto(markdownList, "\n")
    }

    suspend fun downloadJson(date: String) {
        try {
            val tempDir = File(appContext.cacheDir, "dTemp")
            val file = scope.downloadDonateJson(tempDir)
            if (file != null && file.exists()) {
                val jsonEncrypt = withContext(Dispatchers.IO) { AESCrypt.encrypt(file.readText()) }
                val dd = File(appContext.filesDir, "dd")
                withContext(Dispatchers.IO) {
                    dd.writeText(jsonEncrypt)
                    file.delete()
                }
                state.set(lastUpdateKey, date)
                loadJson()?.let { markdown = it }
            } else {
                c.showToast("Exception while download data!")
            }
        } catch (e: Exception) {
            c.showToast("Exception while download data!")
            LogUtils.e("downloadJson", "download", e.toString(), true)
        }
    }

    suspend fun checkDonateData() {
        try {
            val getJson = scope.Get<String>(DONATE_DATA_URL).await()
            val date = JSONObject(getJson).optString("name", "")
            if (date.isBlank()) return
            val lastUpdateDate = state.getString(lastUpdateKey, "null")
            if (date != lastUpdateDate) downloadJson(date)
            else loadJson()?.let { markdown = it }
        } catch (e: Exception) {
            c.showToast("Exception while checking data!")
            LogUtils.e("checkDonateData", "checking", e.toString(), true)
        }
    }

    suspend fun initData() {
        withContext(Dispatchers.IO) {
            val tempDir = File(appContext.cacheDir, "dTemp")
            if (tempDir.exists()) tempDir.delete()
        }
        val dd = File(appContext.filesDir, "dd")
        if (dd.exists()) checkDonateData()
        else downloadJson(formatDate("YYYYMMddHHmm"))
    }

    suspend fun reload() {
        ready = false
        initData()
        ready = true
    }
    donateReloader = ::reload
    DisposableEffect(Unit) {
        onDispose { donateReloader = null }
    }
    LaunchedEffect(Unit) { reload() }

    // ---- UI（旧布局顺序：搜索框在上，Markdown 内容在下） ----

    val rerunFilter: () -> Unit = {
        scope.launch { loadJson()?.let { markdown = it } }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        AppSearchField(
            query = filterString,
            enabled = ready,
            onQueryChange = {
                filterString = it
                rerunFilter()
            },
            onSortClick = { showSheet = true },
            modifier = Modifier.padding(bottom = 8.dp),
            label = "Name",
        )

        val markwon = remember {
            Markwon.builder(appContext).usePlugin(TablePlugin.create(appContext)).build()
        }
        AndroidView(
            factory = { ctx -> android.widget.TextView(ctx) },
            modifier = Modifier.fillMaxWidth(),
            update = { tv ->
                tv.setTextIsSelectable(true)
                if (markdown.isNotBlank()) markwon.setMarkdown(tv, markdown)
            },
        )
    }

    if (showSheet) {
        if (miuix) {
            OverlayBottomSheet(
                show = true,
                title = stringResource(R.string.appinfo_sort_and_filter),
                onDismissRequest = { showSheet = false },
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
                ) {
                    val sorts = remember(showDetail) {
                        val list = arrayListOf(
                            c.getString(R.string.donate_info_time),
                            c.getString(R.string.donate_info_money),
                            c.getString(R.string.donate_info_order),
                        )
                        if (showDetail) list.removeLastOrNull()
                        list
                    }
                    MiuixPrefCategoryHeader(c.getString(R.string.appinfo_sort_by))
                    sorts.forEachIndexed { index, title ->
                        MiuixRadioItem(
                            title = title,
                            selected = sortMode == index,
                            onClick = {
                                sortMode = index
                                rerunFilter()
                            },
                        )
                    }
                    MiuixSwitchItem(
                        title = c.getString(R.string.appinfo_reverse),
                        checked = isReverse,
                        onCheckedChange = {
                            isReverse = it
                            rerunFilter()
                        },
                    )
                    MiuixPrefCategoryHeader(c.getString(R.string.appinfo_filter))
                    MiuixCheckboxItem(
                        title = c.getString(R.string.donate_detailed_data),
                        checked = showDetail,
                        onCheckedChange = {
                            showDetail = it
                            state.set(showDetailedKey, it)
                            rerunFilter()
                        },
                    )
                    MiuixCheckboxItem(
                        title = c.getString(R.string.donate_other_currency),
                        checked = otherCurrency,
                        onCheckedChange = {
                            otherCurrency = it
                            state.set(showOtherCurrencyKey, it)
                            rerunFilter()
                        },
                    )
                }
            }
        } else {
            ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
                ) {
                    Text(
                        c.getString(R.string.appinfo_sort_and_filter),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(c.getString(R.string.appinfo_sort_by), Modifier.weight(1f))
                        Text(c.getString(R.string.appinfo_reverse))
                        ExpressiveSwitch(
                            checked = isReverse,
                            onCheckedChange = {
                                isReverse = it
                                rerunFilter()
                            },
                        )
                    }
                    val sorts = remember(showDetail) {
                        val list = arrayListOf(
                            c.getString(R.string.donate_info_time),
                            c.getString(R.string.donate_info_money),
                            c.getString(R.string.donate_info_order),
                        )
                        if (showDetail) list.removeLastOrNull()
                        list
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        sorts.forEachIndexed { index, title ->
                            FilterChip(
                                selected = sortMode == index,
                                onClick = {
                                    sortMode = index
                                    rerunFilter()
                                },
                                label = { Text(title) },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(c.getString(R.string.appinfo_filter))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = showDetail,
                            onClick = {
                                showDetail = !showDetail
                                state.set(showDetailedKey, showDetail)
                                rerunFilter()
                            },
                            label = { Text(c.getString(R.string.donate_detailed_data)) },
                        )
                        FilterChip(
                            selected = otherCurrency,
                            onClick = {
                                otherCurrency = !otherCurrency
                                state.set(showOtherCurrencyKey, otherCurrency)
                                rerunFilter()
                            },
                            label = { Text(c.getString(R.string.donate_other_currency)) },
                        )
                    }
                }
            }
        }
    }
}
