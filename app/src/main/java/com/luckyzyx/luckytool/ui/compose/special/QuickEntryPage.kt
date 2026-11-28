package com.luckyzyx.luckytool.ui.compose.special

import android.content.Intent
import android.provider.Settings
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.checkResolveActivity
import com.topjohnwu.superuser.ShellUtils

/**
 * QuickEntry 页：两个分类下的纯点击跳转入口。
 * 每项先用 checkResolveActivity 判断目标 Activity 是否存在，不存在就不显示。
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

        group(title = c.getString(R.string.SystemDebuggingRelated)) {
            click(title = c.getString(R.string.engineering_mode)) { IntentUtils(c).jumpEngineermode() }
            click(title = c.getString(R.string.charging_test)) { IntentUtils(c).jumpBatteryInfo() }
            click(title = c.getString(R.string.developer_option)) { IntentUtils(c).jumpSettingsDev() }
            click(
                title = c.getString(R.string.system_interface_adjustment),
                visible = c.checkResolveActivity(
                    "com.android.systemui",
                    "com.android.systemui.DemoMode",
                ),
            ) { IntentUtils(c).jumpSystemUIDemoMode() }
            click(
                title = c.getString(R.string.AOSPSettingsPage),
                visible = c.checkResolveActivity(
                    "com.android.settings",
                    "com.android.settings.homepage.DeepLinkHomepageActivityInternal",
                ),
            ) {
                ShellUtils.fastCmd(
                    "am start -n com.android.settings/.homepage.DeepLinkHomepageActivityInternal"
                )
            }
        }
        group(title = c.getString(R.string.HidePageRelated)) {
            click(title = c.getString(R.string.process_manager)) { IntentUtils(c).jumpRunningApp() }
            // 进 LT 自己的极暗模式页：系统极暗页在部分 ROM 上被过滤成空白页，不能用它做入口。
            // 恒显示 —— LT 的页面在 C16 / C17 都能用。
            click(title = c.getString(R.string.very_dark_mode)) {
                navigate?.invoke("extra_dim", c.getString(R.string.very_dark_mode))
            }
            click(
                title = c.getString(R.string.open_battery_health),
                visible = c.checkResolveActivity(
                    "com.oplus.battery",
                    "com.oplus.powermanager.fuelgaue.BatteryHealthActivity",
                ),
            ) {
                ShellUtils.fastCmd(
                    "am start -n com.oplus.battery/com.oplus.powermanager.fuelgaue.BatteryHealthActivity"
                )
            }
            click(title = c.getString(R.string.battery_optimization)) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    c.startActivity(this)
                }
            }
            click(
                title = c.getString(R.string.camera_algo_page),
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
}