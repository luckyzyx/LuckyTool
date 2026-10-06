package com.luckyzyx.luckytool.ui.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.betterandroid.system.extension.component.Intent
import com.highcapable.betterandroid.ui.extension.component.fragmentManager
import com.highcapable.betterandroid.ui.extension.view.toast
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.databinding.ActivityMainBinding
import com.luckyzyx.luckytool.service.ActivityManagerService
import com.luckyzyx.luckytool.service.AdbService
import com.luckyzyx.luckytool.service.GlobalFuncService
import com.luckyzyx.luckytool.service.PackagesService
import com.luckyzyx.luckytool.service.PowerService
import com.luckyzyx.luckytool.service.RefreshRateService
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.service.UserService
import com.luckyzyx.luckytool.ui.activity.base.BaseActivity
import com.luckyzyx.luckytool.ui.compose.MainShell
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
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
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.exitModule
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.verityPackage
import kotlinx.coroutines.flow.MutableStateFlow
import org.lsposed.lsparanoid.Obfuscate
import kotlin.system.exitProcess

@Obfuscate
@Suppress("PrivatePropertyName")
open class MainActivity : BaseActivity<ActivityMainBinding>() {
    //检测Prefs状态
    private val KEY_PREFIX = classOf<MainActivity>().name + '.'
    private val EXTRA_SAVED_INSTANCE_STATE = KEY_PREFIX + "SAVED_INSTANCE_STATE"

    companion object {
        /** Function 子树 NavHostFragment 的 FM tag（MainShell/FunctionHost 共用） */
        const val FUNCTION_NAV_TAG = "function_nav_host_fragment"
    }

    /**
     * 旧功能树导航控制器（P2 过渡：Function tab 以 AndroidView 承载旧 NavHostFragment 子树，
     * 其余四个顶级页面为 Compose。P3 全量迁移完成后移除）。
     */
    lateinit var functionNavController: NavController
        private set

    /** 当前是否位于 Function tab（由 MainShell 维护，供 onResume 显示恢复） */
    var isOnFunctionTab by mutableStateOf(false)

    /** 当前是否位于 Home tab（冷启动默认 true = 起始目的地；checkOs 判断用） */
    var isOnHomeTab by mutableStateOf(true)

    /** 跨 tab 跳转请求队列：Compose 页面 → 旧功能树页面 */
    val functionNavRequests = MutableStateFlow<Pair<Int, String?>?>(null)

    private var pendingFunctionNavigation: ((NavController) -> Unit)? = null
    private var checkSuDialog: AlertDialog? = null

    private fun newIntent(context: Context): Intent {
        return Intent<MainActivity>(context)
    }

