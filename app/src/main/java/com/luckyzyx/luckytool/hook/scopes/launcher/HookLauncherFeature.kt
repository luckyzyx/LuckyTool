package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

class HookLauncherFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        loadHooker(HookFeatureOption(dexKitBridge))
        loadHooker(HookLauncherSettings(dexKitBridge))
        if (osCode >= 34) loadHooker(HookAppFeature(dexKitBridge))
    }

    @Obfuscate
    class HookAppFeature(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val disableAutoSwitch =
                preferences(ModulePrefs).getBoolean("disable_auto_switch_last_task", false)
            if (!disableAutoSwitch) return

            //Source AppFeatureUtils (all versions)
            //Legacy gate, new Launcher still consults it in StackPagedViewEx,
            //getToRecentsFocusPage, AppToOverviewAnimationProvider and
            //StandardInterruptHelper.canFocusToNextPage
            val appFeatureUtils =
                "com.android.common.util.AppFeatureUtils".toClassOrNull()?.resolve()
            if (appFeatureUtils != null) {
                appFeatureUtils.apply {
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
            } else {
                // 17.0.1 起该类被混淆进默认包、判定方法也改名成单字母（探针：反编译后是 wy.d1），
                // 认不出名字了就按形状取：它是全工程唯一「boolean 入参 + 返回 boolean + 调
                // DisplayController.hasNavigationBar()」的方法，调用方覆盖 StackPagedViewEx、
                // getToRecentsFocusPage、AppToOverviewAnimationProvider（判定失败时打
                // "getFocusToNextPageTarget fail, no support"）与卡片优先策略
                dexKitBridge.findMethod {
                    matcher {
                        paramCount(1)
                        paramTypes(classOf<Boolean>())
                        returnType(classOf<Boolean>())
                        addInvoke {
                            name("hasNavigationBar")
                            declaredClass("com.android.launcher3.util.DisplayController")
                        }
                    }
                }.apply {
                    checkDataList(
                        "find method AppFeatureUtils.isSupportAutoFocusToNextPageInOverviewState"
                    )
                }.singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
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
    class HookFeatureOption(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val appUpdateDot =
                preferences(ModulePrefs).getBoolean("enable_display_app_update_dot", false)
            val disableDockerMax =
                preferences(ModulePrefs).getBoolean("remove_docker_max_number_limit", false)
            val allowWidget =
                preferences(ModulePrefs).getBoolean("remove_widgets_add_request_whitelist", false)

            //Source FeatureOption
            // 17.0.1 起这个功能总闸连类名带成员名一起混淆进默认包（探针：反编译后是 rp3），
            // 类还在的版本直接用原名挂钩，类不在了再按调用点特征定位
            val featureOption = "com.android.common.config.FeatureOption".toClassOrNull()?.resolve()
            if (featureOption != null) {
                featureOption.apply {
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
                    // 组件添加请求的白名单总闸：宿主把它判定为「不支持白名单控制」时
                    // 才放行所有 App 的添加请求（isAllowedAddWidget 里 !这个值 就直接 return true），
                    // 所以这里要给 false，而不是 true
                    if (allowWidget) {
                        firstMethodOrNull { name = "isSupportWhiteListControl" }?.hook {
                            intercept(false)
                        }
                    }
                }
                return
            }

            // 混淆版只有 Dock 最大数量还能救：它是「无参 + 返回 boolean」且用 !isTablet() 判定的那个方法。
            // 平板判定本身也被混淆，改用调用点当特征——热座参数计算类 com.android.common.util.c
            // 里 hotseatNormalItemMaxCount 取 5 用的就是这次调用，该类对该总闸的其余引用都是字段。
            // 应用更新红点没有可挂钩的入口：isSupportAppUpdateDotSwitch 字段与 isSupportAppUpdateDot
            // 方法在 17.0.0、17.0.1 都不存在，红点由宿主自身开关控制。
            // 小组件白名单的判定在新版已搬进 SecurityPermission，见 RemoveWidgetsAddRequestWhitelist
            if (!disableDockerMax) return
            // 总闸类靠它自己的调试输出（FeatureOption 全量开关表）定位，字符串只出现在该类上
            val featureOptionClass = dexKitBridge.findClass {
                matcher {
                    usingStrings("Features:\n\tisLayout4x4: ")
                }
            }.checkDataList("find class FeatureOption").singleOrNull()?.name ?: return
            dexKitBridge.findMethod {
                matcher {
                    declaredClass(featureOptionClass)
                    paramCount(0)
                    returnType(classOf<Boolean>())
                    callerMethods { add { declaredClass("com.android.common.util.c") } }
                }
            }.apply {
                checkDataList("find method FeatureOption isDockerMax5")
            }.singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
                intercept(false)
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

                singleOrNull()?.name?.toClass()?.resolve()?.apply {
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
