package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.SettingsPrefs

/**
 * BatteryInfo 页（旧 ui.fragment.extension.BatteryInfoFragment 的 Compose 等价物）。
 *
 * 旧页为 no-op 壳：init() 为空，BroadcastReceiver 仅打日志（ACTION_BATTERY_CHANGED /
 * "android.intent.action.ADDITIONAL_BATTERY_CHANGED" 触发 init()，无任何 UI 更新），
 * 旧布局 fragment_battery_info.xml 仅显示静态文本 "Test"。
 * 按迁移原则不迁移无功能的 receiver，仅保留旧布局可见文案。
 */
object BatteryInfoPage {

    val spec = ScopePageSpec(
        pageKey = "battery_info",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        custom(key = "battery_info_placeholder") {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                // 旧 fragment_battery_info.xml 中的静态可见文案
                Text("Test")
            }
        }
    }
}
