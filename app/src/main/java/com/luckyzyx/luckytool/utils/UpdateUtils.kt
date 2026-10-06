package com.luckyzyx.luckytool.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.drake.net.Get
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.scope.NetCoroutineScope
import com.drake.net.utils.scopeNet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.hikage.extension.setView
import com.highcapable.hikage.widget.androidx.core.widget.NestedScrollView
import com.highcapable.hikage.widget.com.google.android.material.textview.MaterialTextView
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.databinding.DialogDownloadLayoutBinding
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

    @SuppressLint("SetTextI18n")
    fun checkUpdate(result: (String, Int, () -> Unit) -> Unit) {
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

                result(name, code.toInt()) {
                    MaterialAlertDialogBuilder(context, dialogCentered).apply {
                        setTitle(context.getString(R.string.check_update_hint))
                        setCancelable(isDev)
                        setView {
                            NestedScrollView {
                                MaterialTextView(
                                    lparams = LayoutParams(widthMatchParent = true),
                                    init = {
                                        updatePadding(horizontal = 20.dp)
                                        val version =
                                            "${context.getString(R.string.version_name)}: $name($code)"
                                        val count =
                                            "${context.getString(R.string.download_count)}: $downloadCount"
                                        val size = "${context.getString(R.string.file_size)}: " +
                                                formatFileSize(fileSize)
                                        val time =
                                            "${context.getString(R.string.update_time)}: $updateTime"
                                        val finalText =
                                            "# LuckyTool v$name\r\n- $version\r\n- $count\r\n- $size\r\n- $time\r\n$changeLog"
                                        Markwon.create(context).setMarkdown(this, finalText)
                                    }
                                )
                            }
                        }
                        setNeutralButton(context.getString(R.string.go_download_page)) { _, _ ->
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, downloadPage.toUri())
                            )
                        }
                        setPositiveButton(context.getString(R.string.direct_update)) { _, _ ->
                            readyDownload(context, fileName, downloadUrl)
                        }
                        show()
                    }
                }
            }
        }.catch {
            it.printStackTrace()
            context.showToast(context.getString(R.string.check_update_error))
        }
    }

    private fun showDownloadItems(context: Context, apkFile: File, downloadUrl: String) {
        val items = arrayListOf("Github")
        val urls = arrayListOf(downloadUrl)
        mapOf(
            "Ucdn" to "https://wget.la/",
            "Catmak" to "https://gh.catmak.name/",
            "Fastly" to "https://cdn.gh-proxy.org/",
            "JSDelivr" to "https://fastly.jsdelivr.net/gh/",
            "FastGit" to "https://fastgit.cc/",
        ).forEach { (k, v) ->
            items.add(k)
            urls.add(v)
        }

        MaterialAlertDialogBuilder(context, dialogCentered).apply {
            setTitle(context.getString(R.string.select_download_source))
            setCancelable(isDev)
            setItems(items.toTypedArray()) { _, which ->
                downloadFile(context, apkFile, urls[which])
            }
        }.show()
    }

    private fun readyDownload(context: Context, fileName: String, downloadUrl: String) {
        val toolDir = FileUtils.checkDownloadDir(context, "LuckyTool")
        val apkFile = File(toolDir, fileName).apply {
            if (isDirectory) delete()
        }
        if (apkFile.exists()) {
            val size = FileUtils.getFileSize(apkFile)
            val formatSize = formatFileSize(size.toFloat())

            MaterialAlertDialogBuilder(context, dialogCentered).apply {
                setTitle(R.string.downloaded)
                setMessage("${apkFile.name}\n${formatSize}")
                setNeutralButton(context.getString(R.string.download_again)) { _, _ ->
                    apkFile.delete()
                    showDownloadItems(context, apkFile, downloadUrl)
                }
                setPositiveButton(context.getString(R.string.install)) { _, _ ->
                    installApk(context, apkFile)
                }
                show()
            }
            return
        }
        showDownloadItems(context, apkFile, downloadUrl)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun downloadFile(context: Context, apkFile: File, url: String) {
        var downloadScope: NetCoroutineScope? = null
        val binding = DialogDownloadLayoutBinding.inflate(context.layoutInflater)
        val downloadDialog = MaterialAlertDialogBuilder(context, dialogCentered).apply {
            setTitle(context.getString(R.string.downloading))
            setCancelable(false)
            setView(binding.root)
        }.show()
        downloadScope = scopeNet {
            if (apkFile.exists()) {
                installApk(context, apkFile)
                downloadDialog.dismiss()
                return@scopeNet
            }
            binding.cancelButton.apply {
                text = context.getString(R.string.cancel_button)
                setOnClickListener {
                    apkFile.delete()
                    downloadScope?.cancel()
                    downloadDialog.dismiss()
                }
            }
            val downProgress = binding.downProgress
            val downTv = binding.downTv
            val downFile = Get<File>(url) {
                setDownloadDir(apkFile)
                setDownloadMd5Verify()
                addDownloadListener(object : ProgressListener(100) {
                    @SuppressLint("SetTextI18n")
                    override fun onProgress(p: Progress) {
                        downProgress.post {
                            val ps = p.progress()
                            downProgress.apply {
                                isIndeterminate = true
                                if (ps > 0) {
                                    isIndeterminate = false
                                    progress = ps
                                }
                            }
                            downTv.text = """
                                ${context.getString(R.string.download_progress)}: $ps%
                                ${context.getString(R.string.download_speed)}: ${p.speedSize()}
                                ${context.getString(R.string.remain_size)}: ${p.remainSize()}
                                ${context.getString(R.string.downloaded)}: ${p.currentSize()} / ${p.totalSize()}
                                ${context.getString(R.string.used_time)}: ${p.useTime()}
                                ${context.getString(R.string.remain_time)}: ${p.remainTime()}
                            """.trimIndent()
                        }
                    }
                })
            }.await()
            binding.installButton.apply {
                isVisible = true
                text = context.getString(R.string.install_button)
                setOnClickListener {
                    downloadDialog.setCancelable(true)
                    installApk(context, downFile)
                }
            }
            installApk(context, downFile)
            downloadDialog.dismiss()
        }
    }

    private fun installApk(context: Context, apkFile: File) {
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
