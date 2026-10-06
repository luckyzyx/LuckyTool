package com.luckyzyx.luckytool.hook.scopes.browser

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveBrowserSearchBarAppPromotion(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val appHostCls = "com.heytap.browser.platform.app.AppHost"
        if (appHostCls.toClassOrNull() == null) return

        //Source MultiSugItemData
        val app = dexKitBridge.findClass {
            matcher {
            addFieldForType(classOf<String>())
                addMethod { paramCount(0);returnType(classOf<String>()) }
                usingStrings("res", "initialState", "sugNaturalApp")
            }
        }.checkDataList("RemoveBrowserSearchBarAppPromotion App")

        //Source MultiSugItemData
        val ads = dexKitBridge.findClass {
            matcher {
                addFieldForType(classOf<Int>())
                addFieldForType(classOf<String>())
                addMethod { name("getTitle") }
                addMethod { name("getCategoryType") }
                addMethod { paramCount(0);returnType(classOf<Int>()) }
                addMethod { paramCount(0);returnType(classOf<String>()) }
                usingStrings("res", "ad", "sugAd")
            }
        }.checkDataList("RemoveBrowserSearchBarAppPromotion Ads")

        //Source SugMultiAdapter
        dexKitBridge.findClass {
            matcher {
                fields {
                    addForType(classOf<List<*>>())
                    addForType(classOf<ArrayList<*>>())
                    addForType(classOf<Map<*,*>>())
                    addForType(classOf<Int>())
                    addForType(appHostCls)
                }
                methods {
                add { paramCount(0);returnType(classOf<List<*>>()) }
                    add {
                        paramTypes(
                            classOf<Int>(),
                            classOf<Int>(),
                            classOf<Int>(),
                            classOf<Int>()
                        )
                    }
                    add { paramTypes(classOf<List<*>>());returnType(Void.TYPE) }
                    add { paramTypes(appHostCls);returnType(Void.TYPE) }
                    add { name("getItemCount") }
                    add { name("getItemViewType") }
                    add { name("onAttachedToRecyclerView") }
                    add { name("onBindViewHolder") }
                    add { name("onCreateViewHolder") }
                    add { name("onViewAttachedToWindow") }
                }
            }
        }.apply {
            checkDataList("RemoveBrowserSearchBarAppPromotion Adapter")
            findMethod {
                matcher {
                    paramTypes(classOf<List<*>>())
                    returnType(Void.TYPE)
                    usingStrings("linkEdit")
                    addCaller {
                        paramTypes(classOf<List<*>>())
                        returnType(Void.TYPE)
                        usingStrings("headerData", "linkEdit")
                    }
                }
            }.apply {
                checkDataList("RemoveBrowserSearchBarAppPromotion Method")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        parameters(List::class)
                    }.hook {
                        before {
                            val list = firstArg().get<ArrayList<Any>>() ?: return@before
                            list.removeIf {
//                                YLog.debug("${list.indexOf(it)} -> $it")
                                it.javaClass.name == app.singleOrNull()?.name || it.javaClass.name == ads.singleOrNull()?.name
                            }
                        }
                    }
                }
            }
        }
    }
}