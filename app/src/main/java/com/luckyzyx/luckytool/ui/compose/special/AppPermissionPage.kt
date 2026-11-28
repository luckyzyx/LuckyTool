package com.luckyzyx.luckytool.ui.compose.special

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefTextButton
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.utils.AppPermission
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.PermissionUtils
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用权限页（pageKey = `app_permission`）。
 *
 * 同一份内容 [AppPermissionContent] 有两个消费方，各自的宿主 Activity 不同：
 * - 设置页入口（本 [spec]）：二级页由 PageActivity 承载，正常态、可返回；
 * - 冷启动阻塞门（MainShell 内提前渲染，宿主为 MainActivity）：阻塞态，无底栏/抽屉、返回键无效、无跳过出口。
 *
 * 因此内容侧只要求「任意 Activity」，不能把人限定成 MainActivity。
 */
object AppPermissionPage {

    val spec = PageSpec(
        pageKey = "app_permission",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        custom(key = "app_permission_content") {
            // 水平内缩由内容里的 PrefGroup 按外观线给，宿主不再套 PrefRowInset
            AppPermissionContent(
                activity = LocalContext.current.findActivity(),
                blocking = false,
            )
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
 * 行级水平内缩由分组容器 [PrefGroup] 按外观线给，宿主（设置页列表 / 阻塞态整屏）都不再补，
 * 因此本页在两种宿主下排版一致。
 *
 * [activity] 是宿主 Activity（阻塞态为 MainActivity，设置页入口为 PageActivity），
 * 取不到时只展示权限名称、不给任何授权入口 —— 因此解析要认「任意 Activity」而不是某一种宿主。
 */
@SuppressLint("ReplaceWithLifecycleExtension")
@Composable
fun AppPermissionContent(
    activity: Activity?,
    blocking: Boolean,
    modifier: Modifier = Modifier,
    onAllGranted: () -> Unit = {},
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    // 本页的偏好文件（SettingsPrefs）：分组体只用展示行，也要一个 state 承接行 API
    val prefsState = remember { PrefState.of(context.applicationContext, SettingsPrefs) }

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

    // 行级水平内缩由分组容器（PrefGroup）按外观线给，宿主（设置页列表 / 阻塞态整屏）都不再补；
    // 页面底部留白也由页面自己收尾，宿主容器不提供列表底部内边距
    Column(modifier = modifier.padding(bottom = 24.dp)) {
        // ===== Root / LSPosed：纯只读，整块放最上面（不参与门禁判定，无任何跳转）=====
        // Root / Xposed 状态合并成单行 "title: value"，不再用 title + summary 两行展示
        val rootStatus = stringResource(
            if (rootInfo?.rooted == true) R.string.app_permission_root_yes else R.string.app_permission_root_no
        )
        val moduleStatus = stringResource(
            if (moduleActive) R.string.app_permission_module_active else R.string.app_permission_module_inactive
        )
        PrefGroup(state = prefsState, title = stringResource(R.string.app_permission_root_section)) {
            // 行都带显式 key：缺省 key 是条目序号，条件行一插一删会让后面各行换 key、连带换掉行状态
            // 顺序固定：Root 状态 → Root 来源 → Xposed 状态 → 框架版本
            custom(key = "root_status") {
                PrefRow(title = "${stringResource(R.string.app_permission_root_status)}: $rootStatus")
            }
            // 版本行始终在位：查询未就绪时只占位，避免整行忽隐忽现（起 shell 的查询可能要等一会儿）
            custom(key = "root_version") {
                PrefRow(title = rootInfo?.rootVersion ?: "${stringResource(R.string.root_source)} —")
            }
            custom(key = "xposed_status") {
                PrefRow(title = "${stringResource(R.string.app_permission_module_status)}: $moduleStatus")
            }
            custom(key = "framework_version") {
                PrefRow(title = rootInfo?.frameworkVersion ?: "${stringResource(R.string.framework_version)} —")
            }
        }

        PrefGroup(
            state = prefsState,
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
        ) {
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
                custom(key = kind.name) {
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
                                    granted -> PrefTextButton(
                                        text = stringResource(R.string.app_permission_granted),
                                        enabled = false,
                                        onClick = {},
                                    )

                                    onOpenSettings != null -> PrefTextButton(
                                        text = stringResource(R.string.app_permission_open_settings),
                                        onClick = onOpenSettings,
                                    )

                                    else -> PrefTextButton(
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
            // 无跳过出口：阻塞态只提供逐项授权与退出应用
            PrefGroup(state = prefsState) {
                custom(key = "exit") {
                    PrefRow(
                        title = stringResource(R.string.app_permission_exit),
                        onClick = onExit,
                    )
                }
            }
        }
    }
}

/** LocalContext 可能是包装 Context：逐层解包取宿主 Activity，取不到就是 null */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
