package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode

/**
 * 天气页：去掉页面底部广告、禁止跳浏览器、展开 15 天预报与恢复降雨云图的开关。
 */
object OplusWeatherPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_weather",
        prefsName = ModulePrefs,
        packName = "com.coloros.weather2",
        scopes = arrayOf("com.coloros.weather2"),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_weather_some_page_bottom_ads",
            title = c.getString(R.string.remove_weather_some_page_bottom_ads),
        )
        switch(
            key = "disable_weather_jump_browser",
            title = c.getString(R.string.disable_weather_jump_browser),
        )
        switch(
            key = "enable_15_day_weather_expand_list",
            title = c.getString(R.string.enable_15_day_weather_expand_list),
        )
        if (getOSVersionCode in 30..34) {
            switch(
                key = "restore_rainfall_cloud_map_page",
                title = c.getString(R.string.restore_rainfall_cloud_map_page),
            )
        }
    }
}
