package com.luckyzyx.luckytool.hook.utils.sysui

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.createInstance
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.safeOf
import com.oplus.util.OplusChineseDateAndSolarDate
import org.lsposed.lsparanoid.Obfuscate
import java.util.Date

@Obfuscate
@Suppress("unused")
class LunarHelperUtils(val classLoader: ClassLoader?) {
    private val tags = "LunarHelperUtils"

    val clazz = VariousClass(
        "com.oplusos.systemui.keyguard.clock.LunarHelper",  //C13
        "com.oplus.systemui.keyguard.clock.LunarHelper"  //C14 C15
    ).load(classLoader)

    companion object {
        val mLeapMonth = arrayOf("闰", "月")
        val mAnimals = arrayOf(
            "鼠年", "牛年", "虎年", "兔年", "龙年", "蛇年",
            "马年", "羊年", "猴年", "鸡年", "狗年", "猪年"
        )
        val mTianGan = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        val mDiZhi = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        val mChineseMonthTable =
            arrayOf("月", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "腊")
        val mChineseDayTable = arrayOf(
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
        )
    }

    /**
     * 获取LunarHelper实例
     * @param context Context
     * @return Any?
     */
    fun getInstance(context: Context): Any {
        return clazz.createInstance(context, isPublic = false)
    }

    /**
     * 调整农历月份
     * @param i Int
     * @param i2 Int
     * @return Int?
     */
    private fun adjustLunarMonth(i: Int, i2: Int): Int = safeOf(0) {
        if (i2 !in 1..<13) return i
        val i3 = i2 + 1
        return if (i3 == i) i2 + 12 else if (i > i3) i - 1 else i
    }


    /**
     * 获取动物年
     * @param i Int
     * @return String?
     */
    private fun getAnimalsYear(i: Int): String = safeOf("") {
        return mAnimals[(i - 4) % 12]
    }

    /**
     * 获取中国农历日期字符串
     * @param i Int
     * @return String?
     */
    private fun getChineseLunarDayString(i: Int): String = safeOf("") {
        return mChineseDayTable[i - 1]
    }

    /**
     * 获取中国农历月份字符串
     * @param i Int
     * @return String?
     */
    private fun getChineseLunarMonthString(i: Int): String = safeOf("") {
        var i2 = i
        if (i > 12) {
            i2 = i - 12
        }
        return mChineseMonthTable[i2] + mLeapMonth[1]
    }


    /**
     * 获取周期
     * @param i Int
     * @return String?
     */
    private fun getCyclical(i: Int): String = safeOf("") {
        val i3 = (i - 1900) + 36
        return mTianGan[i3 % 10] + mDiZhi[i3 % 12]
    }

    /**
     * 获取闰月字符串
     * @param i Int
     * @param i Int
     * @return String?
     */
    private fun getLeapLunarMonthString(i: Int, i2: Int): String = safeOf("") {
        if (i2 in 1..12 && i - 12 == i2) {
            return mLeapMonth[0] + getChineseLunarMonthString(i2)
        }
        return getChineseLunarMonthString(i)
    }

    /**
     * 获取日历字符串
     * @param instance Any
     * @param time Long
     * @return String?
     */
    fun getDateToString(instance: Any?, time: Long = System.currentTimeMillis()): String? {
        return clazz.resolve().firstMethod {
            name = "getDateToString"
            parameters(Long::class)
        }.of(instance).invoke<String>(time)
    }

    /**
     * 生成农历日期字符串
     * @return String
     */
    @Suppress("LocalVariableName")
    fun generateLunarDate(level: Int = 4, time: Long = System.currentTimeMillis()): String {
        return try {
            val date = Date(time)
            val year = formatDate("yyyy", date).toInt()
            val month = formatDate("MM", date).toInt()
            val day = formatDate("dd", date).toInt()
            val SunDateToChineseDate =
                OplusChineseDateAndSolarDate.SunDateToChineseDate(year, month, day)
            val GetChLeapMonth =
                OplusChineseDateAndSolarDate.GetChLeapMonth(SunDateToChineseDate[0])
            SunDateToChineseDate[1] = adjustLunarMonth(SunDateToChineseDate[1], GetChLeapMonth)
            val getCyclical = getCyclical(SunDateToChineseDate[0])
            val getAnimalsYear = getAnimalsYear(SunDateToChineseDate[0])
            val getLeapLunarMonthString =
                getLeapLunarMonthString(SunDateToChineseDate[1], GetChLeapMonth)
            val getChineseLunarDayString = getChineseLunarDayString(SunDateToChineseDate[2])
            val final = when (level) {
                1 -> getChineseLunarDayString
                2 -> getLeapLunarMonthString + getChineseLunarDayString
                3 -> getAnimalsYear + getLeapLunarMonthString + getChineseLunarDayString
                4 -> getCyclical + getAnimalsYear + getLeapLunarMonthString + getChineseLunarDayString
                else -> getCyclical + getAnimalsYear + getLeapLunarMonthString + getChineseLunarDayString
            }
//            LogUtils.d("generateLunarDate", "final", final, true)
            final
        } catch (e: Exception) {
            LogUtils.e(tags, "generateLunarDate", "$e")
            ""
        }
    }
}