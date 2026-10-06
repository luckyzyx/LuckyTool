@file:Suppress("ConstPropertyName")

package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Context
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate
import java.util.function.Supplier

@Obfuscate
object LockScreenComponentStyle : YukiBaseHooker() {
    override fun onHook() {
        if (SDK == A14) loadHooker(LockScreenComponentStyleV14)
        if (SDK < A14) loadHooker(LockScreenComponentStyleV13)
        if (preferences(ModulePrefs).getBoolean("force_display_clock_style_options", false)) {
            if (SDK == A13) loadHooker(ForceDisplayClockStyleOptionsV13)
        }
    }

    @Obfuscate
    object LockScreenComponentStyleV14 : YukiBaseHooker() {
        private const val singleClockProvider =
            "com.oplus.systemui.shared.clocks.SingleClockProvider" //C14
        private const val dualClockProvider =
            "com.oplus.systemui.shared.clocks.DualClockProvider" //C14
        private const val redHorizontalSingleClockProvider =
            "com.oplus.systemui.shared.clocks.RedHorizontalSingleClockProvider" //C14
        private const val redHorizontalDualClockProvider =
            "com.oplus.systemui.shared.clocks.RedHorizontalDualClockProvider" //C14
        private const val sysuiColorExtractor =
            "com.android.systemui.colorextraction.SysuiColorExtractor" //C14
        private val clockSettings = VariousClass(
            "com.android.systemui.plugins.ClockSettings", //C14
            "com.android.systemui.plugins.clocks.ClockSettings"  //C15
        )

        override fun onHook() {
            val mode = preferences(ModulePrefs).getString("lock_screen_custom_clock_component_style", "0")

            //Source ClockRegistry lock_screen_custom_clock_face
            "com.android.systemui.shared.clocks.ClockRegistry".toClass().resolve().apply {
                firstMethod { name = "getSettings" }.hook {
                    after {
                        if (mode == "0") return@after
                        val res = result<Any>() ?: return@after
                        val clockId =
                            res.asResolver().firstMethod { name = "getClockId" }.invoke<String>()
                                ?: return@after
                        val isSingle = !clockId.contains("DualClock")
                        val provider = when (mode) {
                            "1" -> if (isSingle) singleClockProvider else dualClockProvider
                            "2" -> if (isSingle) redHorizontalSingleClockProvider else redHorizontalDualClockProvider
                            else -> return@after
                        }
                        provider.toClassOrNull() ?: return@after
                        result = clockSettings.toClass().resolve().firstConstructor {
                            parameters(String::class, classOf<Int>(primitiveType = false))
                        }.create(provider, null)
                    }
                }
            }

            //Source ClockSwitchHelper
            "com.oplus.systemui.keyguard.clock.ClockSwitchHelper".toClass().resolve().apply {
                firstMethod { name = "buildAllClockProviders" }.hook {
                    before {
                        if (mode == "0") return@before
                        val context = firstField { name = "mContext" }.of(instance).get<Context>()
                            ?: return@before
                        val layoutInflater = context.layoutInflater
                        val colorExtractor = firstArg().get() ?: return@before
                        val singleClock = singleClockProvider.toClassOrNull()
                            ?.createInstance(
                                context,
                                layoutInflater,
                                colorExtractor,
                                isPublic = false
                            )
                        val dualClock = dualClockProvider.toClassOrNull()
                            ?.createInstance(
                                context,
                                layoutInflater,
                                colorExtractor,
                                isPublic = false
                            )
                        val redHorizontalSingleClock =
                            redHorizontalSingleClockProvider.toClassOrNull()
                                ?.createInstance(
                                    context,
                                    layoutInflater,
                                    colorExtractor,
                                    isPublic = false
                                )
                        val redHorizontalDualClock = redHorizontalDualClockProvider.toClassOrNull()
                            ?.createInstance(
                                context,
                                layoutInflater,
                                colorExtractor,
                                isPublic = false
                            )
                        val list = arrayListOf(
                            singleClock, dualClock,
                            redHorizontalSingleClock, redHorizontalDualClock
                        ).apply {
                            removeIf { it == null }
                            if (isEmpty()) {
                                YLog.error("Clock Providers is empty!")
                                return@before
                            }
                        }
                        result = ArrayList(list)
                    }
                }
            }
        }
    }

