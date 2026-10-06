package com.luckyzyx.luckytool.hook.scopes.systemui

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ForceDisplayOfRingingStatusToggleTiles : YukiBaseHooker() {
    override fun onHook() {
        //Source QSTileHostHelper
        VariousClass(
            "com.oplusos.systemui.qs.helper.QSTileHostHelper",  //C13
            "com.oplus.systemui.qs.helper.QSTileHostHelper"  //C14 C15 C16
        ).toClass().resolve().apply {
            firstMethod { name = "getDfltUnsupportedTileString" }.hook {
                after {
                    val list = result<List<String>>() ?: return@after
                    val new = list.toMutableList().apply {
                        remove("ringermode")
                    }
                    result = ArrayList(new)
                }
            }
        }

        //Source RingerModeTile
        VariousClass(
            "com.oplusos.systemui.qs.tiles.FlavorOneRingerModeTile",  //C13
            "com.oplus.systemui.qs.tiles.FlavorOneRingerModeTile",  //C14.0 C14.0.1
            "com.oplus.systemui.qs.tiles.ThreeStageRingerModeTile" //C14.1 C15 C16
        ).toClass().resolve().apply {
            //修复实例为Null无法获取spec问题
            firstConstructor().hook {
                after {
                    firstMethod { name = "setTileSpec"; superclass() }.of(instance)
                        .invoke("ringermode")
                }
            }
            firstMethod { name = "isAvailable"; superclass() }.hook {
                intercept(true)
            }
        }

        //Source OplusQSFactoryImpl
        VariousClass(
            "com.oplusos.systemui.qs.qstileimpl.OplusQSFactoryImpl",  //C13
            "com.oplus.systemui.qs.qstileimpl.OplusQSFactoryImpl",  //C14
            "com.oplus.systemui.qs.tileimpl.OplusQSFactoryImpl"  //C15 C16
        ).toClass().resolve().apply {
            firstMethod {
                name = "createTile"
                parameters(String::class)
            }.hook {
                before {
                    val key = firstArg().get<String>() ?: ""
                    if (key == "ringermode") {
                        val provider = firstField {
//                            name = "mFlavorOneRingerModeTileProvider"
//                            name = "mThreeStageRingerModeTileProvider"
                            name { it.contains("RingerModeTileProvider") }
                        }.of(instance).get() ?: return@before
                        result = provider.asResolver().firstMethod {
                            name = "get"; returnType = Any::class
                        }.invoke()
                    }
                }
            }
        }
    }
}