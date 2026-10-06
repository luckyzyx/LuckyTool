package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalPmsFeature
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.hook.scopes.android.ADBInstallConfirm
import com.luckyzyx.luckytool.hook.scopes.android.ActionButtonKeyInterceptor
import com.luckyzyx.luckytool.hook.scopes.android.AllowUntrustedTouch
import com.luckyzyx.luckytool.hook.scopes.android.AppSplashScreen
import com.luckyzyx.luckytool.hook.scopes.android.BatteryOptimizationWhitelist
import com.luckyzyx.luckytool.hook.scopes.android.DarkModeService
import com.luckyzyx.luckytool.hook.scopes.android.DisableAccessibilityWarningDialog
import com.luckyzyx.luckytool.hook.scopes.android.DisableAppArchiving
import com.luckyzyx.luckytool.hook.scopes.android.DisableAudioFocus
import com.luckyzyx.luckytool.hook.scopes.android.DisableMaliciousAppIntercept
import com.luckyzyx.luckytool.hook.scopes.android.EnableKeepNotificationWhenAppStop
import com.luckyzyx.luckytool.hook.scopes.android.EnableVideoMemcFrameInsertion
import com.luckyzyx.luckytool.hook.scopes.android.ForceAllAppsSupportSplitScreen
import com.luckyzyx.luckytool.hook.scopes.android.ForceEnable32BitSupport
import com.luckyzyx.luckytool.hook.scopes.android.HookFloatMirageWindow
import com.luckyzyx.luckytool.hook.scopes.android.HookGMSRestrict
import com.luckyzyx.luckytool.hook.scopes.android.HookIPackageManager
import com.luckyzyx.luckytool.hook.scopes.android.HookMediaProjectionManager
import com.luckyzyx.luckytool.hook.scopes.android.HookOplusWifiService
import com.luckyzyx.luckytool.hook.scopes.android.HookWindowManagerService
import com.luckyzyx.luckytool.hook.scopes.android.LTPODynamicRefreshRate
import com.luckyzyx.luckytool.hook.scopes.android.MediaVolumeLevel
import com.luckyzyx.luckytool.hook.scopes.android.MultiAppConfig
import com.luckyzyx.luckytool.hook.scopes.android.RemoveAccessDeviceLogDialog
import com.luckyzyx.luckytool.hook.scopes.android.RemoveAlwaysAllowAppStartList
import com.luckyzyx.luckytool.hook.scopes.android.RemoveAppUninstallButtonBlackList
import com.luckyzyx.luckytool.hook.scopes.android.RemovePasswordTimeoutVerification
import com.luckyzyx.luckytool.hook.scopes.android.RemoveStatusBarTopNotification
import com.luckyzyx.luckytool.hook.scopes.android.RemoveVPNActiveNotification
import com.luckyzyx.luckytool.hook.scopes.android.ReplaceSystemRootStateDetection
import com.luckyzyx.luckytool.hook.scopes.android.ScrollToTopWhiteList
import com.luckyzyx.luckytool.hook.scopes.android.SetAppUpdateDotDisplayMode
import com.luckyzyx.luckytool.hook.scopes.android.SystemEnableVolumeKeyControlFlashlight
import com.luckyzyx.luckytool.hook.scopes.android.ZoomWindowConfig
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookAndroid : YukiBaseHooker() {

    override fun onHook() {
        val osCode = getOSVersionCode

        loadHooker(HookGlobalFeatureConfig)
        loadHooker(HookGlobalSystemProperties)
        loadHooker(HookGlobalPmsFeature)
        loadHooker(HookGlobalSystemConfig)

        //禁止App启动
//        loadHooker(HookAppStartForbidden)

        //移除状态栏上层警告
        loadHooker(RemoveStatusBarTopNotification)

        //移除VPN已激活通知
        loadHooker(RemoveVPNActiveNotification)

        //Oplus Wifi Service
        loadHooker(HookOplusWifiService)

        //Hook HookWindowManagerService
        if (osCode >= 23) loadHooker(HookWindowManagerService)

        //音量阶数
        loadHooker(MediaVolumeLevel)

        //应用分身限制
        loadHooker(MultiAppConfig)

        //USB安装确认
        loadHooker(ADBInstallConfirm)

        //移除72小时密码验证
        loadHooker(RemovePasswordTimeoutVerification)

        //移除系统截屏延迟
//        loadHooker(RemoveSystemScreenshotDelay)

        //移除遮罩Splash Screen
        if (osCode >= 26) loadHooker(AppSplashScreen)

        //允许不受信任的触摸
        if (osCode >= 23) loadHooker(AllowUntrustedTouch)

        //浮窗
        loadHooker(ZoomWindowConfig())

        //暗色模式服务
        loadHooker(DarkModeService)

        //电池优化白名单
        loadHooker(BatteryOptimizationWhitelist)

        //允许APP回到顶部
        if (osCode >= 26) loadHooker(ScrollToTopWhiteList)

        //禁用访问设备日志对话框
        if (osCode >= 26) loadHooker(RemoveAccessDeviceLogDialog)

        //LTPO动态刷新率
        loadHooker(LTPODynamicRefreshRate)

        //启用音量键控制手电筒手势
        loadHooker(SystemEnableVolumeKeyControlFlashlight)

        //快捷键拦截（无操作接管 / 自定义响铃切换）
        if (osCode >= 36) loadHooker(ActionButtonKeyInterceptor)

        //全局关闭应用自动归档（空间不足自动卸载应用）
        if (osCode >= 36) loadHooker(DisableAppArchiving)

        //强制所有应用支持分屏
        if (osCode in 26..33) loadHooker(ForceAllAppsSupportSplitScreen)

        //移除应用禁止卸载黑名单
        if (osCode >= 26) loadHooker(RemoveAppUninstallButtonBlackList)

        //三方应用通话录音保护
        if (osCode == 30) loadHooker(HookMediaProjectionManager)

        //视频动态插帧
        loadHooker(EnableVideoMemcFrameInsertion)

        //安全窗口标志
//        loadHooker(OplusWindowSecureFlag)

        //App图标更新圆点
        if (osCode >= 33) loadHooker(SetAppUpdateDotDisplayMode)

        loadHooker(HookFloatMirageWindow)

        loadHooker(ReplaceSystemRootStateDetection)

        if (SDK >= A13) loadHooker(ForceEnable32BitSupport)

        loadHooker(HookGMSRestrict)

        loadHooker(EnableKeepNotificationWhenAppStop)

        loadHooker(HookIPackageManager())

        loadHooker(RemoveAlwaysAllowAppStartList)

        //禁用风险应用拦截
        if (preferences(ModulePrefs).getBoolean("disable_malicious_app_intercept", false)) {
            if (osCode >= 38) loadHooker(DisableMaliciousAppIntercept)
        }

        //禁用无障碍警告对话框
        if (preferences(ModulePrefs).getBoolean("disable_accessibility_warning_dialog", false)) {
            if (osCode >= 38) loadHooker(DisableAccessibilityWarningDialog)
        }

        //禁用音频焦点
        if (preferences(ModulePrefs).getBoolean("disable_audio_focus", false)) {
            loadHooker(DisableAudioFocus)
        }

        //Source OplusMediaControlService
//        if (false) {
//            "com.android.server.media.OplusMediaControlService".toClass().resolve().apply {
//                firstMethod {
//                    name = "setMediaControlDenyList"
//                    parameters("java.util.List")
//                }.hook {
//                    intercept()
//                }
//                firstMethod {
//                    name = "isInHistoryPlayInfoWhiteList"
//                    parameters(String::class)
//                    returnType = Boolean::class
//                }.hook {
//                    replaceToTrue()
//                }
//                firstMethod {
//                    name = "isInMediaBlackList"
//                    parameters(String::class)
//                    returnType = Boolean::class
//                }.hook {
//                    replaceToFalse()
//                }
//            }
//        }


        //三段式按键
//        loadHooker(HookAlertSlider)

//Source ScanPackageUtils
//        findClass("com.android.server.pm.ScanPackageUtils").hook {
//            injectMember {
//                method { name = "assertMinSignatureSchemeIsValid";paramCount(2) }
//                beforeHook {
//                    val clazz = "com.android.server.pm.pkg.parsing.ParsingPackageUtils"
//                        .toClassOrNull()
//                    val isSystemDir = clazz?.field { name = "PARSE_IS_SYSTEM_DIR";type(IntType) }
//                        ?.get()?.cast<Int>() ?: return@beforeHook
//                    val parseFlags = lastArg().get<Int>() ?: return@beforeHook
//                    if ((parseFlags and isSystemDir) != 0) result = null
//                }
//            }
//        }

//Source ApkSignatureVerifier
//        findClass("android.util.apk.ApkSignatureVerifier").hook {
//            injectMember {
//                method { name = "unsafeGetCertsWithoutVerification";paramCount(3) }
//                beforeHook {
//                    val clazz = "android.content.pm.SigningDetails\$SignatureSchemeVersion"
//                        .toClassOrNull()
//                    val jar = clazz?.field { name = "JAR";type(IntType) }?.get()?.cast<Int>()
//                        ?: return@beforeHook
//                    lastArg().set(jar)
//                }
//            }
//        }

//电源菜单显示延迟
//loadHooker(ReducePowerMenuDisplayDelay)

//OPLUS_FEATURE_POWERKEY_SHORT_PRESS_SHUTDOWN = "oplus.software.short_press_powerkey_shutdown";
//OPLUS_FEATURE_POWERKEY_SHUTDOWN = "oplus.software.long_press_powerkey_shutdown";
    }
}