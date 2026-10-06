package com.luckyzyx.luckytool.hook.scopes.pictorial

import android.graphics.Bitmap
import android.os.Handler
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.io.File

@Obfuscate
class RemoveImageSaveWaterMark(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Search ImageSaveManager
        //Search getWaterMaskBitmap -> standard_water_mask_template / high_quality_water_mask_template
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<File>())
                    addForType(classOf<Handler>())
                    addForType(classOf<Long>())
                    addForType(classOf<Boolean>())
                    addForType(classOf<String>())
                }
                methods {
                    add { returnType(classOf<Bitmap>()) }
                    add { paramCount(5); returnType(classOf<Bitmap>()) }
                    add { paramTypes("com.heytap.pictorial.core.bean.BasePictorialData") }
                }
                usingStrings("ImageSaveManager")
            }
        }.apply {
            checkDataList("ImageSaveManager")
            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(Boolean::class, VagueType, Bitmap::class, Boolean::class)
                    returnType = Bitmap::class
                }.hook {
                    after {
                        result = arg(2).get<Bitmap>() ?: return@after
                    }
                }
            }
        }
    }
}