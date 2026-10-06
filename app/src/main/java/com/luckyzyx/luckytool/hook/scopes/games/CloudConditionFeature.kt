package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.data.AppVerInfo
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class CloudConditionFeature(
    private val appVer: AppVerInfo?, val dexKitBridge: DexKitBridge
) : YukiBaseHooker() {
    override fun onHook() {
        val versionCode = appVer?.versionCode?.takeIf { it > 80130000 } ?: 0

        loadHooker(HookOplusFeature)
        loadHooker(HookCloudCondition)
        if (versionCode > 80130000) loadHooker(HookCloudApiImpl(dexKitBridge))
    }

    @Obfuscate
    private object HookOplusFeature : YukiBaseHooker() {
        override fun onHook() {
            //Source GpuSettingHelper
            val gpuControl = preferences(ModulePrefs).getBoolean("enable_adreno_gpu_controller", false)
            //Source GameFrameInsertInfo
            val pickleFeature =
                preferences(ModulePrefs).getBoolean("enable_increase_fps_limit_feature", false)
            val fpsFeature = preferences(ModulePrefs).getBoolean("enable_increase_fps_feature", false)
            val powerFeature = preferences(ModulePrefs).getBoolean("enable_optimise_power_feature", false)
            //Source Feats
            val gtMode = preferences(ModulePrefs).getBoolean("enable_gt_mode_feature", false)
            //Source SuperResolutionHelper
            val superResolution =
                preferences(ModulePrefs).getBoolean("enable_super_resolution_feature", false)
            //Source CoolingBackClipHelper
            val xMode = preferences(ModulePrefs).getBoolean("enable_x_mode_feature", false)

            val companion = "com.oplus.addon.OplusFeatureHelper\$Companion".toClassOrNull()
            if (companion == null) {
                //Source OplusFeatureHelper
                "com.oplus.addon.OplusFeatureHelper".toClass().resolve().apply {
                    firstMethod {
                        parameters(String::class, Boolean::class)
                        returnType = Boolean::class
                    }.hook {
                        after {
                            when (firstArg().get<String>() ?: "") {
                                //feature -> isSupportFrameInsert
                                "oplus.software.display.game.memc_enable" -> if (pickleFeature || fpsFeature || powerFeature) result = true
                                //插帧pickleFeature -> isSupportUniqueFrameInsert
                                "oplus.software.display.game.memc_increase_fps_limit_mode" -> if (pickleFeature) result = true
                                //提升帧率feature -> isSupportIncreaseFps
                                "oplus.software.display.game.memc_increase_fps_mode" -> if (fpsFeature) result = true
                                //优化功耗feature -> isSupportOptimisePower
                                "oplus.software.display.game.memc_optimise_power_mode" -> if (powerFeature) result = true
                                //GPU控制器 -> isSupportGpuControl
                                "oplus.gpu.controlpanel.support" -> if (gpuControl) result = true
                                //GT模式 -> isSupportGtMode
                                "oplus.software.support.gt.mode" -> if (gtMode) result = true
                                //超级分辨率 -> issupportSupperResolution
                                "oplus.software.display.game.sr_enable" -> if (superResolution) result = true
                                //全超分辨率 -> isSupportFullSupperResolution
                                "oplus.software.display.game.sr.fully_enable" -> if (superResolution) result = true
                                //X模式 -> isSupportBackClipFull
                                "oplus.software.general.cooling.back.clip.enable" -> if (xMode) result = true
//                            //isSupportBackClipFull
//                            "oplus.software.general.cooling.back.clip.enable" -> {
//                                loggerD(msg = "isSupportBackClipFull")
//                                result = false
//                            }
//                            //极客性能面板 -> isSupportCpuSettingExtension
//                            "oplus.software.performance_setting_extension" -> result = true

                            }
                        }
                    }
                }
                return
            }
            //Source OplusFeatureHelper
            "com.oplus.addon.OplusFeatureHelper\$Companion".toClass().resolve().apply {
                firstMethod {
                    parameters(String::class, Boolean::class)
                    returnType = Boolean::class
                }.hook {
                    after {
                        when (firstArg().get<String>() ?: "") {
                            //feature -> isSupportFrameInsert
                            "oplus.software.display.game.memc_enable" -> if (pickleFeature || fpsFeature || powerFeature) result = true
                            //插帧pickleFeature -> isSupportUniqueFrameInsert
                            "oplus.software.display.game.memc_increase_fps_limit_mode" -> if (pickleFeature) result = true
                            //提升帧率feature -> isSupportIncreaseFps
                            "oplus.software.display.game.memc_increase_fps_mode" -> if (fpsFeature) result = true
                            //优化功耗feature -> isSupportOptimisePower
                            "oplus.software.display.game.memc_optimise_power_mode" -> if (powerFeature) result = true
                            //GPU控制器 -> isSupportGpuControl
                            "oplus.gpu.controlpanel.support" -> if (gpuControl) result = true
                            //GT模式 -> isSupportGtMode
                            "oplus.software.support.gt.mode" -> if (gtMode) result = true
                            //超级分辨率 -> issupportSupperResolution
                            "oplus.software.display.game.sr_enable" -> if (superResolution) result = true
                            //全超分辨率 -> isSupportFullSupperResolution
                            "oplus.software.display.game.sr.fully_enable" -> if (superResolution) result = true
                            //X模式 -> isSupportBackClipFull
                            "oplus.software.general.cooling.back.clip.enable" -> if (xMode) result = true
//                            //isSupportBackClipFull
//                            "oplus.software.general.cooling.back.clip.enable" -> {
//                                loggerD(msg = "isSupportBackClipFull")
//                                result = false
//                            }
//                            //极客性能面板 -> isSupportCpuSettingExtension
//                            "oplus.software.performance_setting_extension" -> result = true

                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    private object HookCloudCondition : YukiBaseHooker() {
        override fun onHook() {
            //Source GpuSettingHelper
            val gpuControl = preferences(ModulePrefs).getBoolean("enable_adreno_gpu_controller", false)
            //Source GameFrameInsertInfo
            val pickleFeature =
                preferences(ModulePrefs).getBoolean("enable_increase_fps_limit_feature", false)
            val fpsFeature = preferences(ModulePrefs).getBoolean("enable_increase_fps_feature", false)
            val powerFeature = preferences(ModulePrefs).getBoolean("enable_optimise_power_feature", false)
            //Source CoolingBackClipHelper
            val xMode = preferences(ModulePrefs).getBoolean("enable_x_mode_feature", false)
            //Source SuperResolutionHelper
            val superResolution =
                preferences(ModulePrefs).getBoolean("enable_super_resolution_feature", false)
            //Source CloudConditionUtil
            val oneplusCharacteristic =
                preferences(ModulePrefs).getBoolean("enable_one_plus_characteristic", false)
            //Search magic_voice_config
            val magicVoice =
                preferences(ModulePrefs).getBoolean("remove_game_voice_changer_whitelist", false)
            //Source AIPlayFeature
            val aiPlay = preferences(ModulePrefs).getBoolean("enable_game_ai_play", false)

            //Source CloudConditionUtil
            "com.coloros.gamespaceui.config.cloud.CloudConditionUtil".toClass().resolve().apply {
                firstMethod {
                    parameters(String::class, Map::class, Int::class, Any::class)
                    returnType = Boolean::class
                }.hook {
                    before {
                        when (firstArg().get<String>() ?: "") {
                            //pickle插帧云控 -> cloudFrameInsertEnable
                            "frame_insert" -> if (pickleFeature) result = true
                            //提升帧率云控 -> cloudIncreaseFpsEnable
                            "increase_fps" -> if (fpsFeature) result = true
                            //优化功耗云控 -> cloudOptimisePowerEnable
                            "optimise_power" -> if (powerFeature) result = true
                            //GPU控制器云控 -> isCloudSupportGpuControlPanel
                            "gpu_control_panel" -> if (gpuControl) result = true
                            //X模式 -> isSupportXMode
                            "cool_back_clip_blacklist" -> if (xMode) result = true
                            //OnePlus特性
                            "one_plus_characteristic" -> if (oneplusCharacteristic) result = true
                            //游戏滤镜
//                            "game_filter_config" -> result = true
                            //AI辅助
                            "game_ai_play_key" -> if (aiPlay) result = true
                        }
                    }
                }
                firstMethod {
                    parameters(String::class, Map::class)
                    returnType = Boolean::class
                }.hook {
                    before {
                        when (firstArg().get<String>() ?: "") {
                            //超级分辨率云控 -> cloudSRSupport
                            "super_resolution_config" -> if (superResolution) result = true
                        }
                    }
                }
                firstMethod {
                    parameters { it[0] == classOf<String>() && it[1] == classOf<Map<*, *>>() }
                    parameterCount = 3
                }.hook {
                    after {
                        when (firstArg().get<String>() ?: "") {
                            //游戏变声
                            "magic_voice_config" -> if (magicVoice) result = true
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    private class HookCloudApiImpl(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            //Source GpuSettingHelper
            val gpuControl = preferences(ModulePrefs).getBoolean("enable_adreno_gpu_controller", false)
            //Source SuperResolutionHelper
            val superResolution =
                preferences(ModulePrefs).getBoolean("enable_super_resolution_feature", false)
            //Source CloudConditionUtil
            val oneplusCharacteristic =
                preferences(ModulePrefs).getBoolean("enable_one_plus_characteristic", false)

            //Source CloudApiImpl
            dexKitBridge.findClass {
                matcher {
                    usingStrings("cloudKey", "defaultDate", "spFileName")
                    methods {
                        add { paramCount(0);returnType(classOf<List<*>>()) }
                        add { paramCount(1);returnType(classOf<List<*>>()) }
                        add { paramCount(2);returnType(classOf<Boolean>()) }
                    }
                }
            }.apply {
                checkDataList("HookCloudApiImpl")
                single().name.toClass().resolve().apply {
                    firstMethod {
                        name = "isFunctionEnabledFromCloud"
                        parameterCount = 2
                    }.hook {
                        before {
                            when (firstArg().get<String>() ?: "") {
                                //GPU控制器云控 -> isCloudSupportGpuControlPanel
                                "gpu_control_panel" -> if (gpuControl) result = true
                                //OnePlus特性
                                "one_plus_characteristic" -> if (oneplusCharacteristic) result = true
                                //超级分辨率云控 -> cloudSRSupport
                                "super_resolution_config" -> if (superResolution) result = true
                                //全超分辨率云控 -> isSupportFullSupperResolution
                                "super_resolution_config_full" -> if (superResolution) result = true
                                //游戏滤镜
                                //"game_filter_config" -> result = true
                            }
                        }
                    }
                }
            }
        }
    }
}