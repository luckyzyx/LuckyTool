package com.luckyzyx.luckytool.hook.scopes.screenshot

import android.graphics.Bitmap
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class EnablePNGSaveFormat(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        //Source ImageFileFormat -> JPEG / PNG
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<String>())
                    addForType(classOf<Bitmap.CompressFormat>())
                }
                methods {
                    add { name("values") }
                    add { returnType(classOf<String>()) }
                    add { returnType(classOf<Bitmap.CompressFormat>()) }
                }
                usingStrings("image/jpeg", "image/png")
            }
        }.apply {
            checkDataList("EnablePNGSaveFormat")
            single().name.toClass().resolve().apply {
                method { returnType = String::class }.hookAll {
                    after {
                        result = when (result<String>()) {
                            "image/jpeg" -> "image/png"
                            ".jpg" -> ".png"
                            else -> return@after
                        }
                    }
                }
                firstMethod { returnType = classOf<Bitmap.CompressFormat>() }.hook {
                    after {
                        result = when (result<Bitmap.CompressFormat>()) {
                            Bitmap.CompressFormat.JPEG -> Bitmap.CompressFormat.PNG
                            else -> return@after
                        }
                    }
                }
            }
        }
    }
}