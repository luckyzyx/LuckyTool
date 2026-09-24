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
import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.hookAll
import com.luckyzyx.luckytool.hook.core.instance
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.utils.A11
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getCharColor
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOf
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ControlCenterClockStyle : Hooker {
    override fun onHook() {
        val osCode = getOSVersionCode

        val rmClock = prefs(ModulePrefs).getBoolean("remove_control_center_clock_view", false)
        if (osCode >= 34 && rmClock) {
            loadHooker(RemoveControlCenterClock)
        }

        if (SDK == A11) loadHooker(ControlCenterClockStyleA11)
        else loadHooker(ControlCenterClock)
    }

    @Obfuscate
    object RemoveControlCenterClock : Hooker {
        override fun onHook() {
            val newQsClock =
                "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent"
                    .toClassOrNull()?.resolve()?.optional(true)
                    ?.method { name = "updateClockViewLayoutByOrientation" }
                    ?.isEmpty() == false

            if (newQsClock) {
                //Source OplusQSQuickEntranceComponent
                "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent".toClass()
                    .resolve().optional(true).apply {
                        firstMethod { name = "updateClockViewLayoutByOrientation" }.hook {
                            before {
                                firstField { name = "clockView" }.of(instance).get<View>()
                                    ?.isVisible = false
                            }
                        }
                    }
            } else if ("com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent"
                    .toClassOrNull() != null
            ) {
                // ColorOS 17 的快捷入口组件不再持有 clockView，直接处理时钟视图。
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
            } else {
                //Source OplusSeparateQSQuickEntranceManager QSQuickEntranceImpl
                "com.oplus.systemui.separate.OplusSeparateQSQuickEntranceManager\$QSQuickEntranceImpl"
                    .toClass().resolve().optional(true).apply {
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
    object ControlCenterClock : Hooker {
        override fun onHook() {
            val showSecond =
                prefs(ModulePrefs).getBoolean("control_center_clock_show_second", false)
            var redOneMode =
                prefs(ModulePrefs).getString("statusbar_control_center_clock_red_one_mode", "0")
            dataChannel.wait<String>("statusbar_control_center_clock_red_one_mode") {
                redOneMode = it
            }
            var colonStyle =
                prefs(ModulePrefs).getString("statusbar_control_center_clock_colon_style", "0")
            dataChannel.wait<String>("statusbar_control_center_clock_colon_style") {
                colonStyle = it
            }

            //Source Clock
            "com.android.systemui.statusbar.policy.Clock".toClass().resolve().optional(true).apply {
                (firstMethodOrNull { name = "setShowSecondsAndUpdate" }
                    ?: firstMethod { name = "updateShowSeconds"; emptyParameters() }).hook {
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
                        if (showSecond) {
                            if (args.isNotEmpty()) args().first().setTrue()
                            else firstField { name = "mShowSeconds" }.of(instance).set(true)
                        }
                    }
                }
            }

            //Source BaseClockExt
            VariousClass(
                "com.oplusos.systemui.ext.BaseClockExt", //C13
                "com.oplus.systemui.common.clock.OplusClockExImpl" //C14
            ).toClass().resolve().optional(true).apply {
                firstMethod {
                    name = "setTextWithRedOneStyle"
                    parameterCount { it in 2..3 }
                }.hook {
                    after {
                        if (redOneMode == "0" && colonStyle == "0") return@after
                        val view = args().first().cast<TextView>() ?: return@after
                        val clockName = safeOfNull {
                            view.context.resources.getResourceEntryName(view.id)
                        } ?: return@after
                        when (clockName) {
                            "qs_footer_clock" -> {}  //经典模式时钟
                            "oplus_qs_clock" -> {}  //分离模式时钟
                            else -> return@after
                        }
                        val char = args(1).cast<CharSequence>() ?: return@after
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
                                ForegroundColorSpan(color),
                                i2, i2 + 1, 0
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
    object ControlCenterClockStyleA11 : Hooker {
        override fun onHook() {
            val showSecond =
                prefs(ModulePrefs).getBoolean("control_center_clock_show_second", false)
            var redOneMode =
                prefs(ModulePrefs).getString("statusbar_control_center_clock_red_one_mode", "0")
            dataChannel.wait<String>("statusbar_control_center_clock_red_one_mode") {
                redOneMode = it
            }

            //Source Clock
            "com.android.systemui.statusbar.policy.Clock".toClass().resolve().optional(true).apply {
                firstMethod { name = "setShowSecondsAndUpdate" }.hook {
                    before {
                        val view = instance<TextView>()
                        if (view.context.resources.getResourceEntryName(view.id) != "qs_footer_clock") return@before
                        if (showSecond) args().first().setTrue()
                    }
                }
                firstMethod {
                    name = "setTextWithOpStyle"
                    parameterCount = 1
                }.hook {
                    after {
                        val view = instance<TextView>()
                        if (view.context.resources.getResourceEntryName(view.id) != "qs_footer_clock") return@after
                        val char = args().first().cast<CharSequence>() ?: return@after
                        setStyle(view, char, "0", redOneMode)
                    }
                }
            }
        }
    }
}