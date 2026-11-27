package com.luckyzyx.luckytool.ui.compose.special

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefCategoryHeader
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.components.coui.CouiTextButton
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.AppPermission
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.PermissionUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用权限页（pageKey = `app_permission`）。
 *
 * 同一份内容 [AppPermissionContent] 有两个消费方，各自的宿主 Activity 不同：
 * - 设置页入口（本 [spec]）：二级页由 SecondaryActivity 承载，正常态、可返回；
 * - 冷启动阻塞门（MainShell 内提前渲染，宿主为 MainActivity）：阻塞态，无底栏/抽屉、返回键无效、无跳过出口。
 *
 * 因此内容侧只要求「任意 Activity」，不能把人限定成 MainActivity。
 */
object AppPermissionPage {

    val spec = ScopePageSpec(
        pageKey = "app_permission",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        custom(key = "app_permission_content") {
            // custom() 逃生舱：行内容需自带 16dp 内缩
            PrefRowInset {
                AppPermissionContent(
                    activity = LocalContext.current.findActivity(),
                    blocking = false,
                )
            }
        }
    }
}

/** 权限行的查询状态快照（[PermissionUtils] 的纯查询结果） */
private data class PermissionRowState(val granted: Boolean, val doNotAskAgain: Boolean)

/** Root / LSPosed 只读信息（查询要起 shell，放 IO 线程） */
private data class RootInfo(
    val rooted: Boolean,
    val rootVersion: String,
    val frameworkVersion: String,
)

/**
 * 应用权限页内容（阻塞态与设置页入口共用）。
 *
 * [blocking] = true 时是冷启动阻塞态：4 项齐全后回调 [onAllGranted] 放行，并显示「退出应用」。
 * 本页只做纯查询展示，申请与跳转都只能由用户手势发起（行右侧按钮或整行点击）。
 *
 * [selfInset] = true 表示宿主不提供行级水平内缩（阻塞态独立成屏），由本页自补，否则内容贴边；
 * 被作用域页列表包裹时（设置页入口）保持 false，避免与宿主内缩叠加成 24/32dp。
 *
 * [activity] 是宿主 Activity（阻塞态为 MainActivity，设置页入口为 SecondaryActivity），
 * 取不到时只展示权限名称、不给任何授权入口 —— 因此解析要认「任意 Activity」而不是某一种宿主。
 */
