package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.InputStream

@Obfuscate
class FixAppSpecificMediaVolumePage(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        //Source EffectiveCompositionFactory
        dexKitBridge.findClass {
            matcher {
                className("com.oplus.anim", StringMatchType.StartsWith)
            }
        }.findMethod {
            matcher {
                paramTypes(classOf<Context>(), classOf<String>(), classOf<String>())
                usingStrings(".zip", ".lottie")
            }
        }.apply {
            checkDataList("FixAppSpecificMediaVolumePage")
            single().className.toClass().resolve().apply {
                firstMethod {
                    //fromAssetSync
                    name = single().methodName
                    parameters(Context::class, String::class, String::class)
                }.hook {
                    before {
                        val context = firstArg().get<Context>() ?: return@before
                        val path = arg(1).get<String>() ?: ""
                        val key = lastArg().get<String>() ?: ""
                        if (path.contains("multi_app_volume").not()) return@before

                        val assetsInputStream = safeOfNull { context.assets.open(path) }
                        if (assetsInputStream != null) return@before

                        context.injectModuleResources()
                        if (!path.endsWith(".zip") && !path.endsWith(".lottie")) {
                            val resName = path.substringAfter("/").substringBefore(".json")
                            val resId = context.resources.getIdentifier(
                                resName, "raw", BuildConfig.APPLICATION_ID
                            )
                            if (resId == 0) return@before
                            val rawInputStream = safeOfNull {
                                context.resources.openRawResource(resId)
                            } ?: return@before
                            result = firstMethod {
//                                name = "fromJsonInputStreamSync"
                                parameters(InputStream::class, String::class)
                                returnType = method.returnType
                            }.invoke(rawInputStream, key) ?: return@before
                        }
                    }
                }
            }
        }
    }
}