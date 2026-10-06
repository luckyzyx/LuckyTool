package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A11
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getCharColor
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOf
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControlCenterClockStyle : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        val rmClock = preferences(ModulePrefs).getBoolean("remove_control_center_clock_view", false)
        if (rmClock) {
            if (osCode >= 40) loadHooker(RemoveControlCenterClock)
            else if (osCode >= 34) loadHooker(RemoveControlCenterClockV16)
        }

        if (SDK == A11) loadHooker(ControlCenterClockStyleA11)
        else loadHooker(ControlCenterClock)
    }

    @Obfuscate
    object RemoveControlCenterClock : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusQSQuickEntranceComponent
            "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent".toClassOrNull()
                ?: return
            //Source Clock
            "com.android.systemui.statusbar.policy.Clock".toClass().resolve().method {
                name { it == "onAttachedToWindow" || it == "updateClock" }
            }.hookAll {
                after {
                    val view = instance<TextView>()
                    val name = safeOfNull { view.resources.getResourceEntryName(view.id) }
                    if (name == "oplus_qs_clock" || name == "qs_footer_clock") {
                        view.isVisible = false
                    }
                }
            }
        }
    }

    @Obfuscate
    object RemoveControlCenterClockV16 : YukiBaseHooker() {
        override fun onHook() {
            val newQsClock =
                "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent".toClassOrNull()
                    ?.resolve()?.method { name = "updateClockViewLayoutByOrientation" }
                    ?.isEmpty() == false

            if (newQsClock) {
                //Source OplusQSQuickEntranceComponent
                "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent".toClass()
                    .resolve().apply {
                        firstMethod { name = "updateClockViewLayoutByOrientation" }.hook {
                            before {
                                firstField { name = "clockView" }.of(instance)
                                    .get<View>()?.isVisible = false
                            }
                        }
                    }
            } else {
                //Source OplusSeparateQSQuickEntranceManager QSQuickEntranceImpl
                "com.oplus.systemui.separate.OplusSeparateQSQuickEntranceManager\$QSQuickEntranceImpl".toClass()
                    .resolve().apply {
                        firstMethod {
                            name = "getClockView"
                            returnType = TextView::class
                        }.hook {
                            intercept()
                        }
                    }
            }
        }
    }

    @Obfuscate
    object ControlCenterClock : YukiBaseHooker() {
        override fun onHook() {
            val osCode = getOSVersionCode
            val showSecond =
                preferences(ModulePrefs).getBoolean("control_center_clock_show_second", false)
            var redOneMode = preferences(ModulePrefs).getString(
                "statusbar_control_center_clock_red_one_mode", "0"
            )
            dataChannel.wait<String>("statusbar_control_center_clock_red_one_mode") {
                redOneMode = it
            }
            var colonStyle = preferences(ModulePrefs).getString(
                "statusbar_control_center_clock_colon_style", "0"
            )
            dataChannel.wait<String>("statusbar_control_center_clock_colon_style") {
                colonStyle = it
            }

            //Source Clock
            "com.android.systemui.statusbar.policy.Clock".toClass().resolve().apply {
                if (osCode >= 37) {
                    //C16+: setShowSecondsAndUpdate 已移除,直接接管 mShowSeconds 字段
                    firstMethod { name = "updateShowSeconds" }.hook {
                        before {
                            val view = instance<TextView>()
                            val clockName = safeOfNull {
                                view.context.resources.getResourceEntryName(view.id)
                            } ?: return@before
                            when (clockName) {
                                "qs_footer_clock" -> {}  //经典模式时钟
                                "oplus_qs_clock" -> {}  //分离模式时钟
                                else -> return@before
                            }
                            if (showSecond) firstField { name = "mShowSeconds" }.of(instance)
                                .set(true)
                        }
                    }
                } else {
                    firstMethod { name = "setShowSecondsAndUpdate" }.hook {
                        before {
                            val view = instance<TextView>()
                            val clockName = safeOfNull {
                                view.context.resources.getResourceEntryName(view.id)
                            } ?: return@before
                            when (clockName) {
                                "qs_footer_clock" -> {}  //经典模式时钟
                                "oplus_qs_clock" -> {}  //分离模式时钟
                                else -> return@before
                            }
                            if (showSecond) firstArg().set(true)
                        }
                    }
                }
            }

            //Source BaseClockExt
            VariousClass(
                "com.oplusos.systemui.ext.BaseClockExt", //C13
                "com.oplus.systemui.common.clock.OplusClockExImpl" //C14
            ).toClass().resolve().apply {
                firstMethod {
                    name = "setTextWithRedOneStyle"
                    parameterCount { it in 2..3 }
                }.hook {
                    after {
                        if (redOneMode == "0" && colonStyle == "0") return@after
                        val view = firstArg().get<TextView>() ?: return@after
                        val clockName = safeOfNull {
                            view.context.resources.getResourceEntryName(view.id)
                        } ?: return@after
                        when (clockName) {
                            "qs_footer_clock" -> {}  //经典模式时钟
                            "oplus_qs_clock" -> {}  //分离模式时钟
                            else -> return@after
                        }
                        val char = (if (osCode >= 40) arg(1) else lastArg()).get<CharSequence>()
                            ?: return@after
                        if (char.isBlank()) return@after
                        setStyle(view, char, colonStyle, redOneMode)
                    }
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun setStyle(
        view: TextView, char: CharSequence, colonStyle: String, redStyle: String
    ) {
        val colonMode = if (colonStyle == "1") 1 else if (colonStyle == "2") 2 else 0
        val redMode = if (redStyle == "1") 1 else if (redStyle == "2") 2 else 0
        var sb = StringBuilder(view.text)
        if (colonMode != 0) {
            when (colonMode) {
                1 -> sb = StringBuilder(char)
                2 -> for (i in char.indices) {
                    if (sb[i].toString() == ":") {
                        sb = sb.replace(i, i + 1, "\u200e\u2236")
                    }
                }
            }
        }
        if (redMode != 2) {
            val sp = SpannableString(sb)
            for (i2 in 0 until 2) {
                if (sb[i2].toString() == "1") {
                    when (redMode) {
                        0 -> {
                            val color = getCharColor(view.text)
                            if (color != null) sp.setSpan(
                                ForegroundColorSpan(color), i2, i2 + 1, 0
                            )
                        }

                        1 -> {
                            val color = safeOf("#c41442".toColorInt()) {
                                view.context.getColor(
                                    view.resources.getIdentifier(
                                        "red_clock_hour_color", "color", packageName
                                    )
                                )
                            }
                            sp.setSpan(ForegroundColorSpan(color), i2, i2 + 1, 0)
                        }
                    }
                }
            }
            view.setText(sp, TextView.BufferType.SPANNABLE)
        } else view.text = sb
    }

    @Obfuscate
    object ControlCenterClockStyleA11 : YukiBaseHooker() {
        override fun onHook() {
            val showSecond =
                preferences(ModulePrefs).getBoolean("control_center_clock_show_second", false)
            var redOneMode = preferences(ModulePrefs).getString(
                "statusbar_control_center_clock_red_one_mode", "0"
            )
            dataChannel.wait<String>("statusbar_control_center_clock_red_one_mode") {
                redOneMode = it
            }

            //Source Clock
            "com.android.systemui.statusbar.policy.Clock".toClass().resolve().apply {
                firstMethod { name = "setShowSecondsAndUpdate" }.hook {
                    before {
                        val view = instance<TextView>()
                        if (view.context.resources.getResourceEntryName(view.id) != "qs_footer_clock") return@before
                        if (showSecond) firstArg().set(true)
                    }
                }
                firstMethod {
                    name = "setTextWithOpStyle"
                    parameterCount = 1
                }.hook {
                    after {
                        val view = instance<TextView>()
                        if (view.context.resources.getResourceEntryName(view.id) != "qs_footer_clock") return@after
                        val char = firstArg().get<CharSequence>() ?: return@after
                        setStyle(view, char, "0", redOneMode)
                    }
                }
            }
        }
    }
}