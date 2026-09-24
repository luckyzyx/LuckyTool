package com.luckyzyx.luckytool.hook

import com.luckyzyx.luckytool.hook.core.HookRouter
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
import com.luckyzyx.luckytool.hook.hookers.HookSecurePay
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
import org.lsposed.lsparanoid.Obfuscate

/**
 * libxposed 路由注册表：全部迁移自 legacy YukiEntry 的 loadApp/loadSystem 清单。
 * 由 LibXposedEntry.onModuleLoaded 调用一次，随后按宿主包名分发。
 */
@Obfuscate
object HookRouterInit {

    fun register() {
        //系统框架
        HookRouter.system(HookAndroid)

        //系统界面
        HookRouter.app("com.android.systemui", HookSystemUI)

        //经典主题 Clock
        HookRouter.app("com.oplus.keyguard.clock.base", HookKeyguardClock)
        HookRouter.app("com.oplus.keyguard.personality.clocks", HookKeyguardClock)

        //通知管理
        HookRouter.app("com.oplus.notificationmanager", HookNotificationManager)

        //时钟
        HookRouter.app("com.coloros.alarmclock", HookAlarmClock)

        //桌面
        HookRouter.app("com.oppo.launcher", HookLauncher)
        HookRouter.app("com.android.launcher", HookLauncher)

        //百变引擎
        HookRouter.app("com.oplus.uiengine", HookUIEngine)

        //截屏
        HookRouter.app("com.oplus.screenshot", HookScreenshot)

        //安全中心
        HookRouter.app("com.oplus.safecenter", HookSafeCenter)
        HookRouter.app("com.coloros.safecenter", HookSafeCenter)

        //应用安装器
        HookRouter.app("com.android.packageinstaller", HookPackageInstaller)

        //外部存储设备
        HookRouter.app("com.android.externalstorage", HookExternalStorage)

        //电池
        HookRouter.app("com.oplus.battery", HookBattery)

        //设置
        HookRouter.app("com.android.settings", HookSettings)

        //相机
        HookRouter.app("com.oplus.camera", HookCamera)
        HookRouter.app("com.oneplus.camera", HookCamera)

        //相册
        HookRouter.app("com.coloros.gallery3d", HookGallery)

        //主题商店
        HookRouter.app("com.heytap.themestore", HookThemeStore)
        HookRouter.app("com.oplus.themestore", HookThemeStore)

        //云服务
        HookRouter.app("com.heytap.cloud", HookCloudService)

        //游戏助手
        HookRouter.app("com.oplus.games", HookOplusGames)

        //应用增强服务
        HookRouter.app("com.oplus.cosa", HookOplusCosa)

        //软件更新
        HookRouter.app("com.oplus.ota", HookOplusOta)

        //乐划锁屏
        HookRouter.app("com.heytap.pictorial", HookPictorial)

        //信息
        HookRouter.app("com.android.mms", HookOplusMMS)

        //电话服务
        HookRouter.app("com.android.phone", HookPhone)

        //浏览器
        HookRouter.app("com.heytap.browser", HookBrowser)

        //手势体感
        HookRouter.app("com.oplus.gesture", HookGesture)

        //权限控制器
        HookRouter.app("com.android.permissioncontroller", HookPermissionController)

        //小布助手
        HookRouter.app("com.heytap.speechassist", HookSpeechAssist)

        //小布识屏
        HookRouter.app("com.coloros.directui", HookDirectUI)

        //全局搜索
        HookRouter.app("com.heytap.quicksearchbox", HookQuickSearchBox)

        //软件商店
        HookRouter.app("com.heytap.market", HookMarket)

        //天气
        HookRouter.app("com.coloros.weather2", HookWeather)

        //日历
        HookRouter.app("com.coloros.calendar", HookCalendar)

        //智能侧边栏
        HookRouter.app("com.coloros.smartsidebar", HookSmartSidebar)

        //手机管家
        HookRouter.app("com.coloros.phonemanager", HookPhoneManager)

        //支付保护
        HookRouter.app("com.coloros.securepay", HookSecurePay)

        //应用分身
        HookRouter.app("com.oplus.multiapp", HookMultiApp)

        //录音
        HookRouter.app("com.coloros.soundrecorder", HookSoundRecorder)

        //三方应用通话录音 / 智慧语音
        HookRouter.app("com.oplus.audiomonitor", HookAudioMonitor)

        //audioEffectCenter
        HookRouter.app("com.oplus.audio.effectcenter", HookAudioEffectCenter)

        //MediaController
        HookRouter.app("com.oplus.mediacontroller", HookMediaController)

        //无网畅聊
        HookRouter.app("com.oplus.beaconlink", HookBeaconLink)

        //无线设置
        HookRouter.app("com.oplus.wirelesssettings", HookWirelessSettings)

        //健康
        HookRouter.app("com.heytap.health", HookHealth)

        //NFC服务
        HookRouter.app("com.android.nfc", HookNfc)

        //互传
        HookRouter.app("com.coloros.oshare", HookOShare)

        //权限管理
        HookRouter.app("com.oplus.securitypermission", HookSecuritypPermission)

        //文件管理
        HookRouter.app("com.coloros.filemanager", HookFileManager)

        //工程模式
        HookRouter.app("com.oplus.engineermode", HookEngineerMode)

        HookRouter.app("com.oplus.claw", HookClaw)

        //其他APP
        HookRouter.app("com.theappninjas.fakegpsjoystick", HookFakeGpsJoyStick)
        HookRouter.app("com.ruet_cse_1503050.ragib.appbackup.pro", HookAlphaBackupPro)
        HookRouter.app("ru.kslabs.ksweb", HookKsWeb)
        HookRouter.app("com.dv.adm", HookADM)
    }
}