    @Obfuscate
    object LockScreenComponentStyleV13 : YukiBaseHooker() {
        private const val singleClockController =
            "com.oplusos.systemui.keyguard.clock.SingleClockController"
        private const val dualClockController =
            "com.oplusos.systemui.keyguard.clock.DualClockController"
        private const val redHorizontalSingleClockController =
            "com.oplusos.systemui.keyguard.clock.RedHorizontalSingleClockController"
        private const val redHorizontalDualClockController =
            "com.oplusos.systemui.keyguard.clock.RedHorizontalDualClockController"
        private const val sysuiColorExtractor =
            "com.android.systemui.colorextraction.SysuiColorExtractor"

        override fun onHook() {
            val mode = preferences(ModulePrefs).getString("lock_screen_custom_clock_component_style", "0")

            //Source SettingsWrapper lock_screen_custom_clock_face
            "com.android.keyguard.clock.SettingsWrapper".toClass().resolve().apply {
                firstMethod { name = "getLockScreenCustomClockFace" }.hook {
                    after {
                        if (mode == "0") return@after
                        val res = result<String>() ?: return@after
                        val controller = if (res.contains("DualClock").not()) when (mode) {
                            "1" -> singleClockController
                            "2" -> redHorizontalSingleClockController
                            else -> return@after
                        } else when (mode) {
                            "1" -> dualClockController
                            "2" -> redHorizontalDualClockController
                            else -> return@after
                        }
                        controller.toClassOrNull() ?: return@after
                        result = controller
                    }
                }
            }

            //Source ClockManager
            "com.android.keyguard.clock.ClockManager".toClass().resolve().apply {
                firstConstructor { parameterCount = 8 }.hook {
                    after {
                        if (mode == "0") return@after
                        val context = firstArg().get<Context>() ?: return@after
                        val layoutInflater = context.layoutInflater
                        val colorExtractor = arg(3).get() ?: return@after
                        val opKeyguardClock = Supplier {
                            firstMethod { name = "loadClockByName" }.of(instance).invoke(
                                "com.oplusos.keyguard.OpKeyguardClockController",
                                layoutInflater, colorExtractor
                            )
                        }
                        val singleClock = Supplier {
                            singleClockController.toClassOrNull()
                                ?.createInstance(
                                    context, layoutInflater, colorExtractor,
                                    isPublic = false
                                )
                        }
                        val dualClock = Supplier {
                            dualClockController.toClassOrNull()
                                ?.createInstance(
                                    context, layoutInflater, colorExtractor,
                                    isPublic = false
                                )
                        }
                        val redHorizontalSingleClock = Supplier {
                            redHorizontalSingleClockController.toClassOrNull()
                                ?.createInstance(
                                    context, layoutInflater, colorExtractor,
                                    isPublic = false
                                )
                        }
                        val redHorizontalDualClock = Supplier {
                            redHorizontalDualClockController.toClassOrNull()
                                ?.createInstance(
                                    context, layoutInflater, colorExtractor,
                                    isPublic = false
                                )
                        }
                        arrayListOf(
                            opKeyguardClock, singleClock, dualClock,
                            redHorizontalSingleClock, redHorizontalDualClock
                        ).apply {
                            if (isEmpty()) {
                                YLog.error("Clock Providers is empty!")
                                return@after
                            }
                            forEach {
                                if (it.get() == null) return@forEach
                                firstMethod { name = "addBuiltinClock" }.of(instance).invoke(it)
                            }
                        }
                    }
                }
            }
        }
    }
}