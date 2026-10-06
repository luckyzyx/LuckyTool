package com.luckyzyx.luckytool.hook.scopes.gesture

import android.content.Context
import android.util.ArrayMap
import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class CustomAonGestureScrollPageWhitelist(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val pageSet = preferences(ModulePrefs).getStringSet(
            "custom_aon_gesture_scroll_page_whitelist_list", ArraySet()
        )
//        val videoSet = preferences(ModulePrefs).getStringSet(
//            "custom_aon_gesture_video_whitelist_list", ArraySet()
//        )
        if (pageSet.isEmpty()) return

        //Source ConfigDataUtils
        //Search com.ss.android.ugc.aweme / com.smile.gifmaker
        dexKitBridge.findClass {
            matcher {
                fields {
                addForType(classOf<Context>())
                    addForType(classOf<ArrayList<*>>())
                    addForType(classOf<ArrayMap<*,*>>())
                    addForType(classOf<Int>())
                    addForType(classOf<Float>())
                    addForType(classOf<List<*>>())
                }
                methods {
                add { paramTypes(classOf<String>());returnType(classOf<Int>()) }
                    add { paramTypes(classOf<List<*>>());returnType(Void.TYPE) }
                }
                usingStrings("com.ss.android.ugc.aweme", "com.smile.gifmaker")
            }
        }.apply {
            checkDataList("CustomAonGestureScrollPageWhitelist")
            single().name.toClass().resolve().apply {
                method {
                    emptyParameters()
                    returnType = List::class
                }.hookAll {
                    after {
                        val res = result<List<String>>() ?: return@after
                        if (res.isEmpty()) return@after
                        result = res.toMutableList().apply {
                            if (contains("com.ss.android.ugc.aweme") || contains("com.smile.gifmaker")) {
                                addAll(pageSet)
                            }
                        }
                    }
                }
            }
        }

        //Source GestureUtil
        "com.oplus.gesture.util.GestureUtil".toClass().resolve().apply {
            firstMethod { name = "getLocalAonAppListTurnPage" }.hook {
                after {
                    val list = result<List<String>>() ?: return@after
                    result = list.toMutableList().apply {
                        addAll(pageSet)
                    }
                }
            }
//            injectMember {
//                method { name = "getLocalAonAppListPauseOrPlay" }
//                after {
//                    if (videoList.isBlank() || videoList == "None") return@after
//                    val list = result<List<String>>() ?: return@after
//                    result = list.toMutableList().apply {
//                        if (videoList.contains("\n")) {
//                            videoList.replace(" ", "").split("\n")
//                                .forEach { if (it.isNotBlank()) add(it) }
//                        } else add(videoList)
//                    }
//                }
//            }
        }
    }
}