@SuppressLint("ReplaceWithLifecycleExtension")
@Composable
fun AppPermissionContent(
    activity: Activity?,
    blocking: Boolean,
    modifier: Modifier = Modifier,
    selfInset: Boolean = false,
    onAllGranted: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current

    // 授予状态快照：revision 自增即重算（从系统设置页返回、逐项申请回调后）
    var revision by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // 去系统设置页授权后返回：只重新查询状态，不申请、不跳转
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val states = remember(activity, revision) {
        if (activity == null) {
            emptyMap()
        } else {
            AppPermission.entries.associateWith { kind ->
                PermissionRowState(
                    granted = PermissionUtils.isGranted(activity, kind),
                    doNotAskAgain = PermissionUtils.isDoNotAskAgain(activity, kind),
                )
            }
        }
    }
    val allGranted = states.isNotEmpty() && states.values.all { it.granted }
    // 4 项齐全即放行（不重启 Activity）：阻塞标志由 MainActivity 的纯查询驱动
    LaunchedEffect(blocking, allGranted) {
        if (blocking && allGranted) onAllGranted()
    }

    // Root / LSPosed 纯只读展示：查询要起 shell，放 IO 线程，进入页面查一次
    val rootInfo by produceState<RootInfo?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            RootInfo(
                rooted = DeviceUtils.getRootStatus(),
                rootVersion = DeviceUtils.getRootVersion(context),
                frameworkVersion = DeviceUtils.getFrameWorkVersion(context),
            )
        }
    }
    val moduleActive by XposedServiceBridge.isModuleActive.collectAsStateWithLifecycle()

    // selfInset：阻塞态独立成屏、宿主列表不在场时补行级水平内缩。
    // COUI 线由 CouiCard / 库的 Group 自带头 16dp，本页再叠加会把卡片顶到 32dp，故只对 Material 线生效；
    // 设置页入口 selfInset 恒为 false，仍由宿主列表提供，保持不叠加
    val lineInset = if (LocalUiMode.current == UiMode.Coui) 0.dp else 16.dp
    Column(
        modifier = modifier.then(
            if (selfInset) Modifier.padding(horizontal = lineInset) else Modifier
        )
    ) {
        // ===== Root / LSPosed：纯只读，整块放最上面（不参与门禁判定，无任何跳转）=====
        PrefCategoryHeader(
            title = stringResource(R.string.app_permission_root_section),
            materialInset = 0.dp,
        )
        // Root / Xposed 状态合并成单行 "title: value"，不再用 title + summary 两行展示
        val rootStatus = stringResource(
            if (rootInfo?.rooted == true) R.string.app_permission_root_yes else R.string.app_permission_root_no
        )
        val moduleStatus = stringResource(
            if (moduleActive) R.string.app_permission_module_active else R.string.app_permission_module_inactive
        )
        PrefGroup {
            item {
                PrefRow(title = "${stringResource(R.string.app_permission_root_status)}: $rootStatus")
            }
            val info = rootInfo
            // 顺序固定：Root 来源 → Xposed 状态 → 框架版本
            if (info != null && info.rootVersion.isNotBlank()) {
                item { PrefRow(title = info.rootVersion) }
            }
            item {
                PrefRow(title = "${stringResource(R.string.app_permission_module_status)}: $moduleStatus")
            }
            if (info != null && info.frameworkVersion.isNotBlank()) {
                item { PrefRow(title = info.frameworkVersion) }
            }
        }

        Spacer(Modifier.height(12.dp))
        PrefCategoryHeader(
            title = stringResource(
                if (blocking) R.string.app_permission_blocked_title else R.string.app_permission
            ),
            summary = stringResource(
                if (blocking) {
                    R.string.app_permission_blocked_summary
                } else {
                    R.string.app_permission_summary
                }
            ),
            materialInset = 0.dp,
        )
        PrefGroup {
            AppPermission.entries.forEach { kind ->
                val state = states[kind]
                val granted = state?.granted == true
                val doNotAskAgain = state?.doNotAskAgain == true
                // 授权：只申请、不自动跳转，回调后重算状态
                val onGrant: (() -> Unit)? = if (!granted && activity != null) {
                    { PermissionUtils(activity).request(kind) { revision++ } }
                } else {
                    null
                }
                // 被「不再询问」永久拒绝：只有用户点「去设置」才跳系统设置页
                val onOpenSettings: (() -> Unit)? =
                    if (!granted && doNotAskAgain && activity != null) {
                        { PermissionUtils(activity).openSystemPermissionSettings(kind) }
                    } else {
                        null
                    }
                item {
                    PrefRow(
                        title = stringResource(kind.labelRes),
                        summary = stringResource(
                            if (granted) {
                                R.string.app_permission_granted
                            } else {
                                R.string.app_permission_denied
                            }
                        ),
                        // 整行点击与右侧按钮同义：永久拒绝时两者都只跳系统设置（且必须由用户手势发起）
                        onClick = onOpenSettings ?: onGrant,
                        // 右侧按钮给出明确可点的目标：授权 / 已授权（弱化不可点）/ 去设置
                        trailing = if (activity == null) {
                            null
                        } else {
                            {
                                when {
                                    granted -> ActionButton(
                                        text = stringResource(R.string.app_permission_granted),
                                        enabled = false,
                                        onClick = {},
                                    )

                                    onOpenSettings != null -> ActionButton(
                                        text = stringResource(R.string.app_permission_open_settings),
                                        onClick = onOpenSettings,
                                    )

                                    else -> ActionButton(
                                        text = stringResource(R.string.app_permission_action_grant),
                                        onClick = onGrant ?: {},
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        if (blocking) {
            Spacer(Modifier.height(24.dp))
            // 无跳过出口：阻塞态只提供逐项授权与退出应用
            PrefCard(
                title = stringResource(R.string.app_permission_exit),
                onClick = onExit,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 动作按钮：Material 线用 material3 `TextButton`，COUI 线用 [CouiTextButton]；
 * [enabled] = false 走禁用弱化样式（COUI 侧把文字降到 onSurfaceVariant 低透明度）。
 */
@Composable
private fun ActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    when (LocalUiMode.current) {
        UiMode.Material -> TextButton(onClick = onClick, enabled = enabled) { Text(text) }
        UiMode.Coui -> CouiTextButton(text = text, onClick = onClick, enabled = enabled)
    }
}

/** LocalContext 可能是包装 Context：逐层解包取宿主 Activity，取不到就是 null */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
