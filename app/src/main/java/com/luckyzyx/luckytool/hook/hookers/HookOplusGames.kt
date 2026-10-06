package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureConfig
import com.luckyzyx.luckytool.hook.globals.HookGlobalFeatureProvider
import com.luckyzyx.luckytool.hook.scopes.games.CloudConditionFeature
import com.luckyzyx.luckytool.hook.scopes.games.CompetitionModeSound
import com.luckyzyx.luckytool.hook.scopes.games.CustomBarrageNotificationWhitelist
import com.luckyzyx.luckytool.hook.scopes.games.CustomMediaPlayerSupport
import com.luckyzyx.luckytool.hook.scopes.games.EnableDeveloperPage
import com.luckyzyx.luckytool.hook.scopes.games.EnableGameRunInBackground
import com.luckyzyx.luckytool.hook.scopes.games.EnableSupportCompetitionMode
import com.luckyzyx.luckytool.hook.scopes.games.EnableXModeFeature
import com.luckyzyx.luckytool.hook.scopes.games.RemoveGameAssistantTemperatureDetection
import com.luckyzyx.luckytool.hook.scopes.games.RemoveRootCheck
import com.luckyzyx.luckytool.hook.scopes.games.RemoveSomeVipLimit
import com.luckyzyx.luckytool.hook.scopes.games.RemoveStartupAnimation
import com.luckyzyx.luckytool.hook.scopes.games.RemoveToolRecommendationCard
import com.luckyzyx.luckytool.hook.scopes.games.RemoveWelfarePage
import com.luckyzyx.luckytool.hook.utils.getAppVerInfo
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookOplusGames : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        val appVer = preferences(ModulePrefs).getAppVerInfo(packageName)
        //非ColorOS官方安装器直接返回
        if (appVer?.versionCommit == "0") return
        val isNew = (appVer?.versionName?.substringBefore(".")?.toIntOrNull() ?: 10) >= 10

        loadHooker(HookGlobalFeatureConfig)

        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            loadHooker(HookGlobalFeatureProvider(dexKitBridge))
            //HookCloudConditionFeature
            if (!isNew) loadHooker(CloudConditionFeature(appVer, dexKitBridge))
            //游戏滤镜-->Root检测
            if (preferences(ModulePrefs).getBoolean("remove_root_check", false)) {
                loadHooker(RemoveRootCheck(dexKitBridge))
            }
            //移除启动动画
            if (preferences(ModulePrefs).getBoolean("remove_startup_animation", false)) {
                loadHooker(RemoveStartupAnimation(dexKitBridge))
            }
            //启用赛事支持模式
            if (preferences(ModulePrefs).getBoolean("enable_support_competition_mode", false)) {
                loadHooker(EnableSupportCompetitionMode(dexKitBridge))
            }
            //移除赛事模式音效
            if (preferences(ModulePrefs).getBoolean("remove_competition_mode_sound", false)) {
                loadHooker(CompetitionModeSound(dexKitBridge))
            }
            //移除游戏助手福利页面
            if (!isNew && preferences(ModulePrefs).getBoolean("remove_welfare_page", false)) {
                loadHooker(RemoveWelfarePage(dexKitBridge))
            }
            //启用游戏助手后台挂机
            if (preferences(ModulePrefs).getBoolean("enable_game_run_in_background", false)) {
                if (osCode >= 27) loadHooker(EnableGameRunInBackground(dexKitBridge))
            }
        }

        //自定义媒体播放器支持
        loadHooker(CustomMediaPlayerSupport)

        //启用开发者选项
        if (preferences(ModulePrefs).getBoolean("enable_developer_page", false)) {
            loadHooker(EnableDeveloperPage)
        }
        //启用X模式
        if (preferences(ModulePrefs).getBoolean("enable_x_mode_feature", false)) {
            loadHooker(EnableXModeFeature)
        }
        //移除部分VIP限制
        if (preferences(ModulePrefs).getBoolean("remove_some_vip_limit", false)) {
            loadHooker(RemoveSomeVipLimit)
        }
        //移除游戏助手温度检测
        if (preferences(ModulePrefs).getBoolean("remove_game_assistant_temperature_detection", false)) {
            loadHooker(RemoveGameAssistantTemperatureDetection)
        }
        //自定义弹幕通知白名单
        loadHooker(CustomBarrageNotificationWhitelist)
        //移除游戏助手工具推荐卡片
        if (!isNew && preferences(ModulePrefs).getBoolean("remove_tool_recommendation_card")) {
            if ((appVer?.versionCode ?: 0) >= 90000000) loadHooker(RemoveToolRecommendationCard)
        }

        //Source GameEyeProtectionFeature cosa_version_not_support
//        "business.module.gameeyeprotection.GameEyeProtectionFeature".toClass().apply {
//            method { name = "I";param(StringClass);returnType = BooleanType }.hook {
//                after {
//                    YLog.debug("isSupportGameEyeProtect -> $result")
//                    result = true
//                }
//            }
//        }

        //res/layout/layout_perf_cpu_setting_panel_land.xml
        //res/layout/layout_perf_cpu_setting_panel_child.xml
        //<string name="cpu_control_panel">CPU 性能面板</string>
        //<string name="cpu_control_panel_extension">极客性能面板</string>
        //<string name="cpu_switch_tlitle_extension">CPU调频</string> cpu_switch_title cpu_switch

        //oplus.software.performance_setting_extension
        //Source GameCpuSettingViewModel -> isSupportCpuFreqCtrlPanel
//            "business.module.cpusetting.GameCpuSettingViewModel".toClass().apply {
//                method { param(StringClass);returnType = BooleanType }.hook {
//                    after {
//                        val key = firstArg().get<String>() ?: ""
//                        YLog.debug("isSupportCpuFreqCtrlPanel ($key) -> $result")
//                        result = true
//                    }
//                }
//            }

//            val clazz = "com.coloros.gamespaceui.config.ServerConfigManager"
//                .toClass(initialize = true).classes[0].simpleName
//            findClass("com.coloros.gamespaceui.config.ServerConfigManager\$$clazz").hook {
//                injectMember {
//                    method { emptyParam();returnType = MapClass }
//                    afterHook {
//                        val res = result<Map<String, String>>()
//                        loggerD(msg = res?.keys.toString())
//                        res?.forEach { (key, value) ->
//                            if (value.contains("com.tencent.tmgp.sgame")) {
//                                loggerD(msg = "com.tencent.tmgp.sgame -> $key")
//                            }
//                        }
//                    }
//                }
//            }

        //game_color_plus_config_map

        //Search GamePerfModeModel -> perf_touch_response_extreme_rb
        //<string name="perf_touch_response_extreme_response">极致触控</string>
        //mPerfTouchResponseExtreme click -> setTouchResponse

        //闪电启动
        //business.secondarypanel.view.GameFastStartFloatView
//            <string name="fast_start_title_tips">OSWAP 闪电启动中</string>
//            <string name="fast_start_title_tips_no_oswap">闪电启动中</string>
//            <string name="fast_start_title_success">闪电启动成功，游戏快人一步</string>
//            <string name="fast_start_success_save_time_tip">闪电启动成功，本次节省 %d s</string>

        //GPA极限稳帧
        //com.oplus.cosa.gpalibrary.core.GpaCore

    }
}