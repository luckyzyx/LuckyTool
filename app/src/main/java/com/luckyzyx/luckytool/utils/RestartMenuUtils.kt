package com.luckyzyx.luckytool.utils

import android.content.Context
import android.os.Process
import android.os.RemoteException
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.highcapable.betterandroid.ui.extension.view.toast
import com.luckyzyx.luckytool.IPackageServiceController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.ActivityManagerService
import com.luckyzyx.luckytool.service.PackagesService
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import io.github.libxposed.service.HotReloadResult
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
            context.getString(R.string.restart_scope),
            context.getString(R.string.re_optimize_dex),
            context.getString(R.string.reload_hooker),
            context.getString(R.string.reboot),
            context.getString(R.string.fast_reboot)
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
            else -> AlertDialog(
                onDismissRequest = onDismiss,
                text = {
                    Column {
                        items.forEachIndexed { index, label ->
                            Text(
                                label,
                                Modifier.fillMaxWidth().clickable {
                                    when (index) {
                                        0 -> showConfirmScope = true
                                        1 -> showOptimize = true
                                        2 -> showConfirmHooker = true
                                        3 -> {
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
                                }.padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }

    /**
     * 重启部分作用域 Compose 对话框
     * @param scopes Array<String>
     */
    @Composable
    fun RestartScopeDialog(
        context: Context,
        scopes: Array<String>,
        isSystem: Boolean = false,
        onDismiss: () -> Unit
    ) {
        if (scopes.isEmpty()) return
        val isSystemPage = isSystem && scopes.first() == "system"
        val items = if (isSystemPage) {
            arrayOf(
                R.string.reload_hooker,
                R.string.reload_only_this_page_hooker
            ).map { context.getString(it) }
        } else {
            arrayOf(
                R.string.restart_scope,
                R.string.re_optimize_dex,
                R.string.reload_hooker,
                R.string.restart_only_this_page_scope,
                R.string.optimize_only_this_page_scope,
                R.string.reload_only_this_page_hooker
            ).map { context.getString(it) }
        }
        var showConfirmScope by remember { mutableStateOf(false) }
        var showConfirmHooker by remember { mutableStateOf(false) }
        var showOptimize by remember { mutableStateOf(false) }
        var optimizeScopes by remember { mutableStateOf(arrayMapOf<String, CharSequence>()) }
        var optimizeConfirmFirst by remember { mutableStateOf(false) }
        when {
            showConfirmScope -> ConfirmRestartAllScopeDialog(context, onDismiss)
            showConfirmHooker -> ConfirmRestartAllHookerDialog(context, onDismiss)
            showOptimize -> OptimizeDexDialog(context, optimizeScopes, optimizeConfirmFirst, onDismiss)
            else -> AlertDialog(
                onDismissRequest = onDismiss,
                text = {
                    Column {
                        items.forEachIndexed { index, label ->
                            Text(
                                label,
                                Modifier.fillMaxWidth().clickable {
                                    if (isSystemPage) {
                                        when (index) {
                                            0 -> showConfirmHooker = true
                                            else -> {
                                                restartHooker(context, scopes)
                                                onDismiss()
                                            }
                                        }
                                    } else {
                                        when (index) {
                                            0 -> showConfirmScope = true
                                            1 -> {
                                                optimizeScopes = buildScopeMaps(
                                                    context,
                                                    context.resources.getStringArray(R.array.xposed_scope)
                                                )
                                                optimizeConfirmFirst = true
                                                showOptimize = true
                                            }
                                            2 -> showConfirmHooker = true
                                            3 -> {
                                                restartScope(context, scopes)
                                                onDismiss()
                                            }
                                            4 -> {
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
                                }.padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }

    /**
     * 重启全部作用域确认对话框
     */
    @Composable
    private fun ConfirmRestartAllScopeDialog(context: Context, onDismiss: () -> Unit) {
        val xposedScope = remember { context.resources.getStringArray(R.array.xposed_scope) }
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(context.getString(R.string.restart_scope_message)) },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch(Dispatchers.Default) {
                        restartScope(context, xposedScope)
                    }
                    onDismiss()
                }) { Text(context.getString(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(context.getString(android.R.string.cancel)) }
            }
        )
    }

    /**
     * 重载全部作用域 Hooker 确认对话框
     */
    @Composable
    private fun ConfirmRestartAllHookerDialog(context: Context, onDismiss: () -> Unit) {
        val xposedScope = remember { context.resources.getStringArray(R.array.xposed_scope) }
        AlertDialog(
            onDismissRequest = onDismiss,
            text = { Text(context.getString(R.string.reload_hooker_message)) },
            confirmButton = {
                TextButton(onClick = {
                    restartHooker(context, xposedScope)
                    onDismiss()
                }) { Text(context.getString(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(context.getString(android.R.string.cancel)) }
            }
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
        val base = XposedServiceBridge.xposedService ?: return
        base.runningTargets.filter {
            scopes.contains(it.processName.substringBefore(":"))
        }.forEach {
            base.hotReloadModule(it, null) { target, result ->
                LogUtils.d(
                    "restartHooker",
                    target.processName,
                    "${result.status} | ${result.message}",
                    true
                )
                if (result.status != HotReloadResult.Status.SUCCEEDED) {
                    context.toast("${target.processName}: ${result.message}")
                }
            }
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
            forEach { it ->
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
        fun launch(target: ArrayMap<String, CharSequence>) {
            current = target
            failedApps = arrayMapOf()
            stage = OptimizeStage.Running
        }
        when (stage) {
            OptimizeStage.Confirm -> AlertDialog(
                onDismissRequest = onDismiss,
                text = { Text(context.getString(R.string.re_optimize_dex_message)) },
                confirmButton = {
                    TextButton(onClick = { launch(current) }) {
                        Text(context.getString(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(android.R.string.cancel))
                    }
                }
            )
            OptimizeStage.Running -> {
                LaunchedEffect(Unit) {
                    runOptimize(context, current, { progressText = it }) { failed ->
                        if (failed.isNotEmpty()) {
                            failedApps = failed
                            stage = OptimizeStage.Failed
                        } else {
                            context.showToast(context.getString(R.string.re_optimize_dex_completed))
                            onDismiss()
                        }
                    }
                }
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(context.getString(R.string.re_optimize_dex_optimizing)) },
                    text = { Text(progressText) },
                    confirmButton = {}
                )
            }
            OptimizeStage.Failed -> AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.re_optimize_dex_failed)) },
                text = {
                    Text(
                        context.getString(
                            R.string.re_optimize_dex_faile_message,
                            failedApps.values.joinToString("\n")
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { launch(failedApps) }) {
                        Text(context.getString(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(android.R.string.cancel))
                    }
                }
            )
        }
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
