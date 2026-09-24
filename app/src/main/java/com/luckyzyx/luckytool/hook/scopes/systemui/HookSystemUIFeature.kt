package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.toClass
import com.luckyzyx.luckytool.hook.core.Hooker
import com.luckyzyx.luckytool.hook.core.hook
import com.luckyzyx.luckytool.hook.core.hookAll
import com.luckyzyx.luckytool.hook.core.toClass
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.globals.HookGlobalSystemProperties
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookSystemUIFeature(val dexKitBridge: DexKitBridge) : Hooker {

    companion object {
        var callback: ((key: String, value: Any) -> Unit)? = null
    }

    override fun onHook() {
        val osCode = getOSVersionCode


        loadHooker(HookGlobalFeatureConfig)
        loadHooker(HookGlobalSystemProperties)
        loadHooker(HookGlobalFeatureProvider(dexKitBridge))

        loadHooker(HookFeatureOption)
//        if (osCode >= 34) loadHooker(HookFeatureFlags)
        if (osCode < 34) loadHooker(HookStatusBarFeature)
        loadHooker(HookFlavorOneFeature)
        if (osCode >= 30) loadHooker(HookVolumeFeatureOption)
        if (osCode >= 31) loadHooker(HookQSFeatureOption)
    }

    @Obfuscate
    private object HookFeatureOption : Hooker {
        override fun onHook() {
            //音量条位置
            val volumePosition =
                prefs(ModulePrefs).getString("set_volume_bar_display_position", "0")
            //锁屏充电显示瓦数
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            dataChannel.wait<Boolean>("force_lock_screen_charging_show_wattage") {
                showWattage = it
                callback?.invoke("force_lock_screen_charging_show_wattage", it)
            }
            //WARP充电器样式
            var warpCharge =
                prefs(ModulePrefs).getString("set_lock_screen_warp_charging_style", "0")
            dataChannel.wait<String>("set_lock_screen_warp_charging_style") { warpCharge = it }
            //移除我的设备
            val removeMyDevice =
                prefs(ModulePrefs).getBoolean("remove_control_center_mydevice", false)
            //强制显示时钟样式选项
            val forceDisplayClockStyle =
                prefs(ModulePrefs).getBoolean("force_display_clock_style_options", false)

            //Source FeatureOption
            "com.oplusos.systemui.common.feature.FeatureOption".toClass().resolve().optional(true).apply {
                //C13 C14
                firstMethodOrNull { name = "isOplusVolumeKeyInRight" }?.hook {
                    before {
                        when (volumePosition) {
                            "1" -> resultFalse()
                            "2" -> resultTrue()
                        }
                    }
                }
                //C13
                firstMethodOrNull { name = "isSupportShowWattage" }?.hook {
                    if (warpCharge == "2" && showWattage) replaceToTrue()
                }
                //C12 C13
                if (SDK == A13) {
                    firstMethodOrNull { name = "isUseWarpCharge" }?.hook {
                        before {
                            when (warpCharge) {
                                "1" -> resultTrue()
                                "2" -> resultFalse()
                            }
                        }
                    }
                }
                //C13 C14
                firstMethodOrNull { name = "isSupportMyDevice" }?.hook {
                    if (removeMyDevice) replaceToFalse()
                }
                //C12
                if (SDK < A13) {
                    firstMethodOrNull { name = "isSupportLandClock" }?.hook {
                        if (forceDisplayClockStyle) replaceToTrue()
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookStatusBarFeature : Hooker {
        override fun onHook() {
            //隐藏未使用信号标签 config_isSystemUiExpSignalUi
            val hideSignalLabels =
                prefs(ModulePrefs).getBoolean("hide_inactive_signal_labels_gen2x2", false)

            //Source StatusBarFeatureOption
            VariousClass(
                "com.oplusos.systemui.statusbar.feature.StatusBarFeatureOption", //C13
                "com.oplusos.systemui.common.feature.StatusBarFeatureOption" //C14 C15
            ).toClass().resolve().apply {
                firstMethod { name = "loadAppFeature" }.hook {
                    after {
                        if (hideSignalLabels) firstField {
                            name = "isSystemUiExpSignalUi"
                        }.set(true)
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookFlavorOneFeature : Hooker {
        override fun onHook() {
            //全局搜索按钮
            val searchBtnMode =
                prefs(ModulePrefs).getString("set_control_center_search_button_mode", "0")
            //锁屏充电显示瓦数
            var showWattage =
                prefs(ModulePrefs).getBoolean("force_lock_screen_charging_show_wattage", false)
            //应用专属媒体音量
            val specificVolume =
                prefs(ModulePrefs).getBoolean("enable_app_specific_media_volume", false)

            callback = { key: String, value: Any ->
                when (key) {
                    "force_lock_screen_charging_show_wattage" -> showWattage = value as Boolean
                }
            }

            //Source FlavorOneFeatureOption
            "com.oplusos.systemui.common.feature.FlavorOneFeatureOption".toClass().resolve().optional(true).apply {
                firstMethodOrNull { name = "isSupportSearch" }?.hook {
                    before {
                        when (searchBtnMode) {
                            "1" -> resultTrue()
                            "2" -> resultFalse()
                        }
                    }
                }
                //C14 Realme
                firstMethodOrNull { name = "isShowChargingWattage" }?.hook {
                    if (showWattage) replaceToTrue()
                }
                //C13.1 C14.0
                firstMethodOrNull { name = "isFlavorOneMultiMediaDevice" }?.hook {
                    if (specificVolume) replaceToTrue()
                }
            }
        }
    }

    @Obfuscate
    private object HookVolumeFeatureOption : Hooker {
        override fun onHook() {
            //音量对话框背景透明度
            var volumeBlur =
                prefs(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            dataChannel.wait<Int>("custom_volume_dialog_background_transparency") {
                volumeBlur = it
            }

            //Source VolumeFeatureOption
            "com.oplusos.systemui.common.feature.VolumeFeatureOption".toClass().resolve().optional(true).apply {
                firstMethodOrNull { name = "isVolumeBlurDisabled" }?.hook {
                    if (volumeBlur > -1) replaceToFalse()
                }
                // ColorOS 17 的独立应用音量开关已迁入 VolumeFeatureOption。
                firstMethodOrNull { name = "getSFlavorOneMultiMediaDevice" }?.hook {
                    if (prefs(ModulePrefs).getBoolean("enable_app_specific_media_volume", false)) {
                        replaceToTrue()
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookQSFeatureOption : Hooker {
        override fun onHook() {
            //自定义控制中心音量条模式
            val volumnSeekbarMode =
                prefs(ModulePrefs).getString("set_control_center_volume_seekbar_mode", "0")

            //Source QSFeatureOption
            "com.oplusos.systemui.common.feature.QSFeatureOption".toClass().resolve().optional(true).apply {
                firstMethodOrNull { name = "isSupportVolumeSeekBar" }?.hook {
                    when (volumnSeekbarMode) {
                        "1" -> replaceToTrue()
                        "2" -> replaceToFalse()
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookFeatureFlags : Hooker {
        override fun onHook() {
            //Source FeatureFlagsClassicRelease
            "com.android.systemui.flags.FeatureFlagsClassicRelease".toClass().resolve().apply {
                method {
                    name = "isEnabled"
                    parameterCount = 1
                    returnType = Boolean::class
                }.hookAll {
                    after {
                        val flag = args().first().any() ?: return@after
                        val key = flag.asResolver().firstField { name = "name" }.get<String>()
                        val namespace =
                            flag.asResolver().firstField { name = "namespace" }.get<String>()
//                        YLog.debug("${method.name}(${method.parameterTypes.firstOrNull()?.simpleName}) -> $key | $namespace : $result")
//                        when (key) {
//                            "charging_ripple" -> resultTrue()
//                        }
                    }
                }
            }
        }
    }
}