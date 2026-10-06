package com.luckyzyx.luckytool.hook.scopes.gallery

import android.annotation.SuppressLint
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.safeOf
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookSystemStorage(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        val osCode = getOSVersionCode

        //替换OnePlus机型水印
        val notOplus = preferences(ModulePrefs).getBoolean("replace_oneplus_model_watermark", false)
        //高级筛选
        val seniorPicked =
            preferences(ModulePrefs).getBoolean("enable_photo_listview_senior_picked", false)
        //拇指线
        val thumbLine = preferences(ModulePrefs).getString("set_photo_view_thumb_line_display_mode", "0")
        //GIF合成
        val gifSynthesis = preferences(ModulePrefs).getBoolean("enable_photo_editor_gif_synthesis", false)
        //闪速抠图
        val lnsImage = preferences(ModulePrefs).getBoolean("enable_lns_cut_photo", false)
        //画框水印
        val frameWaterMark = preferences(ModulePrefs).getBoolean("enable_frame_watermark", false)
        //哈苏水印
        val hasselWaterMark = preferences(ModulePrefs).getBoolean("enable_hassel_watermark", false)
        //AI大师水印
        val aiWaterMark = preferences(ModulePrefs).getBoolean("enable_ai_master_watermark", false)
        //隐私水印
        val privicyWaterMark = preferences(ModulePrefs).getBoolean("enable_privacy_watermark", false)
        //新春水印
        val springFestival =
            preferences(ModulePrefs).getBoolean("enable_spring_festival_watermark", false)
        //国庆水印
        val nationalDay =
            preferences(ModulePrefs).getBoolean("enable_national_day_watermark", false)

        //Source OtherSystemStorage
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<Context>())
                }
                methods {
                    add { paramCount(2);returnType(classOf<Int>(primitiveType = false)) }
                    add { paramCount(2);returnType(classOf<Long>(primitiveType = false)) }
                    add { paramCount(2); returnType(classOf<Boolean>(primitiveType = false)) }
                    add { paramCount(2);returnType(classOf<String>()) }
                    add { paramCount(2);returnType(Void.TYPE) }
                    add { paramCount(0);returnType(classOf<Boolean>()) }
//                    add { paramCount(4);returnType(Boolean::class.java) }
                }
                usingStrings("configNode")
            }
        }.apply {
            checkDataList("HookSystemStorage")
            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(VagueType, Boolean::class)
                    returnType = classOf<Boolean>(primitiveType = false)
                }.hook {
                    after {
                        val context = firstField { type = Context::class }.of(instance)
                            .get<Context>() ?: return@after
                        val configNode = firstArg().get().toString()
                        when {
                            //com.oplus.camera.support.custom.hasselblad.watermark
                            configNode.contains("feature_is_support_watermark") -> {
                                if (osCode < 30 && hasselWaterMark) result = true
                                if (osCode >= 34 && aiWaterMark) result = true
                            }
                            //com.oplus.camera.support.custom.hasselblad.watermark
                            configNode.contains("feature_is_support_hassel_watermark") -> if (hasselWaterMark) result = true
                            //com.oplus.feature.custom.makeup.watermark.support
//                            configNode.contains("feature_is_support_street_watermark") -> if (waterMark) result = true
                            //debug.enable.ipu
//                            configNode.contains("feature_is_support_ipu_watermark") -> if (waterMark) result = true
                            //com.oplus.camera.support.custom.color.watermark
//                            configNode.contains("feature_is_support_color_watermark") -> if (waterMark) result = true
                            //com.oplus.camera.support.custom.lonely.planet.watermark
//                            configNode.contains("feature_is_support_lonely_planet_watermark") -> if (waterMark) result = true

                            //isRegionIndia / isInTime / is_realme_brand
//                            configNode.contains("feature_is_support_diwali_festival_watermark") -> if (waterMark) result = true

                            //is_realme_brand / debug.gallery.photo.editor.watermark.switcher
//                            configNode.contains("feature_is_support_photo_editor_watermark") -> if (hasselWaterMark) result = true
                            //first_api_level
//                            configNode.contains("feature_is_support_photo_editor_frame_watermark") -> result =
//                                frameWaterMark

                            //is_realme_brand / debug.gallery.photo.editor.watermark.switcher
                            configNode.contains("feature_is_support_privacy_watermark") -> if (privicyWaterMark) result = true
                            //os.graphic.gallery.photolistview.senior_picked
                            configNode.contains("feature_is_support_senior_picked") -> if (seniorPicked) result = true
                            //debug.gallery.photo.photothumbline / os.graphic.gallery.photoview.thumb_line
                            configNode.contains("feature_is_support_photo_thumb_line") -> when (thumbLine) {
                                "1" -> result = true
                                "2" -> result = false
                            }
                            //debug.gallery.gif.support.synthesis / os.graphic.gallery.photoeditor.gif_synthesis
                            configNode.contains("feature_is_support_gif_synthesis") -> if (gifSynthesis) result = true
                            //debug.gallery.lns / os.graphic.gallery.photoview.lns
                            configNode.contains("feature_is_support_lns") -> if (lnsImage) result = true

                            //isOsSupport 29 -> 27 (Downgrade)
                            //debug.gallery.photo.editor.watermark.switcher / is_region_cn (property_domestic)
                            configNode.contains("feature_is_support_spring_festival_watermark") -> {
                                if (springFestival && osCode in 27..33 && isRegionCN(context)) result = true
                            }
                            //debug.gallery.photo.editor.watermark.switcher / is_region_cn (property_domestic)
                            configNode.contains("feature_is_support_national_day_watermark") -> {
                                if (nationalDay && osCode in 27..33 && isRegionCN(context)) result = true
                            }
                        }
                    }
                }
            }
        }

        if (hasselWaterMark) {
            //Source WatermarkInfo / WatermarkDevice
            dexKitBridge.findClass {
                matcher {
                addFieldForType(classOf<Int>())
                    addFieldForType(classOf<Boolean>())
                    addFieldForType(classOf<String>())
                    usingStrings("WatermarkDevice", "isHasselDevice")
                }
            }.apply {
                checkDataList("WatermarkDevice HasselDevice", onlyOne = false)
                forEachIndexed { _, classData ->
                    classData.name.toClass().resolve().apply {
                        firstFieldOrNull { type = Boolean::class }?.let {
                            constructor { }.hookAll {
                                after {
                                    it.copy().of(instance).set(true)
                                }
                            }
                        } ?: run {
                            YLog.debug("WatermarkDevice HasselDevice hook error! -> ${classData.name}")
                        }
                    }
                }
            }
        }

        //Source ConfigAbilityImpl
        dexKitBridge.findClass {
            matcher {
//                fields {
//                    addForType(Context::class.java)
//                }
                methods {
                    add { name = "close";paramCount(0) }
                    add { name = "contains";paramTypes(classOf<String>()) }
                    add { returnType(classOf<AutoCloseable>()) }
                    add {
//                        paramTypes(String::class.java, Int::class.java)
                        paramCount(2)
                        returnType(classOf<Int>(primitiveType = false))
                    }
                    add {
//                        paramTypes(String::class.java, Long::class.java)
                        paramCount(2)
                        returnType(classOf<Long>(primitiveType = false))
                    }
                    add {
                    paramTypes(classOf<String>(), classOf<String>())
                        returnType(classOf<String>())
                    }
//                    add {
//                        paramTypes(String::class.java, Boolean::class.java)
//                        paramCount(2)
//                        returnType(Boolean::class.javaObjectType)
//                    }
                }
                usingStrings("ConfigAbilityImpl")
            }
        }.apply {
            checkDataList("HookConfigAbility")
            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(String::class, Boolean::class)
                    returnType = classOf<Boolean>(primitiveType = false)
                }.hook {
                    after {
                        when (firstArg().get<String>() ?: "") {
                            "is_oneplus_brand" -> if (notOplus) result = false
//                            "feature_is_support_watermark" -> if (waterMark) result = true
//                            "feature_is_support_hassel_watermark" -> if (hasselWaterMark) result = true
//                            "feature_is_support_photo_editor_watermark" -> if (waterMark) result = true
//                            "feature_is_support_privacy_watermark" -> if (privicyWaterMark) result = true
//                            "feature_is_support_senior_picked" -> if (seniorPicked) result = true
//                            "feature_is_support_photo_thumb_line" -> when (thumbLine) {
//                                "1" -> result = true
//                                "2" -> result = false
//                            }
//
//                            "feature_is_support_gif_synthesis" -> if (gifSynthesis) result = true
//                            "feature_is_support_lns" -> if (lnsImage) result = true

//                            photopage_detail_ic_dolby_vision
//                            "brighten_version_dolby" -> if (gifSynthesis) result = true
//                            "feature_is_support_dolby_brighten" -> if (gifSynthesis) result = true
//                            "is_support_dolby_decode" -> if (gifSynthesis) result = true
//                            "is_support_dolby_encode" -> if (gifSynthesis) result = true
//                            "is_support_dolby_encode_accelerate" -> if (gifSynthesis) result = true
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    fun isRegionCN(context: Context): Boolean {
        return safeOf(isZh(context)) {
            context.resources.getBoolean(
                context.resources.getIdentifier(
                    "property_domestic", "bool", this@HookSystemStorage.packageName
                )
            )
        }
    }
}