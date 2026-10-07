package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.drake.net.Get
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.scope.NetCoroutineScope
import com.drake.net.utils.scopeNet
import com.highcapable.betterandroid.ui.extension.view.toast
import com.luckyzyx.luckytool.R
import io.noties.markwon.Markwon
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
class UpdateUtils(val context: Context, private val isDev: Boolean = false) {

    @Suppress("unused")
    val coolmarketUrl =
        "https://dl.coolapk.com/down?pn=com.coolapk.market&id=NDU5OQ&h=46bb9d98&from=from-web"

    data class UpdateInfo(
        val name: String,
        val code: Int,
        val changeLog: String,
        val fileName: String,
        val downloadUrl: String,
        val downloadPage: String,
        val downloadCount: String,
        val fileSize: Float,
        val updateTime: String
    )

    fun checkUpdate(result: (UpdateInfo) -> Unit) {
        scopeNet {
            val latestUrl =
                "https://api.github.com/repos/Xposed-Modules-Repo/com.luckyzyx.luckytool/releases/latest"
            val getJson = Get<String>(latestUrl).await()
            JSONObject(getJson).apply {
                val name = optString("name")
                val code = optString("tag_name").split("-")[0]
                val changeLog = optString("body")
                val assets = optJSONArray("assets") ?: JSONArray()
                val updateTime = optString("published_at").replace("T", " ").replace("Z", "")
                val firstFile = assets.optJSONObject(0) ?: JSONObject()
                val fileName = firstFile.optString("name")
                val downloadUrl = firstFile.optString("browser_download_url")
                val downloadPage = optString("html_url")
                val downloadCount = firstFile.optString("download_count")
                val fileSize = firstFile.optString("size").toFloat()

                result(
                    UpdateInfo(
                        name, code.toInt(), changeLog, fileName, downloadUrl,
                        downloadPage, downloadCount, fileSize, updateTime
                    )
                )
            }
        }.catch {
            it.printStackTrace()
            context.showToast(context.getString(R.string.check_update_error))
        }
    }

    /**
     * 准备下载文件路径
     */
    fun prepareApkFile(fileName: String): File =
        File(FileUtils.checkDownloadDir(context, "LuckyTool"), fileName).apply {
            if (isDirectory) delete()
        }

    /**
     * 下载源列表（Github 直链 + 5 个镜像加速）
     */
    fun downloadSources(downloadUrl: String): List<Pair<String, String>> = listOf(
        "Github" to downloadUrl,
        "Ucdn" to "https://wget.la/$downloadUrl",
        "Catmak" to "https://gh.catmak.name/$downloadUrl",
        "Fastly" to "https://cdn.gh-proxy.org/$downloadUrl",
        "JSDelivr" to "https://fastly.jsdelivr.net/gh/$downloadUrl",
        "FastGit" to "https://fastgit.cc/$downloadUrl"
    )

    fun installApk(apkFile: File) {
        if (context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apkFile)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            context.startActivity(intent)
        } else {
            context.showToast(context.getString(R.string.install_apk_toast))
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
        }
    }
}

/**
 * 更新日志 Compose 对话框
 */
@Composable
fun UpdateChangelogDialog(
    context: Context,
    info: UpdateUtils.UpdateInfo,
    isDev: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val version = "${context.getString(R.string.version_name)}: ${info.name}(${info.code})"
    val count = "${context.getString(R.string.download_count)}: ${info.downloadCount}"
    val size = "${context.getString(R.string.file_size)}: ${formatFileSize(info.fileSize)}"
    val time = "${context.getString(R.string.update_time)}: ${info.updateTime}"
    val finalText =
        "# LuckyTool v${info.name}\n- $version\n- $count\n- $size\n- $time\n${info.changeLog}"
    val markwon = remember(context) { Markwon.create(context) }
    AlertDialog(
        onDismissRequest = { if (isDev) onDismiss() },
        title = { Text(context.getString(R.string.check_update_hint)) },
        text = {
            AndroidView(
                factory = { ctx -> android.widget.TextView(ctx) },
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                update = { tv ->
                    tv.setTextIsSelectable(true)
                    markwon.setMarkdown(tv, finalText)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text(context.getString(R.string.direct_update)) }
        },
        dismissButton = {
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, info.downloadPage.toUri()))
            }) { Text(context.getString(R.string.go_download_page)) }
        }
    )
}

/**
 * 下载源选择 Compose 对话框
 */
@Composable
fun UpdateDownloadSourceDialog(
    context: Context,
    downloadUrl: String,
    isDev: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sources = remember(downloadUrl) { UpdateUtils(context).downloadSources(downloadUrl) }
    AlertDialog(
        onDismissRequest = { if (isDev) onDismiss() },
        title = { Text(context.getString(R.string.select_download_source)) },
        text = {
            Column {
                sources.forEach { (name, url) ->
                    Text(
                        name,
                        Modifier.fillMaxWidth().clickable { onSelect(url) }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * 已下载安装 Compose 对话框
 */
@Composable
fun UpdateDownloadedDialog(
    context: Context,
    apkFile: File,
    isDev: Boolean,
    onDismiss: () -> Unit,
    onDownloadAgain: () -> Unit,
    onInstall: () -> Unit
) {
    val size = formatFileSize(FileUtils.getFileSize(apkFile).toFloat())
    AlertDialog(
        onDismissRequest = { if (isDev) onDismiss() },
        title = { Text(context.getString(R.string.downloaded)) },
        text = { Text("${apkFile.name}\n$size") },
        confirmButton = {
            TextButton(onClick = onInstall) { Text(context.getString(R.string.install)) }
        },
        dismissButton = {
            TextButton(onClick = onDownloadAgain) {
                Text(context.getString(R.string.download_again))
            }
        }
    )
}

/**
 * 下载进度 Compose 对话框（完成后自动安装）
 */
@Composable
fun UpdateDownloadProgressDialog(
    context: Context,
    apkFile: File,
    url: String,
    onDismiss: () -> Unit
) {
    var progressText by remember { mutableStateOf("") }
    var downloadScope by remember { mutableStateOf<NetCoroutineScope?>(null) }
    LaunchedEffect(Unit) {
        downloadScope = scopeNet {
            if (apkFile.exists()) {
                UpdateUtils(context).installApk(apkFile)
                onDismiss()
                return@scopeNet
            }
            val downFile = Get<File>(url) {
                setDownloadDir(apkFile)
                setDownloadMd5Verify()
                addDownloadListener(object : ProgressListener(100) {
                    override fun onProgress(p: Progress) {
                        val ps = p.progress()
                        progressText = """
                            ${context.getString(R.string.download_progress)}: $ps%
                            ${context.getString(R.string.download_speed)}: ${p.speedSize()}
                            ${context.getString(R.string.remain_size)}: ${p.remainSize()}
                            ${context.getString(R.string.downloaded)}: ${p.currentSize()} / ${p.totalSize()}
                            ${context.getString(R.string.used_time)}: ${p.useTime()}
                            ${context.getString(R.string.remain_time)}: ${p.remainTime()}
                        """.trimIndent()
                    }
                })
            }.await()
            UpdateUtils(context).installApk(downFile)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(context.getString(R.string.downloading)) },
        text = {
            Column {
                Text(progressText)
                TextButton(onClick = {
                    downloadScope?.cancel()
                    apkFile.delete()
                    onDismiss()
                }) { Text(context.getString(R.string.cancel_button)) }
            }
        },
        confirmButton = {}
    )
}
