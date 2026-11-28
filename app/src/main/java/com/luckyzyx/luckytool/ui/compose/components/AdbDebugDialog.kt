package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.copyStr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 上次使用的端口：与 [com.luckyzyx.luckytool.service.tiles.RemoteAdbTile] 共用同一个键 */
private const val KEY_ADB_PORT = "adb_port"
private const val DEFAULT_ADB_PORT = "6666"
private val ADB_PORT_RANGE = 1..65535
private val AdbPortKeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

/** 对话框当前要提示的错误：端口为空 / 端口越界 / 服务端操作失败 */
private enum class AdbDebugError {
    PortBlank,
    PortOutOfRange,
    OperationFailed,
}

/**
 * 远程 ADB 调试对话框的状态机。
 *
 * 视图只读这里的状态、只调这里的方法：`runCatching` 只用来判定成败，
 * 失败只置 [error]、不动 [enabled]，用户重试即重跑；错误提示与连接命令分开存放，互不覆盖。
 *
 * @param adb 服务端控制器（AIDL）
 * @param otherPrefs 记录上次端口的 prefs 槽
 * @param scope 对话框自己的协程作用域，随对话框离开组合而取消
 */
@Stable
private class AdbDebugDialogState(
    private val adb: IAdbDebugController,
    private val otherPrefs: PrefState,
    private val scope: CoroutineScope,
) {
    // 控制器是 Binder 代理：服务进程若已回收，读属性会抛 DeadObjectException，构造期不能让它崩掉组合
    private val initialPort = runCatching { adb.adbPort }.getOrDefault(0)
    private val initialEnabled = initialPort != 0 && initialPort != -1
    private val initialIp = runCatching { adb.wifiIP }.getOrNull() ?: "IP"

    var portText by mutableStateOf(
        (if (initialEnabled) initialPort.toString()
        else otherPrefs.getString(KEY_ADB_PORT, DEFAULT_ADB_PORT)) ?: DEFAULT_ADB_PORT
    )
    var enabled by mutableStateOf(initialEnabled)
    var busy by mutableStateOf(false)

    /** 端口输入错误或操作失败时的提示；与连接命令分开，互不覆盖 */
    var error by mutableStateOf<AdbDebugError?>(null)

    /** 开启后展示的连接命令；关闭时为 null，此时不渲染状态卡 */
    var command by mutableStateOf<String?>(
        if (initialEnabled) "adb connect $initialIp:$initialPort" else null
    )

    /** 开关回调：忙碌中忽略，避免切一半又被点回去 */
    fun toggle(checked: Boolean) {
        if (busy || checked == enabled) return
        if (checked) enable() else disable()
    }

    /** 端口输入：限制成纯数字、最多 5 位，从输入源头挡住越界字符，并清掉端口类提示 */
    fun updatePortText(value: String) {
        portText = value.filter { it.isDigit() }.take(5)
        if (error != AdbDebugError.OperationFailed) error = null
    }

    private fun enable() {
        val port = portText.toIntOrNull()
        if (port == null) {
            error = if (portText.isBlank()) AdbDebugError.PortBlank else AdbDebugError.PortOutOfRange
            return
        }
        if (port !in ADB_PORT_RANGE) {
            error = AdbDebugError.PortOutOfRange
            return
        }
        error = null
        scope.launch {
            busy = true
            val succeeded = runCatching {
                withContext(Dispatchers.IO) {
                    adb.adbPort = port
                    adb.restartAdb()
                }
            }.isSuccess
            busy = false
            if (!succeeded) {
                error = AdbDebugError.OperationFailed
                return@launch
            }
            val ip = runCatching { adb.wifiIP }.getOrNull() ?: "IP"
            otherPrefs.set(KEY_ADB_PORT, port.toString())
            enabled = true
            command = "adb connect $ip:$port"
        }
    }

    private fun disable() {
        error = null
        scope.launch {
            busy = true
            val succeeded = runCatching {
                withContext(Dispatchers.IO) {
                    adb.adbPort = -1
                    adb.restartAdb()
                    adb.adbPort = 0
                }
            }.isSuccess
            busy = false
            if (!succeeded) {
                error = AdbDebugError.OperationFailed
                return@launch
            }
            enabled = false
            command = null
        }
    }
}

