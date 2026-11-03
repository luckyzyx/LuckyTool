package com.luckyzyx.luckytool.ui.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.highcapable.betterandroid.system.extension.component.Intent
import com.highcapable.betterandroid.ui.extension.component.launch
import com.highcapable.betterandroid.ui.extension.view.toast
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.ActivityManagerService
import com.luckyzyx.luckytool.service.AdbService
import com.luckyzyx.luckytool.service.GlobalFuncService
import com.luckyzyx.luckytool.service.PackagesService
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.service.RefreshRateService
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.compose.ActivationLoadingLayer
import com.luckyzyx.luckytool.ui.compose.FunctionRequest
import com.luckyzyx.luckytool.ui.compose.LuckySplashHost
import com.luckyzyx.luckytool.ui.compose.MainShell
import com.luckyzyx.luckytool.ui.compose.components.GateDialogSpec
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.service.awaitActivation
import com.luckyzyx.luckytool.ui.theme.LuckyAppTheme
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.BiometricUtils
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.PermissionUtils
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.verityPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.lsposed.lsparanoid.Obfuscate

/** 激活检测超时（ms）：框架服务绑定是异步的，超过这个时间仍未激活就按未激活处理 */
private const val ActivationCheckTimeoutMillis = 3000L

@Obfuscate
@Suppress("PrivatePropertyName")
open class MainActivity : AppCompatActivity() {
    //检测Prefs状态
    private val KEY_PREFIX = classOf<MainActivity>().name + '.'
    private val EXTRA_SAVED_INSTANCE_STATE = KEY_PREFIX + "SAVED_INSTANCE_STATE"

    /** 当前是否位于 Function tab（由 MainShell 维护，供 onResume 显示恢复） */
    var isOnFunctionTab by mutableStateOf(false)

    /** 当前是否位于 Home tab（冷启动默认 true = 起始目的地；checkOs 判断用） */
    var isOnHomeTab by mutableStateOf(true)

    /** 跨标签页打开作用域页，由 MainShell 入栈并保留来源标签。 */
    val functionNavRequests = MutableStateFlow<FunctionRequest?>(null)

    /** 启动阻塞门对话框（Xposed 未激活 / 无 root / 系统不支持）：非空即由 MainShell 渲染，见 GateDialog */
    var gateDialog by mutableStateOf<GateDialogSpec?>(null)

    /** 权限阻塞门是否生效（模块自身 4 项权限未全授予）：只由 [refreshPermissionGate] 的纯查询驱动 */
    var isPermissionGateActive by mutableStateOf(false)
        private set

    /** 激活检测结果是否仍未决（框架服务绑定是异步的）：未决时只组合加载层，出结果即取消，见 [checkXposed] */
    var isActivationPending by mutableStateOf(false)
        private set

    /** 当前阻塞门是否由「未激活」触发：激活迟到到达时要撤掉它，不去动无 root / 系统不支持的门 */
    private var activationGateShown = false

    private fun newIntent(context: Context): Intent {
        return Intent<MainActivity>(context)
    }

