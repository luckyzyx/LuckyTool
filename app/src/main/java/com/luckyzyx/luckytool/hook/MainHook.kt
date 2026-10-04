package com.luckyzyx.luckytool.hook

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.annotation.xposed.YukiHookLibXposedEntry
import com.highcapable.yukihookapi.hook.factory.configure
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.YukiHookXposedModule
import com.highcapable.yukihookapi.hook.xposed.bridge.event.registerFrameworkEvents
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onModuleLoaded
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onPackageLoaded
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onPackageReady
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onSystemServerStarting
import com.luckyzyx.luckytool.hook.hookers.HookAlarmClock
import com.luckyzyx.luckytool.hook.hookers.HookAndroid
import com.luckyzyx.luckytool.hook.hookers.HookAudioEffectCenter
import com.luckyzyx.luckytool.hook.hookers.HookAudioMonitor
import com.luckyzyx.luckytool.hook.hookers.HookBattery
import com.luckyzyx.luckytool.hook.hookers.HookBeaconLink
import com.luckyzyx.luckytool.hook.hookers.HookBrowser
import com.luckyzyx.luckytool.hook.hookers.HookCalendar
import com.luckyzyx.luckytool.hook.hookers.HookCamera
import com.luckyzyx.luckytool.hook.hookers.HookClaw
import com.luckyzyx.luckytool.hook.hookers.HookCloudService
import com.luckyzyx.luckytool.hook.hookers.HookDirectUI
import com.luckyzyx.luckytool.hook.hookers.HookEngineerMode
import com.luckyzyx.luckytool.hook.hookers.HookExternalStorage
import com.luckyzyx.luckytool.hook.hookers.HookFileManager
import com.luckyzyx.luckytool.hook.hookers.HookGallery
import com.luckyzyx.luckytool.hook.hookers.HookGesture
import com.luckyzyx.luckytool.hook.hookers.HookHealth
import com.luckyzyx.luckytool.hook.hookers.HookKeyguardClock
import com.luckyzyx.luckytool.hook.hookers.HookKeyguardPersonality
import com.luckyzyx.luckytool.hook.hookers.HookLauncher
import com.luckyzyx.luckytool.hook.hookers.HookMarket
import com.luckyzyx.luckytool.hook.hookers.HookMediaController
import com.luckyzyx.luckytool.hook.hookers.HookMultiApp
import com.luckyzyx.luckytool.hook.hookers.HookNfc
import com.luckyzyx.luckytool.hook.hookers.HookNotificationManager
import com.luckyzyx.luckytool.hook.hookers.HookOShare
import com.luckyzyx.luckytool.hook.hookers.HookOplusCosa
import com.luckyzyx.luckytool.hook.hookers.HookOplusGames
import com.luckyzyx.luckytool.hook.hookers.HookOplusMMS
import com.luckyzyx.luckytool.hook.hookers.HookOplusOta
import com.luckyzyx.luckytool.hook.hookers.HookPackageInstaller
import com.luckyzyx.luckytool.hook.hookers.HookPermissionController
import com.luckyzyx.luckytool.hook.hookers.HookPhone
import com.luckyzyx.luckytool.hook.hookers.HookPhoneManager
import com.luckyzyx.luckytool.hook.hookers.HookPictorial
import com.luckyzyx.luckytool.hook.hookers.HookQuickSearchBox
import com.luckyzyx.luckytool.hook.hookers.HookSafeCenter
import com.luckyzyx.luckytool.hook.hookers.HookScreenshot
import com.luckyzyx.luckytool.hook.hookers.HookSecuritypPermission
import com.luckyzyx.luckytool.hook.hookers.HookSettings
import com.luckyzyx.luckytool.hook.hookers.HookSmartSidebar
import com.luckyzyx.luckytool.hook.hookers.HookSoundRecorder
import com.luckyzyx.luckytool.hook.hookers.HookSpeechAssist
import com.luckyzyx.luckytool.hook.hookers.HookSystemUI
import com.luckyzyx.luckytool.hook.hookers.HookThemeStore
import com.luckyzyx.luckytool.hook.hookers.HookUIEngine
import com.luckyzyx.luckytool.hook.hookers.HookWeather
import com.luckyzyx.luckytool.hook.hookers.HookWirelessSettings
import com.luckyzyx.luckytool.hook.scopes.otherapp.HookADM
import com.luckyzyx.luckytool.hook.scopes.otherapp.HookAlphaBackupPro
import com.luckyzyx.luckytool.hook.scopes.otherapp.HookFakeGpsJoyStick
import com.luckyzyx.luckytool.hook.scopes.otherapp.HookKsWeb
import io.github.lsposed.disableflagsecure.DisableFlagSecure
import org.lsposed.corepatch.XposedMain
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@YukiHookLibXposedEntry(
    entryClassName = "Entry",
    minApiVersion = 102,
    targetApiVersion = 102,
    hotReload = YukiHookLibXposedEntry.HotReload.MANUAL,
    scope = [
        "system",
        "com.android.systemui",
        "com.oplus.battery",
        "com.android.settings",
        "com.android.packageinstaller",
        "com.android.launcher",
        "com.oppo.launcher",
        "com.oneplus.camera",
        "com.oplus.camera",
        "com.coloros.gallery3d",
        "com.oplus.safecenter",
        "com.coloros.safecenter",
        "com.coloros.alarmclock",
        "com.oplus.notificationmanager",
        "com.coloros.phonemanager",
        "com.oplus.aod",
        "com.oplus.uiengine",
        "com.heytap.themestore",
        "com.oplus.games",
        "com.oplus.cosa",
        "com.heytap.cloud",
        "com.oplus.screenshot",
        "com.oplus.ota",
        "com.oplus.sau",
        "com.heytap.pictorial",
        "com.android.mms",
        "com.android.incallui",
        "com.android.phone",
        "com.android.externalstorage",
        "com.heytap.browser",
        "com.oplus.gesture",
        "com.android.permissioncontroller",
        "com.coloros.directui",
        "com.heytap.quicksearchbox",
        "com.heytap.market",
        "com.coloros.weather2",
        "com.coloros.calendar",
        "com.coloros.smartsidebar",
        "com.oplus.multiapp",
        "com.coloros.soundrecorder",
        "com.oplus.audiomonitor",
        "com.oplus.atlas",
        "com.oplus.audio.effectcenter",
        "com.oplus.appplatform",
        "com.oplus.eyeprotect",
        "com.oplus.mediacontroller",
        "com.oplus.exsystemservice",
        "com.oplus.keyguard.clock.base",
        "com.oplus.keyguard.personality.clocks",
        "com.oplus.beaconlink",
        "com.heytap.speechassist",
        "com.oplus.wirelesssettings",
        "com.heytap.health",
        "com.android.nfc",
        "com.coloros.oshare",
        "com.android.bluetooth",
        "com.android.contacts",
        "com.oplus.securitypermission",
        "com.coloros.filemanager",
        "com.oplus.engineermode",
        "com.heytap.mydevices",
        "com.heytap.accessory",
        "com.heytap.mcs",
        "com.oplus.claw",
        "ru.kslabs.ksweb",
        "com.dv.adm",
        "com.ruet_cse_1503050.ragib.appbackup.pro",
        "com.theappninjas.fakegpsjoystick",
    ],
)
@Obfuscate
class MainHook : YukiHookXposedModule {

