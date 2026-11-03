package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
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
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import io.noties.markwon.Markwon
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
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
    val version = "${stringResource(R.string.version_name)}: ${info.name}(${info.code})"
    val count = "${stringResource(R.string.download_count)}: ${info.downloadCount}"
    val size = "${stringResource(R.string.file_size)}: ${formatFileSize(info.fileSize)}"
    val time = "${stringResource(R.string.update_time)}: ${info.updateTime}"
    val finalText =
        "# LuckyTool v${info.name}\n- $version\n- $count\n- $size\n- $time\n${info.changeLog}"
    val markwon = remember(context) { Markwon.create(context) }
    when (LocalUiMode.current) {
        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(R.string.check_update_hint),
            onDismissRequest = { if (isDev) onDismiss() },
        ) {
            AndroidView(
                factory = { ctx -> android.widget.TextView(ctx) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                update = { tv ->
                    tv.setTextIsSelectable(true)
                    markwon.setMarkdown(tv, finalText)
                },
            )
            Row(modifier = Modifier.padding(top = 12.dp)) {
                MiuixTextButton(
                    text = stringResource(R.string.go_download_page),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, info.downloadPage.toUri()))
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                MiuixTextButton(
                    text = stringResource(R.string.direct_update),
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        UiMode.Material -> AlertDialog(
            onDismissRequest = { if (isDev) onDismiss() },
            title = { Text(stringResource(R.string.check_update_hint)) },
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
                TextButton(onClick = onDownload) { Text(stringResource(R.string.direct_update)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, info.downloadPage.toUri()))
                }) { Text(stringResource(R.string.go_download_page)) }
            }
        )
    }
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
    // key 直接用下载地址：六个源的地址前缀互不相同，天然唯一
    val items = sources.map { (name, url) -> ChoiceItem(key = url, title = name) }
    ActionListDialog(
        title = stringResource(R.string.select_download_source),
        items = items,
        // release 版不允许关闭（与原实现一致）：连「取消」也不画，否则会出现点了没反应的按钮
        onDismiss = { if (isDev) onDismiss() },
        onSelect = { item ->
            // 只回调 onSelect：调用方在 onSelect 里切 UpdateStage 让对话框自然消失，
            // 这里再补一次 onDismiss 会把刚发起的更新取消掉
            onSelect(item.key)
        },
        showCancel = isDev,
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
    when (LocalUiMode.current) {
        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(R.string.downloaded),
            onDismissRequest = { if (isDev) onDismiss() },
        ) {
            MiuixText(
                text = "${apkFile.name}\n$size",
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            Row {
                MiuixTextButton(
                    text = stringResource(R.string.download_again),
                    onClick = onDownloadAgain,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                MiuixTextButton(
                    text = stringResource(R.string.install),
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }

        UiMode.Material -> AlertDialog(
            onDismissRequest = { if (isDev) onDismiss() },
            title = { Text(stringResource(R.string.downloaded)) },
            text = { Text("${apkFile.name}\n$size") },
            confirmButton = {
                TextButton(onClick = onInstall) { Text(stringResource(R.string.install)) }
            },
            dismissButton = {
                TextButton(onClick = onDownloadAgain) {
                    Text(stringResource(R.string.download_again))
                }
            }
        )
    }
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
    // 下载进度回调内没有 Composer，标签先在这里取一次
    val downloadProgressLabel = stringResource(R.string.download_progress)
    val downloadSpeedLabel = stringResource(R.string.download_speed)
    val remainSizeLabel = stringResource(R.string.remain_size)
    val downloadedLabel = stringResource(R.string.downloaded)
    val usedTimeLabel = stringResource(R.string.used_time)
    val remainTimeLabel = stringResource(R.string.remain_time)
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
                            $downloadProgressLabel: $ps%
                            $downloadSpeedLabel: ${p.speedSize()}
                            $remainSizeLabel: ${p.remainSize()}
                            $downloadedLabel: ${p.currentSize()} / ${p.totalSize()}
                            $usedTimeLabel: ${p.useTime()}
                            $remainTimeLabel: ${p.remainTime()}
                        """.trimIndent()
                    }
                })
            }.await()
            UpdateUtils(context).installApk(downFile)
            onDismiss()
        }
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(R.string.downloading),
            onDismissRequest = {},
        ) {
            Column {
                MiuixText(text = progressText)
                MiuixTextButton(
                    text = stringResource(R.string.cancel_button),
                    onClick = {
                        downloadScope?.cancel()
                        apkFile.delete()
                        onDismiss()
                    },
                )
            }
        }

        UiMode.Material -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.downloading)) },
            text = {
                Column {
                    Text(progressText)
                    TextButton(onClick = {
                        downloadScope?.cancel()
                        apkFile.delete()
                        onDismiss()
                    }) { Text(stringResource(R.string.cancel_button)) }
                }
            },
            confirmButton = {}
        )
    }
}
