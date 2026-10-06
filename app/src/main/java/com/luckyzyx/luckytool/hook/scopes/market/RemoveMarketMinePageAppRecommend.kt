package com.luckyzyx.luckytool.hook.scopes.market

import android.content.Context
import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveMarketMinePageAppRecommend(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val viewLayerWrapDto = "com.heytap.cdo.card.domain.dto.ViewLayerWrapDto"
        val mineActionBarView = "com.heytap.market.mine.view.MineActionBarView"

        //Source MineFragment
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<Map<*,*>>())
                    addForType(classOf<String>())
                    addForType(classOf<Boolean>())
                    addForType(classOf<Bundle>())
                    addForType(classOf<Context>())
                    addForType(mineActionBarView)
                }
                methods {
                    add { name("onCreate") }
                    add { name("onCreateView") }
                    add { name("onDestroy") }
                    add { name("onDestroyView") }
                    add { name("onConfigurationChanged") }
//                    add { returnType(mineActionBarView) }
//                    add { returnType(cdoNestedScrollListView) }
                    add {
                        paramTypes(viewLayerWrapDto)
                        returnType(classOf<Map<*,*>>())
                    }
                    add {
                        paramTypes(viewLayerWrapDto, classOf<Boolean>().name)
                        returnType(Void.TYPE)
                    }
                }
                usingStrings("MineFragment")
            }
        }.apply {
            checkDataList("MineFragment")
            single().name.toClass().resolve().apply {
                firstMethod {
                    parameters(viewLayerWrapDto, Boolean::class)
                    returnType(Void.TYPE)
                }.hook {
                    before {
                        val dto = firstArg().get() ?: return@before
                        val cards = dto.asResolver().firstMethod {
                            name = "getCards"
                        }.invoke<List<Any>>()?.toMutableList()?.apply {
                            removeIf { indexOf(it) != 0 }
                        } ?: return@before
                        dto.asResolver().firstMethod { name = "setCards" }.invoke(ArrayList(cards))
                    }
                }
            }
        }
    }
}