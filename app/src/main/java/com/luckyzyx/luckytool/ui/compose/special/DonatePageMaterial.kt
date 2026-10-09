@file:OptIn(ExperimentalMaterial3Api::class)

package com.luckyzyx.luckytool.ui.compose.special
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.drake.net.Get
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DonateDetailInfo
import com.luckyzyx.luckytool.data.DonateInfo
import com.luckyzyx.luckytool.ui.compose.components.material.ExpressiveSwitch
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat
import android.content.Context
import androidx.compose.runtime.Composable
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder

/** Material 线渲染体（自 DonatePage.kt 逐字搬运，仅接收者/可见性/缩进调整）。 */
@Composable
internal fun PrefScopeBuilder.DonateBodyMaterial(c: Context) {
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
