package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialTonalCard
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixDialogScrollBody
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixSwitchItem
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.copyStr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
 * 两条渲染线共用这一份状态：`runCatching` 只用来判定成败，
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
 * 远程 ADB 调试对话框（Material / Miuix 双线）。
 *
 * 只有一份状态：状态机与正文合一，两个壳（Miuix / material3）渲染同一个 [AdbDebugDialogState]，
 * 因此错误提示与连接命令不会互相覆盖。
 * Miuix 线走 `overlay.OverlayDialog`（由根部 Miuix Scaffold 的 popup host 承载，须组合在 MainShell 内），
 * Material 线走 material3 `AlertDialog`。
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
    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixAdbDebugDialog(state, onDismiss)
        UiMode.Material -> MaterialAdbDebugDialog(state, onDismiss)
    }
}

@Composable
private fun MiuixAdbDebugDialog(state: AdbDebugDialogState, onDismiss: () -> Unit) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.remote_adb_debug_title),
        onDismissRequest = onDismiss,
    ) {
        MiuixDialogScrollBody(
            actions = {
                MiuixTextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            },
        ) {
            AdbDebugContent(state, isMiuix = true)
        }
    }
}

@Composable
private fun MaterialAdbDebugDialog(state: AdbDebugDialogState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remote_adb_debug_title)) },
        text = { AdbDebugContent(state, isMiuix = false) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

/** 正文：开关 → 端口 → 错误提示 → 忙碌进度 → 连接命令卡 → 复制提示，两条线共用 */
@Composable
private fun AdbDebugContent(state: AdbDebugDialogState, isMiuix: Boolean) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AdbEnableRow(state, isMiuix)
        Spacer(Modifier.height(12.dp))
        AdbPortField(state, isMiuix)
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            AdbErrorText(error, isMiuix)
        }
        if (state.busy) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
        }
        state.command?.let { command ->
            Spacer(Modifier.height(12.dp))
            AdbCommandCard(
                command = command,
                busy = state.busy,
                isMiuix = isMiuix,
                onCopy = { context.copyStr(command) },
            )
        }
    }
}

@Composable
private fun AdbEnableRow(state: AdbDebugDialogState, isMiuix: Boolean) {
    val title = stringResource(R.string.enable_remote_adb_debugging)
    if (isMiuix) {
        MiuixSwitchItem(
            title = title,
            checked = state.enabled,
            onCheckedChange = state::toggle,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f))
            Switch(
                checked = state.enabled,
                onCheckedChange = state::toggle,
                enabled = !state.busy,
            )
        }
    }
}

/** 端口输入：关闭时可编辑、开启后只读；非法态两条线各用原生方式着色 */
@Composable
private fun AdbPortField(state: AdbDebugDialogState, isMiuix: Boolean) {
    val label = stringResource(R.string.adb_port)
    val editable = !state.enabled && !state.busy
    val invalid = state.error == AdbDebugError.PortBlank || state.error == AdbDebugError.PortOutOfRange
    if (isMiuix) {
        MiuixTextField(
            value = state.portText,
            onValueChange = state::updatePortText,
            enabled = editable,
            label = label,
            useLabelAsPlaceholder = true,
            singleLine = true,
            cornerRadius = 14.dp,
            colors = if (invalid) {
                MiuixTextFieldDefaults.textFieldColors(
                    backgroundColor = MiuixTheme.colorScheme.errorContainer,
                    labelColor = MiuixTheme.colorScheme.onErrorContainer,
                    borderColor = MiuixTheme.colorScheme.error,
                )
            } else {
                MiuixTextFieldDefaults.textFieldColors()
            },
            keyboardOptions = AdbPortKeyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        OutlinedTextField(
            value = state.portText,
            onValueChange = state::updatePortText,
            enabled = editable,
            label = { Text(label) },
            singleLine = true,
            isError = invalid,
            keyboardOptions = AdbPortKeyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 连接命令卡：整卡可点即复制，右侧另有显式复制按钮，长按同样复制 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdbCommandCard(
    command: String,
    busy: Boolean,
    isMiuix: Boolean,
    onCopy: () -> Unit,
) {
    val copy = { if (!busy) onCopy() }
    val contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    if (isMiuix) {
        MiuixCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = copy, onLongClick = copy),
            insideMargin = contentPadding,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiuixText(
                    text = command,
                    style = MiuixTheme.textStyles.body2.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                )
                MiuixIconButton(onClick = copy) {
                    MiuixIcon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    } else {
        MaterialTonalCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = copy,
            onLongClick = copy,
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = command,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = copy) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdbErrorText(error: AdbDebugError, isMiuix: Boolean) {
    val text = stringResource(
        when (error) {
            AdbDebugError.PortBlank -> R.string.adb_debug_port_cannot_null
            AdbDebugError.PortOutOfRange -> R.string.adb_port_out_of_range
            AdbDebugError.OperationFailed -> R.string.adb_debug_operation_failed
        }
    )
    if (isMiuix) {
        MiuixText(
            text = text,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
