package com.luckyzyx.luckytool.utils

import android.content.Context
import android.os.Process
import android.os.RemoteException
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.highcapable.betterandroid.ui.extension.view.toast
import com.highcapable.yukihookapi.YukiHook
import com.highcapable.yukihookapi.hook.xposed.reload.entity.ModuleReloadStatus
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.shape.CouiShape
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.IPackageServiceController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.ActivityManagerService
import com.luckyzyx.luckytool.service.PackagesService
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiCircularProgressIndicator
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RestartMenuUtils {

    private val TAG = "RestartMenuUtils"

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * 重启主菜单 Compose 对话框（5 项）
     */
    @Composable
    fun RestartMenuDialog(context: Context, onDismiss: () -> Unit) {
        val items = listOf(
            ChoiceItem(key = "restart_scope", title = stringResource(R.string.restart_scope)),
            ChoiceItem(key = "re_optimize_dex", title = stringResource(R.string.re_optimize_dex)),
            ChoiceItem(key = "reload_hooker", title = stringResource(R.string.reload_hooker)),
            ChoiceItem(key = "reboot", title = stringResource(R.string.reboot)),
            ChoiceItem(key = "fast_reboot", title = stringResource(R.string.fast_reboot)),
        )
        var showConfirmScope by remember { mutableStateOf(false) }
        var showConfirmHooker by remember { mutableStateOf(false) }
        var showOptimize by remember { mutableStateOf(false) }
        when {
            showConfirmScope -> ConfirmRestartAllScopeDialog(context, onDismiss)
            showConfirmHooker -> ConfirmRestartAllHookerDialog(context, onDismiss)
            showOptimize -> OptimizeDexDialog(
                context,
                buildScopeMaps(context, context.resources.getStringArray(R.array.xposed_scope)),
                confirmFirst = true,
                onDismiss
            )

            // 点行执行动作、关闭由动作自己决定：前三项只把本层换成另一层对话框，后两项执行完再关
            else -> ActionListDialog(
                title = stringResource(R.string.menu_reboot),
                items = items,
                onDismiss = onDismiss,
                onSelect = { item ->
                    when (item.key) {
                        "restart_scope" -> showConfirmScope = true
                        "re_optimize_dex" -> showOptimize = true
                        "reload_hooker" -> showConfirmHooker = true
                        "reboot" -> {
                            PowerService.get(context) { controller ->
                                controller?.reboot(false, null, false)
                            }
                            onDismiss()
                        }

                        else -> {
                            ShellUtils.fastCmd(CommandUtils.killzygote)
                            onDismiss()
                        }
                    }
                },
            )
        }
    }

    /**
     * 重启部分作用域 Compose 对话框
     * @param scopes Array<String>
     */
    @Composable
    fun RestartScopeDialog(
        context: Context, scopes: Array<String>, isSystem: Boolean = false, onDismiss: () -> Unit
    ) {
        if (scopes.isEmpty()) return
        val isSystemPage = isSystem && scopes.first() == "system"
        val items = if (isSystemPage) {
            listOf(
                ChoiceItem(key = "reload_hooker", title = stringResource(R.string.reload_hooker)),
                ChoiceItem(
                    key = "reload_page_hooker",
                    title = stringResource(R.string.reload_only_this_page_hooker),
                ),
            )
        } else {
            listOf(
                ChoiceItem(key = "restart_scope", title = stringResource(R.string.restart_scope)),
                ChoiceItem(key = "re_optimize_dex", title = stringResource(R.string.re_optimize_dex)),
                ChoiceItem(key = "reload_hooker", title = stringResource(R.string.reload_hooker)),
                ChoiceItem(
                    key = "restart_page_scope",
                    title = stringResource(R.string.restart_only_this_page_scope),
                ),
                ChoiceItem(
                    key = "optimize_page_scope",
                    title = stringResource(R.string.optimize_only_this_page_scope),
                ),
                ChoiceItem(
                    key = "reload_page_hooker",
                    title = stringResource(R.string.reload_only_this_page_hooker),
                ),
            )
        }
        var showConfirmScope by remember { mutableStateOf(false) }
        var showConfirmHooker by remember { mutableStateOf(false) }
        var showOptimize by remember { mutableStateOf(false) }
        var optimizeScopes by remember { mutableStateOf(arrayMapOf<String, CharSequence>()) }
        var optimizeConfirmFirst by remember { mutableStateOf(false) }
        when {
            showConfirmScope -> ConfirmRestartAllScopeDialog(context, onDismiss)
            showConfirmHooker -> ConfirmRestartAllHookerDialog(context, onDismiss)
            showOptimize -> OptimizeDexDialog(
                context, optimizeScopes, optimizeConfirmFirst, onDismiss
            )

            // 点行执行动作、关闭由动作自己决定（换另一层对话框的项不关）
            else -> ActionListDialog(
                title = stringResource(R.string.menu_reboot),
                items = items,
                onDismiss = onDismiss,
                onSelect = { item ->
                    if (isSystemPage) {
                        when (item.key) {
                            "reload_hooker" -> showConfirmHooker = true
                            else -> {
                                restartHooker(context, scopes)
                                onDismiss()
                            }
                        }
                    } else {
                        when (item.key) {
                            "restart_scope" -> showConfirmScope = true
                            "re_optimize_dex" -> {
                                optimizeScopes = buildScopeMaps(
                                    context,
                                    context.resources.getStringArray(R.array.xposed_scope)
                                )
                                optimizeConfirmFirst = true
                                showOptimize = true
                            }

                            "reload_hooker" -> showConfirmHooker = true
                            "restart_page_scope" -> {
                                restartScope(context, scopes)
                                onDismiss()
                            }

                            "optimize_page_scope" -> {
                                optimizeScopes = buildScopeMaps(context, scopes)
                                optimizeConfirmFirst = false
                                showOptimize = true
                            }

                            else -> {
                                restartHooker(context, scopes)
                                onDismiss()
                            }
                        }
                    }
                },
            )
        }
    }

    /**
     * 重启全部作用域确认对话框
     */
    @Composable
    private fun ConfirmRestartAllScopeDialog(context: Context, onDismiss: () -> Unit) {
        val xposedScope = remember { context.resources.getStringArray(R.array.xposed_scope) }
        val message = stringResource(R.string.restart_scope_message)
        // 确认动作两条线共用：先触发重启，再关掉弹窗
        val onConfirm = {
            coroutineScope.launch(Dispatchers.Default) {
                restartScope(context, xposedScope)
            }
            onDismiss()
        }
        when (LocalUiMode.current) {
            UiMode.Material -> MaterialConfirmRestartScopeDialog(message, onConfirm, onDismiss)
            UiMode.Coui -> CouiConfirmRestartScopeDialog(message, onConfirm, onDismiss)
        }
    }

    /** Material 线：只有一句提示，正文用 material3 弹窗的原生文本槽 */
    @Composable
    private fun MaterialConfirmRestartScopeDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            })
    }

    /** Coui 线：确认是主操作，取消放取消位 */
    @Composable
    private fun CouiConfirmRestartScopeDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        CouiAlertDialog(
            onDismissRequest = onDismiss,
            message = message,
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onConfirm,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
            ),
        )
    }

    /**
     * 重载全部作用域 Hooker 确认对话框
     */
    @Composable
    private fun ConfirmRestartAllHookerDialog(context: Context, onDismiss: () -> Unit) {
        val xposedScope = remember { context.resources.getStringArray(R.array.xposed_scope) }
        val message = stringResource(R.string.reload_hooker_message)
        val onConfirm = {
            restartHooker(context, xposedScope)
            onDismiss()
        }
        when (LocalUiMode.current) {
            UiMode.Material -> MaterialConfirmRestartHookerDialog(message, onConfirm, onDismiss)
            UiMode.Coui -> CouiConfirmRestartHookerDialog(message, onConfirm, onDismiss)
        }
    }

    /** Material 线：只有一句提示，正文用 material3 弹窗的原生文本槽 */
    @Composable
    private fun MaterialConfirmRestartHookerDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            })
    }

    /** Coui 线：确认是主操作，取消放取消位 */
    @Composable
    private fun CouiConfirmRestartHookerDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        CouiAlertDialog(
            onDismissRequest = onDismiss,
            message = message,
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onConfirm,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
            ),
        )
    }

    /**
     * 重启部分作用域
     * @receiver Context
     * @param scopes Array<String>
     */
    private fun restartScope(context: Context, scopes: Array<String>) {
        val killSystemUI = scopes.contains("com.android.systemui")
        AppUtils(context).getAllAppVerInfo(scopes)
        ActivityManagerService.get(context) { controller ->
            scopes.forEachIndexed { _, packName ->
                if (packName == "com.android.systemui") return@forEachIndexed
                val uid = Process.myUid() / 100000
                controller?.forceStopPackage(packName, uid)
                LogUtils.d("restartScope", "forceStopPackage", packName, true)
            }
        }
        if (killSystemUI) ShellUtils.fastCmd(CommandUtils.killSysui)
    }

    /**
     * 重载部分Hooker
     * @receiver Context
     * @param scopes Array<String>
     */
    private fun restartHooker(context: Context, scopes: Array<String>) {
        val reload = YukiHook.Module.reload
        if (!reload.isAvailable) return
        val targets = runCatching { reload.targets }.onFailure {
            LogUtils.e("restartHooker", "query reload targets failed", "$it", true)
        }.getOrDefault(emptyList())
        runCatching {
            targets.filter {
                scopes.contains(it.processName.substringBefore(":"))
            }.forEach { target ->
                // skippedIfUpToDate = false：不做「代码已是最新」的跳过，无论目标进程的 jar 是否变化都重新加载
                reload.request(target, skippedIfUpToDate = false) { result ->
                    LogUtils.d(
                        "restartHooker",
                        result.target.processName,
                        "${result.status} | ${result.message}",
                        true
                    )
                    if (result.status != ModuleReloadStatus.SUCCEEDED) {
                        context.toast("${result.target.processName}: ${result.message}")
                    }
                }
            }
        }.onFailure {
            // request 内部还有一次 API 版本校验，失败会抛到这里（调用点是 Compose 回调，不能让它崩）
            LogUtils.e("restartHooker", "request module reload failed", "$it", true)
        }
    }

    /**
     * 重启选项
     * @param reason String
     */
    fun shellReboot(reason: String = "") {
        if (reason == "recovery") {
            // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
            Shell.getShell().newJob().add("/system/bin/input keyevent 26").exec()
        }
        Shell.getShell().newJob()
            .add("/system/bin/svc power reboot $reason || /system/bin/reboot $reason").exec()
    }

    /**
     * 构建作用域映射（包名 -> 应用名），过滤 android/system 与未安装应用
     */
    fun buildScopeMaps(context: Context, scopes: Array<String>): ArrayMap<String, CharSequence> {
        val scopeMaps = arrayMapOf<String, CharSequence>()
        scopes.toMutableList().apply {
            removeIf { it == "android" || it == "system" }
            removeIf { PackageUtils(context.packageManager).getPackageInfo(it, 0) == null }
            forEach {
                val name = PackageUtils(context.packageManager).getApplicationInfo(it, 0)
                    ?.loadLabel(context.packageManager)
                scopeMaps[it] = name
            }
        }
        return scopeMaps
    }

    /**
     * 优化 Dex 进度 Compose 对话框
     * @param confirmFirst 是否先弹出确认对话框
     */
    @Composable
    fun OptimizeDexDialog(
        context: Context,
        scopes: ArrayMap<String, CharSequence>,
        confirmFirst: Boolean = false,
        onDismiss: () -> Unit
    ) {
        var stage by remember {
            mutableStateOf(if (confirmFirst) OptimizeStage.Confirm else OptimizeStage.Running)
        }
        var current by remember { mutableStateOf(scopes) }
        var progressText by remember { mutableStateOf("") }
        var failedApps by remember { mutableStateOf(arrayMapOf<String, CharSequence>()) }
        // 优化完成提示在 LaunchedEffect 回调里弹出（那里没有 Composer），字符串先在这里取一次
        val dexCompletedText = stringResource(R.string.re_optimize_dex_completed)
        fun launch(target: ArrayMap<String, CharSequence>) {
            current = target
            failedApps = arrayMapOf()
            stage = OptimizeStage.Running
        }
        when (stage) {
            OptimizeStage.Confirm -> {
                val message = stringResource(R.string.re_optimize_dex_message)
                when (LocalUiMode.current) {
                    UiMode.Material -> MaterialOptimizeConfirmDialog(
                        message = message,
                        onConfirm = { launch(current) },
                        onDismiss = onDismiss,
                    )

                    UiMode.Coui -> CouiOptimizeConfirmDialog(
                        message = message,
                        onConfirm = { launch(current) },
                        onDismiss = onDismiss,
                    )
                }
            }

            OptimizeStage.Running -> {
                LaunchedEffect(Unit) {
                    runOptimize(context, current, { progressText = it }) { failed ->
                        if (failed.isNotEmpty()) {
                            failedApps = failed
                            stage = OptimizeStage.Failed
                        } else {
                            context.showToast(dexCompletedText)
                            onDismiss()
                        }
                    }
                }
                val title = stringResource(R.string.re_optimize_dex_optimizing)
                when (LocalUiMode.current) {
                    UiMode.Material -> MaterialOptimizeRunningDialog(title, progressText)
                    UiMode.Coui -> CouiOptimizeRunningDialog(title, progressText)
                }
            }

            OptimizeStage.Failed -> {
                val title = stringResource(R.string.re_optimize_dex_failed)
                val message = stringResource(
                    R.string.re_optimize_dex_faile_message,
                    failedApps.values.joinToString("\n")
                )
                when (LocalUiMode.current) {
                    UiMode.Material -> MaterialOptimizeFailedDialog(
                        title = title,
                        message = message,
                        onRetry = { launch(failedApps) },
                        onDismiss = onDismiss,
                    )

                    UiMode.Coui -> CouiOptimizeFailedDialog(
                        title = title,
                        message = message,
                        onRetry = { launch(failedApps) },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    /** Material 线：执行前的确认弹窗 */
    @Composable
    private fun MaterialOptimizeConfirmDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            })
    }

    /** Coui 线：执行前的确认弹窗，开始是主操作，取消放取消位 */
    @Composable
    private fun CouiOptimizeConfirmDialog(
        message: String,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        CouiAlertDialog(
            onDismissRequest = onDismiss,
            message = message,
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onConfirm,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
            ),
        )
    }

    /** Material 线：执行中的进度弹窗，只有居中的进度圈和一行进度文字 */
    @Composable
    private fun MaterialOptimizeRunningDialog(title: String, progressText: String) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = progressText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {})
    }

    /**
     * Coui 线：执行中的进度弹窗。
     *
     * COUI 弹窗要求确认按钮必填，而执行期间没有任何可点动作，所以用裸 Dialog 承载一张 COUI 配色卡片：
     * 标题、进度圈、进度文字纵向排列，进度文字随包序号实时刷新；执行期间点外部也不关闭。
     */
    @Composable
    private fun CouiOptimizeRunningDialog(title: String, progressText: String) {
        val colors = CouiTheme.colorScheme
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(CouiShape(24.dp, 2f))
                    .background(colors.container)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CouiText(
                    text = title,
                    kind = CouiTextKind.ItemTitle,
                    color = colors.onSurface,
                )
                CouiCircularProgressIndicator(color = colors.accent)
                CouiText(
                    text = progressText,
                    kind = CouiTextKind.Search,
                    textAlign = TextAlign.Start,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }

    /** Material 线：有失败应用时列出包名，确认后重新执行 */
    @Composable
    private fun MaterialOptimizeFailedDialog(
        title: String,
        message: String,
        onRetry: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onRetry) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            })
    }

    /** Coui 线：失败提示，重新执行是主操作，取消放取消位 */
    @Composable
    private fun CouiOptimizeFailedDialog(
        title: String,
        message: String,
        onRetry: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        CouiAlertDialog(
            onDismissRequest = onDismiss,
            title = title,
            message = message,
            confirmAction = CouiDialogAction(
                text = stringResource(android.R.string.ok),
                onClick = onRetry,
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
            ),
        )
    }

    private enum class OptimizeStage { Confirm, Running, Failed }

    /**
     * 执行 Dex 优化，进度与结果经回调返回
     */
    private fun runOptimize(
        context: Context,
        scopes: ArrayMap<String, CharSequence>,
        onProgress: (String) -> Unit,
        onDone: (ArrayMap<String, CharSequence>) -> Unit
    ) {
        PackagesService.get(context) { controller ->
            coroutineScope.launch {
                AppUtils(context).getAllAppVerInfo(scopes.keys.toTypedArray(), true)
                val failedApps = optimizeApps(controller, scopes, onProgress)
                onDone(failedApps)
            }
        }
    }

    /**
     * 优化多个应用，动态更新进度信息
     */
    private suspend fun optimizeApps(
        controller: IPackageServiceController?,
        scopes: ArrayMap<String, CharSequence>,
        onProgress: (String) -> Unit
    ): ArrayMap<String, CharSequence> {
        val failedApps = arrayMapOf<String, CharSequence>()
        withContext(Dispatchers.IO) {
            scopes.keys.forEachIndexed { index, pack ->
                val name = scopes[pack] ?: pack
                withContext(Dispatchers.Main) {
                    onProgress("$name (${index + 1}/${scopes.size})")
                }
                val success = try {
                    controller?.clearApplicationProfileData(pack)
                    controller?.performDexOptMode(pack) == true
                } catch (e: RemoteException) {
                    LogUtils.e("performAllScopeDex", pack, e.toString(), true)
                    false
                }
                if (success) {
                    LogUtils.d("performAllScopeDex", pack, "success", true)
                } else {
                    LogUtils.e("performAllScopeDex", pack, "fail", true)
                    failedApps[pack] = scopes[pack]
                }
            }
        }
        return failedApps
    }
}
