package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

class HookLauncherFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        loadHooker(HookFeatureOption)
        loadHooker(HookLauncherSettings(dexKitBridge))
        if (osCode >= 34) loadHooker(HookAppFeature)
    }

    @Obfuscate
    object HookAppFeature : YukiBaseHooker() {
        override fun onHook() {
            val disableAutoSwitch =
                preferences(ModulePrefs).getBoolean("disable_auto_switch_last_task", false)
            if (!disableAutoSwitch) return

            //Source AppFeatureUtils (all versions)
            //Legacy gate, new Launcher still consults it in StackPagedViewEx,
            //getToRecentsFocusPage, AppToOverviewAnimationProvider and
            //StandardInterruptHelper.canFocusToNextPage
            "com.android.common.util.AppFeatureUtils".toClass().resolve().apply {
                //Source OplusGridRecentsConfig isEnable
                firstMethodOrNull {
                    name = "isSupportAutoFocusToNextPageInOverviewState"
                    parameterCount = 1
                }?.hook {
                    intercept(false)
                }
                firstMethod {
                    name = "isSupportAutoFocusToNextPageInOverviewState"
                    emptyParameters()
                }.hook {
                    intercept(false)
                }
            }

            //Source RecentInterruptAnimUtilKt (new Launcher only)
            //computeNonInterruptFocusToNextPageTarget dropped the gate and returns
            //runningTaskIndex + 1, which StackRecentsViewDelegate.updateStackLayoutNextPage
            //uses to overwrite the hooked value; -1 falls back to getNextPage()
            "com.oplus.quickstep.utils.RecentInterruptAnimUtilKt".toClassOrNull()?.resolve()
                ?.apply {
                    firstMethodOrNull {
                        name = "computeNonInterruptFocusToNextPageTarget"
                        parameterCount = 1
                    }?.hook {
                        intercept(-1)
                    }
                }

            //Source TileCardFirstInterruptFocusPolicy (new Launcher only, card-first path)
            //resolveFocusPageFallback decides the card-first settle page without the gate;
            //-1 falls back to getNextPage()
            "com.oplus.quickstep.utils.tilecardfirst.policy.TileCardFirstInterruptFocusPolicy"
                .toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull {
                        name = "resolveFocusPageFallback"
                        parameterCount = 2
                    }?.hook {
                        intercept(-1)
                    }
                }
        }
    }

    @Obfuscate
    object HookFeatureOption : YukiBaseHooker() {
        override fun onHook() {
            val appUpdateDot = preferences(ModulePrefs).getBoolean("enable_display_app_update_dot", false)
            val disableDockerMax =
                preferences(ModulePrefs).getBoolean("remove_docker_max_number_limit", false)
            val allowWidget =
                preferences(ModulePrefs).getBoolean("remove_widgets_add_request_whitelist", false)

            //Source FeatureOption
            "com.android.common.config.FeatureOption".toClass().resolve().apply {
                firstMethod { name = "initFeature" }.hook {
                    after {
                        if (appUpdateDot) {
                            firstFieldOrNull { name = "isSupportAppUpdateDotSwitch" }?.set(true)
                        }
                    }
                }
                if (disableDockerMax) {
                    firstMethodOrNull { name = "isDockerMax5" }?.hook {
                        intercept(false)
                    }
                }
                if (allowWidget) {
                    firstMethodOrNull { name = "isSupportWhiteListControl" }?.hook {
                        intercept(true)
                    }
                }
            }
        }
    }

    @Obfuscate
    class HookLauncherSettings(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val appUpdateDot = preferences(ModulePrefs).getBoolean("enable_display_app_update_dot", false)

            //Source LauncherSettingsUtils
            dexKitBridge.findClass {
                matcher {
                    usingStrings("content://com.android.launcher.settings", "LauncherSettingsUtils")
                }
            }.apply {
                checkDataList("find clazz LauncherSettingsUtils")

                single().name.toClass().resolve().apply {
                    if (appUpdateDot) {
                        firstMethodOrNull { name = "isSupportAppUpdateDot" }?.hook {
                            intercept(true)
                        }
                    }
                }
            }
        }
    }
}
