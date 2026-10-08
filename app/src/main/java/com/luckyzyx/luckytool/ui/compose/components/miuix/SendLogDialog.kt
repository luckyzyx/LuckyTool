package com.luckyzyx.luckytool.ui.compose.components.miuix

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.logcatToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 加载中对话框句柄（对齐 KernelSU `ui/component/dialog/Dialog.kt` 的 `LoadingDialogHandle` 契约）。
 *
 * 基准仓库把 [LoadingDialogHandle] 与 `DialogHandle` 宿主一起放在对话框基础设施里；
 * 本仓没有对应设施，故接口与 [SendLogDialog] 同文件声明，并由 [rememberLoadingDialogHandle]
 * 给出基于 Miuix `OverlayDialog` 的真实实现（非占位 stub）。
 */
interface LoadingDialogHandle {
    /** 显示加载中对话框。 */
    fun show()

    /** 隐藏加载中对话框。 */
    fun hide()

    /** 在显示加载中对话框的同时执行 [block]，无论成功与否都会收起。 */
    suspend fun <R> withLoading(block: suspend () -> R): R
}

/**
 * 构造一个基于 Miuix `OverlayDialog` 的 [LoadingDialogHandle]。
 *
 * 须与 [SendLogDialog] 一样常挂载在页面组合内（`var` 状态由本函数持有），不要在 `if (visible)` 内使用。
 */
@Composable
fun rememberLoadingDialogHandle(): LoadingDialogHandle {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    val handle = remember(scope) {
        object : LoadingDialogHandle {
            override fun show() {
                scope.launch { visible = true }
            }

            override fun hide() {
                scope.launch { visible = false }
            }

            override suspend fun <R> withLoading(block: suspend () -> R): R =
                scope.async {
                    try {
                        visible = true
                        block()
                    } finally {
                        visible = false
                    }
                }.await()
        }
    }
    OverlayDialog(
        show = visible,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = colorScheme.primary,
                    )
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = stringResource(R.string.loading),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = colorScheme.onSurface,
                    )
                }
            }
        },
    )
    return handle
}

/**
 * 日志导出/分享对话框（迁移自 KernelSU `ui/component/miuix/SendLogDialog.kt`）。
 *
 * 基准仓库导出的是 bugreport（gzip），本仓改为导出 logcat 纯文本：
 * 落盘走既有 [logcatToFile]（写入 `cacheDir`，`file_paths.xml` 已含 `cache-path`），
 * 分享走既有 FileProvider（authority 为 `${applicationId}.FileProvider`）。
 *
 * 注意：本组件依赖 `rememberLauncherForActivityResult`，调用方必须**常挂载**它
 * （`SendLogDialog(show = ..., onDismissRequest = { ... }, loadingDialog = ...)` 无条件调用），
 * 不要在 `if (show)` 内使用，否则系统文件选择器返回时组合已卸载、回调会丢失。
 *
 * @param show 是否显示。
 * @param onDismissRequest 请求关闭。
 * @param loadingDialog 加载中句柄，可用 [rememberLoadingDialogHandle] 构造。
 */
@Composable
fun SendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logSavedText = stringResource(R.string.log_save_success)
    val logSaveFailedText = stringResource(R.string.log_save_failed)
    val sendLogText = stringResource(R.string.share)

    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            loadingDialog.show()
            val saved = withContext(Dispatchers.IO) {
                val logFile = buildLogFile(context)
                logFile != null && runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        logFile.inputStream().use { it.copyTo(output) }
                    } != null
                }.getOrDefault(false)
            }
            loadingDialog.hide()
            Toast.makeText(
                context,
                if (saved) logSavedText else logSaveFailedText,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(0.dp, 0.dp),
        content = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                text = stringResource(R.string.nav_log),
                fontSize = MiuixTheme.textStyles.title4.fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = colorScheme.onSurface,
            )
            ArrowPreference(
                title = stringResource(id = R.string.save),
                startAction = {
                    Icon(
                        Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onSurface,
                    )
                },
                onClick = {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
                    val current = LocalDateTime.now().format(formatter)
                    exportLogLauncher.launch("LuckyTool_log_${current}.log")
                    onDismissRequest()
                },
                insideMargin = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 12.dp,
                ),
            )
            ArrowPreference(
                title = stringResource(id = R.string.share),
                startAction = {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = colorScheme.onSurface,
                    )
                },
                onClick = {
                    scope.launch {
                        onDismissRequest()
                        val logFile = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) {
                                buildLogFile(context)
                            }
                        }
                        if (logFile == null) {
                            Toast.makeText(context, logSaveFailedText, Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.FileProvider",
                            logFile,
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            setDataAndType(uri, "text/plain")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                sendLogText,
                            ),
                        )
                    }
                },
                insideMargin = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 12.dp,
                ),
            )
            TextButton(
                text = stringResource(id = android.R.string.cancel),
                onClick = {
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp)
                    .padding(horizontal = 24.dp),
            )
        },
    )
}

/**
 * 导出 logcat 到缓存目录；失败（无 root 或命令异常）返回 null。
 */
private fun buildLogFile(context: Context): File? {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm")
    val current = LocalDateTime.now().format(formatter)
    val logFile = File(context.cacheDir, "LuckyTool_log_${current}.log")
    return if (logcatToFile(logFile)) logFile else null
}
