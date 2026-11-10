package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text as CouiText
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.IAdbDebugController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.AppCard
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiAlertDialog
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiCircularProgressIndicator
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiIconButton
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextField
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
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
 * 远程 ADB 调试对话框。
 *
 * 只有一份状态：两条渲染线共用 [AdbDebugDialogState]，因此错误提示与连接命令不会互相覆盖。
 * Material 线走 material3 `AlertDialog`，Coui 线走 COUI 弹窗，差异只在视图层。
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
        UiMode.Material -> MaterialAdbDebugDialog(state, onDismiss)
        UiMode.Coui -> CouiAdbDebugDialog(state, onDismiss)
    }
}

@Composable
private fun MaterialAdbDebugDialog(state: AdbDebugDialogState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remote_adb_debug_title)) },
        text = { AdbDebugContent(state) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

/** 正文：开关 → 端口 → 错误提示 → 忙碌进度 → 连接命令卡 → 复制提示 */
@Composable
private fun AdbDebugContent(state: AdbDebugDialogState) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AdbEnableRow(state)
        Spacer(Modifier.height(12.dp))
        AdbPortField(state)
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            AdbErrorText(error)
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
                onCopy = { context.copyStr(command) },
            )
        }
    }
}

@Composable
private fun AdbEnableRow(state: AdbDebugDialogState) {
    val title = stringResource(R.string.enable_remote_adb_debugging)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        AppSwitch(
            checked = state.enabled,
            onCheckedChange = state::toggle,
            enabled = !state.busy,
        )
    }
}

/** 端口输入：关闭时可编辑、开启后只读；非法态用 M3 `isError` 着色 */
@Composable
private fun AdbPortField(state: AdbDebugDialogState) {
    val label = stringResource(R.string.adb_port)
    val editable = !state.enabled && !state.busy
    val invalid = state.error == AdbDebugError.PortBlank || state.error == AdbDebugError.PortOutOfRange
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

@Composable
private fun AdbErrorText(error: AdbDebugError) {
    val text = stringResource(
        when (error) {
            AdbDebugError.PortBlank -> R.string.adb_debug_port_cannot_null
            AdbDebugError.PortOutOfRange -> R.string.adb_port_out_of_range
            AdbDebugError.OperationFailed -> R.string.adb_debug_operation_failed
        }
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Coui 线弹窗：COUI 弹窗自带底部按钮区，确定按钮只有一处，与原行为一致。
 */
@Composable
private fun CouiAdbDebugDialog(state: AdbDebugDialogState, onDismiss: () -> Unit) {
    CouiAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.remote_adb_debug_title),
        confirmAction = CouiDialogAction(
            text = stringResource(android.R.string.ok),
            onClick = onDismiss,
            style = CouiDialogActionStyle.Primary,
            colors = CouiDialogDefaults.primaryActionColors(CouiTheme.colorScheme.accent),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 弹窗正文槽本身不带留白，左右与上下边距由调用方补齐
                .padding(
                    start = CouiDialogDefaults.contentPadding,
                    end = CouiDialogDefaults.contentPadding,
                    top = 6.dp,
                    bottom = 16.dp,
                ),
        ) {
            AdbDebugCouiContent(state)
        }
    }
}

/**
 * Coui 线正文：开关 → 端口 → 错误提示 → 忙碌进度 → 连接命令卡。
 * 正文槽本身可滚动，内容变高时由弹窗滚动，按钮区保持固定。
 */
@Composable
private fun AdbDebugCouiContent(state: AdbDebugDialogState) {
    val context = LocalContext.current
    val colors = CouiTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouiText(
                text = stringResource(R.string.enable_remote_adb_debugging),
                kind = CouiTextKind.ItemTitle,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            AppSwitch(
                checked = state.enabled,
                onCheckedChange = state::toggle,
                enabled = !state.busy,
            )
        }
        Spacer(Modifier.height(12.dp))
        CouiAdbPortField(state)
        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            CouiAdbErrorText(error)
        }
        if (state.busy) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                // 不传尺寸：控件内部已固定 32dp，外部再套 size 会互相打架
                CouiCircularProgressIndicator()
            }
        }
        state.command?.let { command ->
            Spacer(Modifier.height(12.dp))
            CouiAdbCommandCard(
                command = command,
                busy = state.busy,
                onCopy = { context.copyStr(command) },
            )
        }
    }
}

/**
 * 端口输入：关闭时可编辑，开启后与忙碌时只读。
 * 非法态交给输入框自己的描边表达，不额外加红字前缀。
 */
@Composable
private fun CouiAdbPortField(state: AdbDebugDialogState) {
    val invalid = state.error == AdbDebugError.PortBlank ||
            state.error == AdbDebugError.PortOutOfRange
    CouiTextField(
        value = state.portText,
        onValueChange = state::updatePortText,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.enabled && !state.busy,
        label = stringResource(R.string.adb_port),
        isError = invalid,
        singleLine = true,
        keyboardOptions = AdbPortKeyboardOptions,
    )
}

/**
 * 连接命令卡：整卡点击即复制，右侧另给显式复制按钮，长按同样复制。
 * 卡壳复用 [AppCard]，它在两条线上各自取当前主题的卡片底色。
 */
@Composable
private fun CouiAdbCommandCard(command: String, busy: Boolean, onCopy: () -> Unit) {
    val colors = CouiTheme.colorScheme
    val copy = { if (!busy) onCopy() }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = copy,
        onLongClick = copy,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouiAdbCommandText(text = command, modifier = Modifier.weight(1f))
            CouiIconButton(
                onClick = copy,
                contentDescription = stringResource(R.string.copy),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    tint = colors.onSurface,
                )
            }
        }
    }
}

/**
 * 连接命令文本：命令要等宽才对齐，而 COUI 的 Text 不开放字体族，
 * 因此沿用 COUI 的字号语义、只把字体族换成等宽，用基础文本绘制。
 */
@Composable
private fun CouiAdbCommandText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = rememberCouiTextStyle(CouiTextKind.Search).copy(
            color = CouiTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        ),
    )
}

/**
 * 错误提示：COUI 色板没有 error 角色，用满强度 onSurface 与常态正文区分，
 * 口径与输入框的非法描边保持一致。
 */
@Composable
private fun CouiAdbErrorText(error: AdbDebugError) {
    CouiText(
        text = stringResource(
            when (error) {
                AdbDebugError.PortBlank -> R.string.adb_debug_port_cannot_null
                AdbDebugError.PortOutOfRange -> R.string.adb_port_out_of_range
                AdbDebugError.OperationFailed -> R.string.adb_debug_operation_failed
            }
        ),
        kind = CouiTextKind.GroupFooter,
        color = CouiTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
}
