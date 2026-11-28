package com.luckyzyx.luckytool.ui.compose.special

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.database.getStringOrNull
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.PrefButton
import com.luckyzyx.luckytool.ui.compose.components.PrefNote
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.AESCrypt
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.FileUtils.cacheChild
import com.luckyzyx.luckytool.utils.SQLiteUtils
import com.luckyzyx.luckytool.utils.SQLiteUtils.getTableData
import com.luckyzyx.luckytool.utils.SQLiteUtils.readOnly
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.copyStr
import com.luckyzyx.luckytool.utils.formatFileSize
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.getFingerPrintModel
import com.luckyzyx.luckytool.utils.getModelMarketName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.safeOfNull
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ExtractOTA 页：读出系统 OTA 数据库（ota.db 的 pkgList 表）里各应用包的 OTA 信息并列出。
 * 无 Xposed 作用域（packName="" / scopes=arrayOf()），数据仅本地读取展示。
 * 下拉刷新（onRefresh）重跑加载流程。
 * 页内文字与按钮交给行层的 [PrefNote] / [PrefButton]，两条外观线各自取对应的字号、配色与按钮形态。
 */
@SuppressLint("SdCardPath")
object ExtractOTAPage {

    /** 由内容组合时注册的加载器驱动 onRefresh（suspend 回调在内容重组时重绑定） */
    private var reloader: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "extract_ota",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { reloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "extract_ota_body") {
            var dataList by remember { mutableStateOf<List<String>>(emptyList()) }

            suspend fun load(): List<String> = withContext(Dispatchers.IO) {
                try {
                    val command =
                        "${CommandUtils.cp} ${CommandUtils.otaDatabasePath} ${c.cacheDir}"
                    ShellUtils.fastCmd(command)
                    val dbFile = c.cacheChild("ota.db")
                    val cursor = if (dbFile.exists()) {
                        SQLiteUtils.openDataBase(dbFile.path, readOnly)?.getTableData("pkgList")
                    } else null

                    val list = ArrayList<String>()
                    if (cursor != null) {
                        val packNameIndex = cursor.getColumnIndex("package_name")
                        val sizeIndex = cursor.getColumnIndex("size")
                        val md5Index = cursor.getColumnIndex("md5")
                        val activeUrlIndex = cursor.getColumnIndex("active_url")
                        val urlIndex = cursor.getColumnIndex("url")

                        val otaList = ArrayList<String>()
                        while (cursor.moveToNext()) {
                            val packName = cursor.getStringOrNull(packNameIndex) ?: continue
                            val size = cursor.getStringOrNull(sizeIndex) ?: ""
                            val md5 = cursor.getStringOrNull(md5Index) ?: ""
                            val activeUrl = cursor.getStringOrNull(activeUrlIndex) ?: ""
                            val url = cursor.getStringOrNull(urlIndex) ?: ""

                            otaList.add(packName)
                            if (activeUrl.isNotBlank()) otaList.add("ActiveUrl: $activeUrl")
                            if (url.isNotBlank()) otaList.add("Url: $url")
                            if (md5.isNotBlank()) otaList.add("MD5: $md5")
                            if (size.isNotBlank()) {
                                otaList.add("Size: ${formatFileSize(size.toFloatOrNull())} ($size)")
                            }
                            otaList.add("")
                        }

                        if (otaList.isNotEmpty()) {
                            list.add("Model: ${getModelMarketName()} $getFingerPrintModel")
                            list.add("")
                            list.addAll(otaList)

                            val data = DeviceUtils.getOTACOnfigs()
                            val encrypt = safeOfNull {
                                AESCrypt.encrypt(data, CommandUtils.otaCryptKey, true)
                            } ?: ""
                            if (encrypt.isNotBlank()) list.add("Verity: $encrypt")
                            list.add("Source: #LuckyToolOTA")
                        }
                    }
                    list
                } catch (_: Exception) {
                    emptyList()
                }
            }

            suspend fun reload() {
                dataList = load()
            }
            reloader = ::reload
            DisposableEffect(Unit) {
                onDispose { reloader = null }
            }
            LaunchedEffect(Unit) { reload() }

            val noDataText = stringResource(R.string.no_ota_data)
            val copyText = stringResource(android.R.string.copy)
            val tipsText = if (isZh(c)) "使用此功能时,禁止删除与遗漏数据" else ""
            val fullText = if (dataList.isNotEmpty()) formatStringAuto(dataList, "\n") else ""

            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                // 页内文字统一走 PrefNote：字号、配色按外观线分派，这里只声明对齐与留白
                if (tipsText.isNotBlank()) {
                    PrefNote(
                        text = tipsText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                if (fullText.isNotBlank()) {
                    PrefNote(
                        text = fullText,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Start,
                    )
                } else {
                    PrefNote(
                        text = noDataText,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                // 复制动作是页面级主操作，交给 PrefButton：COUI 线 accent 胶囊，material 线 M3 填充按钮
                PrefButton(
                    text = copyText,
                    onClick = { c.copyStr(fullText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = fullText.isNotBlank(),
                )
            }
        }
    }
}
