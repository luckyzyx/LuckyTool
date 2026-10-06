package com.luckyzyx.luckytool.hook.scopes.systemui

import android.util.LayoutDirection
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.layoutDirection
import com.highcapable.betterandroid.ui.extension.view.textToString
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.LunarHelperUtils
import com.luckyzyx.luckytool.hook.utils.sysui.WeatherInfoParseHelper
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getScreenOrientation
import org.lsposed.lsparanoid.Obfuscate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Obfuscate
@Suppress("LocalVariableName", "DiscouragedApi")
object ControlCenterDateStyle : YukiBaseHooker() {
    override fun onHook() {
        var removeComma = preferences(ModulePrefs).getBoolean("remove_control_center_date_comma", false)
        dataChannel.wait<Boolean>("remove_control_center_date_comma") { removeComma = it }
        var showLunar =
            preferences(ModulePrefs).getBoolean("statusbar_control_center_date_show_lunar", false)
        dataChannel.wait<Boolean>("statusbar_control_center_date_show_lunar") { showLunar = it }
        var disableScroll = preferences(ModulePrefs).getBoolean(
            "statusbar_control_center_date_disable_text_scroll", false
        )
        dataChannel.wait<Boolean>("statusbar_control_center_date_disable_text_scroll") {
            disableScroll = it
        }
        var displayMode = preferences(ModulePrefs).getString(
            "statusbar_control_center_date_set_display_mode_horizontal",
            "0"
        )
        dataChannel.wait<String>("statusbar_control_center_date_set_display_mode_horizontal") {
            displayMode = it
        }

        var lunarInstance: Any? = null

        //Source OplusQSDateView
        VariousClass(
            "com.oplusos.systemui.qs.widget.OplusQSDateView", //C13
            "com.oplus.systemui.qs.widget.OplusQSDateView" //C14 C15
        ).toClass().resolve().apply {
            firstMethod { name = "updateClock";emptyParameters() }.hook {
                before {
                    if (!removeComma && !showLunar) return@before

                    val dateView = instance<TextView>()
                    val timeInfo = WeatherInfoParseHelper(hostClassLoader!!)
                        .getLocalTimeInfo(dateView.context)
                    val mLastText = firstField { name = "mLastText" }.of(instance).get<String>()
                    if (timeInfo != null) {
                        val dateInfo =
                            timeInfo.asResolver().firstMethod { name = "getDateInfo" }.invoke<String>()
                                ?: ""
                        if (dateInfo != mLastText && dateInfo.isNotBlank()) dateView.text = dateInfo
                    } else {
                        val mDateFormat =
                            firstField { type = DateTimeFormatter::class }.of(instance)
                                .get<DateTimeFormatter>()
                        if (mDateFormat == null) {
                            val mDatePattern =
                                firstField { name = "mDatePattern" }.of(instance).get<String>()
                            firstField { type = DateTimeFormatter::class }.of(instance).set(
                                DateTimeFormatter.ofPattern(mDatePattern, Locale.getDefault())
                            )
                        }
                        val format = LocalDateTime.now().format(mDateFormat)
                        if (format != mLastText) dateView.text = format
                    }
                    if (dateView.text.isNotBlank()) {
                        var res = dateView.textToString()
                        if (removeComma) res = res.replace("，", " ")
                        if (showLunar) {
                            LunarHelperUtils(hostClassLoader!!).apply {
                                if (lunarInstance == null) lunarInstance =
                                    getInstance(dateView.context)
                                val lunarInfo = generateLunarDate(2)
                                if (lunarInfo.isNotBlank()) res += " $lunarInfo"
                            }
                        }
                        dateView.text = res
                        firstField { name = "mLastText" }.of(instance).set(res)
                    }
                    result = null
                }
            }
        }

        if (SDK < A13) return

        var translationX = 0
        //Source OplusQSFooterImpl
        VariousClass(
            "com.oplusos.systemui.qs.OplusQSFooterImpl", //C13
            "com.oplus.systemui.qs.OplusQSFooterImpl" //C14
        ).toClass().resolve().apply {
            firstMethodOrNull { name = "updateQsDateView" }?.hook {
                after {
                    val res = instance<ViewGroup>().resources
                    val mTmpConstraintSet =
                        firstField { name = "mTmpConstraintSet" }.of(instance).get()
                            ?: return@after
                    val mClockView = firstField { name = "mClockView" }.of(instance).get<TextView>()
                        ?: return@after
                    val mQsDateView =
                        firstField { name = "mQsDateView" }.of(instance).get<TextView>()
                            ?: return@after

                    if (disableScroll) mTmpConstraintSet.asResolver().firstMethod {
                        name = "constrainWidth"
                    }.invoke(mQsDateView.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)

                    if (showLunar && (displayMode != "0")) {
                        //162dp
                        val qs_footer_date_width = res.getDimensionPixelSize(
                            res.getIdentifier(
                                "qs_footer_date_width", "dimen",
                                packageName
                            )
                        )
                        //10dp
                        val qs_footer_date_margin_start = res.getDimensionPixelSize(
                            res.getIdentifier(
                                "qs_footer_date_margin_start", "dimen",
                                packageName
                            )
                        )
                        //51dp
                        val qs_footer_date_expand_translation_y = res.getDimensionPixelSize(
                            res.getIdentifier(
                                "qs_footer_date_expand_translation_y", "dimen",
                                packageName
                            )
                        )
                        val isRtl = Locale.getDefault().layoutDirection == LayoutDirection.RTL
                        val width = mClockView.width + qs_footer_date_margin_start
                        if (abs(translationX) < abs(width)) translationX =
                            if (!isRtl) (-width) else width
                        val translationY = qs_footer_date_expand_translation_y / 2

                        getScreenOrientation(res) {
                            if (it) return@getScreenOrientation
                            if (translationX == 0 || translationY == 0) return@getScreenOrientation

                            mTmpConstraintSet.asResolver().apply {
                                firstMethod { }
                                when (displayMode) {
                                    "1" -> firstMethod {
                                        name = "constrainWidth"
                                    }.invoke(mQsDateView.id, qs_footer_date_width * 2)

                                    "2" -> {
                                        firstMethod {
                                            name = "constrainWidth"
                                        }.invoke(
                                            mQsDateView.id,
                                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        firstMethod {
                                            name = "setTranslationX"
                                        }.invoke(mQsDateView.id, translationX.toFloat())
                                        firstMethod {
                                            name = "setTranslationY"
                                        }.invoke(mQsDateView.id, translationY.toFloat())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}