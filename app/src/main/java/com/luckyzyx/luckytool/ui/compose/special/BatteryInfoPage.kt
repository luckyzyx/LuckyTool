package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.SettingsPrefs
import top.yukonga.miuix.kmp.basic.Text as MiuixText

/**
 * BatteryInfo 页。
 *
 * 无功能占位页：不读任何键、不触发系统行为，只渲染一条静态文本 "Test"，
 * 用来在功能树里保留这个入口位置。
 *
 * 因此这里不监听电池广播：ACTION_BATTERY_CHANGED 与
 * "android.intent.action.ADDITIONAL_BATTERY_CHANGED" 在本模块里没有可驱动的东西
 * （既不读电量、也没有要刷新的 UI），接进来只会多一条空转的 receiver。
 *
 * 文本按 [LocalUiMode] 分派：Miuix 线用 `MiuixText`，material 线保持 material3 `Text`。
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
                // 本页唯一的可见内容：静态占位文本
                when (LocalUiMode.current) {
                    UiMode.Miuix -> MiuixText(text = "Test")
                    UiMode.Material -> Text("Test")
                }
            }
        }
    }
}
