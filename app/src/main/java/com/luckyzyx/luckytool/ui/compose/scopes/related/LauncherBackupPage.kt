package com.luckyzyx.luckytool.ui.compose.scopes.related

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.LauncherBackupHint
import com.luckyzyx.luckytool.utils.LauncherBackupPrefs
import com.luckyzyx.luckytool.utils.LauncherBackupTransfer
import com.luckyzyx.luckytool.utils.LauncherBackupUtils
import com.luckyzyx.luckytool.utils.LauncherSnapshot
import com.luckyzyx.luckytool.utils.LauncherSnapshotTag
import com.luckyzyx.luckytool.utils.LauncherTaskError
import com.luckyzyx.luckytool.utils.LauncherTaskResult
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.formatFileSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面布局备份/恢复页（入口挂在「桌面相关」页里，本身不再是功能树顶层条目）。
 *
 * 执行层在 launcher 进程（hook 的 LauncherBackupHooker），本页只做三件事：
 *  - 文件打包落盘：Download/LuckyTool/launcher_backup/<yyyyMMdd_HHmmss>_<tag>.ltbackup（标准 zip：meta.json + layout.json）；
 *  - 把文件 uri 经 dataChannel 指令交给桌面进程，并提前 grantUriPermission（跨 uid 不授权必抛 SecurityException）；
 *  - 列本地文件、恢复前补拍、二次确认。
 *
 * 没有回传通道：备份的 ack 是「文件长度由 0 变非 0」，恢复只提示已下发，结果看桌面 toast。
 *
 * 页面拆三个分类：「备份设置」（自动备份开关与周期，走 prefs 键）、「操作」（状态/立即备份/导入/目录）、
 * 「快照历史」（一行一个快照，点击进恢复二次确认、行尾垃圾桶与长按进删除二次确认）。快照来自文件系统而非 prefs，所以列表、忙碌态、
 * 结果提示都是对象级 Compose 状态：DSL 行在页面内容组合期读这些状态（读入 ScopeScreen 的快照订阅，
 * 写入即重建行），写入发生在点击行与宿主协程里。
 */
object LauncherBackupPage {

    /** 由页面宿主在组合期登记的协程作用域与文件选择器启动器（点击行不在组合内，拿不到 remember） */
    private var pageScope: CoroutineScope? = null
    private var importLauncher: (() -> Unit)? = null

    /** 桌面作用域包名（与 LauncherRelatedPage.spec.scopes 一致；模块作用域清单里就是这两个） */
    private val LAUNCHER_SCOPE_PACKAGES = arrayOf("com.android.launcher", "com.oppo.launcher")

    /** 页面级动态状态：快照列表 / 忙碌 / 结果提示 / 待确认恢复或删除的目标 */
    private val snapshotsState = mutableStateOf<List<LauncherSnapshot>>(emptyList())
    private val busyState = mutableStateOf(false)
    private val busyHintState = mutableStateOf<LauncherBackupHint?>(null)
    private val noticeState = mutableStateOf("")
    private val pendingRestoreState = mutableStateOf<LauncherSnapshot?>(null)
    private val pendingDeleteState = mutableStateOf<LauncherSnapshot?>(null)

