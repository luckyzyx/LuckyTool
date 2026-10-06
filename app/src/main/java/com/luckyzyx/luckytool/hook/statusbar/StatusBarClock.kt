package com.luckyzyx.luckytool.hook.statusbar

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.LunarHelperUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.is24
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

@Obfuscate
class StatusBarClock : YukiBaseHooker() {

    val clockMode = preferences(ModulePrefs).getString("statusbar_clock_mode", "0")
    val isYear = preferences(ModulePrefs).getBoolean("statusbar_clock_show_year", false)
    val isMonth = preferences(ModulePrefs).getBoolean("statusbar_clock_show_month", false)
    val isDay = preferences(ModulePrefs).getBoolean("statusbar_clock_show_day", false)
    val isWeek = preferences(ModulePrefs).getBoolean("statusbar_clock_show_week", false)
    val isPeriod = preferences(ModulePrefs).getBoolean("statusbar_clock_show_period", false)
    val isDoubleHour =
        preferences(ModulePrefs).getBoolean("statusbar_clock_show_double_hour", false)
    val isSecond = preferences(ModulePrefs).getBoolean("statusbar_clock_show_second", false)
    val isHideSpace = preferences(ModulePrefs).getBoolean("statusbar_clock_hide_spaces", false)
    val isDoubleRow = preferences(ModulePrefs).getBoolean("statusbar_clock_show_doublerow", false)

    var clockAlignment =
        preferences(ModulePrefs).getString("statusbar_clock_text_alignment", "center")

    var singleRowFontSize =
        preferences(ModulePrefs).getInt("statusbar_clock_singlerow_fontsize", 0)
    var doubleRowFontSize =
        preferences(ModulePrefs).getInt("statusbar_clock_doublerow_fontsize", 0)

    var customFormat =
        preferences(ModulePrefs).getString("statusbar_clock_custom_format", "HH:mm:ss")
    var customFontsize = preferences(ModulePrefs).getInt("statusbar_clock_custom_fontsize", 0)
    var customMinimumWidth =
        preferences(ModulePrefs).getInt("statusbar_clock_custom_minimum_width", 0)

    val userTypeface = preferences(ModulePrefs).getBoolean("statusbar_clock_user_typeface", false)
    var useBoldFont =
        preferences(ModulePrefs).getBoolean("statusbar_clock_use_bold_font_style", false)

    val customPadding = preferences(ModulePrefs).getBoolean("statusbar_clock_custom_padding", false)
    var customTopPadding = preferences(ModulePrefs).getInt("statusbar_clock_custom_top_padding", 0)
    var customBottomPadding = preferences(ModulePrefs).getInt("statusbar_clock_custom_bottom_padding", 0)
    var customLeftPadding = preferences(ModulePrefs).getInt("statusbar_clock_custom_left_padding", 0)
    var customRightPadding = preferences(ModulePrefs).getInt("statusbar_clock_custom_right_padding", 0)

    var lunarInstance: Any? = null
    var newline = ""

