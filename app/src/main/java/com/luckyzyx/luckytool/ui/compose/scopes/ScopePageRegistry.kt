package com.luckyzyx.luckytool.ui.compose.scopes

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.components.preference.PrefScopeBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusAlarmClockPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBatteryPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBeaconLinkPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusBrowserPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCalendarPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCameraPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusCloudServicePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusDirectUIPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusEngineerModePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusEyeProtectPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusFileManagerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGalleryPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGamesPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGesturePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusHealthPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusLinkerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMarketPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMMSPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMcsPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusMyDevicesPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusNfcPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusOSharePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusOTAPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPermissionControllerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPhoneManagerPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusPictorialPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusScreenshotPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSearchBoxPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSecuritypPermissionPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSettingsPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSmartSidebarPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSoundRecorderPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusSpeechAssistPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusTeleServicePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusThemeStorePage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusWeatherPage
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusWirelessSettingsPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarBatteryPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarControlCenterPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarIconPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarLayoutPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNetWorkSpeedPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNotifyPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarNotifyRemovalPage
import com.luckyzyx.luckytool.ui.compose.scopes.statusbar.StatusBarTilesPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.AndroidRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.AodRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.ApplicationRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.CorePatchPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.DialogRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.FingerPrintRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LauncherRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.LockScreenRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.MiscellaneousPage
import com.luckyzyx.luckytool.ui.compose.scopes.related.SoundRelatedPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.ADMPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.AlphaBackupProPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.ClawPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.GpsJoyStickPage
import com.luckyzyx.luckytool.ui.compose.scopes.others.KsWebPage
import com.luckyzyx.luckytool.ui.compose.special.BatteryInfoPage
import com.luckyzyx.luckytool.ui.compose.special.DonatePage
import com.luckyzyx.luckytool.ui.compose.special.ExtractOTAPage
import com.luckyzyx.luckytool.ui.compose.special.ForceFpsPage
import com.luckyzyx.luckytool.ui.compose.special.QuickEntryPage
import com.luckyzyx.luckytool.ui.compose.special.DarkModePage
import com.luckyzyx.luckytool.ui.compose.special.HideAppIntentPage
import com.luckyzyx.luckytool.ui.compose.special.MemcConfigPage
import com.luckyzyx.luckytool.ui.compose.special.MultiAppPage
import com.luckyzyx.luckytool.ui.compose.special.ZoomWindowPage

/**
 * 一个 Compose 作用域页的声明——旧 [BaseScopePreferenceFeagment] 子类中
 * scopes/currentPrefsName/loadPreferences 等字段的注册单元等价物。
 *
 * @param pageKey        页面注册键（nav_container.xml 中 page_key 参数值）
 * @param prefsName      SharedPreferences 文件名（ModulePrefs 等）
 * @param packName       宿主包名（sendPrefsValue 通知目标）
 * @param scopes         Xposed 作用域包名列表（重启作用域对话框用）
 * @param restartEnabled 是否显示"重启作用域"菜单
 * @param onRefresh      下拉刷新回调（null = 无下拉刷新；如 OTA 提取、捐赠数据页）
 * @param content        页面内容 DSL（每次重组重跑，条件可见性写 Kotlin if）
 */
class ScopePageSpec(
    val pageKey: String,
    val prefsName: String,
    val packName: String,
    val scopes: Array<String>,
    val restartEnabled: Boolean,
    val onRefresh: (suspend () -> Unit)? = null,
    val fullContent: (@Composable LazyItemScope.(PrefScopeBuilder) -> Unit)? = null,
    val content: PrefScopeBuilder.() -> Unit,
)

/** Compose 页注册表：ComposeScopeFragment 按 page_key 查表渲染 */
object ScopePageRegistry {
    private val pages = LinkedHashMap<String, ScopePageSpec>()

    fun register(spec: ScopePageSpec) {
        pages[spec.pageKey] = spec
    }

    operator fun get(key: String): ScopePageSpec? = pages[key]

    /** 全部已注册页（搜索索引用） */
    fun all(): List<ScopePageSpec> = pages.values.toList()

