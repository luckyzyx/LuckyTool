package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.util.concurrent.atomic.AtomicBoolean

@Obfuscate
class RemoveMarketSplashPageAppRecommend(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val isV4 = "com.heytap.cdo.splash.domain.dto.v4.SplashDtoV4".toClassOrNull() != null
        if (isV4) loadHooker(MarketSplashPageV4(dexKitBridge))
        else loadHooker(MarketSplashPageV2(dexKitBridge))
    }

    @Obfuscate
    class MarketSplashPageV4(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val splashDto = "com.heytap.cdo.splash.domain.dto.v4.SplashDtoV4"
            val mediaDto = "com.heytap.cdo.splash.domain.dto.v4.MediaComponentDtoV4"
            val imageDto = "com.heytap.cdo.splash.domain.dto.v4.ImageComponentDtoV4"

            //Source SplashTransaction
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Int>())
                        addForType(classOf<Long>())
                        addForType(classOf<Boolean>())
                        addForType(classOf<AtomicBoolean>())
                    }
                    methods {
                        add { paramTypes(classOf<String>()); returnType(classOf<Boolean>()) }
                        add { paramTypes(classOf<Boolean>()); returnType(splashDto) }
                        add {
                            paramTypes(classOf<Boolean>().name, classOf<Int>().name, splashDto)
                            returnType(Void.TYPE)
                        }
                        add { paramTypes(splashDto, classOf<Boolean>().name, mediaDto) }
                        add { paramTypes(splashDto, classOf<Boolean>().name, imageDto) }
                    }
                    usingStrings("getSplashData", "isRequestByNet")
                }
            }.apply {
                checkDataList("RemoveMarketSplashPageAppRecommend")
                single().name.toClass().resolve().apply {
                    firstMethod {
                        parameters(classOf<Boolean>())
                        returnType(splashDto)
                    }.hook {
                        intercept()
                    }
                }
            }
        }
    }

    @Obfuscate
    class MarketSplashPageV2(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val splashDto = "com.heytap.cdo.splash.domain.dto.v2.SplashDto"
            val mediaDto = "com.heytap.cdo.splash.domain.dto.v2.MediaComponentDto"
            val imageDto = "com.heytap.cdo.splash.domain.dto.v2.ImageComponentDto"

            //Source SplashTransaction
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Int>())
                        addForType(classOf<Long>())
                        addForType(classOf<Boolean>())
                        addForType(classOf<AtomicBoolean>())
                    }
                    methods {
                        add { paramTypes(classOf<String>()); returnType(classOf<Boolean>()) }
                        add { paramTypes(classOf<Boolean>()); returnType(splashDto) }
                        add {
                            paramTypes(classOf<Boolean>().name, classOf<Int>().name, splashDto)
                            returnType(Void.TYPE)
                        }
                        add { paramTypes(splashDto, classOf<Boolean>().name, mediaDto) }
                        add { paramTypes(splashDto, classOf<Boolean>().name, imageDto) }
                    }
                    usingStrings("getSplashData")
                }
            }.apply {
                checkDataList("RemoveMarketSplashPageAppRecommend")
                single().name.toClass().resolve().apply {
                    firstMethod {
                        parameters(classOf<Boolean>())
                        returnType(splashDto)
                    }.hook {
                        intercept()
                    }
                }
            }
        }
    }
}