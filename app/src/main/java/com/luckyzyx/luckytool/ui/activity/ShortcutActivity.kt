package com.luckyzyx.luckytool.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.service.tiles.ChargingTestTile
import com.luckyzyx.luckytool.service.tiles.ExtraDimModeTile
import com.luckyzyx.luckytool.service.tiles.FiveGTile
import com.luckyzyx.luckytool.service.tiles.ProcessManagerTile
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.SDK
import com.topjohnwu.superuser.ShellUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("DEPRECATION")
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /** 设置透明窗口 */
        window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window?.statusBarColor = getColor(R.color.transparent)
        window?.navigationBarColor = getColor(R.color.transparent)
        intent.extras?.apply {
            when (getString("Shortcut", "null")) {
                "module_shortcut_status_lsposed" -> {
                    ShellUtils.fastCmd(
                        "am start 'intent:#Intent;action=android.intent.action.MAIN;category=org.lsposed.manager.LAUNCH_MANAGER;launchFlags=0x80000;component=com.android.shell/.BugreportWarningActivity;end'"
                    )
                }

                "module_shortcut_status_oplusgames" -> ShellUtils.fastCmd(
                    "am start -n com.oplus.games/business.compact.activity.GameBoxCoverActivity"
                )

                "module_shortcut_status_chargingtest" -> IntentUtils(this@ShortcutActivity).jumpBatteryInfo()
                "module_shortcut_status_processmanager" -> IntentUtils(this@ShortcutActivity).jumpRunningApp()
                "module_shortcut_status_performance" -> if (SDK >= A15) IntentUtils(this@ShortcutActivity).jumpHighPerformance()
                else IntentUtils(this@ShortcutActivity).jumpBattery()
            }
        }
        checkTileLongClick()
        finish()
    }

    @SuppressLint("NewApi")
    fun checkTileLongClick() {
        if (SDK < A13) return
        val componentName: ComponentName = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
            ?: return
//        LogUtils.d("checkTileLongClick", "componentName", componentName.toString(), true)
        when (componentName.className) {
            classOf<ChargingTestTile>().name -> IntentUtils(this).jumpBatteryInfo()
            classOf<ProcessManagerTile>().name -> IntentUtils(this).jumpRunningApp()
            classOf<ExtraDimModeTile>().name -> IntentUtils(this).jumpVeryDarkMode()
            classOf<FiveGTile>().name -> IntentUtils(this).jumpMobileNetwork()
        }
    }
}