    /** 功能树页面顺序（对齐旧 XposedFragment.loadPreferences 的 addFragmentPreference 顺序，49 页） */
    val treeOrder: List<String> = listOf(
        "android_related", "statusbar", "launcher", "aod", "lock_screen", "application", "miscellaneous",
        "oplus_screenshot", "oplus_battery", "oplus_alarm_clock", "oplus_settings",
        "oplus_wireless_settings", "oplus_tele_service", "oplus_mms", "oplus_browser",
        "oplus_camera", "oplus_gallery", "oplus_games", "theme_store", "oplus_market",
        "oplus_cloud_service", "oplus_ota", "oplus_pictorial", "oplus_gesture",
        "oplus_speech_assist", "oplus_direct_ui", "oplus_search_box", "oplus_weather",
        "oplus_calendar", "oplus_smart_sidebar", "oplus_phone_manager", "oplus_health",
        "oplus_sound_recorder", "oplus_eye_protect", "oplus_beacon_link", "oplus_nfc",
        "oplus_oshare", "oplus_permission_controller", "oplus_linker",
        "oplus_securityp_permission", "oplus_file_manager", "oplus_engineer_mode",
        "oplus_my_devices", "oplus_mcs", "claw", "alpha_backup_pro", "ks_web", "adm",
        "gps_joy_stick",
    )

    /**
     * 树标题覆盖表（pageKey → 字符串资源）：对齐旧功能树 root 标题。
     * 其余页标题 = AppUtils.getAppLabel(packName)（旧 root key == packName）；
     * android_related 特殊：旧标题 = getAppLabel("android")（见 FunctionPage.pageTitle）。
     */
    val treeTitleRes: Map<String, Int> = mapOf(
        "statusbar" to R.string.StatusBar,
        "launcher" to R.string.Desktop,
        "aod" to R.string.AodRelated,
        "lock_screen" to R.string.LockScreen,
        "application" to R.string.Application,
        "miscellaneous" to R.string.Miscellaneous,
    )

    /** DSL page(target=...) 的旧 nav id 名 → pageKey（tools/p3_nav_switch.ps1 $pairs + $specialPairs + P1 两页） */
    val pageTargetMap: Map<String, String> = mapOf(
        "statusBarClock" to "statusbar_clock",
        "statusBar" to "statusbar",
        "oplusAlarmClock" to "oplus_alarm_clock",
        "oplusBattery" to "oplus_battery",
        "oplusBeaconLink" to "oplus_beacon_link",
        "oplusBrowser" to "oplus_browser",
        "oplusCalendar" to "oplus_calendar",
        "oplusCamera" to "oplus_camera",
        "oplusCloudService" to "oplus_cloud_service",
        "oplusDirectUI" to "oplus_direct_ui",
        "oplusEngineerMode" to "oplus_engineer_mode",
        "oplusEyeProtect" to "oplus_eye_protect",
        "oplusFileManager" to "oplus_file_manager",
        "oplusGallery" to "oplus_gallery",
        "oplusGames" to "oplus_games",
        "oplusGesture" to "oplus_gesture",
        "oplusHealth" to "oplus_health",
        "oplusLinker" to "oplus_linker",
        "oplusMarket" to "oplus_market",
        "oplusMMS" to "oplus_mms",
        "oplusMcs" to "oplus_mcs",
        "oplusMyDevices" to "oplus_my_devices",
        "oplusNfc" to "oplus_nfc",
        "oplusOShare" to "oplus_oshare",
        "oplusOta" to "oplus_ota",
        "oplusPermissionController" to "oplus_permission_controller",
        "oplusPhoneManager" to "oplus_phone_manager",
        "oplusPictorial" to "oplus_pictorial",
        "oplusScreenshot" to "oplus_screenshot",
        "oplusSearchBox" to "oplus_search_box",
        "oplusSecuritypPermission" to "oplus_securityp_permission",
        "oplusSettings" to "oplus_settings",
        "oplusSmartSidebar" to "oplus_smart_sidebar",
        "oplusSoundRecorder" to "oplus_sound_recorder",
        "oplusSpeechAssist" to "oplus_speech_assist",
        "oplusTeleService" to "oplus_tele_service",
        "themeStore" to "theme_store",
        "oplusWeather" to "oplus_weather",
        "oplusWirelessSettings" to "oplus_wireless_settings",
        "statusBarNotify" to "statusbar_notify",
        "statusBarIcon" to "statusbar_icon",
        "statusBarControlCenter" to "statusbar_control_center",
        "statusBarLayout" to "statusbar_layout",
        "statusBarBattery" to "statusbar_battery",
        "statusBarNetWorkSpeed" to "statusbar_network_speed",
        "statusBarTiles" to "statusbar_tiles",
        "statusBarNotifyRemoval" to "statusbar_notify_removal",
        "androidRelated" to "android_related",
        "launcher" to "launcher",
        "lockScreen" to "lock_screen",
        "application" to "application",
        "miscellaneous" to "miscellaneous",
        "dialogRelated" to "dialog_related",
        "fingerPrintRelated" to "finger_print_related",
        "soundRelated" to "sound_related",
        "aod" to "aod",
        "corePatch" to "core_patch",
        "alphaBackupPro" to "alpha_backup_pro",
        "claw" to "claw",
        "ksWeb" to "ks_web",
        "adm" to "adm",
        "gpsJoyStick" to "gps_joy_stick",
        "multiAppFragment" to "multi_app",
        "zoomWindowFragment" to "zoom_window",
        "darkModeFragment" to "dark_mode",
        "forceFpsFragment" to "force_fps",
        "extractOTAFragment" to "extract_ota",
        "memcConfigFragment" to "memc_config",
        "hideAppIntentFragment" to "hide_app_intent",
        "batteryInfoFragment" to "battery_info",
        "donateFragment" to "donate",
        "systemQuickEntry" to "quick_entry",
    )

