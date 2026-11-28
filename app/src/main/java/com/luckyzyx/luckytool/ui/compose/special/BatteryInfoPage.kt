package com.luckyzyx.luckytool.ui.compose.special

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.ui.compose.components.PrefNote
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.utils.SettingsPrefs

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
 * 占位文本交给行层的 [PrefNote]：字号与配色由它按外观线分派，这里只给内缩。
 */
object BatteryInfoPage {

    val spec = PageSpec(
        pageKey = "battery_info",
        prefsName = SettingsPrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        custom(key = "battery_info_placeholder") {
            // 本页唯一的可见内容：静态占位文本
            PrefNote(
                text = "Test",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}
