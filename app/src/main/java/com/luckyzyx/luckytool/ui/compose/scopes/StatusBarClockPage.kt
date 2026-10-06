package com.luckyzyx.luckytool.ui.compose.scopes

import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.formatDate

/**
 * 状态栏时钟页（旧 ui.fragment.scopes.statusbar.StatusBarClock 的 Compose 等价物）。
 * 逐项对齐：键、默认值、条件可见性、notify（sendPrefsValue）、restart 回调。
 */
object StatusBarClockPage {

    val spec = ScopePageSpec(
        pageKey = "statusbar_clock",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        list(
            key = "statusbar_clock_mode",
            title = c.getString(R.string.statusbar_clock_mode),
            entries = c.resources.getStringArray(R.array.statusbar_clock_mode_entries),
            entryValues = arrayOf("0", "1", "2"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
            onChange = { restart?.invoke() },
        )
        when (state.getString("statusbar_clock_mode", "0")) {
            "1" -> {
                switch("statusbar_clock_show_year", c.getString(R.string.statusbar_clock_show_year))
                switch("statusbar_clock_show_month", c.getString(R.string.statusbar_clock_show_month))
                switch("statusbar_clock_show_day", c.getString(R.string.statusbar_clock_show_day))
                switch("statusbar_clock_show_week", c.getString(R.string.statusbar_clock_show_week))
                switch("statusbar_clock_show_period", c.getString(R.string.statusbar_clock_show_period))
                switch("statusbar_clock_show_double_hour", c.getString(R.string.statusbar_clock_show_double_hour))
                switch("statusbar_clock_show_second", c.getString(R.string.statusbar_clock_show_second))
                switch("statusbar_clock_hide_spaces", c.getString(R.string.statusbar_clock_hide_spaces))
                switch(
                    key = "statusbar_clock_show_doublerow",
                    title = c.getString(R.string.statusbar_clock_show_doublerow),
                    onChange = { restart?.invoke() },
                )
                if (state.getBoolean("statusbar_clock_show_doublerow")) {
                    list(
                        key = "statusbar_clock_text_alignment",
                        title = c.getString(R.string.statusbar_clock_text_alignment),
                        entries = c.resources.getStringArray(R.array.statusbar_clock_text_alignment_entries),
                        entryValues = arrayOf("left", "center", "right"),
                        default = "center",
                        summary = c.getString(R.string.current_mode) + ": %s",
                        notify = true,
                    )
                }
                slider(
                    key = "statusbar_clock_singlerow_fontsize",
                    title = c.getString(R.string.statusbar_clock_singlerow_fontsize),
                    valueRange = 0..28,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
                slider(
                    key = "statusbar_clock_doublerow_fontsize",
                    title = c.getString(R.string.statusbar_clock_doublerow_fontsize),
                    valueRange = 0..20,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
            }
            "2" -> {
                editText(
                    key = "statusbar_clock_custom_format",
                    title = c.getString(R.string.statusbar_clock_custom_format),
                    dialogMessage = CUSTOM_FORMAT_HELP,
                    default = "HH:mm:ss",
                    notify = true,
                    onChange = { restart?.invoke() },
                )
                // 旧逻辑：自定义格式含多行（带换行）才显示对齐选项
                if ((state.getString("statusbar_clock_custom_format", "HH:mm:ss") ?: "HH:mm:ss").split("\n").size >= 2) {
                    list(
                        key = "statusbar_clock_text_alignment",
                        title = c.getString(R.string.statusbar_clock_text_alignment),
                        entries = c.resources.getStringArray(R.array.statusbar_clock_text_alignment_entries),
                        entryValues = arrayOf("left", "center", "right"),
                        default = "center",
                        summary = c.getString(R.string.current_mode) + ": %s",
                        notify = true,
                    )
                }
                slider(
                    key = "statusbar_clock_custom_fontsize",
                    title = c.getString(R.string.statusbar_clock_custom_fontsize),
                    valueRange = 0..30,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
            }
        }
        if (state.getString("statusbar_clock_mode", "0") != "0") {
            slider(
                key = "statusbar_clock_custom_minimum_width",
                title = c.getString(R.string.statusbar_clock_custom_minimum_width),
                valueRange = 0..50,
                summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                notify = true,
            )
            switch(
                key = "statusbar_clock_custom_padding",
                title = c.getString(R.string.statusbar_clock_custom_padding),
                onChange = { restart?.invoke() },
            )
            if (state.getBoolean("statusbar_clock_custom_padding")) {
                slider(
                    key = "statusbar_clock_custom_top_padding",
                    title = c.getString(R.string.statusbar_clock_custom_top_padding),
                    valueRange = -30..30,
                    default = 0,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
                slider(
                    key = "statusbar_clock_custom_bottom_padding",
                    title = c.getString(R.string.statusbar_clock_custom_bottom_padding),
                    valueRange = -30..30,
                    default = 0,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
                slider(
                    key = "statusbar_clock_custom_left_padding",
                    title = c.getString(R.string.statusbar_clock_custom_left_padding),
                    valueRange = -30..30,
                    default = 0,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
                slider(
                    key = "statusbar_clock_custom_right_padding",
                    title = c.getString(R.string.statusbar_clock_custom_right_padding),
                    valueRange = -30..30,
                    default = 0,
                    summary = c.getString(R.string.statusbar_clock_if_zero_summary),
                    notify = true,
                )
            }
            switch(
                key = "statusbar_clock_user_typeface",
                title = c.getString(R.string.use_user_typeface),
                onChange = { restart?.invoke() },
            )
            if (state.getBoolean("statusbar_clock_user_typeface")) {
                switch(
                    key = "statusbar_clock_use_bold_font_style",
                    title = c.getString(R.string.use_bold_font_style),
                    notify = true,
                )
            }
        }
    }

    /** 与旧 EditTextPreference.dialogMessage 完全一致的格式速查表 */
    private val CUSTOM_FORMAT_HELP = """
        yyyy/MM/dd -> ${formatDate("yyyy/MM/dd")}
        y/M/d/E/a -> ${formatDate("y/M/d/E/a")}
        yy/yyyy -> ${formatDate("yy/yyyy")}
        M/MM/MMM/MMMM/MMMMM -> ${formatDate("M/MM/MMM/MMMM/MMMMM")}
        d/dd/ddd/dddd -> ${formatDate("d/dd/d号/dd号")}
        E/EE/EEE/EEEE/EEEEE -> ${formatDate("E/EE/EEE/EEEE/EEEEE")}
        H/HH (0-23) k/kk (1-24) -> ${formatDate("H/HH k/kk")}
        K/KK (0-11) h/hh (1-12) -> ${formatDate("K/KK h/hh")}
        HH:mm:ss -> ${formatDate("HH:mm:ss")}
        m/mm/mmm/mmmm -> ${formatDate("m/mm/mmm/mmmm")}
        s/ss/sss/ssss -> ${formatDate("s/ss/sss/ssss")}
        z -> ${formatDate("z")}
        G -> ${formatDate("G")}
        GG -> 子时/丑时/寅时/卯时
        N -> 初一
        NN -> 二月初一
        NNN -> 兔年二月初一
        NNNN -> 癸卯兔年二月初一
        FF -> 凌晨/上午/傍晚/晚上
    """.trimIndent()
}
