package com.luckyzyx.luckytool.hook.scopes.alarmclock

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import androidx.collection.arrayMapOf
import androidx.core.graphics.toColorInt
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class AlarmClockWidget(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    companion object {
        private lateinit var redMode: String

        private val BaseClockWidget = "com.coloros.widget.smallweather.BaseClockWidget"
        private val OnePlusWidget = "com.coloros.widget.smallweather.OnePlusWidget"
//        val OppoWeather = "com.coloros.widget.smallweather.OppoWeather"
//        val OppoWeatherSingle = "com.coloros.widget.smallweather.OppoWeatherSingle"
//        val OppoWeatherVertical = "com.coloros.widget.smallweather.OppoWeatherVertical"
//        val OppoWeatherMultiVertical = "com.coloros.widget.smallweather.OppoWeatherMultiVertical"
//        val RealmeWeather = "com.coloros.widget.smallweather.RealmeWeather"
//        val OxygenWeatherSingle = "com.coloros.widget.smallweather.OxygenWeatherSingle"

        fun setCharRedOne(format: CharSequence): CharSequence {
            val sp = SpannableStringBuilder(format)
            val length = if (format.contains(":")) format.toString().substringBefore(":").length
            else if (format.contains("\u2236")) format.toString().substringBefore("\u2236").length
            else format.length
            for (i in 0 until length) {
                if (format[i].toString() == "1") {
                    val colorRes = "#c41442".toColorInt()
                    sp.setSpan(ForegroundColorSpan(colorRes), i, i + 1, 34)
                }
            }
            return sp
        }

        /**
         * 新版时钟V14+替换RemoteViews
         * @receiver Class<*>
         */
        fun YukiBaseHooker.hookBaseClock(clazz: Class<Any>) {
            clazz.resolve().apply {
                method {
                    emptyParameters()
                    returnType = RemoteViews::class
                }.forEach {
                    it.hook {
                        after {
                            val context = (field { type = Context::class }.firstOrNull()
                                ?: field { type = Context::class; superclass() }.firstOrNull())
                                ?.of(instance)?.get<Context>() ?: return@after
                            context.injectModuleResources()
                            val res = result<RemoteViews>() ?: return@after
                            val layoutName = safeOfNull {
                                context.resources.getResourceEntryName(res.layoutId)
                            } ?: return@after
                            val replaceLayoutId = getReplaceLayout(context, layoutName, redMode)
                                ?: return@after
                            result = RemoteViews(context.packageName, replaceLayoutId)
                        }
                    }
                }
            }
        }

        /**
         * 获取要替换的布局
         * @param context Context
         * @param layoutName String
         * @param redMode String
         * @return Int?
         */
        @SuppressLint("DiscouragedApi")
        fun getReplaceLayout(context: Context, layoutName: String, redMode: String): Int? {
            val isRedMode = layoutName.contains("red")
            val replaceLayoutName = when (redMode) {
                "1" -> if (isRedMode) null else convertLayoutResMap(layoutName, redMode)
                "2" -> if (isRedMode) convertLayoutResMap(layoutName, redMode) else null
                else -> null
            } ?: return null
            val resId =
                context.resources.getIdentifier(replaceLayoutName, "layout", context.packageName)
            return resId.takeIf { it != 0 }
        }

        /**
         * 转换布局名
         * @param layoutName String
         * @param redMode String
         * @return String?
         */
        fun convertLayoutResMap(layoutName: String, redMode: String): String? {
            val layouts = arrayMapOf(
                //OnePlusWidget
                "op_double_clock_red_widget_land_view" to "op_double_clock_widget_land_view",
                "op_double_clock_red_widget_view" to "op_double_clock_widget_view",
                "one_plus_red_widget_land_view" to "one_plus_widget_land_view",
                "one_plus_red_widget_view" to "one_plus_widget_view",
                "table_op_double_clock_red_widget_land_view" to "table_op_double_clock_widget_land_view",
                "table_op_double_clock_red_widget_view" to "table_op_double_clock_widget_view",
                "table_one_plus_red_widget_land_view" to "table_one_plus_widget_land_view",
                "table_one_plus_red_widget_view" to "table_one_plus_widget_view",
                //OppoWeather
                "hor_double_clock_red_widget_land_view_t" to "hor_double_clock_widget_land_view_t",
                "hor_double_clock_red_widget_view_t" to "hor_double_clock_widget_view_t",
                "hor_single_clock_red_widget_land_view_t" to "hor_single_clock_widget_land_view_t",
                "hor_single_clock_red_widget_view_t" to "hor_single_clock_widget_view_t",
                "table_hor_double_clock_red_widget_land_view_t" to "table_hor_double_clock_widget_land_view_t",
                "table_hor_double_clock_red_widget_view_t" to "table_hor_double_clock_widget_view_t",
                "table_hor_single_clock_red_widget_land_view_t" to "table_hor_single_clock_widget_land_view_t",
                "table_hor_single_clock_red_widget_view_t" to "table_hor_single_clock_widget_view_t",
                //OppoWeatherSingle
                "one_line_double_clock_red_widget_land_view_t" to "one_line_double_clock_widget_land_view_t",
                "one_line_double_clock_red_widget_view_t" to "one_line_double_clock_widget_view_t",
                "one_line_hor_single_clock_red_widget_land_view_t" to "one_line_hor_single_clock_widget_land_view_t",
                "one_line_hor_single_clock_red_widget_view_t" to "one_line_hor_single_clock_widget_view_t",
                "table_one_line_double_clock_red_widget_land_view_t" to "table_one_line_double_clock_widget_land_view_t",
                "table_one_line_double_clock_red_widget_view_t" to "table_one_line_double_clock_widget_view_t",
                "table_one_line_hor_single_clock_red_widget_land_view_t" to "table_one_line_hor_single_clock_widget_land_view_t",
                "table_one_line_hor_single_clock_red_widget_view_t" to "table_one_line_hor_single_clock_widget_view_t",
                //OppoWeatherVertical
                "vertical_double_clock_red_widget_land_view_t" to "vertical_double_clock_widget_land_view_t",
                "vertical_double_clock_red_widget_view_t" to "vertical_double_clock_widget_view_t",
                "vertical_single_clock_red_widget_land_view_t" to "vertical_single_clock_widget_land_view_t",
                "vertical_single_clock_red_widget_view_t" to "vertical_single_clock_widget_view_t",
                "table_vertical_double_clock_red_widget_land_view_t" to "table_vertical_double_clock_widget_land_view_t",
                "table_vertical_double_clock_red_widget_view_t" to "table_vertical_double_clock_widget_view_t",
                "table_vertical_single_clock_red_widget_land_view_t" to "table_vertical_single_clock_widget_land_view_t",
                //OppoWeatherMultiVertical
                "hor_double_clock_red_widget_land_view_t" to "hor_double_clock_widget_land_view_t",
                "hor_double_clock_red_widget_view_t" to "hor_double_clock_widget_view_t",
                "hor_single_clock_red_widget_land_view_t" to "hor_single_clock_widget_land_view_t",
                "vertical_multi_clock_red_widget_view_t" to "vertical_multi_clock_widget_view_t",
                "table_hor_double_clock_red_widget_land_view_t" to "table_hor_double_clock_widget_land_view_t",
                "table_hor_double_clock_red_widget_view_t" to "table_hor_double_clock_widget_view_t",
                "table_hor_single_clock_red_widget_land_view_t" to "table_hor_single_clock_widget_land_view_t",
                "table_vertical_multi_clock_red_widget_view_t" to "table_vertical_multi_clock_widget_view_t",
                //OxygenWeatherSingle

                //RealmeWeather

            )
            val filter = layouts.filter { it.key == layoutName || it.value == layoutName }
            return filter.firstNotNullOfOrNull {
                when (redMode) {
                    "1" -> it.key
                    "2" -> it.value
                    else -> null
                }
            }
        }
    }


    override fun onHook() {
        redMode = preferences(ModulePrefs).getString("alarmclock_widget_redone_mode", "0")
        dataChannel.wait<String>("alarmclock_widget_redone_mode") { redMode = it }

        val onePlusWidget = OnePlusWidget.toClassOrNull()?.resolve() ?: return
        when {
            onePlusWidget.optional(true).firstMethodOrNull {
                parameters(String::class, String::class)
                returnType = CharSequence::class
            } != null -> loadHooker(AlarmClock12)

            onePlusWidget.optional(true).firstMethodOrNull {
                returnType = RemoteViews::class
            } != null -> loadHooker(AlarmClock130(dexKitBridge))

            else -> {
                val baseClockWidget = BaseClockWidget.toClassOrNull()
                if (baseClockWidget != null) loadHooker(BaseAlarmClock14)
                else loadHooker(BaseAlarmClock15(dexKitBridge))
            }
        }
    }

    private class BaseAlarmClock15(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //Source BaseClockWidget
            //Source OnePlusWidget / OppoWeather / OppoWeatherSingle / OppoWeatherVertical
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Class<*>>())
                        addForType(classOf<Context>())
                        addForType(classOf<Bitmap>())
                        addForType(classOf<Boolean>())
                        addForType(classOf<Int>())
                    }
                    methods {
                        add {
                            paramCount(0)
                            returnType(classOf<Int>())
                        }
                        add {
                            paramTypes(classOf<RemoteViews>(), classOf<Int>(), classOf<String>())
                            usingStrings("setTimeZone")
                        }
                        add {
                            paramTypes(
                                classOf<RemoteViews>(),
                                classOf<Boolean>(),
                                classOf<Boolean>()
                            )
//                            usingStrings(
//                                "com.oplus.widget.smallweather.WEATHER_CLICK",
//                                "com.oplus.widget.smallweather.RESIDENT_CITY_CLICK"
//                            )
                        }
                        add {
                            paramTypes(
                                classOf<RemoteViews>(),
                                classOf<Int>(),
                                classOf<CharSequence>()
                            )
                            usingStrings("setFormat24Hour", "setFormat12Hour")
                        }
                        add {
                            paramTypes(classOf<RemoteViews>())
                            usingStrings("com.oplus.widget.smallweather.REFRESH_CLICK")
                        }
                    }
                }
            }.apply {
                checkDataList("AlarmClock145")
                hookBaseClock(single().name.toClass())
            }
        }
    }

    private object BaseAlarmClock14 : YukiBaseHooker() {
        override fun onHook() {
            //Source BaseClockWidget
            hookBaseClock(BaseClockWidget.toClass())
        }
    }

    private class AlarmClock130(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //OnePlusWidget setTextViewText -> local_hour_txt -> SpannableStringBuilder -> CharSequence
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Boolean>())
                        addForType(classOf<Handler>())
                    }
                    methods {
                        add { returnType(classOf<Boolean>()) }
                        add { returnType(classOf<Handler>()) }
                        add { paramTypes(classOf<Context>()) }
                        add { paramTypes(classOf<Context>(), classOf<String>()) }
                        add {
                            paramTypes(
                                classOf<Context>(),
                                classOf<String>(),
                                classOf<String>()
                            )
                        }
                    }
                }
            }.apply {
                checkDataList("AlarmClock13")
                single().name.toClass().apply {
                    resolve().method {
                        parameters { it[0] == classOf<Context>() && it[1] == classOf<String>() }
                        parameterCount { it in 2..3 }
                    }.forEach {
                        it.hook {
                            after {
                                if (redMode == "0") return@after
                                result = when (redMode) {
                                    "1" -> result<CharSequence>()?.let { s -> setCharRedOne(s) }
                                    "2" -> result<CharSequence>().toString()
                                    else -> result
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private object AlarmClock12 : YukiBaseHooker() {
        override fun onHook() {
            //Source OnePlusWidget
            "com.coloros.widget.smallweather.OnePlusWidget".toClass().resolve().apply {
                firstMethod {
                    parameters(String::class, String::class)
                    returnType = CharSequence::class
                }.hook {
                    after {
                        if (redMode == "0") return@after
                        result = when (redMode) {
                            "1" -> result<CharSequence>()?.let { setCharRedOne(it) }
                            "2" -> result<CharSequence>().toString()
                            else -> result
                        }
                    }
                }
            }
        }
    }
}