    private fun newIntent(savedInstanceState: Bundle, context: Context): Intent {
        return newIntent(context).putExtra(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeUtils.initTheme(this)

        verityPackage()
        checkXposed()
        checkOs()
        checkUserId()

        // 指纹校验的开关在 SettingsPrefs 上，这份 prefs 只读本地文件（见 Context.appPrefs），
        // 与框架 service 绑定无关，所以不必等激活检测结果
        checkBiometric()

        // 冷启动不申请任何权限：这里只做纯查询，检测出缺项就由 MainShell 进入权限阻塞页，
        // 由用户点按钮逐项授权（申请只能由用户手势发起，见 AppPermissionPage）
        refreshPermissionGate()

        setContent {
            // 闪屏与加载层是中性配色、不读主题；主界面的页面数据来自 ModulePrefs（remote 优先），
            // 绑定没落定时读到的还是默认值，所以激活检测出结果后再组合主界面
            LuckySplashHost {
                if (isActivationPending) {
                    ActivationLoadingLayer()
                } else {
                    LuckyAppTheme {
                        MainShell(this)
                    }
                }
            }
        }
    }

    /** Compose 页面跨 tab 跳转到 Function 子树作用域页（pageKey 见 ScopePageRegistry） */
    fun requestFunctionNavigation(pageKey: String, title: CharSequence?) {
        functionNavRequests.value = FunctionRequest(pageKey, title?.toString())
    }

    private fun checkUserId() {
        if ((Process.myUid() / 100000) != 0) {
            toast(getString(R.string.check_app_userid))
            finish()
        }
    }

    private fun checkXposed() {
        if (XposedServiceBridge.isModuleActive.value) return
        // 框架服务绑定是异步的：等待期间只组合加载层（见 setContent），检测到激活即取消显示
        isActivationPending = true
        launch {
            val activated = XposedServiceBridge.isModuleActive
                .awaitActivation(ActivationCheckTimeoutMillis)
            isActivationPending = false
            if (activated) return@launch
            // 超时仍未激活：弹未激活门（无 root / 系统不支持的门可能已经先弹，不抢它）
            if (gateDialog == null) {
                activationGateShown = true
                gateDialog = GateDialogSpec(message = getString(R.string.unsupported_xposed))
            }
            // 激活迟到（框架服务起得慢）时撤掉未激活门：主界面已按本地 prefs 组合过了，
            // 重建一次让主题和页面数据重新走 remote prefs，免得显示的默认值与写入目标不一致
            XposedServiceBridge.isModuleActive.first { it }
            if (activationGateShown) {
                activationGateShown = false
                gateDialog = null
                restart()
            }
        }
    }

    private fun checkSu() {
        val isSu = DeviceUtils.getRootStatus()
        putBoolean(SettingsPrefs, "is_su", isSu)
        putBoolean(SettingsPrefs, "settings_prefs", isSu)
        putBoolean(ModulePrefs, "module_prefs", isSu)
        putBoolean(IntentPrefs, "intent_prefs", isSu)
        putBoolean(OtherPrefs, "other_prefs", isSu)
        if (!isSu && gateDialog == null) {
            gateDialog = GateDialogSpec(
                title = getString(R.string.no_root),
                message = getString(R.string.no_root_summary),
            )
            return
        }
        putBoolean(SettingsPrefs, "enable_module_print_logs", BuildConfig.DEBUG)
        // 权限只做纯查询（MainActivity.refreshPermissionGate）：
        // 本方法在 onResume 里复用，因此这里不得发起任何请求或跳转
    }

    private fun checkOs() {
        val osCode = getOSVersionCode
        if (osCode < 23 && isOnHomeTab) {
            gateDialog = GateDialogSpec(
                title = getString(R.string.unsupported_os),
                message = getString(R.string.unsupported_os_summary),
                // osCode > 0（系统有上报版本号）时额外提供「忽略」，点击只关闭对话框、不改其它门
                neutralLabel = if (osCode > 0) getString(R.string.ignore) else null,
            )
        }
    }

    /** 生物识别解锁校验：开关在 SettingsPrefs（只读本地文件），冷启动时直接调用即可，与框架服务绑定无关 */
    private fun checkBiometric() {
        val enable = getBoolean(SettingsPrefs, "enable_biometric_unlock_verification", false)
        val manager = getSystemService(classOf<KeyguardManager>())
        if (enable && manager.isDeviceSecure) {
            BiometricUtils.showBiometricPrompt(
                this,
                onError = { _, _ -> finish() },
                onFailed = { finish() })
        }
    }

    private fun initAllService() {
        GlobalFuncService.init(this)
        PackagesService.init(this)
        TilesService.init(this)
        RefreshRateService.init(this)
        AdbService.init(this)
        ActivityManagerService.init(this)
        UserService.init(this)
        PowerService.init(this)
    }

    override fun onResume() {
        super.onResume()
        checkSu()
        // 纯查询：只驱动权限阻塞门，绝不发起申请、绝不跳转
        refreshPermissionGate()
        initAllService()
    }

    /**
     * 纯查询刷新权限阻塞门（模块自身 4 项权限）：不申请、不跳转。
     * onResume（从系统设置页返回）与权限页放行回调共用；4 项齐全时门自动关闭，无需重启 Activity。
     */
    fun refreshPermissionGate() {
        isPermissionGateActive = !PermissionUtils.areAllGranted(this)
    }

    override fun onDestroy() {
        // 宿主实例消亡：撤销属于它的在途权限请求，避免 requesting 在进程内永久卡住阻塞页
        PermissionUtils.onHostDestroyed(this)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    fun restart() {
        if (SDK >= A12 || !Process.isApplicationUid(Process.myUid())) {
            recreate()
        } else {
            try {
                val savedInstanceState = Bundle()
                onSaveInstanceState(savedInstanceState)
                finish()
                startActivity(newIntent(savedInstanceState, this))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (_: Throwable) {
                recreate()
            }
        }
    }
}
