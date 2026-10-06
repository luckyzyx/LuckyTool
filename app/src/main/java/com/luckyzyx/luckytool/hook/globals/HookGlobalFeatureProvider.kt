package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookGlobalFeatureProvider(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        val list = ArrayMap<String, Any>().apply {
            //Source SystemUI SystemPromptController updateDeveloperMode 移除状态栏开发者选项警告
            if (preferences(ModulePrefs).getBoolean("remove_statusbar_devmode", false)) {
                put("com.android.systemui.send_developer_mode_notification", false)
            }
            //Source SystemUI AutoBrightnessTile 自动亮度
            when (preferences(ModulePrefs).getString("set_auto_brightness_button_mode", "0")) {
                "1" -> put("com.android.systemui.remove_auto_brightness", false)
                "2" -> put("com.android.systemui.remove_auto_brightness", true)
            }
            //Source SystemUI NotificationFeatureOption 通知重要性
            val notifyImportance = preferences(ModulePrefs).getBoolean(
                "enable_notification_importance_classification", false
            )
            if (notifyImportance) put("com.android.systemui.origin_notification_behavior", true)
            //Source SystemUI OpAssistNavigationDialog 音量条位置
            when (preferences(ModulePrefs).getString("set_volume_bar_display_position", "0")) {
                "1" -> put("com.android.systemui.volume_and_power_key_in_right", false)
                "2" -> put("com.android.systemui.volume_and_power_key_in_right", true)
            }

            //Source SystemUI 全屏充电动画
            when (preferences(ModulePrefs).getString("set_full_screen_charging_animation_mode", "0")) {
                "1" -> put("com.android.systemui.support_fullscreen_charge_anim", true)
                "2" -> put("com.android.systemui.support_fullscreen_charge_anim", false)
            }

            //Source SystemUI 音量对话框背景透明度
            val volumeBlur =
                preferences(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            if (volumeBlur > -1) put("com.android.systemui.disable_volume_blur", false)

            //Source SystemUI 强制启用高斯模糊
            if (preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)) {
                put("com.android.systemui.gauss_blur_disabled", false)
                put("com.android.systemui.pan_view_gauss_blur_disabled", false)
            }

            //Source SystemUI 启用手电筒亮度调节
//            if (preferences(ModulePrefs).getBoolean("disable_flashlight_strength", false)) {
//                put("oplus.camera.disable_flashlight_strength", false)
//            }

            //Source Settings OplusDefaultAutofillPicker -> autofill_password 自动填充密码
            if (preferences(ModulePrefs).getBoolean("disable_cn_special_edition_setting", false)) {
                put("com.android.settings.cn_version", false)
            }
            //Source Settings DisplayTimeOutController displayPreference -> 永不息屏 / 一律不 (24H)
            if (preferences(ModulePrefs).getBoolean("enable_show_never_timeout", false)) {
                put("com.android.settings.show_never_timeout", true)
            }
            //Source Settings com.android.settings.processor_detail / com.android.settings.processor_detail_gen2
            when (preferences(ModulePrefs).getString("set_processor_click_page", "0")) {
                "1" -> {
                    put("com.android.settings.processor_detail", true)
                    put("com.android.settings.processor_detail_gen2", false)
                }

                "2", "3" -> {
                    put("com.android.settings.processor_detail", true)
                    put("com.android.settings.processor_detail_gen2", true)
                }
            }

            //Source Settings 最近任务极致清理
            if (preferences(ModulePrefs).getBoolean("force_display_process_management", false)) {
                put("com.android.settings.ultimate_cleanup", true)
            }
            //Source Settings DeviceInfoUtils 屏幕尺寸显示厘米
            if (preferences(ModulePrefs).getBoolean("screen_physics_size_shown_cm", false)) {
                put("com.android.settings.screen_physics_size_cm", true)
            }
            //Source Settings VerificationDialog isVerificationDialogDisabled 禁用设备管理器验证对话框
            if (preferences(ModulePrefs).getBoolean("disable_device_admin_verification_dialog", false)) {
                put("com.android.settings.verification_dialog.disable", true)
            }
            //Source Settings SmartTouchController isSupportSmartTouch 隔膜触控
            if (preferences(ModulePrefs).getBoolean("enable_touch_membrane_protector_mode", false)) {
                put("feature.super_settings_smart_touch.support", true)
            }
            //Source Settings OtgConnectionOpenedPreferenceController 禁用OTG自动关闭
            if (preferences(ModulePrefs).getBoolean("disable_otg_auto_off", false)) {
                if (osCode >= 30) put("com.android.systemui.otg_auto_close_alarm_disable", true)
            }
            //Source Settings NavBarSettingsValueUtil 启用上滑导航手势
            if (preferences(ModulePrefs).getBoolean("enable_swipe_up_navigation_gesture", false)) {
                put("com.android.systemui.keep_swipup_gestures", true)
            }

            //Source Battery 屏幕省电
            if (preferences(ModulePrefs).getBoolean("open_screen_power_save", false)) {
                put("com.oplus.battery.cabc_level_dynamic_enable", true)
            }
            //Source Battery 电池健康
            if (preferences(ModulePrefs).getBoolean("open_battery_health", false)) {
                put("os.charge.settings.batterysettings.batteryhealth", true)
            }
            //Source Battery 充电至80%
            if (preferences(ModulePrefs).getBoolean("enable_stop_charging_at_80", false)) {
                put("com.oplus.battery.one_key_power_save", true)
            }
            //Source Battery 上次充电亮屏时长
            if (preferences(ModulePrefs).getBoolean("show_phone_usage_screen_time", false)) {
                put("com.oplus.battery.phoneusage.screenon.hide", false)
            }

            //Source Launcher
            if (preferences(ModulePrefs).getBoolean("allow_app_names_display_multiple_lines", false)) {
                put("com.android.launcher.APP_NAME_SHOW_IN_TWO_LINES", true)
//                put("com.android.launcher.two_lines_all_show_textheightdp", -1)
            }

            //Source OplusGame  AI辅助
//            if (preferences(ModulePrefs).getBoolean("enable_game_ai_play", false)) {
//                put("feature.support.game.AI_PLAY", true)
//            }

            //Source Camera UnitSDK 80s美式复古 com.oplus.camera.cola_support
//            put("com.oplus.camera.cola_support", true)

            //Reno12 涟漪
//            put("os.personalization.wallpaper.live.ripple.enable",true)

//            put("com.android.settings.device_rm", true)
//            put("com.oplus.battery.customize_charge_mode", true)

        }
        loadHooker(HookAppFeatureProvider(dexKitBridge, list))
    }
}