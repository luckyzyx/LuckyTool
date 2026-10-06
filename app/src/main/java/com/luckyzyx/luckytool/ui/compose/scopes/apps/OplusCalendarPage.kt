package com.luckyzyx.luckytool.ui.compose.scopes.apps

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * 日历页（旧 ui.fragment.scopes.apps.OplusCalendar 的 Compose 等价物）。
 */
object OplusCalendarPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_calendar",
        prefsName = ModulePrefs,
        packName = "com.coloros.calendar",
        scopes = arrayOf("com.coloros.calendar"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        switch(
            key = "remove_holiday_page_information_flow",
            title = c.getString(R.string.remove_holiday_page_information_flow),
        )
        switch(
            key = "remove_almanac_page_information_flow",
            title = c.getString(R.string.remove_almanac_page_information_flow),
        )
        switch(
            key = "remove_horoscope_page_information_flow",
            title = c.getString(R.string.remove_horoscope_page_information_flow),
        )
    }
}
