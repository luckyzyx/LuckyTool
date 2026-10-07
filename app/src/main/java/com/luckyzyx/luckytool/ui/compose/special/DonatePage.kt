@file:OptIn(ExperimentalMaterial3Api::class)

package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drake.net.Get
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DonateDetailInfo
import com.luckyzyx.luckytool.data.DonateInfo
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AESCrypt
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat

/**
 * Donate 页（旧 ui.fragment.settings.DonateFragment 的 Compose 等价物）。
 * 捐赠数据经 GitHub API 检查更新、GitMirror 下载、AESCrypt 加密缓存后由
 * 自绘 Compose 表格渲染（替代 Markwon+TablePlugin）；筛选/排序选项在 ModalBottomSheet 中。
 * 下拉刷新（onRefresh）重跑 initData 管线。
 */
object DonatePage {

    private val showDetailedKey = "show_detailed_donate_data"
    private val showOtherCurrencyKey = "show_other_currency_donate_data"
    private val lastUpdateKey = "last_update_dd_date"
    private val developKey = "hidden_function"
    private const val DONATE_DATA_URL =
        "https://api.github.com/repos/LuckyOSTeam/LuckyOSTeam.github.io/releases/tags/luckytool_donates"
    private const val DONATE_JSON_URL =
        "https://raw.gitmirror.com/LuckyOSTeam/LuckyOSTeam.github.io/main/LuckyTool/donate.json"

    /** 由内容组合时注册的加载器驱动 onRefresh */
    private var reloader: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "donate",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { reloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "donate_body") {
            val appContext = LocalContext.current.applicationContext
            val scope = rememberCoroutineScope()

            var filterString by remember { mutableStateOf("") }
            var isReverse by remember { mutableStateOf(false) }
            var sortMode by remember { mutableStateOf(0) }
            var showDetail by remember { mutableStateOf(state.getBoolean(showDetailedKey, false)) }
            var otherCurrency by remember { mutableStateOf(state.getBoolean(showOtherCurrencyKey, false)) }
            var tableData by remember { mutableStateOf<List<List<String>>>(emptyList()) }
            var ready by remember { mutableStateOf(false) }
            var showSheet by remember { mutableStateOf(false) }

            val develop = state.getBoolean(developKey, false)

            // ---- 数据管线（对齐旧 DonateFragment；网络/文件 IO 放 IO 调度器） ----

            suspend fun loadJson(): List<List<String>>? {
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
                val tableRows = ArrayList<List<String>>()
                val datas = jsonObject.optJSONArray("datas") ?: JSONArray()
                if (!showDetail) {
                    formatUserInfo(datas, tableRows, otherCurrency, sortMode, isReverse, filterString, develop)
                } else {
                    formatUserDetailInfo(datas, tableRows, otherCurrency, sortMode, isReverse, filterString, develop)
                }
                return tableRows
            }

            suspend fun downloadJson(date: String) {
                try {
                    val tempDir = File(appContext.cacheDir, "dTemp")
                    val file = scope.Get<File>(DONATE_JSON_URL) {
                        setDownloadDir(tempDir)
                        setDownloadMd5Verify()
                        setDownloadTempFile()
                    }.await()
                    if (file.exists()) {
                        val jsonEncrypt = withContext(Dispatchers.IO) { AESCrypt.encrypt(file.readText()) }
                        val dd = File(appContext.filesDir, "dd")
                        withContext(Dispatchers.IO) {
                            dd.writeText(jsonEncrypt)
                            file.delete()
                        }
                        state.set(lastUpdateKey, date)
                        loadJson()?.let { tableData = it }
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
                    else loadJson()?.let { tableData = it }
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
            reloader = ::reload
            DisposableEffect(Unit) {
                onDispose { reloader = null }
            }
            LaunchedEffect(Unit) { reload() }

            // ---- UI（旧布局顺序：搜索框在上，Markdown 内容在下） ----

            val rerunFilter: () -> Unit = {
                scope.launch { loadJson()?.let { tableData = it } }
            }

            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = filterString,
                    onValueChange = {
                        filterString = it
                        rerunFilter()
                    },
                    enabled = ready,
                    singleLine = true,
                    placeholder = { Text("Name") },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
                    },
                    trailingIcon = {
                        Icon(
                            painterResource(R.drawable.baseline_filter_list_24),
                            contentDescription = null,
                            modifier = Modifier.clickable { showSheet = true },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                if (tableData.isNotEmpty()) {
                    DonateTable(
                        rows = tableData,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }

            if (showSheet) {
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
                            Switch(
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

    /** 简表：按用户聚合（Name/Money） */
    private fun formatUserInfo(
        jsonArray: JSONArray,
        rows: ArrayList<List<String>>,
        otherCurrency: Boolean,
        sortMode: Int,
        isReverse: Boolean,
        filterString: String,
        develop: Boolean,
    ) {
        rows.add(listOf("Name", "Money"))
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
                rows.add(listOf(info.name, "${info.money} ${info.unit}"))
            }
        }
    }

    /** 详表：逐条明细（Name/Time/Money/Channel） */
    private fun formatUserDetailInfo(
        jsonArray: JSONArray,
        rows: ArrayList<List<String>>,
        otherCurrency: Boolean,
        sortMode: Int,
        isReverse: Boolean,
        filterString: String,
        develop: Boolean,
    ) {
        rows.add(listOf("Name", "Time", "Money", "Channel"))
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
                rows.add(listOf(info.name, info.time, "${info.money} ${info.unit}", info.channel))
            }
        }
    }

    /** 简易表格渲染（旧 Markwon TablePlugin 的 Compose 等价物）；列宽按列数预设，表头加粗居中 */
    @Composable
    private fun DonateTable(rows: List<List<String>>, modifier: Modifier = Modifier) {
        val columnWidths = when (rows.firstOrNull()?.size) {
            4 -> listOf(110.dp, 140.dp, 96.dp, 110.dp)
            else -> listOf(150.dp, 110.dp)
        }
        Column(modifier.horizontalScroll(rememberScrollState())) {
            rows.forEachIndexed { rowIndex, row ->
                val isHeader = rowIndex == 0
                Row {
                    row.forEachIndexed { colIndex, cell ->
                        Text(
                            text = cell,
                            textAlign = if (isHeader) TextAlign.Center else TextAlign.Start,
                            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(columnWidths.getOrElse(colIndex) { 110.dp })
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                }
                if (isHeader) HorizontalDivider()
            }
        }
    }
}
