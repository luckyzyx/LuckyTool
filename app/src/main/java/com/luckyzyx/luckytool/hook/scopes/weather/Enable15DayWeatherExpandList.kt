package com.luckyzyx.luckytool.hook.scopes.weather

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object Enable15DayWeatherExpandList : YukiBaseHooker() {
    override fun onHook() {
        //Source FutureDayWeatherItem
        "com.oplus.weather.main.view.itemview.FutureDayWeatherItem".toClass().resolve().apply {
            firstFieldOrNull { name = "isAllow15DayExpand";type = Boolean::class } ?: return@apply
            constructor {
                parameters { it.contains(classOf<Boolean>()) }
            }.hookAll {
                before {
                    arg(args.indexOfFirst { it is Boolean }).set(true)
                }
            }
        }

        //Source UIConfigManager
        "com.oplus.weather.uiconfig.UIConfigManager".toClass().resolve().apply {
            method {
                name { it.startsWith("get") && it.contains("Day15ExpandConfig") }
            }.hookAll {
                intercept(true)
            }
        }
    }
}