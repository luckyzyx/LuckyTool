package com.luckyzyx.luckytool.ui.compose.scopes

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
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
    }
}
