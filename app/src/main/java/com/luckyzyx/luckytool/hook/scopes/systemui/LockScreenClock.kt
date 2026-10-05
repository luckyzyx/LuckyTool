package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.allViews
import androidx.core.view.children
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.ClockSwitchHelper
import com.luckyzyx.luckytool.hook.utils.sysui.WeatherInfoParseHelper
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.safeOf
import org.lsposed.lsparanoid.Obfuscate
import java.util.Calendar

@Obfuscate
object LockScreenClock : YukiBaseHooker() {

    override fun onHook() {
        val removeClock = preferences(ModulePrefs).getBoolean("remove_lock_screen_clock_component", false)
        if (removeClock) {
            if (SDK >= A15) loadHooker(RemoveLockScreenClock)
            else loadHooker(RemoveLockScreenClockV14)
        } else loadHooker(LockScreenClockStyleV14)
    }

    @Obfuscate
    object RemoveLockScreenClock : YukiBaseHooker() {
        override fun onHook() {
            //Source KeyguardStyleClockControllerImpl
            "com.oplus.systemui.keyguard.clockstyle.KeyguardStyleClockControllerImpl".toClass()
                .resolve().apply {
                    firstMethod { name = "setKeyguardStyleClockVisibility" }.hook {
                        before {
                            firstField { name = "keyguardStyleClock" }.of(instance)
                                .get<View>()?.isVisible = false
                            result = null
                        }
                    }
                }
        }
    }

