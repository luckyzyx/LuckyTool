package com.luckyzyx.luckytool.hook.scopes.market

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveMarketUpdateDownloadPageAppRecommend(val dexKitBridge: DexKitBridge)  : YukiBaseHooker() {
    override fun onHook() {
        val cardDto = "com.heytap.cdo.card.domain.dto.CardDto"
        val imageLoader = "com.nearme.imageloader.ImageLoader"

        //Source CardDataProcessor
        dexKitBridge.findClass {
            matcher {
                addFieldForName("mDataUtil")
                addMethod {
                    name("processData")
//                    paramTypes(ListClass, IntType, null)
                    returnType(classOf<List<*>>())
                }
            }
        }.apply {
            checkDataList("RemoveMarketUpdateDownloadPageAppRecommend")
            single().name.toClass().resolve().apply {
                firstMethod { name = "processData" }.hook {
                    after {
                        result<ArrayList<Any>>()?.clear()
                    }
                }
            }
        }

        //Source APPUpdateItemHolder list_item_product_upgrade
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<ViewGroup>())
                    addForType(classOf<TextView>())
                    addForType(imageLoader)
                }
                methods {
                    add {
                    paramTypes(classOf<Context>(), classOf<Int>())
                        returnType(Void.TYPE)
                    }
                    add {
                        paramTypes(
                            classOf<Context>(),
                            classOf<String>(),
                            classOf<Int>(),
                            classOf<Int>()
                        )
                        returnType(Void.TYPE)
                    }
                    add {
                    paramTypes(classOf<View>(), classOf<Boolean>())
                        returnType(Void.TYPE)
                    }
                    add {
                        paramCount(0)
                        returnType(classOf<View>())
                    }
                    add {
                    paramTypes(classOf<LayoutInflater>());returnType(classOf<View>())
                    }
                }
            }
        }.apply {
            checkDataList("RemoveMarketUpdatePageAppRecommend APPUpdateItemHolder")
            single().name.toClass().resolve().apply {
                firstMethodOrNull {
                    parameters(
                        cardDto, String::class, VagueType,
                        Map::class, Boolean::class, Long::class
                    )
                    returnType(Void.TYPE)
                }?.hook {
                    intercept()
                }
            }
        }

        //Source AppUpdateFragmentV2
        "com.heytap.cdo.client.ui.upgrademgrv2.AppUpdateFragmentV2".toClassOrNull()?.let {
            dexKitBridge.findClass {
                matcher {
                    className(it.name)
                }
            }.apply {
                checkDataList("RemoveMarketUpdatePageAppRecommend AppUpdateFragmentV2")
                findMethod {
                    matcher {
                    paramTypes(classOf<List<*>>())
                        addInvoke {
                        paramTypes(classOf<Context>(), classOf<Float>())
                            returnType(classOf<Int>())
                        }
                        usingNumbers(114.0F)
                    }
                }.apply {
                    checkDataList("RemoveMarketUpdatePageAppRecommend addDataAndNotifyChanged")
                    it.resolve().firstMethod {
                        name = single().name
                        parameters(List::class)
                    }.hook {
                        before {
                            firstArg().get<java.util.ArrayList<Any>>()?.clear()
                        }
                    }
                }

                findMethod {
                    matcher {
                    paramTypes(classOf<Boolean>())
                        usingNumbers(0, 300L)
                        addUsingField { type(classOf<ValueAnimator>()) }
                        addInvoke { paramCount(0) }
                        usingStrings("mRecommendUpdateContainer", "mNormalUpdateContainer")
                    }
                }.apply {
                    checkDataList("RemoveMarketUpdatePageAppRecommend AutoScrollWhenUpdateAll")
                    it.resolve().firstMethod {
                        name = single().name
                        parameters(Boolean::class)
                    }.hook {
                        intercept()
                    }
                }
            }
        }
    }
}