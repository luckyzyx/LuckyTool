package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drake.net.Get
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DonateDetailInfo
import com.luckyzyx.luckytool.data.DonateInfo
import com.luckyzyx.luckytool.ui.compose.components.AppSearchBar
import com.luckyzyx.luckytool.ui.compose.components.currentPrefStyle
import com.luckyzyx.luckytool.ui.compose.components.pageInset
import com.luckyzyx.luckytool.ui.compose.components.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
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
import java.io.File
import java.text.DecimalFormat

/**
 * Donate 页：捐赠名单展示页（搜索、排序过滤、下拉刷新）。
 * 捐赠数据经 GitHub API 检查更新、jsDelivr 镜像下载、AESCrypt 加密缓存后由
 * Markwon(+TablePlugin) 渲染为 Markdown 表格；筛选/排序选项在排序过滤弹层中。
 * 下拉刷新（onRefresh）重跑 initData 管线。
 *
 * 数据管线与渲染都集中在 [DonateBody]：搜索框复用 [AppSearchBar]，
 * 排序过滤弹层的形态由样式位决定。
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
 * Donate 页渲染体：数据管线与渲染都在此。
 * 搜索框复用 [AppSearchBar]（label 传入 "Name"）；排序过滤弹层的内容在
 * [DonateSortSheet]，外壳与控件由样式位提供。
 */
@Composable
internal fun PrefScopeBuilder.DonateBody(c: Context) {
    val appContext = LocalContext.current.applicationContext
    // 资源文本提到 composable 顶层，避免在普通函数里经 LocalContext 取串
    val decodeErrorText = stringResource(R.string.donate_data_decode_error)
    val scope = rememberCoroutineScope()

    var filterString by remember { mutableStateOf("") }
    var isReverse by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(0) }
    var showDetail by remember { mutableStateOf(state.getBoolean(showDetailedKey, false)) }
    var otherCurrency by remember { mutableStateOf(state.getBoolean(showOtherCurrencyKey, false)) }
    var markdown by remember { mutableStateOf("") }
    var ready by remember { mutableStateOf(false) }
    val showSheet = remember { mutableStateOf(false) }

    val develop = state.getBoolean(developKey, false)

    // ---- 数据管线（网络/文件 IO 放 IO 调度器） ----

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
            c.showToast(decodeErrorText)
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

    // ---- UI（搜索框在上，Markdown 内容在下） ----

    val rerunFilter: () -> Unit = {
        scope.launch { loadJson()?.let { markdown = it } }
    }

    // 两条线共用同一次搜索回调：改词后立即按新词重算 markdown
    val onQueryChange: (String) -> Unit = {
        filterString = it
        rerunFilter()
    }

    Column(Modifier.fillMaxWidth().pageInset().padding(vertical = 8.dp)) {
        AppSearchBar(
            query = filterString,
            onQueryChange = onQueryChange,
            placeholder = "Name",
            modifier = Modifier.padding(bottom = 8.dp),
            enabled = ready,
            filterActive = showSheet.value,
            onFilterClick = { showSheet.value = true },
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

    // 排序项在 remember 计算块里组装（那里没有 Composer），字符串先在这里取一次
    val sortTimeText = stringResource(R.string.donate_info_time)
    val sortMoneyText = stringResource(R.string.donate_info_money)
    val sortOrderText = stringResource(R.string.donate_info_order)
    val sorts = remember(showDetail, sortTimeText, sortMoneyText, sortOrderText) {
        val list = arrayListOf(
            sortTimeText,
            sortMoneyText,
            sortOrderText,
        )
        if (showDetail) list.removeLastOrNull()
        list
    }
    // 弹层参数只在这里建一次，状态与回调都由页面持有
    val sheet = DonateSortSheetModel(
        sorts = sorts,
        sortMode = sortMode,
        reverse = isReverse,
        showDetail = showDetail,
        otherCurrency = otherCurrency,
        visible = showSheet,
        onSortModeChange = { mode ->
            sortMode = mode
            rerunFilter()
        },
        onReverseChange = {
            isReverse = it
            rerunFilter()
        },
        onToggleDetail = {
            showDetail = !showDetail
            state.set(showDetailedKey, showDetail)
            rerunFilter()
        },
        onToggleOtherCurrency = {
            otherCurrency = !otherCurrency
            state.set(showOtherCurrencyKey, otherCurrency)
            rerunFilter()
        },
    )
    DonateSortSheet(sheet)
}

/**
 * 排序过滤弹层的渲染参数：排序项、勾选态与回调都在页面里持有，
 * 弹层自身不缓存任何状态，只把这些字段摆出来。
 */
private class DonateSortSheetModel(
    val sorts: List<String>,
    val sortMode: Int,
    val reverse: Boolean,
    val showDetail: Boolean,
    val otherCurrency: Boolean,
    val visible: MutableState<Boolean>,
    val onSortModeChange: (Int) -> Unit,
    val onReverseChange: (Boolean) -> Unit,
    val onToggleDetail: () -> Unit,
    val onToggleOtherCurrency: () -> Unit,
)

/**
 * 排序过滤弹层内容：外壳、独立开关、筛选片与分隔线交给样式位，这里只排内容。
 *
 * 排序项与筛选项都折行排，项数变化时不会被挤出屏幕外。开关与筛选片即时生效，
 * 这里只上报操作，状态由页面持有并据此重算 markdown。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DonateSortSheet(sheet: DonateSortSheetModel) {
    currentPrefStyle().Sheet(
        visible = sheet.visible,
        title = stringResource(R.string.appinfo_sort_and_filter),
        modifier = Modifier,
        closeLabel = stringResource(R.string.close),
        doneLabel = stringResource(R.string.sheet_done),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            currentPrefStyle().Title(
                text = stringResource(R.string.appinfo_sort_by),
                enabled = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 撑开标题后面的空隙，把开关推到行尾
            Spacer(Modifier.weight(1f))
            currentPrefStyle().Title(
                text = stringResource(R.string.appinfo_reverse),
                enabled = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            currentPrefStyle().Switch(
                checked = sheet.reverse,
                onCheckedChange = sheet.onReverseChange,
                modifier = Modifier.padding(start = 8.dp),
                enabled = true,
            )
        }
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sheet.sorts.forEachIndexed { index, title ->
                currentPrefStyle().FilterChip(
                    label = title,
                    selected = sheet.sortMode == index,
                    onClick = { sheet.onSortModeChange(index) },
                    modifier = Modifier,
                    enabled = true,
                )
            }
        }
        currentPrefStyle().Divider(Modifier.padding(vertical = 8.dp))
        currentPrefStyle().Title(
            text = stringResource(R.string.appinfo_filter),
            enabled = true,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            currentPrefStyle().FilterChip(
                label = stringResource(R.string.donate_detailed_data),
                selected = sheet.showDetail,
                onClick = sheet.onToggleDetail,
                modifier = Modifier,
                enabled = true,
            )
            currentPrefStyle().FilterChip(
                label = stringResource(R.string.donate_other_currency),
                selected = sheet.otherCurrency,
                onClick = sheet.onToggleOtherCurrency,
                modifier = Modifier,
                enabled = true,
            )
        }
    }
}