    override fun onHook() {
        val osCode = getOSVersionCode

        if (clockMode.isBlank() || clockMode == "0") return
        dataChannel.wait<String>("statusbar_clock_text_alignment") { clockAlignment = it }
        dataChannel.wait<String>("statusbar_clock_custom_format") { customFormat = it }
        dataChannel.wait<Int>("statusbar_clock_custom_fontsize") { customFontsize = it }
        dataChannel.wait<Int>("statusbar_clock_custom_minimum_width") { customMinimumWidth = it }
        dataChannel.wait<Int>("statusbar_clock_singlerow_fontsize") { singleRowFontSize = it }
        dataChannel.wait<Int>("statusbar_clock_doublerow_fontsize") { doubleRowFontSize = it }
        dataChannel.wait<Boolean>("statusbar_clock_use_bold_font_style") { useBoldFont = it }
        dataChannel.wait<Int>("statusbar_clock_custom_top_padding") { customTopPadding = it }
        dataChannel.wait<Int>("statusbar_clock_custom_bottom_padding") { customBottomPadding = it }
        dataChannel.wait<Int>("statusbar_clock_custom_left_padding") { customLeftPadding = it }
        dataChannel.wait<Int>("statusbar_clock_custom_right_padding") { customRightPadding = it }

        //Source Clock
        "com.android.systemui.statusbar.policy.Clock".toClass().resolve().apply {
            firstConstructor { parameterCount = 3 }.hook {
                after {
                    val clockView = instance<TextView>().apply {
                        val clockName = safeOfNull { resources.getResourceEntryName(id) }
                        if (clockName != "clock") return@after
                    }

                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            Handler(clockView.context.mainLooper).post {
                                firstMethod { name = "updateClock" }.of(clockView).invoke()
                            }
                        }
                    }, 1000 - System.currentTimeMillis() % 1000, 1000)
                }
            }
            firstMethod { name = "getSmallTime"; returnType = CharSequence::class }.hook {
                after {
                    val clockView = instance<TextView>().apply {
                        val clockName = safeOfNull { resources.getResourceEntryName(id) }
                        if (clockName != "clock") return@after
                        initView()
                    }
                    val context = clockView.context
                    val mCalendar = firstField { name = "mCalendar" }.of(instance).get<Calendar>()
                    val nowTime = mCalendar?.time ?: Date()
                    result = when (clockMode) {
                        "1" -> getDate(context, nowTime) + newline + getTime(context, nowTime)
                        "2" -> formatDate(getFormat(context, customFormat, nowTime), nowTime)
                        else -> return@after
                    }
                }
            }
            firstMethodOrNull { name = "onMeasure" }?.hook {
                before {
                    val height = lastArg().get<Int>() ?: 0
                    val clockView = instance<TextView>().apply {
                        val clockName = safeOfNull { resources.getResourceEntryName(id) }
                        if (clockName != "clock") return@before
                    }
                    if (customMinimumWidth > 0) {
                        clockView.minWidth = customMinimumWidth * 10
                        clockView.minimumWidth = customMinimumWidth * 10
                        firstMethod { name = "setMeasuredDimension"; superclass() }.of(instance)
                            .invoke(customMinimumWidth * 10, height)
                    }
                }
            }
        }

        //Source StatClock
        VariousClass(
            "com.oplusos.systemui.statusbar.widget.StatClock", //C12 C13
            "com.oplus.systemui.statusbar.widget.StatClock" //C14
        ).toClass().resolve().apply {
            firstMethod {
                name { it.startsWith("onConfig") && it.endsWith("Changed") }
            }.hook {
                intercept()
            }
            if (osCode >= 33) {
                firstMethod { name = "onMeasure" }.hook {
                    after {
                        val height = lastArg().get<Int>() ?: 0
                        val clockView = instance<TextView>().apply {
                            val clockName = safeOfNull { resources.getResourceEntryName(id) }
                            if (clockName != "clock") return@after
                            initView()
                        }
                        if (customMinimumWidth > 0) {
                            clockView.minWidth = customMinimumWidth * 10
                            clockView.minimumWidth = customMinimumWidth * 10
                            firstMethod { name = "setMeasuredDimension"; superclass() }.of(instance)
                                .invoke(customMinimumWidth * 10, height)
                        }
                    }
                }
                firstMethodOrNull { name = "updateMinWidth" }?.hook {
                    before {
                        firstField { name = "mShowSeconds"; superclass() }.of(instance).set(true)
                    }
                }
            }
        }
    }

    private fun getLunar(context: Context, level: Int = 4): String {
        LunarHelperUtils(hostClassLoader!!).apply {
            if (lunarInstance == null) lunarInstance = getInstance(context)
            return generateLunarDate(level)
        }
    }

    private fun TextView.initView() {
        if (customPadding) {
            setPadding(
                if (customLeftPadding != 0) customLeftPadding.dp else paddingLeft,
                if (customTopPadding != 0) customTopPadding.dp else paddingTop,
                if (customRightPadding != 0) customRightPadding.dp else paddingRight,
                if (customBottomPadding != 0) customBottomPadding.dp else paddingBottom,
            )
        }
        if (userTypeface) typeface = if (useBoldFont) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (clockMode == "1") {
            isSingleLine = !isDoubleRow
            if (isDoubleRow) {
                newline = "\n"
                if (doubleRowFontSize != 0) {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, doubleRowFontSize.toFloat())
                    setLineSpacing(0F, 0.8F)
                }
            } else {
                if (singleRowFontSize != 0) {
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, singleRowFontSize.toFloat())
                }
            }
        } else if (clockMode == "2") {
            val formatList = customFormat.split("\n")
            val rows = formatList.size
            isSingleLine = rows == 1
            if (customFontsize != 0) {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, customFontsize.toFloat())
            }
            if (rows != 1) setLineSpacing(0F, 0.8F)
        }
        gravity = if (isSingleLine) Gravity.CENTER
        else when (clockAlignment) {
            "left" -> Gravity.START or Gravity.CENTER
            "center" -> Gravity.CENTER
            "right" -> Gravity.END or Gravity.CENTER
            else -> Gravity.START or Gravity.CENTER
        }
    }

    private fun getFormat(context: Context, format: String, nowTime: Date): String {
        var finalFormat: String = format
        if (finalFormat.contains("NNNN")) finalFormat = finalFormat.replace(
            "NNNN", getLunar(context, 4)
        )
        if (finalFormat.contains("NNN")) finalFormat = finalFormat.replace(
            "NNN", getLunar(context, 3)
        )
        if (finalFormat.contains("NN")) finalFormat = finalFormat.replace(
            "NN", getLunar(context, 2)
        )
        if (finalFormat.contains("N")) {
            finalFormat = finalFormat.replace("N", getLunar(context, 1))
        }
        if (finalFormat.contains("dddd")) finalFormat = finalFormat.replace("dddd", "dd号")
        if (finalFormat.contains("ddd")) finalFormat = finalFormat.replace("ddd", "d号")
        if (finalFormat.contains("FF")) finalFormat = finalFormat.replace("FF", getPeriod(nowTime))
        if (finalFormat.contains("GG")) finalFormat =
            finalFormat.replace("GG", getDiZhiHour(nowTime))
        return finalFormat
    }

    private fun getPeriod(nowTime: Date): String {
        return when (formatDate("HH", nowTime)) {
            "00", "01", "02", "03", "04", "05" -> "凌晨"
            "06", "07", "08", "09", "10", "11" -> "上午"
            "12" -> "中午"
            "13", "14", "15", "16", "17" -> "下午"
            "18" -> "傍晚"
            "19", "20", "21", "22", "23" -> "晚上"
            else -> ""
        }
    }

    private fun getDiZhiHour(nowTime: Date): String {
        val diZhiArr = LunarHelperUtils.mDiZhi

        if (diZhiArr.isEmpty()) return ""
        if (diZhiArr.size != 12) return ""

        val curHour = when (formatDate("HH", nowTime)) {
            "23", "00" -> diZhiArr[0]
            "01", "02" -> diZhiArr[1]
            "03", "04" -> diZhiArr[2]
            "05", "06" -> diZhiArr[3]
            "07", "08" -> diZhiArr[4]
            "09", "10" -> diZhiArr[5]
            "11", "12" -> diZhiArr[6]
            "13", "14" -> diZhiArr[7]
            "15", "16" -> diZhiArr[8]
            "17", "18" -> diZhiArr[9]
            "19", "20" -> diZhiArr[10]
            "21", "22" -> diZhiArr[11]
            else -> ""
        }.let { if (it.isNotBlank()) it + "时" else "" }
        return curHour
    }

    private fun getDate(context: Context, nowTime: Date): String {
        var dateFormat = ""
        if (isZh(context)) {
            if (isYear) dateFormat += "YY年"
            if (isMonth) dateFormat += "M月"
            if (isDay) dateFormat += "d日"
            if (isWeek) dateFormat += "E"
            if (!isHideSpace && !isDoubleRow) dateFormat += " "
        } else {
            if (isWeek) dateFormat += "E"
            if (!isHideSpace && !isDoubleRow) dateFormat += " "
            if (isMonth) {
                dateFormat += "M"
                if (isDay || isYear) dateFormat += "/"
            }
            if (isDay) {
                dateFormat += "d"
                if (isYear) dateFormat += "/"
            }
            if (isYear) {
                dateFormat += "YY"
            }
            if (!isHideSpace && !isDoubleRow) dateFormat += " "
        }
        return formatDate(dateFormat, nowTime)
    }

    private fun getTime(context: Context, nowTime: Date): String {
        var period: String
        var doubleHour: String
        var timeFormat = ""
        timeFormat += if (context.is24) "HH:mm" else "hh:mm"
        if (isSecond) timeFormat += ":ss"
        timeFormat = formatDate(timeFormat, nowTime)
        if (isPeriod) {
            if (isZh(context)) {
                period = getPeriod(nowTime)
                if (!isHideSpace) period += " "
                timeFormat = period + timeFormat
            } else {
                period = " " + formatDate("a", nowTime)
                timeFormat += period
            }
        }
        if (isDoubleHour) {
            doubleHour = getDiZhiHour(nowTime)
            if (!isHideSpace) doubleHour = "$doubleHour "
            timeFormat = doubleHour + timeFormat
        }
        return timeFormat
    }
}