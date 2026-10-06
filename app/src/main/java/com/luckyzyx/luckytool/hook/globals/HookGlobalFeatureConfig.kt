package com.luckyzyx.luckytool.hook.globals

import android.provider.Settings
import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A12
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.A15
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalFeatureConfig : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        val list = ArrayMap<String, Boolean>().apply {
            //Android
//            put("oplus.software.audio.alert_slider", false)

            //Source OplusBgSceneManager setMarketRegion / isChinaMode -> registerGmsRestrictObserver
//            if (preferences(ModulePrefs).getBoolean("remove_gms_usage_restrictions", false)) {
//                put("oplus.software.hans_restriction", false)
//            }

            //Source Android FlexibleWindowUtils isSupportMultiMode 支持多窗口
            if (preferences(ModulePrefs).getBoolean("force_enable_multi_window_mode", false)) {
                put("oplus.software.support.zoom.multi_mode", true)
            }

            //Source Android PhoneWindowManagerExtImpl HomeKeyLongPressRunnable
            val disableSpeechAssist = preferences(ModulePrefs).getBoolean(
                "disable_long_press_home_key_start_speech_asssist", false
            )
            if (disableSpeechAssist) {
                put("oplus.software.speech_assist_for_breeno", false)
            }

            //Source Android OplusMultiAppDataManager getMaxCreatedNum 分身创建数量限制
            if (preferences(ModulePrefs).getBoolean(
                    "remove_multi_app_created_num_limit_for_users",
                    false
                )
            ) {
                put("oplus.software.multiapp_max_open_number_limited", false)
            }

            //Source Android OemTelephonyUtils isHfpCommSharedSupported IPhone互联
            if (preferences(ModulePrefs).getBoolean("force_enable_iphone_shared_support", false)) {
                put("oplus.software.radio.hfp_comm_shared_support", true)
                put("oplus.software.radio.hfp_sms_shared_not_support", false)
                put("oplus.software.radio.hfp_call_shared_not_support", false)
            }

            //Source SystemUI 强制启用高斯模糊
            if (preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)) {
                put("oplus.software.display.osie_aisdr2hdr_support", false) //C12
            }

            //Source SystemUI 启用超级音量模式
            if (SDK >= A13 &&
                preferences(ModulePrefs).getBoolean("enable_super_volume_mode", false)
            ) {
                put("oplus.software.audio.super_volume", true)
                put("oplus.software.audio.super_volume_3x", true)
            }
            //Source SystemUI 启用通话超级音量模式
            if (osCode >= 27 &&
                preferences(ModulePrefs).getBoolean("enable_super_volume_mode_for_calls", false)
            ) {
                put("oplus.software.audio.super_volume_call_earpiece", true)
                put("oplus.software.audio.super_volume_call_earpiece_disable", false)
            }
            //Source SystemUI FlavorOneFeatureOption 启用应用专属媒体音量
            if (osCode >= 27 &&
                preferences(ModulePrefs).getBoolean("enable_app_specific_media_volume", false)
            ) {
                put("oplus.software.multi_app.volume.adjust.support", true)
            }
            //Source SystemUI / Launcher OplusShellStartingWindowUtils 全应用遮罩
            if (SDK >= A13 && preferences(ModulePrefs).getBoolean(
                    "disable_preload_splash",
                    false
                )
            ) {
                put("oplus.software.wms.disable_preload_splash", true)
            }

            //Source Settings 启用RGB色温球 ColorModeFragment
            if (preferences(ModulePrefs).getBoolean(
                    "enable_screen_color_temperature_rgb_ball",
                    false
                )
            ) {
                val uri = Settings.System.getUriFor("oplus_settings_switch_color_mode")
                if (osCode >= 27 && uri != null) {
                    put("oplus.software.display.rgb_ball_support", true)
                }
            }
            //Source Settings 启用RGB调色板 ColorModeFragment
            if (preferences(ModulePrefs).getBoolean(
                    "enable_screen_color_temperature_rgb_space",
                    false
                )
            ) {
                val uri = Settings.System.getUriFor("color_space_adjustment")
                if (osCode >= 30 && uri != null) {
                    put("oplus.software.display.color_space_support", true)
                }
            }

            //Source Settings 启用游戏专属内存 GameBounceUtils
            if (preferences(ModulePrefs).getBoolean("enable_dedicated_ram_for_games", false)) {
                put("oplus.software.game_bounce_support", true)
            }
            //Source Settings ScreenResolutionFragment isResolutionAutoDisableSupport 启用智能切换屏幕分辨率
            if (preferences(ModulePrefs).getBoolean(
                    "enable_smart_switching_screen_resolutions",
                    false
                )
            ) {
                put("oplus.software.display.resolution_switch_disableauto_support", false)
            }

            //Source Settings Iris5SettingsFragment iris5_motion_fluency_optimization_switch 启用视频动态插帧
            if (preferences(ModulePrefs).getBoolean("enable_video_memc_frame_insertion", false)
            ) {
                put("oplus.software.display.pixelworks_enable", true)
                put("oplus.software.display.iris_enable", true)
                put("oplus.software.display.memc_enable", true)
                put("oplus.software.display.game.memc_enable", true)
            }
            //Source Settings ReduceWhitePointPreferenceController 降低白点值
            if (preferences(ModulePrefs).getBoolean(
                    "force_enable_reduce_white_point_value",
                    false
                )
            ) {
                put("oplus.software.display.reduce_white_point", true)
            }
            //Source Settings NavBarSettingsValueUtil 启用上滑导航手势
            if (preferences(ModulePrefs).getBoolean("enable_swipe_up_navigation_gesture", false)) {
                put("com.android.systemui.keep_swipup_gestures", true)
            }

            //Source Lanucher 无限屏
