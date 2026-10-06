package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveToolRecommendationCard : YukiBaseHooker() {
    override fun onHook() {
        //Source ToolsRecommendCardLayout
        "business.module.toolsrecommend.ToolsRecommendCardLayout".toClassOrNull()?.resolve()
            ?.apply {
                firstMethod {
                    parameters(List::class)
                    returnType = Void.TYPE
                }.hook {
                    before {
                        firstArg().set(ArrayList<Any>())
                    }
                }
            }

        //Source GameToolTileAdapter V9.0.0+
//        "business.toolpanel.adapter.GameToolTileAdapter".toClassOrNull()?.apply {
//            method { name = "onCreateViewHolder" }.hook {
//                after {
//                    val parent = firstArg().get<ViewGroup>() ?: return@after
//                    val id = lastArg().get<Int>() ?: 0
//                    if (id == 10005) result<ViewHolder>()?.itemView?.isVisible = false
//                }
//            }
//        }
    }
}