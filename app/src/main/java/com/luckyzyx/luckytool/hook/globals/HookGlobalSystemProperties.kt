package com.luckyzyx.luckytool.hook.globals

import android.util.ArrayMap
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookGlobalSystemProperties : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        val list = ArrayMap<String, Any>().apply {
            //Source Android OplusPackageManagerHelper allowInstall32BitApp
            if (preferences(ModulePrefs).getBoolean("force_enable_32_bit_support", false)) {
                put("persist.sys.oplus_support_app32_status", "1")
                put("ro.vendor.oplus.app32_boost_support", "1")
            }

            //Source Android LTPO VRR ADFR
            when (preferences(ModulePrefs).getString("set_ltpo_refresh_rate_mode", "0")) {
                "1" -> {
                    put("persist.oplus.display.vrr", "1")
                    put("persist.oplus.display.vrr.adfr", "2")
                }

                "2" -> {
                    put("persist.oplus.display.vrr", "0")
                    put("persist.oplus.display.vrr.adfr", "0")
                }
            }
            //Source Android OplusPlatformLevelUtils GaussianLevel
            when (preferences(ModulePrefs).getString(
                "customized_gaussian_blur_effect_level",
                "0"
            )) {
                "0" -> put("ro.oplus.gaussianlevel", 0)
                "1" -> put("ro.oplus.gaussianlevel", 1)
                "2" -> put("ro.oplus.gaussianlevel", 2)
                "3" -> put("ro.oplus.gaussianlevel", 3)
            }
            //Source Android OplusFeatureMEMC 启用视频动态插帧
            if (preferences(ModulePrefs).getBoolean("enable_video_memc_frame_insertion", false)
            ) {
                put("ro.oplus.display.memc_video_refreshrate", true)
                put("vendor.display.show_memc_tomast", true)
            }

            //Source SystemUI OplusVolumeDialogImpl 音量条位置
            when (preferences(ModulePrefs).getString("set_volume_bar_display_position", "0")) {
                "1" -> put("persist.oplus.software.audio.right_volume_key", false)
                "2" -> put("persist.oplus.software.audio.right_volume_key", true)
            }
            //Source SystemUI 音量对话框背景透明度
            val volumeBlur =
                preferences(ModulePrefs).getInt("custom_volume_dialog_background_transparency", -1)
            if (volumeBlur > -1) put("ro.oplus.display.disable.volume_blur", false)
            //Source SystemUI 强制启用高斯模糊
            if (preferences(ModulePrefs).getBoolean("force_enable_systemui_blur_feature", false)) {
                put("ro.surface_flinger.supports_background_blur", true)
                put("persist.sys.sf.disable_blurs", false)
            }

            //Source Settings SysFeatureUtils isHoloAudioSupported 启用全息音频
            if (preferences(ModulePrefs).getBoolean("enable_holographic_audio", false)) {
                put("ro.oplus.audio.support.meta_audio", 1)
                if (osCode >= 31) {
                    put("ro.oplus.audio.support.meta_audio_speaker", 1)
                    put("ro.oplus.audio.support.meta_suspend_effect", 1)
                }
            }
            //Source Settings ScreenMinBrightnessController isMinBrightnessSp 启用最低自动亮度
            if (preferences(ModulePrefs).getBoolean("enable_lowest_allowed_brightness", false)) {
                put("ro.oplus.display.brightness.min_settings.rm", "1,2,15,4.0,0")
            }
            //Source Settings DeviceInfoUtils 马里亚纳NPU
            if (preferences(ModulePrefs).getBoolean(
                    "enable_mariana_npu_introduction_page",
                    false
                )
            ) {
                put("ro.vendor.oplus.camera.isSupportExplorer", true)
            }
            //Source Settings DeviceInfoUtils 哈苏影像
            if (preferences(ModulePrefs).getBoolean(
                    "enable_hasselblad_camera_introduction_page", false
                )
            ) {
                put("ro.vendor.oplus.camera.isHasselbladCamera", true)
            }

            //Source Phone 启用5G
            if (preferences(ModulePrefs).getBoolean("force_display_five_g_switch", false)) {
                put("ro.oplus.radio.hide_nr_switch", -1)
            }

            //Source SoundRecorder / AtlasService 三方应用通话录音
            if (preferences(ModulePrefs).getBoolean(
                    "enable_record_calls_on_third_party_apps",
                    false
                )
            ) {
                if (osCode == 30) put("ro.oplus.audio.voip_record_white_app_support", true)
            }

            //Source OTA
            if (preferences(ModulePrefs).getBoolean("disable_dm_verity_verification", false)) {
//            put("persist.sys.assert.panic", "true")
                put("ro.boot.veritymode", "enforcing")
                put("ro.boot.flash.locked", "1")
                put("ro.boot.vbmeta.device_state", "locked")
//            put("persist.vendor.oplus.verify_result", "")
            }

            //Source RkpdApp Settings
            if (preferences(ModulePrefs).getBoolean("remove_gms_usage_restrictions", false)) {
                val host =
                    preferences(ModulePrefs).getString("custom_remote_provisioning_hostname", "")
                if (host.isNotBlank()) put("remote_provisioning.hostname", host)
            }

            //Source FNOsUtils isSupportedFeiNiuNas
            if (preferences(ModulePrefs).getBoolean(
                    "force_enable_feiniu_cloud_nas_option",
                    false
                )
            ) {
                put("ro.oplus.feiniunas.support", true)
            }

            //Source Mcs
            if (packageName == "com.heytap.mcs") {
                val region =
                    preferences(ModulePrefs).getString("custom_system_message_region_defaults", "")
                if (region.isNotBlank()) put("ro.vendor.oplus.regionmark", region)
            }

            //Source COSA
//            if (packageName == "com.oplus.cosa") {
//                put("ro.product.brand", "realme")
//            }
        }
        loadHooker(HookSystemProperties(list))
    }
}