    init {
        register(StatusBarClockPage.spec)
        register(StatusBarRelatedPage.spec)
        // P3 apps 批
        register(OplusAlarmClockPage.spec)
        register(OplusBatteryPage.spec)
        register(OplusBeaconLinkPage.spec)
        register(OplusBrowserPage.spec)
        register(OplusCalendarPage.spec)
        register(OplusCameraPage.spec)
        register(OplusCloudServicePage.spec)
        register(OplusDirectUIPage.spec)
        register(OplusEngineerModePage.spec)
        register(OplusEyeProtectPage.spec)
        register(OplusFileManagerPage.spec)
        register(OplusGalleryPage.spec)
        register(OplusGamesPage.spec)
        register(OplusGesturePage.spec)
        register(OplusHealthPage.spec)
        register(OplusLinkerPage.spec)
        register(OplusMarketPage.spec)
        register(OplusMMSPage.spec)
        register(OplusMcsPage.spec)
        register(OplusMyDevicesPage.spec)
        register(OplusNfcPage.spec)
        register(OplusOSharePage.spec)
        register(OplusOTAPage.spec)
        register(OplusPermissionControllerPage.spec)
        register(OplusPhoneManagerPage.spec)
        register(OplusPictorialPage.spec)
        register(OplusScreenshotPage.spec)
        register(OplusSearchBoxPage.spec)
        register(OplusSecuritypPermissionPage.spec)
        register(OplusSettingsPage.spec)
        register(OplusSmartSidebarPage.spec)
        register(OplusSoundRecorderPage.spec)
        register(OplusSpeechAssistPage.spec)
        register(OplusTeleServicePage.spec)
        register(OplusThemeStorePage.spec)
        register(OplusWeatherPage.spec)
        register(OplusWirelessSettingsPage.spec)
        // P3 statusbar 批
        register(StatusBarBatteryPage.spec)
        register(StatusBarControlCenterPage.spec)
        register(StatusBarIconPage.spec)
        register(StatusBarLayoutPage.spec)
        register(StatusBarNetWorkSpeedPage.spec)
        register(StatusBarNotifyPage.spec)
        register(StatusBarNotifyRemovalPage.spec)
        register(StatusBarTilesPage.spec)
        register(AndroidRelatedPage.spec)
        register(AodRelatedPage.spec)
        register(ApplicationRelatedPage.spec)
        register(CorePatchPage.spec)
        register(DialogRelatedPage.spec)
        register(FingerPrintRelatedPage.spec)
        register(LauncherRelatedPage.spec)
        register(LockScreenRelatedPage.spec)
        register(MiscellaneousPage.spec)
        register(SoundRelatedPage.spec)
        register(ADMPage.spec)
        register(AlphaBackupProPage.spec)
        register(ClawPage.spec)
        register(GpsJoyStickPage.spec)
        register(KsWebPage.spec)
        // P4 special 批
        register(BatteryInfoPage.spec)
        register(DonatePage.spec)
        register(ExtractOTAPage.spec)
        register(ForceFpsPage.spec)
        register(QuickEntryPage.spec)
        register(DarkModePage.spec)
        register(HideAppIntentPage.spec)
        register(MemcConfigPage.spec)
        register(MultiAppPage.spec)
        register(ZoomWindowPage.spec)
    }
}
