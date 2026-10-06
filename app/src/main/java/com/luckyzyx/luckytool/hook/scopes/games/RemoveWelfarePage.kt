package com.luckyzyx.luckytool.hook.scopes.games

import android.view.View
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.condition.type.VagueType
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveWelfarePage(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val mainPanelView = "business.mainpanel.MainPanelView".toClassOrNull()
        if (mainPanelView == null) {
            "business.mainpanel.main.MainPanelFragment".toClass().resolve().apply {
                firstMethod { name = "addRadioButton" }.hook {
                    before {
                        if ((firstArg().get<String>() ?: "") == "welfare") result = null
                    }
                }
                firstMethod { name = "initView" }.hook {
                    after {
                        firstField { name = "navButtonMap" }.of(instance)
                            .get<HashMap<String, Any>>()
                            ?.remove("welfare")
                    }
                }
            }
            dexKitBridge.findClass {
                matcher {
                    className("business.mainpanel.view.NavigationRadioButton")
                }
            }.findField {
                matcher {
                    type(classOf<String>())
                    addReadMethod {
                        paramTypes(null, classOf<Boolean>(), classOf<Boolean>())
                        returnType(Void.TYPE)
                        usingStrings("perf", "tool")
                    }
                    declaredClass {
                        usingStrings("NavigationRadioButton")
                    }
                }
            }.apply {
                checkDataList("RemoveWelfarePage NavigationRadioButton")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        parameters(VagueType, Boolean::class, Boolean::class)
                        returnType = Void.TYPE
                    }.hook {
                        before {
                            val type = firstField { name = single().fieldName }.of(instance).get()
                            if (type == "welfare") instance<View>().isVisible = false
                        }
                    }
                }
            }
            return
        }

        //Source MainPanelView
        mainPanelView.resolve().apply {
            firstMethod {
                parameters { it[0] == classOf<List<*>>() && it[1] == classOf<Boolean>() }
                parameterCount { it in 2..3 }
                returnType = Void.TYPE
            }.hook {
                before {
                    val list = firstArg().get<List<Any>>() ?: emptyList()
                    val first = list.getOrNull(0) ?: return@before
                    firstArg().set(ArrayList(arrayListOf(first)))
                }
            }
        }
    }
}