/**
 * 远程 ADB 调试对话框。
 *
 * 外壳与正文各一份：弹窗版式取自弹窗样式位，开关行、端口输入、错误提示、进度圈、
 * 命令卡的等宽文字与复制按钮取自设置样式位，本文件只画版式与持有状态。
 * 正文是普通布局而非自带滚动的列表，滚动与高度上限交给外壳。
 *
 * @param otherPrefs 记录上次端口的 prefs 槽
 * @param controller 服务端控制器；为空时不渲染
 * @param onDismiss 关闭对话框
 */
@Composable
fun AdbDebugDialog(
    otherPrefs: PrefState,
    controller: IAdbDebugController?,
    onDismiss: () -> Unit,
) {
    val adb = controller ?: return
    val scope = rememberCoroutineScope()
    val state = remember(adb, otherPrefs) { AdbDebugDialogState(adb, otherPrefs, scope) }
    currentDialogStyle().Alert(
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = onDismiss,
        title = stringResource(R.string.remote_adb_debug_title),
        bodyScrollable = true,
    ) {
        AdbDebugContent(state)
    }
}

/** 正文：开关 → 端口 → 错误提示 → 忙碌进度 → 连接命令卡 */
@Composable
private fun AdbDebugContent(state: AdbDebugDialogState) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AdbEnableRow(state)
        Spacer(Modifier.height(12.dp))
        AdbPortField(state)
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            AdbErrorNote(error)
        }
        if (state.busy) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                currentPrefStyle().Progress(Modifier)
            }
        }
        state.command?.let { command ->
            Spacer(Modifier.height(12.dp))
            AdbCommandCard(
                command = command,
                busy = state.busy,
                onCopy = { context.copyStr(command) },
            )
        }
    }
}

/** 开关行：标题占住剩余宽度，开关固定在行尾 */
@Composable
private fun AdbEnableRow(state: AdbDebugDialogState) {
    val title = stringResource(R.string.enable_remote_adb_debugging)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { PrefRowTitle(title) }
        AppSwitch(
            checked = state.enabled,
            onCheckedChange = state::toggle,
            enabled = !state.busy,
        )
    }
}

/** 端口输入：关闭时可编辑、开启后只读；非法态由样式位的 isError 表达 */
@Composable
private fun AdbPortField(state: AdbDebugDialogState) {
    val editable = !state.enabled && !state.busy
    val invalid = state.error == AdbDebugError.PortBlank || state.error == AdbDebugError.PortOutOfRange
    currentPrefStyle().TextField(
        value = state.portText,
        onValueChange = state::updatePortText,
        modifier = Modifier.fillMaxWidth(),
        enabled = editable,
        singleLine = true,
        leadingIcon = null,
        label = stringResource(R.string.adb_port),
        isError = invalid,
        keyboardOptions = AdbPortKeyboardOptions,
    )
}

/** 连接命令卡：整卡可点即复制，右侧另有显式复制按钮，长按同样复制 */
@Composable
private fun AdbCommandCard(
    command: String,
    busy: Boolean,
    onCopy: () -> Unit,
) {
    val copy = { if (!busy) onCopy() }
    val contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = copy,
        onLongClick = copy,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            currentPrefStyle().MonoText(command, Modifier.weight(1f))
            // 两条外观线各有一套图标资源，两份都给，由样式位挑自己那套
            currentPrefStyle().IconButton(
                onClick = copy,
                contentDescription = stringResource(R.string.copy),
                modifier = Modifier,
                enabled = true,
                icon = Icons.Rounded.ContentCopy,
                painter = painterResource(LucideR.drawable.lucide_ic_copy),
                danger = false,
            )
        }
    }
}

/** 错误提示：文案按错误类型取，配色由样式位的错误语气给 */
@Composable
private fun AdbErrorNote(error: AdbDebugError) {
    PrefNote(
        text = stringResource(
            when (error) {
                AdbDebugError.PortBlank -> R.string.adb_debug_port_cannot_null
                AdbDebugError.PortOutOfRange -> R.string.adb_port_out_of_range
                AdbDebugError.OperationFailed -> R.string.adb_debug_operation_failed
            }
        ),
        modifier = Modifier.fillMaxWidth(),
        textAlign = null,
        tone = NoteTone.Error,
    )
}