    override fun onInit() {
        configure {
            logging {
                tag = MainHook.TAG
            }
        }
        registerFrameworkEvents {
            val corePatch = XposedMain()
            val disableFlagSecure = DisableFlagSecure()
            onModuleLoaded {
                corePatch.onModuleLoaded(this)
                disableFlagSecure.onModuleLoaded(this)
            }
            onPackageLoaded { }
            onPackageReady {
                val prefs = getRemotePreferences("ModulePrefs")
                val disableFlagEnable = !prefs.getBoolean("disable_flag_secure", false)
                if (disableFlagEnable) disableFlagSecure.onPackageReady(it)
            }
            onSystemServerStarting {
                val prefs = getRemotePreferences("ModulePrefs")
                corePatch.onSystemServerStarting(it)
                val disableFlagEnable = !prefs.getBoolean("disable_flag_secure", false)
                if (disableFlagEnable) disableFlagSecure.onSystemServerStarting(it)
            }
        }
    }

    companion object {
        const val TAG = "LuckyTool"

        @SuppressLint("SdCardPath")
        fun isMasterEnabled(): Boolean {
            return try {
                File("/sdcard/disable_lt").exists()
            } catch (_: Throwable) {
                true
            }
        }
    }

    override fun onHook() = encase {
        if (isMasterEnabled()) return@encase

        //系统框架
        loadSystem(HookAndroid)

        //系统界面
        loadApp("com.android.systemui", HookSystemUI)

        //经典主题 Clock
        loadApp("com.oplus.keyguard.clock.base", HookKeyguardClock)
        loadApp("com.oplus.keyguard.personality.clocks", HookKeyguardPersonality)

        //通知管理
        loadApp("com.oplus.notificationmanager", HookNotificationManager)

        //时钟
        loadApp("com.coloros.alarmclock", HookAlarmClock)

        //桌面
        loadApp("com.oppo.launcher", HookLauncher)
        loadApp("com.android.launcher", HookLauncher)

        //百变引擎
        loadApp("com.oplus.uiengine", HookUIEngine)

        //截屏
        loadApp("com.oplus.screenshot", HookScreenshot)

        //安全中心
        loadApp("com.oplus.safecenter", HookSafeCenter)
        loadApp("com.coloros.safecenter", HookSafeCenter)

        //应用安装器
        loadApp("com.android.packageinstaller", HookPackageInstaller)

        //外部存储设备
        loadApp("com.android.externalstorage", HookExternalStorage)

        //电池
        loadApp("com.oplus.battery", HookBattery)

        //设置
        loadApp("com.android.settings", HookSettings)

        //相机
        loadApp("com.oplus.camera", HookCamera)
        loadApp("com.oneplus.camera", HookCamera)

        //相册
        loadApp("com.coloros.gallery3d", HookGallery)

        //主题商店
        loadApp("com.heytap.themestore", HookThemeStore)
        loadApp("com.oplus.themestore", HookThemeStore)

        //云服务
        loadApp("com.heytap.cloud", HookCloudService)

        //游戏助手
        loadApp("com.oplus.games", HookOplusGames)

        //应用增强服务
        loadApp("com.oplus.cosa", HookOplusCosa)

        //软件更新
        loadApp("com.oplus.ota", HookOplusOta)

        //乐划锁屏
        loadApp("com.heytap.pictorial", HookPictorial)

        //信息
        loadApp("com.android.mms", HookOplusMMS)

        //电话服务
        loadApp("com.android.phone", HookPhone)

        //浏览器
        loadApp("com.heytap.browser", HookBrowser)

        //手势体感
        loadApp("com.oplus.gesture", HookGesture)

        //权限控制器
        loadApp("com.android.permissioncontroller", HookPermissionController)

        //小布助手
        loadApp("com.heytap.speechassist", HookSpeechAssist)

        //小布识屏
        loadApp("com.coloros.directui", HookDirectUI)

        //全局搜索
        loadApp("com.heytap.quicksearchbox", HookQuickSearchBox)

        //软件商店
        loadApp("com.heytap.market", HookMarket)

        //天气
        loadApp("com.coloros.weather2", HookWeather)

        //日历
        loadApp("com.coloros.calendar", HookCalendar)

        //智能侧边栏
        loadApp("com.coloros.smartsidebar", HookSmartSidebar)

        //手机管家
        loadApp("com.coloros.phonemanager", HookPhoneManager)

        //应用分身
        loadApp("com.oplus.multiapp", HookMultiApp)

        //录音
        loadApp("com.coloros.soundrecorder", HookSoundRecorder)

        //三方应用通话录音 / 智慧语音
        loadApp("com.oplus.audiomonitor", HookAudioMonitor)

        //audioEffectCenter
        loadApp("com.oplus.audio.effectcenter", HookAudioEffectCenter)

        //MediaController
        loadApp("com.oplus.mediacontroller", HookMediaController)

        //无网畅聊
        loadApp("com.oplus.beaconlink", HookBeaconLink)

        //无线设置
        loadApp("com.oplus.wirelesssettings", HookWirelessSettings)

        //健康
        loadApp("com.heytap.health", HookHealth)

        //NFC服务
        loadApp("com.android.nfc", HookNfc)

        //互传
        loadApp("com.coloros.oshare", HookOShare)

        //权限管理
        loadApp("com.oplus.securitypermission", HookSecuritypPermission)

        //文件管理
        loadApp("com.coloros.filemanager", HookFileManager)

        //工程模式
        loadApp("com.oplus.engineermode", HookEngineerMode)

        loadApp("com.oplus.claw", HookClaw)

        //其他APP
        loadApp("com.theappninjas.fakegpsjoystick", HookFakeGpsJoyStick)
        loadApp("com.ruet_cse_1503050.ragib.appbackup.pro", HookAlphaBackupPro)
        loadApp("ru.kslabs.ksweb", HookKsWeb)
        loadApp("com.dv.adm", HookADM)
    }

}
