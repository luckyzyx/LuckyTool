package com.luckyzyx.luckytool.hook.scopes.systemui

import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.toClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.hook.utils.FlowUtils
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object MobileDataIconRelated : Hooker {
    override fun onHook() {
        val osCode = getOSVersionCode
        when (osCode) {
            in 34..Int.MAX_VALUE -> loadHooker(MobileDataIcon)
            in 23..33 -> loadHooker(MobileDataIconV14)
            else -> loadHooker(MobileDataIconV120)
        }
    }

    @Obfuscate
    object MobileDataIcon : Hooker {
        override fun onHook() {
//            val removeIcon = prefs(ModulePrefs).getBoolean("remove_mobile_data_icon", false)
            val removeInout = prefs(ModulePrefs).getBoolean("remove_mobile_data_inout", false)
            val removeType = prefs(ModulePrefs).getBoolean("remove_mobile_data_type", false)
            val hideNonNetwork = prefs(ModulePrefs).getBoolean("hide_non_network_card_icon", false)
            var hideNoSS = prefs(ModulePrefs).getBoolean("hide_nosim_noservice", false)
            dataChannel.wait<Boolean>("hide_nosim_noservice") { hideNoSS = it }

            //Source OplusMobileIconViewModel
            "com.oplus.systemui.statusbar.pipeline.mobile.ui.viewmodel.OplusMobileIconViewModel".toClass()
                .resolve().apply {
                    if (removeInout) {
                        firstMethod { name = "getMobileActivityResId" }.hook {
                            before {
                                result = FlowUtils(appClassLoader).let {
                                    val mutableStateFlow = it.MutableStateFlow(0) ?: return@before
                                    it.asStateFlow(mutableStateFlow) ?: return@before
                                }
                            }
                        }
                    }
                    if (removeType) {
                        firstMethod { name = "getNetworkTypeIcon" }.hook {
                            before {
                                result = FlowUtils(appClassLoader).let {
                                    val mutableStateFlow =
                                        it.MutableStateFlow(null) ?: return@before
                                    it.asStateFlow(mutableStateFlow) ?: return@before
                                }
                            }
                        }
                    }
                    if (hideNonNetwork) {
                        firstMethod {
                            name = "isVisible"
                            returnType = "kotlinx.coroutines.flow.StateFlow"
                        }.hook {
                            after {
                                if (result == null) return@after

                                val originalValue =
                                    FlowUtils(appClassLoader).getValue<Boolean>(result!!) ?: false
                                if (!originalValue) return@after

                                val subId =
                                    firstField { name = "subscriptionId" }.of(instance).get<Int>()
                                val localSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                                result = FlowUtils(appClassLoader).let {
                                    val mutableStateFlow = it.MutableStateFlow(subId == localSubId)
                                        ?: return@after
                                    it.asStateFlow(mutableStateFlow) ?: return@after
                                }
                            }
                        }
                    }
                }

            //Source MobileIconViewModel
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MobileIconViewModel".toClass()
                .resolve().apply {
                    if (hideNonNetwork) {
                        firstMethod {
                            name = "isVisible"
                            returnType = "kotlinx.coroutines.flow.StateFlow"
                        }.hook {
                            after {
                                if (result == null) return@after

                                val originalValue =
                                    FlowUtils(appClassLoader).getValue<Boolean>(result!!) ?: false
                                if (!originalValue) return@after

                                val subId =
                                    firstField { name = "subscriptionId" }.of(instance).get<Int>()
                                val localSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                                result = FlowUtils(appClassLoader).let {
                                    val mutableStateFlow = it.MutableStateFlow(subId == localSubId)
                                        ?: return@after
                                    it.asStateFlow(mutableStateFlow) ?: return@after
                                }
                            }
                        }
                    }
                }

            //Source LocationBasedMobileViewModel
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel".toClass()
                .resolve().apply {
                    if (hideNonNetwork) {
                        firstMethod {
                            name = "isVisible"
                            returnType = "kotlinx.coroutines.flow.StateFlow"
                        }.hook {
                            after {
                                if (result == null) return@after

                                val originalValue =
                                    FlowUtils(appClassLoader).getValue<Boolean>(result!!) ?: false
                                if (!originalValue) return@after

                                val subId =
                                    firstField { name = "subscriptionId" }.of(instance).get<Int>()
                                val localSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                                result = FlowUtils(appClassLoader).let {
                                    val mutableStateFlow = it.MutableStateFlow(subId == localSubId)
                                        ?: return@after
                                    it.asStateFlow(mutableStateFlow) ?: return@after
                                }
                            }
                        }
                    }
                }

            //Source OplusStatusBarSignalPolicy
            "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarSignalPolicy".toClass()
                .resolve().optional(true).apply {
                    (firstMethodOrNull {
                        name = "updateSlotIconVisibility"
                        parameterCount { it in 3..4 }
                    } ?: firstMethod {
                        name { it.contains("updateSlotIconVisibility") }
                        parameterCount { it in 3..4 }
                    }).hook {
                        before {
                            if (!hideNoSS) return@before
                            val keys = args.filterIsInstance<String>()
                            if (keys.contains("nosim_all")) {
                                args(args.indexOfFirst { it is Int }).set(0)
                            }
                        }
                    }
                }
        }
    }

    @Obfuscate
    object MobileDataIconV14 : Hooker {
        override fun onHook() {
            //        val removeIcon = prefs(ModulePrefs).getBoolean("remove_mobile_data_icon", false)
            val removeInout = prefs(ModulePrefs).getBoolean("remove_mobile_data_inout", false)
            val removeType = prefs(ModulePrefs).getBoolean("remove_mobile_data_type", false)
            var hideNonNetwork = prefs(ModulePrefs).getBoolean("hide_non_network_card_icon", false)
            dataChannel.wait<Boolean>("hide_non_network_card_icon") { hideNonNetwork = it }
            var hideNoSS = prefs(ModulePrefs).getBoolean("hide_nosim_noservice", false)
            dataChannel.wait<Boolean>("hide_nosim_noservice") { hideNoSS = it }

            //Source OplusStatusBarMobileViewExImpl -> initView
            VariousClass(
                "com.oplusos.systemui.statusbar.OplusStatusBarMobileView", //C12.1
                "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarMobileViewExImpl" //C13
            ).toClass().resolve().apply {
                firstMethod { name = "initViewState" }.hook {
                    after {
                        if (hideNonNetwork) {
                            val state = args().first().any()
                            val subId =
                                state?.asResolver()?.firstField { name = "subId" }?.get<Int>()
                            val subId2 = SubscriptionManager.getDefaultDataSubscriptionId()
                            firstField { name = "mMobileGroup" }.of(instance)
                                .get<ViewGroup>()?.isVisible = subId == subId2
                        }
//                    if (removeIcon) field { name = "mMobileGroup" }.get(instance)
//                        .cast<ViewGroup>()?.isVisible = false
                        if (removeInout) firstField { name = "mDataActivity" }.of(instance)
                            .get<View>()?.isVisible = false
                        if (removeType) firstField {
                            name = "mMobileType"
                            if (SDK < A13) superclass()
                        }.of(instance).get<View>()?.isVisible = false
                    }
                }
                firstMethod {
                    name { it.startsWith("update") && it.endsWith("State") }
                }.hook {
                    after {
                        if (hideNonNetwork) {
                            val state = args().first().any()
                            val subId =
                                state?.asResolver()?.firstField { name = "subId" }?.get<Int>()
                            val subId2 = SubscriptionManager.getDefaultDataSubscriptionId()
                            firstField { name = "mMobileGroup" }.of(instance)
                                .get<ViewGroup>()?.isVisible = subId == subId2
                        }
//                    if (removeIcon) field { name = "mMobileGroup" }.get(instance)
//                        .cast<ViewGroup>()?.isVisible = false
                        if (removeInout) firstField { name = "mDataActivity" }.of(instance)
                            .get<View>()?.isVisible = false
                        if (removeType) firstField {
                            name = "mMobileType"
                            if (SDK < A13) superclass()
                        }.of(instance).get<View>()?.isVisible = false
                    }
                }
            }

            //Source OplusStatusBarSignalPolicyExImpl
            VariousClass(
                "com.oplusos.systemui.ext.StatusBarSignalPolicyExt", //C12.1
                "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarSignalPolicyExImpl" //C13
            ).toClass().resolve().apply {
                firstMethod { name = "setNoSims"; parameterCount = 3 }.hook {
                    after {
                        if (!hideNoSS) return@after
                        val iconController = firstMethodOrNull { name = "getIconController" }
                            ?.of(instance)?.invoke() ?: firstField { name = "iconController" }
                            .of(instance).get() ?: return@after
                        val slotNoSim =
                            firstField { name = "slotNoSim" }.of(instance).get<String>()
                        iconController.asResolver().apply {
                            firstMethod {
                                name = "setIconVisibility"
                                firstMethodOrNull { name = "setIconVisibility" } ?: superclass()
                            }.invoke(slotNoSim, false)
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    object MobileDataIconV120 : Hooker {
        override fun onHook() {
//        val removeIcon = prefs(ModulePrefs).getBoolean("remove_mobile_data_icon", false)
            val removeInout = prefs(ModulePrefs).getBoolean("remove_mobile_data_inout", false)
            val removeType = prefs(ModulePrefs).getBoolean("remove_mobile_data_type", false)
            var hideNonNetwork = prefs(ModulePrefs).getBoolean("hide_non_network_card_icon", false)
            dataChannel.wait<Boolean>("hide_non_network_card_icon") { hideNonNetwork = it }
            var hideNoSS = prefs(ModulePrefs).getBoolean("hide_nosim_noservice", false)
            dataChannel.wait<Boolean>("hide_nosim_noservice") { hideNoSS = it }

            //Source StatusBarMobileView
            "com.android.systemui.statusbar.StatusBarMobileView".toClass().resolve().apply {
                firstMethod { name = "initViewState" }.hook {
                    after {
                        if (hideNonNetwork) {
                            val state = firstField { name = "mState" }.of(instance).get()
                            val subId =
                                state?.asResolver()?.firstField { name = "subId" }?.get<Int>()
                            val subId2 = SubscriptionManager.getDefaultDataSubscriptionId()
                            firstField { name = "mMobileGroup" }.of(instance)
                                .get<ViewGroup>()?.isVisible = subId == subId2
                        }
//                    if (removeIcon) field { name = "mMobileGroup" }.get(instance)
//                        .cast<ViewGroup>()?.isVisible = false
                        if (removeInout) firstField { name = "mInoutContainer" }.of(instance)
                            .get<View>()?.isVisible = false
                        if (removeType) firstField { name = "mMobileType" }.of(instance)
                            .get<View>()?.isVisible = false
                    }
                }
                firstMethod { name = "updateState" }.hook {
                    after {
                        if (hideNonNetwork) {
                            val state = firstField { name = "mState" }.of(instance).get()
                            val subId =
                                state?.asResolver()?.firstField { name = "subId" }?.get<Int>()
                            val subId2 = SubscriptionManager.getDefaultDataSubscriptionId()
                            firstField { name = "mMobileGroup" }.of(instance)
                                .get<ViewGroup>()?.isVisible = subId == subId2
                        }
//                    if (removeIcon) field { name = "mMobileGroup" }.get(instance)
//                        .cast<ViewGroup>()?.isVisible = false
                        if (removeInout) firstField { name = "mInoutContainer" }.of(instance)
                            .get<View>()?.isVisible = false
                        if (removeType) firstField { name = "mMobileType" }.of(instance)
                            .get<View>()?.isVisible = false
                    }
                }
            }

            //Source SignalClusterView
            "com.oplusos.systemui.statusbar.widget.SignalClusterView".toClass().resolve().apply {
                firstMethod { name = "updateNoSimView" }.hook {
                    after {
                        if (!hideNoSS) return@after
                        val mNoSims = firstField { name = "mNoSims" }.of(instance).get<View>()
                        firstMethod { name = "animateHide" }.of(instance).invoke(mNoSims, 8)
                    }
                }
            }
        }
    }
}