//            put("oplus.software.pocketstudio.support", true)
//            put("oplus.software.display.google_extension_embedding", true)
//            put("oplus.software.display.google_extension_layout", true)
//            put("oplus.software.display.oplus_activity_embedding", true)

            //Source Launcher 钉流体云
            if (preferences(ModulePrefs).getBoolean("enable_recent_task_pin_capsule", false)) {
                if (osCode >= 37) put("oplus.software.systemui.pin_task", true)
            }

            //Source Mms 移除验证码悬浮窗 FeatureOption.java / com.oplus.common -> C12
            if (preferences(ModulePrefs).getBoolean(
                    "remove_verification_code_floating_window",
                    false
                )
            ) {
                put("oplus.software.inputmethod.verify_code_enable", false)
            }

            //Source Gestures 启用隔空手势
            if (preferences(ModulePrefs).getBoolean("force_enable_aon_gestures", false)) {
                put("oplus.software.aon_enable", true)
                put("oplus.software.aon_gestureui_enable", true)
            }
            //Source Gestures 启用音量键控制手电筒
            if (preferences(ModulePrefs).getBoolean(
                    "enable_volume_key_control_flashlight",
                    false
                )
            ) {
                put("oplus.software.powerkey_disbale_turnoff_torch", false)
                put("oplus.software.key_quickoperate_torch", true)
            }

            //Source PermissionController 解锁默认桌面限制
            if (preferences(ModulePrefs).getBoolean("unlock_default_desktop_limit", false)) {
                put("oplus.software.defaultapp.remove_force_launcher", true)
            }

            //Source MultiApp AppFeatureUtil isRlmPhone 全应用分身
            when (preferences(ModulePrefs).getString("set_multi_app_support_mode", "0")) {
                "1" -> put("oplus.software.multiapp_support_rlm", false)
                "2" -> put("oplus.software.multiapp_support_rlm", true)
            }

            //Source PhoneManager VoiceCallNCVisibilityProvider
            if (SDK >= A14 && preferences(ModulePrefs).getBoolean("enable_clear_voice", false)) {
                put("oplus.hardware.audio.voice_isolation_support", true)
            }

            //Source SystemUI StatusBarFeatureOption isSupportPrivacyCallMode 通话隐私保护
            //Source Phone OplusFeatureOption 通话隐私保护
            if (SDK >= A14 && preferences(ModulePrefs).getBoolean(
                    "enable_sound_sealed_call",
                    false
                )
            ) {
                put("oplus.hardware.audio.dipole_speaker_support", true)
            }

            //Source SmartSidebar EdgePanelFeatureOption 强制启用浮标自动隐藏
            if (SDK == A12 &&
                preferences(ModulePrefs).getBoolean("force_enable_buoy_automatically_hides", false)
            ) {
                put("oplus.software.smart_sidebar", true)
            }

            //Source SettingsProviders SettingsUtils isShowNeverTimeout 永不息屏 / 一律不 (24H)
            if (preferences(ModulePrefs).getBoolean("enable_show_never_timeout", false)) {
                if (SDK < A15) put("oplus.software.screen_off_never_support", true)
            }

            //Source Settings IncreaseBrightnessRangePreferenceController 增大亮度范围
            if (preferences(ModulePrefs).getBoolean("enable_extra_brightness", false)) {
                if (osCode >= 30) put("oplus.software.display.sec_max_brightness_rm", true)
            }

            //Source Settings GameSpeedupController 启用游戏加速
            if (preferences(ModulePrefs).getBoolean("enable_game_acceleration", false)) {
                if (osCode >= 30) put("oplus.software.game.cold.start.speedup.enable", true)
            }

            //Source Settings GameArchitecturePreferenceController 风驰游戏内核
            if (preferences(ModulePrefs).getBoolean("enable_game_architecture_display", false)) {
                if (osCode >= 34) put("oplus.software.game.hummingbird", true)
            }

            //Source NotificationManager FeatureOption isSupportsStealthMode VIP模式
            //Source SafeCenter SettingsInsertProvider VIP模式
//            if (SDK >= A14 && preferences(ModulePrefs).getBoolean("enable_vip_mode", false)) {
//                put("oplus.software.stealth_security_mode", true)
//            }

            //Source EyeProtect PaperTexturePreference 纸质纹理调节
            if (preferences(ModulePrefs).getBoolean(
                    "enable_eyeprotect_paper_texture_support",
                    false
                )
            ) {
                if (osCode >= 33) {
                    put(
                        "oplus.software.display.smart_color_temperature_rhythm_health_support",
                        true
                    )
                    put("oplus.software.display.eyeprotect_paper_texture_support", true)
                }
            }

            //Source EyeProtect 全亮度低频闪
//            put("oplus.hardware.display.full_brightness_DC", true)

            //Source ExSystemService OplusBackgroundStreamController
            if (preferences(ModulePrefs).getBoolean("enable_run_in_background", false)) {
                if (osCode >= 27) put("oplus.software.background_stream_tileservice_enabled", true)
            }
        }
        loadHooker(HookFeatureConfigManager(list))
    }
}