    val spec = ScopePageSpec(
        pageKey = "launcher_backup",
        prefsName = ModulePrefs,
        packName = "",
        // 顶栏「重启作用域」按本页 scopes 弹菜单（RestartMenuUtils.RestartScopeDialog 对空数组直接返回），
        // 与桌面相关页 LauncherRelatedPage 用同一份桌面包名：恢复后桌面进程必须重起才会重读库
        scopes = LAUNCHER_SCOPE_PACKAGES,
        restartEnabled = true,
        // 不开下拉刷新：这一页靠「状态」「备份目录」两行与各操作完成后的重列刷新，
        // PullToRefreshBox 会给列表再套一层竖向手势，和长列表滑动打架
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val busy = busyState.value
        val hint = busyHintState.value
        val notice = noticeState.value
        val snapshots = snapshotsState.value
        val launcherPackage = LauncherBackupUtils.detectLauncherPackage(c)

        // 备份设置
        category(c.getString(R.string.launcher_backup_category_settings))
        switch(
            key = LauncherBackupPrefs.KEY_AUTO_ENABLED,
            title = c.getString(R.string.launcher_backup_auto),
            summary = c.getString(R.string.launcher_backup_auto_summary),
        )
        list(
            key = LauncherBackupPrefs.KEY_AUTO_INTERVAL,
            title = c.getString(R.string.launcher_backup_interval),
            entries = LauncherBackupPrefs.INTERVAL_HOURS.map { intervalLabel(c, it) }.toTypedArray(),
            entryValues = LauncherBackupPrefs.INTERVAL_HOURS.map { it.toString() }.toTypedArray(),
            default = LauncherBackupPrefs.DEFAULT_INTERVAL_HOURS.toString(),
            summary = c.getString(R.string.launcher_backup_interval_summary),
        )

        // 操作
        category(c.getString(R.string.launcher_backup_category_actions))
        click(
            title = c.getString(R.string.launcher_backup_last_auto),
            summary = lastAutoText(c, snapshots),
            onClick = { reload() },
        )
        click(
            title = c.getString(R.string.launcher_backup_status),
            summary = statusText(c, busy, hint, notice, launcherPackage),
            onClick = { reload() },
        )
        click(
            title = c.getString(R.string.launcher_backup_backup_now),
            summary = c.getString(R.string.launcher_backup_backup_now_summary),
            enabled = !busy && launcherPackage != null,
            onClick = { launchTask { backupNow(c) } },
        )
        click(
            title = c.getString(R.string.launcher_backup_import),
            summary = c.getString(R.string.launcher_backup_import_summary),
            enabled = !busy,
            onClick = { importLauncher?.invoke() },
        )
        click(
            title = c.getString(R.string.launcher_backup_root),
            summary = LauncherBackupTransfer.backupDir().absolutePath,
            onClick = { reload() },
        )

        // 快照历史：一个快照一行，点击进恢复二次确认；行尾垃圾桶与长按都进删除二次确认
        category(c.getString(R.string.launcher_backup_category_history))
        if (snapshots.isEmpty()) {
            click(title = c.getString(R.string.launcher_backup_empty), enabled = false)
        } else {
            snapshots.forEach { snapshot ->
                click(
                    // 按快照文件名做稳定的列表项 key：删除后这一项才是「移除并淡出」的那一行，
                    // 默认的槽位 key 会把动画落到编号相同的相邻行上
                    itemKey = "snapshot:" + snapshot.fileName,
                    animate = true,
                    title = snapshotTitle(c, snapshot),
                    summary = snapshotSummary(c, snapshot),
                    enabled = !busy && launcherPackage != null,
                    trailing = {
                        // 删除不需要桌面进程参与：只要求当前没有备份/恢复在跑（两类任务都走 LauncherBackupTransfer.busy）
                        SnapshotDeleteAction(enabled = !busy) { pendingDeleteState.value = snapshot }
                    },
                    onLongClick = if (busy) null else ({ pendingDeleteState.value = snapshot }),
                    onClick = { pendingRestoreState.value = snapshot },
                )
            }
        }

        // 页面宿主：自身不渲染任何内容（bare 不参与分组合并、也没有标题摘要），只承载对话框与组合期才有的能力
        custom(bare = true) {
            val coroutineScope = rememberCoroutineScope()
            val busyFlow by LauncherBackupTransfer.busy.collectAsStateWithLifecycle()
            val busyHintFlow by LauncherBackupTransfer.busyHint.collectAsStateWithLifecycle()
            val importer = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                coroutineScope.launch {
                    noticeState.value = resultText(c, LauncherBackupTransfer.importFile(c, uri))
                    reload()
                }
            }

            DisposableEffect(importer, coroutineScope) {
                pageScope = coroutineScope
                importLauncher = { importer.launch("*/*") }
                onDispose {
                    pageScope = null
                    importLauncher = null
                }
            }

            // 忙碌态与快照列表都不在 prefs 里：由宿主把 flow 写成对象级状态，DSL 行才会重建
            LaunchedEffect(busyFlow) { busyState.value = busyFlow }
            LaunchedEffect(busyHintFlow) { busyHintState.value = busyHintFlow }
            LaunchedEffect(Unit) {
                // 宿主退到媒体目录的快照要先搬回正式目录，搬运可能要读几 MB，放 IO 线程
                LauncherBackupTransfer.adoptHostFallback(c)
                reload()
            }

            // 恢复必须二次确认：会先自动补拍一份 pre_restore，然后整表覆盖布局
            val target = pendingRestoreState.value
            if (target != null) {
                SnapshotConfirmDialog(
                    title = stringResource(R.string.launcher_backup_restore_title),
                    message = stringResource(R.string.launcher_backup_restore_message, target.fileName),
                    confirmText = stringResource(R.string.launcher_backup_restore_confirm),
                    onConfirm = {
                        pendingRestoreState.value = null
                        coroutineScope.launch { restoreNow(c, target) }
                    },
                    onDismiss = { pendingRestoreState.value = null },
                )
            }

            // 删除同样要二次确认：文件删掉就没有回滚点，只能重新备份
            val deleting = pendingDeleteState.value
            if (deleting != null) {
                SnapshotConfirmDialog(
                    title = stringResource(R.string.launcher_backup_delete_title),
                    message = stringResource(R.string.launcher_backup_delete_message, deleting.fileName),
                    confirmText = stringResource(R.string.launcher_backup_delete_confirm),
                    onConfirm = {
                        pendingDeleteState.value = null
                        deleteNow(c, deleting)
                    },
                    onDismiss = { pendingDeleteState.value = null },
                )
            }
        }
    }

    /**
     * 重列本地快照文件（下拉刷新、操作完成、列表首次组合都走这里）；内容不变则不写状态，避免重复组合。
     *
     * 不再用「页面是否已组合」的布尔量把门：组合期登记的行在相邻条目增删时槽位会漂移，
     * LazyColumn 因此重建承载宿主（作用域/对话框）的那一项，onDispose 会把那个布尔量抹成 false，
     * 而写入状态的调用（删除、备份完成后）都在事件里同步发生 —— 门一关，行就留在屏幕上，
     * 表现为「删了一次没反应，再删一次才刷新」。
     */
    private fun reload() {
        val fresh = LauncherBackupTransfer.listSnapshots()
        if (fresh != snapshotsState.value) snapshotsState.value = fresh
    }

    /** 点击行触发的任务：统一置忙碌态并在结束时清掉，避免双击叠跑 */
    private fun launchTask(block: suspend () -> Unit) {
        val scope = pageScope ?: return
        scope.launch {
            busyState.value = true
            try {
                block()
            } finally {
                busyState.value = false
            }
        }
    }

    private suspend fun backupNow(context: Context) {
        noticeState.value = context.getString(R.string.launcher_backup_busy)
        noticeState.value = resultText(context, LauncherBackupTransfer.backup(context, LauncherSnapshotTag.MANUAL))
        reload()
    }

    private suspend fun restoreNow(context: Context, target: LauncherSnapshot) {
        noticeState.value = context.getString(R.string.launcher_backup_busy)
        val result = LauncherBackupTransfer.restore(context, File(LauncherBackupTransfer.backupDir(), target.fileName))
        // 恢复没有回传：下发成功只说明指令到了，结果由桌面 toast 给出
        noticeState.value = if (result is LauncherTaskResult.Success) {
            context.getString(R.string.launcher_backup_result_sent)
        } else {
            resultText(context, result)
        }
        reload()
    }

    /**
     * 删除一份快照：不经过桌面进程。成功时直接把这一行从列表状态里摘掉（文件已删，重列只会得到同一结论，
     * 摘状态才能立刻触发淡出动画）；失败才重列，把行放回原位并照实提示。
     */
    private fun deleteNow(context: Context, target: LauncherSnapshot) {
        when (val result = LauncherBackupTransfer.delete(context, target)) {
            is LauncherTaskResult.Success -> {
                snapshotsState.value = snapshotsState.value.filterNot { it.fileName == target.fileName }
                noticeState.value = context.getString(R.string.launcher_backup_delete_ok, result.file.name)
            }

            is LauncherTaskResult.Failure -> {
                noticeState.value =
                    context.getString(R.string.launcher_backup_result_failed, errorText(context, result))
                reload()
            }
        }
    }

    /** 状态行摘要：忙碌态优先，其次上一次结果提示，然后是就绪/无桌面两态 */
    private fun statusText(
        context: Context,
        busy: Boolean,
        hint: LauncherBackupHint?,
        notice: String,
        launcherPackage: String?,
    ): String = when {
        // 等待桌面进程时显示第几次下发：重启后要等 launcher 起来，否则用户只看到「执行中」然后超时
        busy -> hint?.let {
            context.getString(R.string.launcher_backup_waiting_launcher, it.attempt, it.total)
        } ?: context.getString(R.string.launcher_backup_busy)
        notice.isNotBlank() -> notice
        launcherPackage == null -> context.getString(R.string.launcher_backup_no_launcher)
        else -> context.getString(R.string.launcher_backup_ok_ready, launcherPackage)
    }

    /** 结果文案：成功只说完成与文件名，失败按 [LauncherTaskError] 取本地化说明（不拼英文异常） */
    private fun resultText(context: Context, result: LauncherTaskResult): String = when (result) {
        is LauncherTaskResult.Success ->
            context.getString(R.string.launcher_backup_result_ok) + " · " + result.file.name

        is LauncherTaskResult.Failure ->
            context.getString(R.string.launcher_backup_result_failed, errorText(context, result))
    }

    private fun errorText(context: Context, failure: LauncherTaskResult.Failure): String =
        when (failure.error) {
            LauncherTaskError.NO_LAUNCHER -> context.getString(R.string.launcher_backup_no_launcher)
            LauncherTaskError.CREATE_FAILED ->
                context.getString(R.string.launcher_backup_err_create, failure.detail)

            LauncherTaskError.FILE_EXISTS ->
                context.getString(R.string.launcher_backup_err_exists, failure.detail)

            LauncherTaskError.GRANT_FAILED ->
                context.getString(R.string.launcher_backup_err_grant, failure.detail)

            LauncherTaskError.SEND_FAILED -> context.getString(R.string.launcher_backup_send_failed)
            LauncherTaskError.TIMEOUT -> context.getString(R.string.launcher_backup_err_timeout)
            LauncherTaskError.EMPTY_FILE ->
                context.getString(R.string.launcher_backup_err_empty, failure.detail)

            LauncherTaskError.PRE_RESTORE_FAILED ->
                context.getString(R.string.launcher_backup_err_pre_restore, failure.detail)

            LauncherTaskError.INVALID_FILE ->
                context.getString(R.string.launcher_backup_err_invalid)

            LauncherTaskError.IO_ERROR ->
                context.getString(R.string.launcher_backup_err_io, failure.detail)
        }

    /**
     * 上次自动备份时间。
     *
     * 只能以落盘文件为准：自动备份的「上次成功/尝试时间」记在桌面进程自己的 prefs（LauncherBackupGate）里，
     * 那个目录属于桌面的 uid，app 进程读不到。所以这里取最近的 auto 快照；一份都没有就是从未自动备份过
     * （包含「备份了但全部失败」「快照被淘汰或删除」）。
     */
    private fun lastAutoText(context: Context, snapshots: List<LauncherSnapshot>): String {
        val last = snapshots.firstOrNull { it.tag == LauncherSnapshotTag.AUTO }
            ?: return context.getString(R.string.launcher_backup_auto_never)
        return formatStamp(last.timeMillis)
    }

    /** 备份行标题：时间 + 标签（文件名里的时间戳对用户不直观） */
    private fun snapshotTitle(context: Context, snapshot: LauncherSnapshot): String =
        formatStamp(snapshot.timeMillis) + " · " + tagLabel(context, snapshot.tag)

    private fun formatStamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(millis))

    /** 备份行摘要：文件名与字节数 */
    private fun snapshotSummary(context: Context, snapshot: LauncherSnapshot): String {
        val size = if (snapshot.sizeBytes <= 0L) "-" else formatFileSize(snapshot.sizeBytes.toFloat())
        return snapshot.fileName + " · " + size
    }

    private fun tagLabel(context: Context, tag: LauncherSnapshotTag): String = when (tag) {
        LauncherSnapshotTag.AUTO -> context.getString(R.string.launcher_backup_tag_auto)
        LauncherSnapshotTag.MANUAL -> context.getString(R.string.launcher_backup_tag_manual)
        LauncherSnapshotTag.PRE_RESTORE -> context.getString(R.string.launcher_backup_tag_pre_restore)
    }

    private fun intervalLabel(context: Context, hours: Int): String = when (hours) {
        6 -> context.getString(R.string.launcher_backup_interval_6h)
        12 -> context.getString(R.string.launcher_backup_interval_12h)
        48 -> context.getString(R.string.launcher_backup_interval_48h)
        else -> context.getString(R.string.launcher_backup_interval_24h)
    }
}

/** 快照行尾的删除按钮：Material 与 Miuix 两套行的 trailing 槽共用，各取各主题的 error 色 */
@Composable
private fun SnapshotDeleteAction(enabled: Boolean, onClick: () -> Unit) {
    val tint = if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.error
    }
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.launcher_backup_delete),
            tint = tint,
        )
    }
}

/** 二次确认对话框：Miuix OverlayDialog 与 Material AlertDialog 两套壳子，恢复与删除共用 */
@Composable
private fun SnapshotConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        // Miuix 的 OverlayDialog 没有标题区（原文案自带你做的事），保持只有正文 + 两个按钮
        UiMode.Miuix -> OverlayDialog(show = true, onDismissRequest = onDismiss) {
            MiuixText(text = message, modifier = Modifier.fillMaxWidth())
            MiuixTextButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
            MiuixTextButton(
                text = stringResource(R.string.launcher_backup_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        UiMode.Material -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.launcher_backup_cancel))
                }
            },
        )
    }
}