    private fun newIntent(savedInstanceState: Bundle, context: Context): Intent {
        return newIntent(context).putExtra(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFunctionNavigation()

        verityPackage()
        checkXposed()
        checkOs()
        checkUserId()
        checkBiometric()

        setContent {
            LuckyAppTheme {
                MainShell(this)
            }
        }
    }

    /**
     * 创建/复用 Function 子树 NavHostFragment（headless 或恢复态），导航控制器立即可用。
     * headless 添加（containerId=0）不创建视图；视图由 FunctionHost 组合时收养进 R.id.function_nav_host。
     */
    private fun initFunctionNavigation() {
        val fm = fragmentManager()
        var fragment = fm.findFragmentByTag(FUNCTION_NAV_TAG) as? NavHostFragment
        if (fragment == null) {
            fragment = NavHostFragment.create(R.navigation.function_nav)
            fm.beginTransaction().add(fragment, FUNCTION_NAV_TAG).commitNow()
        }
        functionNavController = fragment.navController
    }

    /** onResume 时恢复显示 Function 子树（容器已存在时）；否则交由 FunctionHost 组合时处理 */
    fun showFunctionFragmentIfNeeded() {
        if (!isOnFunctionTab) return
        val fm = fragmentManager()
        val fragment = fm.findFragmentByTag(FUNCTION_NAV_TAG) as? NavHostFragment ?: return
        if (!fragment.isAdded || !fragment.isHidden) return
        if (findViewById<View>(R.id.function_nav_host) != null) {
            fm.beginTransaction()
                .show(fragment)
                .setPrimaryNavigationFragment(fragment)
                .commitNow()
        }
    }

    /** 保存实例状态前隐藏子树：恢复时 dispatchCreate 找不到容器会崩溃 */
    override fun onSaveInstanceState(outState: Bundle) {
        val fm = fragmentManager()
        val fragment = fm.findFragmentByTag(FUNCTION_NAV_TAG) as? NavHostFragment
        if (fragment != null && fragment.isAdded && !fragment.isHidden) {
            fm.beginTransaction()
                .hide(fragment)
                .setPrimaryNavigationFragment(null)
                .commitNow()
        }
        super.onSaveInstanceState(outState)
    }

    /** Compose 页面跨 tab 跳转到旧功能树页面（先收养子树再导航，见 MainShell） */
    fun requestFunctionNavigation(id: Int, title: CharSequence?) {
        functionNavRequests.value = id to title?.toString()
    }

    /** 由 MainShell 在切换到 Function tab 前写入；FunctionHost 就绪后执行 */
    fun setPendingFunctionNavigation(id: Int, title: String?) {
        pendingFunctionNavigation = { it.navigatePage(id, title) }
    }

    /** FunctionHost 就绪后回调：执行待处理跳转 */
    fun notifyFunctionHostReady() {
        val pending = pendingFunctionNavigation
        if (pending != null) {
            pendingFunctionNavigation = null
            pending(functionNavController)
        }
    }

    private fun checkUserId() {
        if ((Process.myUid() / 100000) != 0) {
            toast(getString(R.string.check_app_userid))
            finish()
        }
    }

    private fun checkXposed() {
        XposedServiceBridge.awaitReady()
        if (!XposedServiceBridge.isModuleActive) {
            MaterialAlertDialogBuilder(this).apply {
                setCancelable(false)
                setMessage(getString(R.string.unsupported_xposed))
                setPositiveButton(android.R.string.ok) { _, _ -> exitProcess(0) }
                setOnDismissListener { exitModule() }
                show()
            }
            return
        }
    }

    private fun checkSu() {
        val isSu = DeviceUtils.getRootStatus()
        putBoolean(SettingsPrefs, "is_su", isSu)
        putBoolean(SettingsPrefs, "settings_prefs", isSu)
        putBoolean(ModulePrefs, "module_prefs", isSu)
        putBoolean(IntentPrefs, "intent_prefs", isSu)
        putBoolean(OtherPrefs, "other_prefs", isSu)
        if (!isSu && (checkSuDialog == null || !checkSuDialog!!.isShowing)) {
            checkSuDialog = MaterialAlertDialogBuilder(this, dialogCentered).apply {
                setCancelable(false)
                setTitle(getString(R.string.no_root))
                setMessage(getString(R.string.no_root_summary))
                setPositiveButton(android.R.string.ok) { _, _ -> exitProcess(0) }
                setOnDismissListener { exitModule() }
            }.show()
            return
        }
        putBoolean(SettingsPrefs, "enable_module_print_logs", BuildConfig.DEBUG)
        PermissionUtils(this).start()
    }

    private fun checkOs() {
        val osCode = getOSVersionCode
        MaterialAlertDialogBuilder(this, dialogCentered).apply {
            setCancelable(false)
            setTitle(getString(R.string.unsupported_os))
            setMessage(getString(R.string.unsupported_os_summary))
            setPositiveButton(android.R.string.ok) { _, _ -> exitProcess(0) }
            if (osCode > 0) setNeutralButton(getString(R.string.ignore), null)
            if (osCode < 23 && isOnHomeTab) show()
        }
    }

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
        initAllService()
        showFunctionFragmentIfNeeded()
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
