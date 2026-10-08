package com.luckyzyx.luckytool.ui.compose.special

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.IRefreshRateController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DisplayMode
import com.luckyzyx.luckytool.service.RefreshRateService
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchCard
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedRadioItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixRadioItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsAutoStart
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyFpsCur
import com.luckyzyx.luckytool.utils.SettingsPrefs
import kotlinx.coroutines.suspendCancellableCoroutine
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/** 沿 ContextWrapper 链向上找 Activity（ComposeView 的 LocalContext 可能是包装 Context） */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * ForceFps 页（旧 ui.fragment.extension.ForceFpsFragment 的 Compose 等价物）。
 * 刷新率控制器状态在 RootService 控制器而非 prefs，故整页用 custom() 手工构建
 * （不用 DSL switch/slider）。下拉刷新重新拉取 controller 并重算全部状态。
 *
 * 线分派：行呈现（`PrefGroup` / `PrefSwitchCard`）由共享层按 [LocalUiMode] 自行分派，
 * 页面只分派自己写死的 material 件 —— 文字（`MiuixText`）、重置按钮（miuix `Button`）、
 * 模式单选项（t11 的 `MiuixRadioItem`，material 线仍为 `SegmentedRadioItem`）。
 */
object ForceFpsPage {

    /** 由内容组合时注册的加载器驱动 onRefresh */
    private var reloader: (suspend () -> Unit)? = null

    val spec = ScopePageSpec(
        pageKey = "force_fps",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { reloader?.invoke() },
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        custom(key = "force_fps_body") {
            var controller by remember { mutableStateOf<IRefreshRateController?>(null) }

            suspend fun fetchController(): IRefreshRateController? =
                suspendCancellableCoroutine { cont ->
                    val activity = c.findActivity()
                    if (activity == null) cont.resume(null) { _, _, _ -> }
                    else RefreshRateService.get(activity) {
                        cont.resume(it) { _, _, _ -> }
                    }
                }

            suspend fun reload() {
                controller = fetchController()
            }
            reloader = ::reload
            DisposableEffect(Unit) {
                onDispose { reloader = null }
            }
            LaunchedEffect(Unit) { reload() }

            // AIDL 声明为裸 List（无泛型），与旧代码一致地强转为 ArrayList<DisplayMode>
            @Suppress("UNCHECKED_CAST")
            val modes =
                (controller?.supportModes ?: ArrayList<DisplayMode>()) as ArrayList<DisplayMode>
            val isUnsupport = modes.isEmpty()
            val fpsCur = state.getInt(keyFpsCur, -1)
            val fpsAutostart = state.getBoolean(keyFpsAutoStart, false)

            // 行呈现由共享层分派；这里只分派页面写死的 material 件
            val miuix = LocalUiMode.current == UiMode.Miuix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // fpsSelfStart（旧逻辑：controller!=null 且 !isUnsupport 且 fpsCur!=-1 才可用）
                // 卡片式开关：Miuix 线由 PrefSwitchCard 内部走 MiuixPrefItem + MiuixSwitchItem
                PrefSwitchCard(
                    title = c.getString(R.string.fps_autostart),
                    checked = fpsAutostart,
                    enabled = controller != null && !isUnsupport && fpsCur != -1,
                    onCheckedChange = { v -> state.set(keyFpsAutoStart, v) },
                )
                if (isUnsupport) {
                    if (miuix) {
                        MiuixText(
                            text = c.getString(R.string.fps_no_data),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            c.getString(R.string.fps_no_data),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    // 模式单选列表（旧 ListView CHOICE_MODE_SINGLE；id 即 index）
                    PrefGroup {
                        modes.forEachIndexed { index, mode ->
                            item {
                                val title =
                                    "${mode.id}   ${mode.width} x ${mode.height}   ${mode.refreshRate}"
                                val onSelect: () -> Unit = {
                                    state.set(keyFpsCur, index)
                                    controller?.setRefreshRateMode(index)
                                }
                                if (miuix) {
                                    // Miuix 线等价件（t11 产出，库内 RadioButtonPreference）
                                    MiuixRadioItem(
                                        title = title,
                                        selected = index == fpsCur,
                                        onClick = onSelect,
                                    )
                                } else {
                                    SegmentedRadioItem(
                                        title = title,
                                        selected = index == fpsCur,
                                        onClick = onSelect,
                                    )
                                }
                            }
                        }
                    }
                }
                // fpsShow（旧代码 isPressed 守卫 → M3 Switch onCheckedChange 仅用户手势触发）
                PrefSwitchCard(
                    title = c.getString(R.string.display_refresh_rate),
                    checked = controller?.refreshRateDisplay == true,
                    enabled = controller != null,
                    onCheckedChange = { v -> controller?.refreshRateDisplay = v },
                )
                // fpsRecover（旧 resetRefreshRate：持久化 -1 + 重置模式；开关可用性随 fpsCur==-1 自动失效）
                if (miuix) {
                    MiuixButton(
                        onClick = {
                            state.set(keyFpsCur, -1)
                            controller?.resetRefreshRateMode()
                        },
                        enabled = controller != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { MiuixText(text = c.getString(R.string.restore_default_refresh_rate)) }
                } else {
                    Button(
                        onClick = {
                            state.set(keyFpsCur, -1)
                            controller?.resetRefreshRateMode()
                        },
                        enabled = controller != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(c.getString(R.string.restore_default_refresh_rate)) }
                }
                if (miuix) {
                    MiuixText(
                        text = c.getString(R.string.fps_tips),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        c.getString(R.string.fps_tips),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
