package com.luckyzyx.luckytool.hook.scopes.android

import android.os.Bundle
import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class ZoomWindowConfig : YukiBaseHooker() {

    var callback: ((key: String, value: Any) -> Unit)? = null

    var mode = "0"
    val list = ArraySet<String>()

    var multiWindow = false
    var multiNum = 2

    fun loadData() {
        mode = preferences(ModulePrefs).getString("custom_app_floating_window_display_mode", "0")
        list.addAll(preferences(ModulePrefs).getStringSet("zoom_window_support_list", ArraySet()))

        dataChannel.wait<String>("custom_app_floating_window_display_mode") {
            mode = it
            YLog.debug("update zoom window configs status -> $it")
        }

        dataChannel.wait("zoom_window_support_list") {
            val new = preferences(ModulePrefs).getStringSet("zoom_window_support_list", ArraySet())
            YLog.debug("update zoom window whitelist configs -> ${list.size} | ${new.size}")
            list.clear()
            list.addAll(new)
        }

        multiWindow = preferences(ModulePrefs).getBoolean("force_enable_multi_window_mode", false)
        dataChannel.wait<Boolean>("force_enable_multi_window_mode") { multiWindow = it }
        multiNum = preferences(ModulePrefs).getInt("custom_multi_window_display_upper_limit", 2)
        dataChannel.wait<Int>("custom_multi_window_display_upper_limit") { multiNum = it }

        YLog.debug("init zoom window configs success -> ${list.size}")
    }

    override fun onHook() {
        loadData()

        val osCode = getOSVersionCode
        loadHooker(HookZoomWindow())
        if (osCode >= 33) loadHooker(HookFlexibleWindow())
    }

    @Obfuscate
    inner class HookZoomWindow : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusZoomWindowConfig
            "com.android.server.wm.OplusZoomWindowConfig".toClass().resolve().apply {
                firstMethod {
                    name = "isSupportZoomMode"
                    parameters(String::class, Int::class, String::class, Bundle::class)
                }.hook {
                    before {
                        when (mode) {
                            "1" -> result = false
                            "2" -> result = true
                            "3" -> {
                                val target = firstArg().get<String>() ?: ""
                                val packName = if (target.contains("/").not()) target
                                else target.split("/")[0]
                                if (list.contains(packName)) result = true
                            }
                        }
                    }
                }
            }
        }
    }

    @Obfuscate
    inner class HookFlexibleWindow : YukiBaseHooker() {
        override fun onHook() {
            //Source FlexibleWindowUtils
            "com.android.server.wm.FlexibleWindowUtils".toClassOrNull()?.resolve()?.apply {
                firstMethod {
                    name = "isSupportFlexibleWindow"
                    parameters(String::class, String::class)
                }.hook {
                    before {
                        when (mode) {
                            "1" -> result = false
                            "2" -> result = true
                            "3" -> {
                                val target = firstArg().get<String>() ?: ""
                                val packName = if (target.contains("/").not()) target
                                else target.split("/")[0]
                                if (list.contains(packName)) result = true
                            }
                        }
                    }
                }
            }

            //Source FlexibleWindowManagerService
            "com.android.server.wm.FlexibleWindowManagerService".toClassOrNull()?.resolve()?.apply {
                firstMethod {
                    name = "getMaxWinNum"
                    returnType = Int::class
                }.hook {
                    after {
//                        val scenario = firstArg().get<Int>() ?: 0
//                        val num = result<Int>() ?: -1
//                        YLog.debug("${method.name} -> $scenario -> $num")
                        if (multiWindow && multiNum > 0) result = multiNum
                    }
                }
            }
        }
    }
}