package com.luckyzyx.luckytool.hook.scopes.settings

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookIris5Controller : YukiBaseHooker() {
    override fun onHook() {
        val isVideoFrameInsertion = true
        //preferences(ModulePrefs).getBoolean("video_display_enhancement_support_2K120", false)
        val isVideoDisplayEnhancement = true
        //preferences(ModulePrefs).getBoolean("video_super_resolution_support_2K120", false)
        val isVideoSuperResolution = true

        //Source Iris5MotionFluencySwitchController
        "com.oplus.settings.feature.display.controller.Iris5MotionFluencySwitchController".toClass()
            .resolve().apply {
                firstMethod { name = "is2kReject" }.hook {
                    if (isVideoFrameInsertion) {
                        intercept(false)
                    }
                }
                firstMethod { name = "isSupport120With2K" }.hook {
                    if (isVideoFrameInsertion) {
                        intercept(true)
                    }
                }
            }
        //Source Iris5MotionFluencyController
        "com.oplus.settings.feature.display.controller.Iris5MotionFluencyController".toClass()
            .resolve().apply {
                firstMethod { name = "is2kReject" }.hook {
                    if (isVideoFrameInsertion) {
                        intercept(false)
                    }
                }
                firstMethod { name = "isSupport120With2K" }.hook {
                    if (isVideoFrameInsertion) {
                        intercept(true)
                    }
                }
            }
        //Source Iris5VideoDisplayEnhancementController
        "com.oplus.settings.feature.display.controller.Iris5VideoDisplayEnhancementController".toClass()
            .resolve().apply {
                firstMethod { name = "is2kReject" }.hook {
                    if (isVideoDisplayEnhancement) {
                        intercept(false)
                    }
                }
                firstMethod { name = "isSupport120With2K" }.hook {
                    if (isVideoDisplayEnhancement) {
                        intercept(true)
                    }
                }
            }
        //Source Iris5VideoSuperResolutionController
        "com.oplus.settings.feature.display.controller.Iris5VideoSuperResolutionController".toClass()
            .resolve().apply {
                firstMethod { name = "is2kReject" }.hook {
                    if (isVideoSuperResolution) {
                        intercept(false)
                    }
                }
                firstMethod { name = "isSupport120With2K" }.hook {
                    if (isVideoSuperResolution) {
                        intercept(true)
                    }
                }
            }
    }
}