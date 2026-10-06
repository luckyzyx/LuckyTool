package com.luckyzyx.luckytool.hook.scopes.camera

import android.util.ArrayMap
import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookCameraConfig : YukiBaseHooker() {
    override fun onHook() {
        val list = ArrayMap<String, Any>().apply {
            //10亿色影像
            if (preferences(ModulePrefs).getBoolean("enable_10_bit_image_support", false)) {
                put("com.oplus.10bits.heic.encode.support", true)
                put("com.oplus.feature.video.10bit.support", true)
            }
            //画框水印
            if (preferences(ModulePrefs).getBoolean("enable_frame_watermark_style", false)) {
                put("com.oplus.camera.support.frame.watermark", true)
            }

            //AI大师水印
            if (preferences(ModulePrefs).getBoolean("enable_ai_master_watermark", false)) {
                put("com.oplus.camera.support.ai.master.watermark", true)
            }

            //画框颜色 色卡贴纸
//            put("com.oplus.camera.support.custom.color.watermark", true)
//            put("com.oplus.camera.support.color.extraction", true)

            //哈苏水印
            if (preferences(ModulePrefs).getBoolean("enable_hasselblad_watermark_style", false)) {
                //禁用画框水印
                put("com.oplus.camera.support.frame.watermark", false)

                //Source SloganUtil -> Shot on OnePlus / Hasselblad
                put("com.oplus.hasselblad.watermark.support.default", true)
                //通用哈苏水印
                put("com.oplus.camera.support.custom.hasselblad.watermark", true)
                //哈苏水印指南
                put("com.oplus.hasselblad.watermark.guide.support", true)
                //<string name="camera_hasselblad_watermark_setting_title_str">哈苏定制水印</string>
                //OptionKey PRE_KEY_WATERMARK_HASSELBLAD / pref_hasselblad_watermark_function_key
                put("com.oplus.use.hasselblad.style.support", true)

//                put("com.oplus.professional.use.hasselblad.style.support", true)
            }

            //Filter FilterGroupManager 通用滤镜
            val universalFilters =
                preferences(ModulePrefs).getStringSet("camera_universal_filter_settings", ArraySet())
            //大师滤镜
            if (universalFilters.contains("master_filter")) {
                put(
                    "com.oplus.photo.master.filter.type.list",
                    "Radiance.cube.rgb.bin,Serenity.cube.rgb.bin,Emerald.cube.rgb.bin"
                )
                put(
                    "com.oplus.portrait.master.filter.type.list",
                    "Radiance.cube.rgb.bin,Serenity.cube.rgb.bin,Emerald.cube.rgb.bin"
                )
            }
            //姜文滤镜
            if (universalFilters.contains("jiangwen_filter")) {
                put("com.oplus.director.filter.support", true)
                put("com.oplus.director.filter.rus", true)
                put("com.oplus.director.filter.upgrade.support", true)
            }
            //盛大旅行滤镜
            if (universalFilters.contains("grand_tour_filter")) {
                put("com.oplus.support.grand.tour.filter", true)
                //StreetModeGrandTour
//                put("com.oplus.street.grand.tour.filter.type.support", true)
            }

            //质感 OS15
            if (universalFilters.contains("os15_zhi_gan_filter")) {
                put("com.oplus.feature.os15.new.filter.support", true)
            }

            //贾樟柯滤镜
            if (universalFilters.contains("jzk_filter")) {
                put("com.oplus.support.jzk.movie.filter", true)
            }

            //电影滤镜 倒带人生,少年奇旅,壮志凌云
            if (universalFilters.contains("vignette_grain_filter")) {
                put("com.oplus.vignette.grain.filter.type.support", true)
            }

            //大漠传奇
            if (universalFilters.contains("desert_filter")) {
                put("com.oplus.desert.filter.type.support", true)
            }

            //光影有声
            if (universalFilters.contains("tol_filter")) {
                put("com.oplus.tol.style.filter.support", true)
            }

            //流光人像
//            put("com.oplus.feature.portrait.streamer.support", true)

            //Filter Portrait
            val portraitFilters =
                preferences(ModulePrefs).getStringSet("camera_portrait_filter_settings", ArraySet())

            //人像留色
            if (portraitFilters.contains("retention")) {
                put("com.oplus.feature.portrait.retention.support", true)
                put("com.oplus.feature.portrait.front.retention.support", true)
                put("com.oplus.feature.portrait.back.retention.support", true)
            }
            //光斑人像
            if (portraitFilters.contains("bokeh_flare_portrait")) {
                put("com.oplus.feature.portrait.neon.support", true)
                put("com.oplus.feature.portrait.neon.front.support", true)
            }

            //Filter Video
            val videoFilters =
                preferences(ModulePrefs).getStringSet("camera_video_filter_settings", ArraySet())

            //赤红/森绿/天蓝
            if (videoFilters.contains("color_extraction")) {
                put("com.oplus.video.color_extraction.support", true)
            }
            //人像留色
            if (videoFilters.contains("retention")) {
                put("com.oplus.video.retention.support", true)
            }
            //光斑人像
            if (videoFilters.contains("bokeh_flare_portrait")) {
                put("com.oplus.video.neon.support", true)
                put("com.oplus.video.only.blur.support", true)
            }

            //夜景30倍变焦
            if (preferences(ModulePrefs).getBoolean("enable_camera_night_zoom_30x", false)) {
                put("com.oplus.night.mode.max.zoom.support", true)
                put("com.oplus.night.zoom.max.value.default", 30)
            }
            //视频录制轮盘变焦
            if (preferences(ModulePrefs).getBoolean("enable_video_capture_roulette_zoom", false)) {
                put("com.oplus.video.inertial.zoom.support", false)
            }
            //移除闪光灯使用限制
            if (preferences(ModulePrefs).getBoolean("remove_camera_flash_limit", false)) {
                put("com.oplus.feature.temperature.protection.support", false)
            }
        }
        loadHooker(HookCameraVendorTag(list))
    }

    @Obfuscate
    private class HookCameraVendorTag(val tags: Map<String, Any>) : YukiBaseHooker() {
        override fun onHook() {
            //Source CameraAdapterUtils
            "com.oplus.ocs.camera.appinterface.adapter.CameraAdapterUtils".toClassOrNull()
                ?.resolve()?.apply {
                    firstMethod { name = "getVendorTagConfig";parameterCount = 1 }.hook {
                        hookVendorTag(tags)
                    }
                }
            //Source ApsUtils
            "com.oplus.ocs.camera.consumer.apsAdapter.adapter.ApsUtils".toClassOrNull()?.resolve()
                ?.apply {
                    firstMethod { name = "getVendorTagConfig";parameterCount = 1 }.hook {
                        hookVendorTag(tags)
                    }
                }
        }

        companion object {
            private fun YukiHookCreator.ClassicMemberHooker.hookVendorTag(tags: Map<String, Any>) {
                after {
                    val key = firstArg().get<String>() ?: ""
                    if (key.isBlank()) return@after
                    val value = tags[key] ?: return@after
//                    YLog.debug("$key -> $value")
                    result = when (value) {
                        is Boolean -> if (value) "1" else "0"
                        is Int -> value.toString()
                        else -> value
                    }

                    when (key) {
                        //孤独星球
                        "com.oplus.camera.support.custom.lonely.planet.watermark" -> {}
                        //美妆定制水印
                        //res/layout/camera_watermark_makeup_visual_layout.xml
                        //imageView_watermark_makeup_visual
                        //key PRE_KEY_WATERMARK_MAKEUP / pref_watermark_makeup_function_key
                        //is_slogan
                        "com.oplus.feature.custom.makeup.watermark.support" -> {}

                        //大师模式
                        "com.oplus.feature.master.mode.support" -> {}
                        "com.oplus.feature.master.ui.mode.support" -> {}
                        "com.oplus.feature.master.mode.professional.name" -> {}

                        //街拍模式
                        "com.oplus.feature.street.mode.support" -> {}

                        //陆川滤镜 / 光影有声
//                            "com.oplus.tol.style.filter.support" -> result(true)

                        //录像轮盘变焦
//                        "com.oplus.video.inertial.zoom.support" -> result = "1"

                    }
                }
            }
        }
    }
}