    @Obfuscate
    object RemoveLockScreenClockV14 : YukiBaseHooker() {
        override fun onHook() {
            //Source KeyguardClockSwitch
            "com.android.keyguard.KeyguardClockSwitch".toClass().resolve().apply {
                if (SDK >= A14) {
                    firstMethod { name = "onFinishInflate" }.hook {
                        after {
                            firstField { name = "mSmallClockFrame" }.of(instance)
                                .get<View>()?.isVisible = false
                        }
                    }
                    firstMethod { name = "updateClockViews" }.hook {
                        after {
                            firstField { name = "mSmallClockFrame" }.of(instance)
                                .get<View>()?.isVisible = false
                        }
                    }
                } else {
                    firstMethod { name = "onFinishInflate" }.hook {
                        after {
                            firstField { name = "mSmallClockFrame" }.of(instance)
                                .get<View>()?.isVisible = false
                        }
                    }
                    firstMethod { name = "setClockPlugin" }.hook {
                        after {
                            firstField { name = "mSmallClockFrame" }.of(instance)
                                .get<View>()?.isVisible = false
                        }
                    }
                    firstMethodOrNull { name = "updateLockScreenMode" }?.hook {
                        after {
                            firstField { name = "mSmallClockFrame" }.of(instance)
                                .get<View>()?.isVisible = false
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object LockScreenClockStyleV14 : YukiBaseHooker() {
        override fun onHook() {
            var redMode = preferences(ModulePrefs).getString("lock_screen_clock_redone_mode", "0")
            dataChannel.wait<String>("lock_screen_clock_redone_mode") { redMode = it }
            var dualClock =
                preferences(ModulePrefs).getBoolean("apply_lock_screen_dual_clock_redone", false)
            dataChannel.wait<Boolean>("apply_lock_screen_dual_clock_redone") { dualClock = it }
            val isCenter = preferences(ModulePrefs).getBoolean("set_lock_screen_centered", false)
            val userTypeface =
                preferences(ModulePrefs).getBoolean("lock_screen_clock_use_user_typeface", false)
            val weatherInfoClazz = WeatherInfoParseHelper(hostClassLoader!!).weatherInfoClazz
            val timeInfoClazz = WeatherInfoParseHelper(hostClassLoader!!).timeInfoClazz

            //OPPO/Realme kgd_single_clock / kgd_dual_clock
            //Source SingleClockView kgd_single_clock
            VariousClass(
                "com.oplusos.systemui.keyguard.clock.SingleClockView", //C13
                "com.oplus.systemui.shared.clocks.SingleClockView" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "onFinishInflate" }.hook {
                    after {
                        if (!isCenter && !userTypeface) return@after
                        instance<ViewGroup>().apply {
                            if (isCenter) {
                                setPadding(0, 20.dp, 0, 0)
                                children.forEachIndexed { _, view ->
                                    view.setCenterHorizontally()
                                }
                            }
                            if (userTypeface) allViews.filter { it is TextView }
                                .forEachIndexed { _, view ->
                                    (view as TextView).typeface = Typeface.DEFAULT
                                }
                        }
                    }
                }
                (firstMethodOrNull { name = "updateKeyguardLandClock" }
                    ?: firstMethod { name { it.contains("updateKeyguardLandClock") } }).hook {
                    after {
                        if (isCenter) instance<ViewGroup>().setPadding(0, 20.dp, 0, 0)
                    }
                }
                firstMethod { name = "updateTime" }.hook {
                    after {
                        if (redMode == "0") return@after
                        val mTimeHour =
                            firstField { name = "mTimeHour" }.of(instance).get<TextView>()
                                ?: return@after
                        val mHour = firstField { name = "mHour" }.of(instance).get<String>()
                        if (mHour.isNullOrBlank()) return@after
                        mTimeHour.setClockRed(mHour, redMode)
                    }
                }
            }
            //Source DualClockView kgd_dual_clock
            VariousClass(
                "com.oplusos.systemui.keyguard.clock.DualClockView", //C13
                "com.oplus.systemui.shared.clocks.DualClockView" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "onFinishInflate" }.hook {
                    after {
                        if (!userTypeface) return@after
                        instance<ViewGroup>().allViews.filter { it is TextView }
                            .forEachIndexed { _, view ->
                                (view as TextView).typeface = Typeface.DEFAULT
                            }
                    }
                }
                method { parameters { it.contains(weatherInfoClazz) } }.hookAll {
                    after {
                        if (!dualClock) return@after
                        val type: String = method.name.let { s ->
                            if (s.contains("updateLocatedTime")) "LocatedTime"
                            else if (s.contains("updateResidentTime")) "ResidentTime"
                            else return@after
                        }
                        val view = if (args.size > 1) firstArg().get() ?: return@after
                        else instance
                        when (type) {
                            "LocatedTime" -> {
                                val mLocatedTimeHour =
                                    firstField { name = "mLocatedTimeHour" }.of(view)
                                        .get<TextView>() ?: return@after
                                val mLocatedTimeInfo =
                                    firstField { name = "mLocatedTimeInfo" }.of(view).get()
                                        ?: return@after
                                val mHour =
                                    mLocatedTimeInfo.asResolver().firstMethod { name = "getHour" }
                                        .invoke<String>() ?: return@after
                                mLocatedTimeHour.setClockRed(mHour, redMode)
                            }

                            "ResidentTime" -> {
                                val mResidentTimeHour =
                                    firstField { name = "mResidentTimeHour" }.of(view)
                                        .get<TextView>() ?: return@after
                                val mResidentTimeInfo =
                                    firstField { name = "mResidentTimeInfo" }.of(view).get()
                                        ?: return@after
                                val mHour =
                                    mResidentTimeInfo.asResolver().firstMethod { name = "getHour" }
                                        .invoke<String>() ?: return@after
                                mResidentTimeHour.setClockRed(mHour, redMode)
                            }
                        }
                    }
                }
            }
            //OnePlus kgd_red_horizontal_single_clock / kgd_red_horizontal_dual_clock
            //Source RedTextClock
            VariousClass(
                "com.oplusos.systemui.keyguard.clock.RedTextClock", //C13
                "com.oplus.systemui.shared.clocks.RedTextClock" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "onTimeChanged" }.hook {
                    after {
                        if (redMode == "0") return@after
                        val mShouldRunTicker =
                            firstField { name = "mShouldRunTicker" }.of(instance).get<Boolean>()
                                ?: false
                        if (!mShouldRunTicker) return@after
                        val format = firstField { name = "format" }.of(instance).get<String>()
                        val mTime = firstField { name = "mTime" }.of(instance).get<Calendar>()
                            ?: return@after
                        val mTimeHour = instance<TextView>()
                        val mHour = DateFormat.format(format, mTime) as String
                        mTimeHour.setClockRed(mHour, redMode)
                    }
                }
            }
            //Source RedHorizontalSingleClockView
            VariousClass(
                "com.oplusos.systemui.keyguard.clock.RedHorizontalSingleClockView", //C13
                "com.oplus.systemui.shared.clocks.RedHorizontalSingleClockView" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "onFinishInflate" }.hook {
                    after {
                        if (!isCenter && !userTypeface) return@after
                        instance<ViewGroup>().apply {
                            if (isCenter) {
                                setPadding(0, 20.dp, 0, 0)
                                children.forEachIndexed { _, view ->
                                    view.setCenterHorizontally()
                                }
                            }
                            if (userTypeface) allViews.filter { it is TextView }
                                .forEachIndexed { _, view ->
                                    (view as TextView).typeface = Typeface.DEFAULT
                                }
                        }
                    }
                }
                (firstMethodOrNull { name = "setTextFont" }
                    ?: firstMethod { name { it.contains("setTextFont") } }).hook {
                    if (userTypeface) intercept()
                }
            }
            //Source RedHorizontalDualClockView
            VariousClass(
                "com.oplusos.systemui.keyguard.clock.RedHorizontalDualClockView", //C13
                "com.oplus.systemui.shared.clocks.RedHorizontalDualClockView" //C14
            ).loadOrNull(hostClassLoader)?.resolve()?.apply {
                firstMethod { name = "onFinishInflate" }.hook {
                    after {
                        if (!userTypeface) return@after
                        instance<ViewGroup>().allViews.filter { it is TextView }
                            .forEachIndexed { _, view ->
                                (view as TextView).typeface = Typeface.DEFAULT
                            }
                    }
                }
                val locateTime = firstMethodOrNull {
                    parameters { it.contains(timeInfoClazz) }
                    parameterCount = 3
                }
                if (locateTime != null) {
                    method {
                        parameters { it.contains(timeInfoClazz) }
                        parameterCount = 3
                    }.hookAll {
                        after {
                            if (!dualClock) return@after
                            val type: String = method.name.let { s ->
                                if (s.contains("updateLocateTime")) "LocateTime"
                                else if (s.contains("updateResidentTime")) "ResidentTime"
                                else return@after
                            }
                            val view = if (args.size > 1) firstArg().get() ?: return@after
                            else instance
                            when (type) {
                                "LocateTime" -> {
                                    val mLocatedTimeHour = view.asResolver().firstField {
                                        name = "mTvHorizontalLocateClockHour"
                                    }.get<TextView>() ?: return@after
                                    val mLocatedTimeInfo = lastArg().get() ?: return@after
                                    val mHour =
                                        mLocatedTimeInfo.asResolver()
                                            .firstMethod { name = "getHour" }
                                            .invoke<String>() ?: return@after
                                    mLocatedTimeHour.setClockRed(mHour, redMode)
                                }

                                "ResidentTime" -> {
                                    val mResidentTimeHour = view.asResolver().firstField {
                                        name = "mTvHorizontalResidentClockHour"
                                    }.get<TextView>() ?: return@after
                                    val mResidentTimeInfo = lastArg().get() ?: return@after
                                    val mHour =
                                        mResidentTimeInfo.asResolver()
                                            .firstMethod { name = "getHour" }
                                            .invoke<String>() ?: return@after
                                    mResidentTimeHour.setClockRed(mHour, redMode)
                                }
                            }
                        }
                    }
                } else {
                    firstMethod { name = "updateLocateTime" }.hook {
                        after {
                            if (!dualClock) return@after
                            val mContext =
                                firstField { name = "mContext" }.of(instance).get<Context>()
                                    ?: return@after
                            val mLocatedTimeHour =
                                firstField { name = "mTvHorizontalLocateClockHour" }.of(instance)
                                    .get<TextView>() ?: return@after
                            val mLocatedTimeInfo =
                                WeatherInfoParseHelper(hostClassLoader!!).getLocalTimeInfo(mContext)
                                    ?: return@after
                            val mHour =
                                mLocatedTimeInfo.asResolver().firstMethod { name = "getHour" }
                                .invoke<String>() ?: return@after
                            mLocatedTimeHour.setClockRed(mHour, redMode)
                        }
                    }
                    firstMethod { name = "updateResidentTime" }.hook {
                        after {
                            if (!dualClock) return@after
                            val mContext =
                                firstField { name = "mContext" }.of(instance).get<Context>()
                                    ?: return@after
                            val mResidentTimeHour =
                                firstField { name = "mTvHorizontalResidentClockHour" }.of(instance)
                                    .get<TextView>() ?: return@after
                            val info = ClockSwitchHelper(hostClassLoader!!).let {
                                it.getInstance(mContext)
                                    ?.let { its -> it.getResidentWeatherInfo(its) }
                            }
                                ?: WeatherInfoParseHelper(hostClassLoader!!).weatherInfoClazz
                                    .createInstance(isPublic = false)
                            val timeZone =
                                info.asResolver().firstMethod { name = "getTimeZone" }.invoke<String>()
                                    ?: "0.0"
                            val mResidentTimeInfo =
                                WeatherInfoParseHelper(hostClassLoader!!).getResidentTimeInfo(
                                    mContext, timeZone
                                ) ?: return@after
                            val mHour =
                                mResidentTimeInfo.asResolver().firstMethod { name = "getHour" }
                                .invoke<String>() ?: return@after
                            mResidentTimeHour.setClockRed(mHour, redMode)
                        }
                    }
                }
                firstMethod { name = "setTextFont" }.hook {
                    if (userTypeface) intercept()
                }
            }
        }
    }

    private fun View.setCenterHorizontally() {
        layoutParams = LinearLayout.LayoutParams(layoutParams).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        when (this) {
            is RelativeLayout -> {
                children.forEachIndexed { _, view ->
                    if (view is LinearLayout) {
                        view.children.forEachIndexed { _, views ->
                            views.layoutParams =
                                LinearLayout.LayoutParams(views.layoutParams).apply {
                                    gravity = Gravity.CENTER_HORIZONTAL
                                }
                        }
                    }
                }
            }

            is FrameLayout -> {
                children.forEachIndexed { _, view ->
                    view.layoutParams = FrameLayout.LayoutParams(view.layoutParams).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    if (view is LinearLayout) {
                        view.children.forEachIndexed { _, views ->
                            views.layoutParams =
                                LinearLayout.LayoutParams(views.layoutParams).apply {
                                    gravity = Gravity.CENTER_HORIZONTAL
                                }
                        }
                    }
                }
            }

            is LinearLayout -> {
                children.forEachIndexed { _, view ->
                    view.layoutParams = LinearLayout.LayoutParams(view.layoutParams).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    if (view is LinearLayout) {
                        view.children.forEachIndexed { _, views ->
                            views.layoutParams =
                                LinearLayout.LayoutParams(views.layoutParams).apply {
                                    gravity = Gravity.CENTER_HORIZONTAL
                                }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun TextView.setClockRed(format: String, redMode: String) {
        val sp = SpannableStringBuilder(format)
        if (redMode == "1") {
            for (i in format.indices) {
                if (format[i].toString() == "1") {
                    val color = safeOf("#E62F2F".toColorInt()) {
                        context.getColor(
                            resources.getIdentifier(
                                "red_clock_hour_color", "color", packageName
                            )
                        )
                    }
                    sp.setSpan(ForegroundColorSpan(color), i, i + 1, 34)
                }
            }
        }
        text = sp
    }
}
