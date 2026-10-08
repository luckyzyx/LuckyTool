package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drake.net.Get
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixCheckboxItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixPrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.utils.AESCrypt
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.io.File

/**
 * Donate 页 Miuix 线渲染体（t21）：数据管线与 Material 线逐条等价
 * （GitHub 检查更新 / GitMirror 下载 / AESCrypt 缓存 / Markwon 渲染 Markdown 表格），
 * 仅把搜索框与排序过滤面板换成 miuix 件：库无 Chip → 排序用 MiuixRadioItem 单选、
 * 两个过滤项用 MiuixCheckboxItem；分隔线改用 MiuixPrefCategoryHeader 分类标题。
 */
@Composable
internal fun PrefScopeBuilder.DonateBodyMiuix(c: Context) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

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
                loadJson()?.let { markdown = it }
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
        TextField(
            value = filterString,
            onValueChange = {
                filterString = it
                rerunFilter()
            },
            enabled = ready,
            singleLine = true,
            label = "Name",
            useLabelAsPlaceholder = true,
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_baseline_search_24), contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { showSheet = true }) {
                    Icon(painterResource(R.drawable.baseline_filter_list_24), contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
    }
}
