package com.luckyzyx.luckytool.ui.compose.special

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Text
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.components.miuix.MiuixListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.checkResolveActivity
import com.topjohnwu.superuser.ShellUtils

/**
 * QuickEntry 页（旧 ui.fragment.others.QuickEntryFragment 的 Compose 等价物）。
 * 纯点击入口页：两个分类 + 若干条件可见（checkResolveActivity）的跳转项。
 */
object QuickEntryPage {

    val spec = ScopePageSpec(
        pageKey = "quick_entry",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }

        fun entry(title: String, visible: Boolean = true, onClick: () -> Unit) {
            if (!visible) return
            custom(key = null, title = title) {
                if (LocalUiMode.current == UiMode.Miuix) {
                    MiuixListItem(title = title, onClick = onClick)
                } else {
                    SegmentedListItem(
                        onClick = onClick,
                        headlineContent = { Text(title) },
                    )
                }
            }
        }

        category(c.getString(R.string.SystemDebuggingRelated))
        entry(c.getString(R.string.engineering_mode)) { IntentUtils(c).jumpEngineermode() }
        entry(c.getString(R.string.charging_test)) { IntentUtils(c).jumpBatteryInfo() }
        entry(c.getString(R.string.developer_option)) { IntentUtils(c).jumpSettingsDev() }
        entry(
            c.getString(R.string.system_interface_adjustment),
            visible = c.checkResolveActivity("com.android.systemui", "com.android.systemui.DemoMode"),
        ) { IntentUtils(c).jumpSystemUIDemoMode() }
        entry(
            c.getString(R.string.AOSPSettingsPage),
            visible = c.checkResolveActivity(
                "com.android.settings",
                "com.android.settings.homepage.DeepLinkHomepageActivityInternal",
            ),
        ) {
            ShellUtils.fastCmd(
                "am start -n com.android.settings/.homepage.DeepLinkHomepageActivityInternal"
            )
        }

        category(c.getString(R.string.HidePageRelated))
        entry(c.getString(R.string.process_manager)) { IntentUtils(c).jumpRunningApp() }
        entry(
            c.getString(R.string.very_dark_mode),
            visible = c.checkResolveActivity(
                "com.android.settings",
                "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity",
            ),
        ) { IntentUtils(c).jumpVeryDarkMode() }
        entry(
            c.getString(R.string.open_battery_health),
            visible = c.checkResolveActivity(
                "com.oplus.battery",
                "com.oplus.powermanager.fuelgaue.BatteryHealthActivity",
            ),
        ) {
            ShellUtils.fastCmd(
                "am start -n com.oplus.battery/com.oplus.powermanager.fuelgaue.BatteryHealthActivity"
            )
        }
        entry(c.getString(R.string.battery_optimization)) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                c.startActivity(this)
            }
        }
        entry(
            c.getString(R.string.camera_algo_page),
            visible = c.checkResolveActivity(
                "com.oplus.camera",
                "com.oplus.camera.ui.menu.algoswitch.AlgoSwitchActivity",
            ),
        ) {
            ShellUtils.fastCmd(
                "am start -n com.oplus.camera/.ui.menu.algoswitch.AlgoSwitchActivity"
            )
        }
    }
}
